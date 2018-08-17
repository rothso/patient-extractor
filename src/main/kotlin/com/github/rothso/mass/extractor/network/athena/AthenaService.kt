package com.github.rothso.mass.extractor.network.athena

import com.github.rothso.mass.extractor.network.athena.response.PracticeResponse
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path

interface AthenaService {

  @GET("{id}/practiceinfo")
  fun getPracticeInfo(@Path("id") id: Int): Observable<PracticeResponse>
}