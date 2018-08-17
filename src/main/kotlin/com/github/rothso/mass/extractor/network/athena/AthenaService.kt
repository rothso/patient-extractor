package com.github.rothso.mass.extractor.network.athena

import com.github.rothso.mass.extractor.network.athena.response.DepartmentResponse
import com.github.rothso.mass.extractor.network.athena.response.EncountersResponse
import com.github.rothso.mass.extractor.network.athena.response.PatientsResponse
import com.github.rothso.mass.extractor.network.athena.response.Summary
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AthenaService {
  @GET("departments")
  fun getDepartments(): Single<DepartmentResponse>

  //  (1) GET /patients /preview1/:practiceid/patients ==> get patientid and fields to be redacted w/ Faker
  //  (2) GET /chart/{patientid}/encounters /preview1/:practiceid/chart/:patientid/encounters ==> get encounterid
  //  (3) GET /chart/encounters/{encounterid}/summary /preview1/:practiceid/chart/encounters/:encounterid/summary ==> get html

  // TODO hard-code the department ID (always 1, everything goes through Memorial system)

  @GET("patients")
  fun getAllPatients(@Query("departmentid") departmentId: Int): Single<PatientsResponse>

  @GET("chart/{pid}/encounters")
  fun getPatientEncounters(@Path("pid") patientId: Int, @Query("departmentid") departmentId: Int): Single<EncountersResponse>

  @GET("chart/encounters/{eid}/summary")
  fun getEncounterSummary(@Path("eid") encounterId: Int): Single<Summary>
}
