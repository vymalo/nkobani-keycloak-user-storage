package com.vymalo.keycloak.nkobani.users

import com.vymalo.keycloak.nkobani.users.openapi.client.model.UserEntity
import feign.FeignException.FeignClientException
import org.apache.commons.lang3.StringUtils
import org.keycloak.common.util.MultivaluedHashMap
import org.keycloak.component.ComponentModel
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.representations.IDToken
import org.keycloak.storage.StorageId
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage

class UserAdapter(
    session: KeycloakSession,
    realm: RealmModel,
    model: ComponentModel,
    private var account: UserEntity,
    private val apiService: ApiService,
) : AbstractUserAdapterFederatedStorage(session, realm, model) {

    override fun getId(): String {
        if (storageId == null) {
            storageId = StorageId(storageProviderModel.id, account.id!!.toString())
        }
        return storageId.id
    }

    override fun getAttributes(): Map<String, List<String>> {
        val map: MultivaluedHashMap<String, String> = MultivaluedHashMap<String, String>()
        map[ENABLED] = listOf(account.enabled.toString())
        map[FIRST_NAME] = listOf(account.firstname)
        map[LAST_NAME] = listOf(account.lastname)
        map[USERNAME] = listOf(account.username)
        map[EMAIL] = listOf(account.email)
        map[EMAIL_VERIFIED] = listOf(account.emailVerified.toString())

        map[IDToken.PHONE_NUMBER] = listOf(account.phoneNumber)
        map[IDToken.PHONE_NUMBER_VERIFIED] = listOf(account.phoneNumberVerified.toString())
        map[IDToken.LOCALE] = listOf(account.locale)

        map[UserEntity.SERIALIZED_NAME_REGION] = listOf(account.region)
        return map
    }

    override fun getUsername(): String? = getFirstAttribute(USERNAME)

    override fun setUsername(username: String) = setSingleAttribute(USERNAME, username)

    override fun getFirstName(): String? = getFirstAttribute(FIRST_NAME)

    override fun setFirstName(firstName: String) = setSingleAttribute(FIRST_NAME, firstName)

    override fun getLastName(): String? = getFirstAttribute(LAST_NAME)

    override fun setLastName(lastName: String) = setSingleAttribute(LAST_NAME, lastName)

    override fun getEmail(): String? = getFirstAttribute(EMAIL)

    override fun setEmail(email: String) = setSingleAttribute(EMAIL, email.lowercase())

    override fun getFirstAttribute(name: String): String? {
        val attributes = attributes
        val values = attributes[name]
        return if (values?.isNotEmpty() == true) values[0] else null
    }

    override fun isEnabled() = getFirstAttribute(ENABLED)?.toBoolean() ?: false

    override fun setEnabled(enabled: Boolean) = setSingleAttribute(ENABLED, enabled.toString())

    override fun isEmailVerified(): Boolean = getFirstAttribute(EMAIL_VERIFIED)?.toBoolean() ?: false

    override fun setEmailVerified(verified: Boolean) = setSingleAttribute(EMAIL_VERIFIED, verified.toString())

    override fun getGroupsCount(): Long = 0

    override fun getGroupsCountByNameContaining(search: String?): Long = 0

    override fun getCreatedTimestamp(): Long = account.createdAt.time

    override fun setAttribute(name: String, values: List<String>?) {
        if (!StringUtils.isEmpty(name)
            && values?.isNotEmpty() == true && !StringUtils.isEmpty(values[0])
        ) {
            val value = values[0]
            setSingleAttribute(name, value)
        }
    }

    override fun setSingleAttribute(name: String, value: String) {
        if (!StringUtils.isEmpty(value)) {
            buildChangeData(name, value)
        }
    }

    private fun buildChangeData(name: String, value: String): Boolean {
        val request = account
        when (name) {
            ENABLED -> request.enabled = value.toBoolean()
            FIRST_NAME -> request.firstname = value
            LAST_NAME -> request.lastname = value
            USERNAME -> request.username = value
            EMAIL -> request.email = value
            EMAIL_VERIFIED -> request.emailVerified = value.toBoolean()

            IDToken.PHONE_NUMBER -> request.phoneNumber = value
            IDToken.PHONE_NUMBER_VERIFIED -> request.phoneNumberVerified = value.toBoolean()
            IDToken.LOCALE -> request.locale = value

            UserEntity.SERIALIZED_NAME_REGION -> request.region = value
            else -> return false
        }

        return try {
            account = apiService.updateAccount(request)
            true
        } catch (e: FeignClientException) {
            false
        }
    }

    override fun toString(): String = "UserAdapter(id=$id, attributes=$attributes)"

}
