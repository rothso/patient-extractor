package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.athena.AthenaService
import com.github.rothso.mass.extractor.network.athena.response.Patient
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException

class PatientExtractor(
    private val athena: AthenaService,
    private val patientFaker: PatientFaker
) {
  var currentPage = 0
    private set

  private data class Context(
      val nextPage: String?,
      val patient: Patient,
      val encounterId: Int,
      val summaryHtml: String
  )

  /**
   * Get the HTML summaries for redaction and storage. This requires three API calls:
   *  1. Get all patientids for the clinic
   *  2. Get every encounterid for each patient
   *  3. Get the HTML summary for every encounter
   */
  fun getSummaries(maxConcurrency: Int, startPage: Int = 0): Flowable<RedactedSummary> {
    return Flowable.range(startPage, Integer.MAX_VALUE - startPage)
        .concatMap { offset ->
          currentPage = offset
          val pageObservable = athena.getAllPatients(offset * 10).cache() // (1)
          val nextPage = pageObservable.blockingGet().next

          pageObservable
              .flattenAsFlowable { it.patients }
              .distinct { it.patientid }
              .onBackpressureBuffer()
              .flatMap({ patient ->
                athena.getPatientEncounters(patient.patientid) // (2)
                    .flattenAsFlowable { it.encounters }
                    .onBackpressureBuffer()
                    .onErrorResumeNext { t: Throwable ->
                      // Silently ignore "400: bad request" errors
                      if (t is HttpException && t.code() == 400) Flowable.empty()
                      else Flowable.error(t)
                    }
                    .map { it.encounterId }
                    .flatMapSingle({ eId ->
                      athena.getEncounterSummary(eId) // (3)
                          .map { Context(nextPage, patient, eId, it.html) }
                    }, false, maxConcurrency)
              }, 1)
        }
        .takeUntil { it.nextPage == null } // stop when there are no pages left
        .observeOn(Schedulers.computation())
        .map { (_, patient, eId, html) ->
          val (alias, redacted) = patientFaker.fake(patient, html)
          RedactedSummary(eId, alias, redacted)
        }
  }
}
