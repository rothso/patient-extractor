package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkContext
import com.github.rothso.mass.extractor.network.athena.AthenaService
import com.github.rothso.mass.extractor.network.athena.response.Patient
import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Publisher
import retrofit2.HttpException

internal abstract class BasePatientExtractor(
    private val athena: AthenaService,
    private val networkCtx: NetworkContext,
    private val patientFaker: PatientFaker
) : PatientExtractor {

  protected inner class PatientToSummary : FlowableTransformer<Patient, RedactedSummary> {

    // TODO: log a message if no encounters were found
    override fun apply(upstream: Flowable<Patient>): Publisher<RedactedSummary> {
      return upstream
          .onBackpressureBuffer()
//          .doOnNext { println("${it.patientid}\t Retrieving patient encounters") }
          .flatMap({ patient ->
            athena.getPatientEncounters(patient.patientid) // (2)
//                .doOnSuccess { println("${patient.patientid}\t Found ${it.encounters.size} encounters") }
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
                      .map { Triple(patient, eId, it.html) }
                }, false, networkCtx.maxConcurrency.get())
          }, 1)
          .observeOn(Schedulers.computation())
          .map { (patient, encounterId, summaryHtml) ->
            val (alias, redacted) = patientFaker.fake(patient, summaryHtml)
            RedactedSummary(encounterId, alias, redacted)
          }
    }
  }
}
