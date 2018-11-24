package com.github.rothso.mass.extractor.network

import com.github.rothso.mass.extractor.network.athena.OAuthService
import okhttp3.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class OAuthAuthenticator(
    private val service: OAuthService,
    apiKey: String,
    apiSecret: String
) : Authenticator, Interceptor {
  private val credentials = Credentials.basic(apiKey, apiSecret)
  private val lock = ReentrantLock()
  private var accessToken: String? = null

  override fun authenticate(route: Route?, response: Response): Request? {
    if (response.request().header("Authorization") != null) {
      return null // Give up, we've already failed to authenticate.
    }

    // Prevent other requests from 401-ing while we're fetching the token
    lock.withLock {
      // Get a token from the OAuth endpoint or implicitly throw an exception
      val tokenResponse = service.login(credentials, "client_credentials").blockingFirst()
      this.accessToken = tokenResponse.accessToken
    }

    // Retry the previous request
    return response.request().newBuilder()
        .addHeader("Authorization", "Bearer ${accessToken!!}")
        .build()
  }

  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()

    // Wait if another thread is already fetching the token
    lock.withLock { accessToken }?.let {
      return chain.proceed(request.newBuilder()
          .addHeader("Authorization", "Bearer $it")
          .build())
    }

    // No token; allow 401 so the authenticator can fetch one
    return chain.proceed(request)
  }
}