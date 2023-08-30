package com.vymalo.keycloak.nkobani.users

import feign.Feign
import feign.Logger
import feign.form.FormEncoder
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.okhttp.OkHttpClient
import feign.slf4j.Slf4jLogger
import org.keycloak.representations.AccessTokenResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TokenRequester(
    keycloakUrl: String,
    private val realmName: String,
    private val clientId: String,
    private val clientSecret: String,
    private val scope: String
) {
    private val serverApi = Feign.builder()
        .logger(Slf4jLogger(KeycloakServerApi::class.java))
        .logLevel(Logger.Level.FULL)
        .client(OkHttpClient())
        .encoder(FormEncoder(JacksonEncoder()))
        .decoder(JacksonDecoder())
        .target(KeycloakServerApi::class.java, keycloakUrl)

    private var response: AccessTokenResponse? = null
    private var nextRefreshAt: LocalDateTime? = null

    fun getAccessToken(forceRefresh: Boolean = false): String {
        if (forceRefresh || nextRefreshAt == null || nextRefreshAt!!.minus(100, ChronoUnit.SECONDS)
                .isBefore(LocalDateTime.now())
        ) {
            requestNewToken()
        }
        return response!!.token
    }

    private fun requestNewToken() {
        response = serverApi.requestToken(realmName, clientId, clientSecret, "client_credentials", scope)
        nextRefreshAt = LocalDateTime.now().plus(response!!.expiresIn, ChronoUnit.SECONDS)
    }
}