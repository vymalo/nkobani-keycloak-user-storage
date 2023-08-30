package com.vymalo.keycloak.nkobani.users

import feign.RequestInterceptor
import feign.RequestTemplate

class TokenRequestInterceptor(
    private val tokenRequester: TokenRequester
) : RequestInterceptor {
    override fun apply(template: RequestTemplate) {
        template.header("Authorization", "Bearer " + tokenRequester.getAccessToken())
    }
}
