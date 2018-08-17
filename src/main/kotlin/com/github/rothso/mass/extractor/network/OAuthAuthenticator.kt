package com.github.rothso.mass.extractor.network

import com.github.rothso.mass.extractor.network.athena.OAuthService
import okhttp3.*

class OAuthAuthenticator(
    private val service: OAuthService,
    apiKey: String,
    apiSecret: String
) : Authenticator, Interceptor {
  private val credentials = Credentials.basic(apiKey, apiSecret)
  private var accessToken: String? = null
  private var refreshToken: String? = null // TODO use

  override fun authenticate(route: Route, response: Response): Request? {
    if (response.request().header("Authorization") != null) {
      return null; // Give up, we've already failed to authenticate.
    }

    // Get a token from the OAuth endpoint or die trying
    val tokenResponse = service.login(credentials, "client_credentials").blockingFirst()
    this.accessToken = tokenResponse.accessToken
    this.refreshToken = tokenResponse.refreshToken

    // Retry the previous request
    return response.request().newBuilder()
        .addHeader("Authorization", "Bearer ${accessToken!!}")
        .build()
  }

  override fun intercept(chain: Interceptor.Chain): Response {
    return with(chain.request()) {
      accessToken?.let {
        chain.proceed(this.newBuilder()
            .addHeader("Authorization", it)
            .build())
      }

      // Let the request 401 if we don't have a token yet
      chain.proceed(this)
    }
  }
}