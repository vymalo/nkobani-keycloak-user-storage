package com.vymalo.keycloak.nkobani.users

import com.vymalo.keycloak.nkobani.users.openapi.client.model.UserEntity
import feign.FeignException.*
import org.keycloak.component.ComponentModel
import org.keycloak.credential.CredentialInput
import org.keycloak.credential.CredentialInputUpdater
import org.keycloak.credential.CredentialInputValidator
import org.keycloak.models.GroupModel
import org.keycloak.models.KeycloakSession
import org.keycloak.models.RealmModel
import org.keycloak.models.UserModel
import org.keycloak.models.credential.PasswordCredentialModel
import org.keycloak.storage.StorageId
import org.keycloak.storage.UserStorageProvider
import org.keycloak.storage.user.UserLookupProvider
import org.keycloak.storage.user.UserQueryProvider
import org.keycloak.storage.user.UserRegistrationProvider
import java.util.*
import java.util.stream.Stream

class AccountStorageProvider(
    private val apiService: ApiService,
    private val session: KeycloakSession,
    private val model: ComponentModel,
) : UserStorageProvider,
    UserLookupProvider,
    UserQueryProvider,
    CredentialInputValidator,
    CredentialInputUpdater,
    UserRegistrationProvider {

    override fun close() {}

    /// Account

    override fun getUserById(realm: RealmModel, id: String): UserModel? {
        val externalId = UUID.fromString(StorageId.externalId(id))
        val account = apiService.getAccountById(externalId)
        return toModel(session, realm, model, account)
    }

    override fun getUserByUsername(realm: RealmModel, username: String): UserModel? = getUserByEmail(realm, username)

    override fun getUserByEmail(realm: RealmModel, email: String?): UserModel? {
        if (email.isNullOrEmpty()) {
            return null
        }

        val account = apiService.getAccount(email)
        return toModel(session, realm, model, account)
    }

    /// Queries

    override fun getUsersCount(realm: RealmModel): Int = 0

    override fun searchForUserStream(realm: RealmModel, params: MutableMap<String, String>): Stream<UserModel> =
        searchForUserStream(realm, params, 0, 20)

    override fun searchForUserStream(
        realm: RealmModel,
        params: MutableMap<String, String>,
        firstResult: Int,
        maxResults: Int
    ): Stream<UserModel> = apiService
        .getMultipleAccount(params, firstResult, maxResults)
        .stream()
        .map { toModel(session, realm, model, it)!! }

    override fun getGroupMembersStream(realm: RealmModel, group: GroupModel): Stream<UserModel> = Stream.of()

    override fun getGroupMembersStream(
        realm: RealmModel,
        group: GroupModel,
        firstResult: Int,
        maxResults: Int
    ): Stream<UserModel> = Stream.of()

    override fun searchForUserByUserAttributeStream(
        realm: RealmModel,
        attrName: String,
        attrValue: String,
    ): Stream<UserModel> = Stream.of()

    ///

    private fun toModel(
        session: KeycloakSession,
        realm: RealmModel,
        model: ComponentModel,
        account: UserEntity?,
    ): UserModel? = if (account != null) UserAdapter(session, realm, model, account, apiService) else null

    override fun addUser(realm: RealmModel, username: String): UserModel? = try {
        val created = apiService.createAccount(username)
        toModel(session, realm, model, created)
    } catch (e: FeignClientException) {
        null
    }

    override fun removeUser(realm: RealmModel, user: UserModel): Boolean = try {
        val externalId = UUID.fromString(StorageId.externalId(user.id))
        apiService.deleteAccount(externalId)
        true
    } catch (e: NotFound) {
        false
    }

    /// Credentials

    override fun supportsCredentialType(credentialType: String): Boolean =
        credentialType == PasswordCredentialModel.TYPE

    override fun updateCredential(realm: RealmModel, user: UserModel, input: CredentialInput): Boolean = try {
        apiService.createPassword(
            StorageId.externalId(user.id),
            input.challengeResponse
        )
        true
    } catch (e: FeignClientException) {
        false
    }

    override fun disableCredentialType(realm: RealmModel, user: UserModel, credentialType: String): Unit =
        apiService.disablePassword(StorageId.externalId(user.id))

    override fun getDisableableCredentialTypesStream(realm: RealmModel, user: UserModel): Stream<String> =
        Stream.empty()

    override fun isConfiguredFor(realm: RealmModel, user: UserModel, credentialType: String): Boolean =
        supportsCredentialType(credentialType)

    override fun isValid(realm: RealmModel, user: UserModel, credentialInput: CredentialInput): Boolean =
        apiService.validateCred(StorageId.externalId(user.id), credentialInput.challengeResponse)

}
