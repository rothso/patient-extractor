package com.github.rothso.mass.extractor.network.athena

import com.github.rothso.mass.extractor.network.athena.response.DepartmentResponse
import com.github.rothso.mass.extractor.network.athena.response.PatientId
import com.github.rothso.mass.extractor.network.athena.response.PracticeResponse
import io.reactivex.Single
import retrofit2.http.*

interface AthenaService {

  @GET("{id}/practiceinfo")
  fun getPracticeInfo(@Path("id") practiceId: Int): Single<PracticeResponse>

  @GET("{id}/departments")
  fun getDepartments(@Path("id") practiceId: Int): Single<DepartmentResponse>

  @FormUrlEncoded
  @POST("{id}/patients")
  fun createPatient(@Path("id") practiceId: Int, @FieldMap patient: Map<String, String>): Single<List<PatientId>>
}

