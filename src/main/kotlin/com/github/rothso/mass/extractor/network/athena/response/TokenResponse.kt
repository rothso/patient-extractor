package com.github.rothso.mass.extractor.network.athena.response

import com.squareup.moshi.Json

data class TokenResponse(
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_in") val expiresIn: Int,
    @Json(name = "refresh_token") val refreshToken: String
)