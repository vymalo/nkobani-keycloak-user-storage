package com.vymalo.keycloak.nkobani.users

import com.vymalo.keycloak.nkobani.users.openapi.client.handler.UserApi
import com.vymalo.keycloak.nkobani.users.openapi.client.handler.UserApi.GetManyBaseUsersControllerUserEntityQueryParams
import com.vymalo.keycloak.nkobani.users.openapi.client.invoker.ApiClient
import com.vymalo.keycloak.nkobani.users.openapi.client.model.UserEntity
import com.vymalo.keycloak.nkobani.users.openapi.client.model.UserPassword
import feign.FeignException
import feign.okhttp.OkHttpClient
import feign.slf4j.Slf4jLogger
import org.keycloak.models.UserModel
import org.keycloak.storage.UserStorageProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class ApiService(val client: ApiClient) {

    private val accountApi: UserApi = client
        .feignBuilder
        .logger(Slf4jLogger(ApiService::class.java))
        .logLevel(feign.Logger.Level.FULL)
        .client(OkHttpClient())
        .target(UserApi::class.java, client.basePath)

    companion object {
        const val SUPPORTED_CRED_TYPE = "password"

        @JvmStatic
        private val log: Logger = LoggerFactory.getLogger(UserStorageProvider::class.java)
    }

    fun getAccountById(externalId: UUID?): UserEntity? = try {
        log.debug("Get user by id {}", externalId)
        if (externalId == null) null
        else accountApi.getOneBaseUsersControllerUserEntity(
            externalId.toString(),
            UserApi.GetOneBaseUsersControllerUserEntityQueryParams()
        )
    } catch (e: FeignException) {
        log.error("Could not get a user: {}", if (e.status() == 404) "Not found" else e.message)
        if (e.status() == 404) null else throw e
    }

    fun createAccount(email: String): UserEntity? = try {
        log.debug("Create user by email $email")
        val body = UserEntity()
        body.email = email
        body.emailVerified = false
        accountApi.createOneBaseUsersControllerUserEntity(body)
    } catch (e: FeignException) {
        log.error("Could not create a user: {}", if (e.status() == 404) "Not found" else e.message)
        if (e.status() == 404) null else throw e
    }

    fun getAccount(email: String): UserEntity? = try {
        log.debug("Get user by email $email")
        val params = GetManyBaseUsersControllerUserEntityQueryParams().filter(listOf("email||\$eq||$email"))
        val result = accountApi.getManyBaseUsersControllerUserEntity(params)
        result.data.firstOrNull { it.email == email }
    } catch (e: FeignException) {
        log.error("Could not get a user: {}", if (e.status() == 404) "Not found" else e.message)
        if (e.status() == 404) null else throw e
    }

    fun getMultipleAccount(
        params: MutableMap<String, String>,
        firstResult: Int,
        maxResults: Int
    ): Collection<UserEntity> {
        log.debug("Get users ({}, {}) by params {}", firstResult, maxResults, params)
        val query = params[UserModel.SEARCH]
        val queryParams = GetManyBaseUsersControllerUserEntityQueryParams()
            .offset(firstResult)
            .limit(maxResults)

        if (query != null) queryParams.or(
            listOf(
                "firstname||\$cont||$query",
                "lastname||\$cont||$query",
                "email||\$cont||$query",
                "username||\$cont||$query",
                "phoneNumber||\$cont||$query",
            )
        )

        return accountApi.getManyBaseUsersControllerUserEntity(queryParams).data
    }

    fun deleteAccount(externalId: UUID) = accountApi.deleteOneBaseUsersControllerUserEntity(externalId.toString())

    fun updateAccount(request: UserEntity): UserEntity =
        accountApi.updateOneBaseUsersControllerUserEntity(request.id, request)

    fun createPassword(externalId: String?, challengeResponse: String) {
        val id = UUID.fromString(externalId).toString()
        val body = UserPassword().also { it.password = challengeResponse }
        return accountApi.saveCredentials(id, SUPPORTED_CRED_TYPE, body)
    }

    fun disablePassword(externalId: String) = try {
        val id = UUID.fromString(externalId).toString()
        accountApi.deleteCredential(id, SUPPORTED_CRED_TYPE)
    } catch (e: FeignException) {
        log.error("Could not disable user password: {}", if (e.status() == 404) "Not found" else e.message)
        if (e.status() == 404) Unit else throw e
    }

    fun validateCred(externalId: String, challengeResponse: String): Boolean {
        val id = UUID.fromString(externalId).toString()
        val body = UserPassword().also { it.password = challengeResponse }
        return accountApi.verifyCredential(id, SUPPORTED_CRED_TYPE, body)
    }

}
