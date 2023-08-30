package com.vymalo.keycloak.nkobani.users

import com.vymalo.keycloak.nkobani.users.openapi.client.invoker.ApiClient
import org.apache.commons.lang3.StringUtils
import org.keycloak.common.util.MultivaluedHashMap
import org.keycloak.component.ComponentModel
import org.keycloak.component.ComponentValidationException
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.provider.ProviderConfigProperty
import org.keycloak.provider.ProviderConfigurationBuilder
import org.keycloak.provider.ServerInfoAwareProviderFactory
import org.keycloak.storage.UserStorageProviderFactory
import java.net.MalformedURLException
import java.net.URL

class AccountStorageFactory : UserStorageProviderFactory<AccountStorageProvider>, ServerInfoAwareProviderFactory {

    companion object {
        const val ID = "nkobani-accounts-storage-provider"

        @JvmStatic
        private val SPI_INFO: MutableMap<String, String> =
            mutableMapOf(
                "provider_id" to ID,
                ID to "0.3.0",
                "maintainer" to "me@ssegning.com",
            )
        const val SERVER_CLIENT_ID = "serverClientId"
        const val SERVER_CLIENT_SECRET = "serverClientSecret"
        const val SERVER_URL_KEY = "serverUrl"
        const val SERVER_KEYCLOAK_URL = "serverKeycloakUrl"
        const val SERVER_SCOPE = "serverScope"

        @JvmStatic
        val CONFIG_PROPERTIES: List<ProviderConfigProperty> = ProviderConfigurationBuilder
            .create()
            .property()
            .name(SERVER_URL_KEY)
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("Server Url")
            .helpText("Url where to call APIs")
            .add()
            //
            .property()
            .name(SERVER_KEYCLOAK_URL)
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("Server Keycloak Url")
            .helpText("Url of the production's auth url")
            .add()
            //
            .property()
            .name(SERVER_CLIENT_ID)
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("Client ID")
            .helpText("Client ID from Keycloak of the Client")
            .add()
            //
            .property()
            .name(SERVER_CLIENT_SECRET)
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("Client Secret")
            .helpText("Client Secret from Keycloak of the client")
            .add()
            //
            .property()
            .name(SERVER_SCOPE)
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("Scope")
            .helpText("Scope for Token request")
            .add()
            //
            .build()
    }

    private var apiService: ApiService? = null

    override fun create(keycloakSession: KeycloakSession, componentModel: ComponentModel): AccountStorageProvider {
        if (apiService == null) {
            createConfig(keycloakSession.context.realm, componentModel.config)
        }
        return AccountStorageProvider(apiService!!, keycloakSession, componentModel)
    }

    override fun getConfigProperties(): List<ProviderConfigProperty> = CONFIG_PROPERTIES.toMutableList()

    override fun getId(): String = ID

    override fun getOperationalInfo(): MutableMap<String, String> = SPI_INFO

    override fun getHelpText(): String = "User REST Account storage"

    @Throws(ComponentValidationException::class)
    override fun validateConfiguration(session: KeycloakSession, realm: RealmModel, model: ComponentModel) {
        val config = model.config

        val serverUrl = config.getFirst(SERVER_URL_KEY)
        if (StringUtils.isEmpty(serverUrl)) {
            throw ComponentValidationException("ServerUrl should not be empty")
        }
        try {
            URL(serverUrl)
        } catch (malformedURLException: MalformedURLException) {
            throw ComponentValidationException("ServerUrl is not a valid url")
        }

        val scopes = config.getFirst(SERVER_SCOPE)
        if (StringUtils.isEmpty(scopes)) {
            throw ComponentValidationException("Scopes cannot be empty")
        }

        val basicUser = config.getFirst(SERVER_CLIENT_ID)
        if (StringUtils.isEmpty(basicUser)) {
            throw ComponentValidationException("Client ID cannot be empty")
        }

        val basicPass = config.getFirst(SERVER_CLIENT_SECRET)
        if (StringUtils.isEmpty(basicPass)) {
            throw ComponentValidationException("Client Secret cannot be empty")
        }

        val keycloakUrl = config.getFirst(SERVER_KEYCLOAK_URL)
        if (StringUtils.isEmpty(keycloakUrl)) {
            throw ComponentValidationException("Keycloak's url cannot be empty")
        }
    }

    override fun onCreate(session: KeycloakSession, realm: RealmModel, model: ComponentModel) =
        createConfig(realm, model.config)

    override fun onUpdate(
        session: KeycloakSession,
        realm: RealmModel,
        oldModel: ComponentModel,
        newModel: ComponentModel
    ) = createConfig(realm, newModel.config)

    private fun createConfig(realm: RealmModel, config: MultivaluedHashMap<String, String>) {
        val apiClient = ApiClient()
        val tokenRequester = TokenRequester(
            keycloakUrl = config.getFirst(SERVER_KEYCLOAK_URL),
            clientId = config.getFirst(SERVER_CLIENT_ID),
            clientSecret = config.getFirst(SERVER_CLIENT_SECRET),
            scope = config.getFirst(SERVER_SCOPE),
            realmName = realm.name,
        )
        apiClient.basePath = config.getFirst(SERVER_URL_KEY)
        apiClient.addAuthorization("keycloak", TokenRequestInterceptor(tokenRequester))
        apiService = ApiService(apiClient)
    }
}