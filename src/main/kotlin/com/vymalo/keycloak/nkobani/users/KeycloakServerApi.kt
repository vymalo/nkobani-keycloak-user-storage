package com.vymalo.keycloak.nkobani.users

import feign.Headers
import feign.Param
import feign.RequestLine
import org.keycloak.representations.AccessTokenResponse

interface KeycloakServerApi {

    @RequestLine("POST /realms/{realm}/protocol/openid-connect/token")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    fun requestToken(
        @Param("realm") realm: String,
        @Param("client_id") clientId: String,
        @Param("client_secret") clientSecret: String,
        @Param("grant_type") grantType: String,
        @Param("scope") scope: String
    ): AccessTokenResponse

}

