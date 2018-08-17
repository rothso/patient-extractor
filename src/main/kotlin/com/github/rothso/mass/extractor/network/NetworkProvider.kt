package com.github.rothso.mass.extractor.network

import com.github.rothso.mass.BuildConfig
import com.github.rothso.mass.extractor.network.athena.AthenaService
import com.github.rothso.mass.extractor.network.athena.OAuthService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkProvider {
  private const val BASE_URL = "https://api.athenahealth.com/preview1/"
  private const val BASE_URL_OAUTH = "https://api.athenahealth.com/oauthpreview/"
  private val retrofit: Retrofit

  init {
    val rxJavaAdapterFactory = RxJava2CallAdapterFactory.create() // TODO scheduling
    val moshiConverterFactory = MoshiConverterFactory.create(Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build())

    val authenticator = let {
      val oAuthService = Retrofit.Builder()
          .baseUrl(BASE_URL_OAUTH)
          .addCallAdapterFactory(rxJavaAdapterFactory)
          .addConverterFactory(moshiConverterFactory)
          .build().create(OAuthService::class.java)

      OAuthAuthenticator(oAuthService, BuildConfig.ATHENA_KEY, BuildConfig.ATHENA_SECRET)
    }

    this.retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addCallAdapterFactory(rxJavaAdapterFactory)
        .addConverterFactory(moshiConverterFactory)
        .client(OkHttpClient.Builder()
            .authenticator(authenticator)
            .addInterceptor(authenticator)
            .build())
        .build()
  }

  fun createAthenaClient(): AthenaService {
    return retrofit.create(AthenaService::class.java)
  }
}
