package com.github.rothso.mass.extractor.network.athena

import com.github.rothso.mass.extractor.network.athena.response.TokenResponse
import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface OAuthService {

  @FormUrlEncoded
  @POST("token")
  fun login(
      @Header("Authorization") basic: String,
      @Field("grant_type") grantType: String
  ): Observable<TokenResponse>
}