package com.github.rothso.mass.extractor.network

import com.github.rothso.mass.extractor.network.athena.AthenaService
import com.github.rothso.mass.extractor.network.athena.OAuthService
import com.github.rothso.mass.extractor.network.athena.RetryingAthenaProxy
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class NetworkProvider(apiKey: String, apiSecret: String, practiceId: Int? = null) {
  private val retrofit: Retrofit

  companion object {
    private const val DEBUG_URL = "https://api.athenahealth.com/preview1/195900/"
    private const val DEBUG_URL_OAUTH = "https://api.athenahealth.com/oauthpreview/"
    private const val PROD_BASE_URL = "https://api.athenahealth.com/v1/"
    private const val PROD_BASE_URL_OAUTH = "https://api.athenahealth.com/oauth/"
  }

  fun createAthenaClient(onRetry: (Int) -> Unit = {}): AthenaService {
    return RetryingAthenaProxy(retrofit.create(AthenaService::class.java), onRetry)
  }

  init {
    val debug = practiceId == null

    val rxJavaAdapterFactory = RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io())
    val moshiConverterFactory = MoshiConverterFactory.create(Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build())

    val authenticator = let {
      val oAuthService = Retrofit.Builder()
          .baseUrl(if (debug) DEBUG_URL_OAUTH else "$PROD_BASE_URL_OAUTH$practiceId/")
          .addCallAdapterFactory(rxJavaAdapterFactory)
          .addConverterFactory(moshiConverterFactory)
          .build().create(OAuthService::class.java)

      OAuthAuthenticator(oAuthService, apiKey, apiSecret)
    }

    this.retrofit = Retrofit.Builder()
        .baseUrl(if (debug) DEBUG_URL else "$PROD_BASE_URL$practiceId/")
        .addCallAdapterFactory(rxJavaAdapterFactory)
        .addConverterFactory(moshiConverterFactory)
        .client(OkHttpClient.Builder()
            .authenticator(authenticator)
            .addInterceptor(authenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build())
        .build()
  }
}
