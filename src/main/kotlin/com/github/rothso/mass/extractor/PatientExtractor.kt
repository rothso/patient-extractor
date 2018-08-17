package com.github.rothso.mass.extractor

import com.github.rothso.mass.BuildConfig
import com.github.rothso.mass.extractor.network.OAuthAuthenticator
import com.github.rothso.mass.extractor.network.athena.AthenaService
import com.github.rothso.mass.extractor.network.athena.OAuthService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Observable
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

class PatientExtractor {

  fun run() {
    val rxJavaAdapterFactory = RxJava2CallAdapterFactory.create() // TODO scheduling
    val moshiConverterFactory = MoshiConverterFactory.create(Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build())

    val authenticator = let {
      val oAuthService = Retrofit.Builder()
          .baseUrl("https://api.athenahealth.com/oauthpreview/")
          .addCallAdapterFactory(rxJavaAdapterFactory)
          .addConverterFactory(moshiConverterFactory)
          .build().create(OAuthService::class.java)

      OAuthAuthenticator(oAuthService, BuildConfig.ATHENA_KEY, BuildConfig.ATHENA_SECRET)
    }

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.athenahealth.com/preview1/")
        .addCallAdapterFactory(rxJavaAdapterFactory)
        .addConverterFactory(moshiConverterFactory)
        .client(OkHttpClient.Builder()
            .authenticator(authenticator)
            .addInterceptor(authenticator)
            .build())
        .build()

    val api = retrofit.create(AthenaService::class.java)

    api.getPracticeInfo(1)
        .flatMap { Observable.fromIterable(it.practiceInfos) }
        .subscribe(
            { info -> print(info) },
            { err -> err.printStackTrace() },
            { println("Done") }
        )
  }
}

