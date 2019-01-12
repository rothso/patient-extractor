package com.github.rothso.mass.extractor.network

import com.github.rothso.mass.extractor.Environment
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

class NetworkProvider(env: Environment) {
  private val retrofit: Retrofit

  companion object {
    private const val PREVIEW_URL = "https://api.athenahealth.com/preview1/"
    private const val PREVIEW_URL_OAUTH = "https://api.athenahealth.com/oauthpreview/"
    private const val PROD_BASE_URL = "https://api.athenahealth.com/v1/"
    private const val PROD_BASE_URL_OAUTH = "https://api.athenahealth.com/oauth/"
  }

  fun createAthenaClient(onRetry: (Int) -> Unit = {}): AthenaService {
    return RetryingAthenaProxy(retrofit.create(AthenaService::class.java), onRetry)
  }

  init {
    val rxJavaAdapterFactory = RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io())
    val moshiConverterFactory = MoshiConverterFactory.create(Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build())

    val authenticator = let {
      val oAuthService = Retrofit.Builder()
          .baseUrl(if (env.previewMode) PREVIEW_URL_OAUTH else PROD_BASE_URL_OAUTH)
          .addCallAdapterFactory(rxJavaAdapterFactory)
          .addConverterFactory(moshiConverterFactory)
          .build().create(OAuthService::class.java)

      OAuthAuthenticator(oAuthService, env.athenaKey, env.athenaSecret)
    }

    this.retrofit = Retrofit.Builder()
        .baseUrl("${if (env.previewMode) PREVIEW_URL else PROD_BASE_URL}${env.practiceId}/")
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
