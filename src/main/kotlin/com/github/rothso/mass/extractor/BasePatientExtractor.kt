package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkContext
import com.github.rothso.mass.extractor.network.athena.AthenaService
import com.github.rothso.mass.extractor.network.athena.response.Patient
import io.reactivex.Flowable
import io.reactivex.FlowableTransformer
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Publisher
import retrofit2.HttpException
import timber.log.Timber
import timber.log.verbose
import timber.log.warn

internal abstract class BasePatientExtractor(
    private val athena: AthenaService,
    private val networkCtx: NetworkContext,
    private val patientFaker: PatientFaker
) : PatientExtractor {

  protected inner class PatientToSummary : FlowableTransformer<Patient, RedactedSummary> {

    override fun apply(upstream: Flowable<Patient>): Publisher<RedactedSummary> {
      return upstream
          .onBackpressureBuffer()
          .doOnNext { Timber.verbose { "${it.patientid}\t Retrieving patient encounters" } }
          .flatMap({ patient ->
            val patientId = patient.patientid
            athena.getPatientEncounters(patientId) // (2)
                .doOnSuccess { res ->
                  res.encounters.size.let {
                    if (it > 0) Timber.verbose { "$patientId\t Found $it encounters" }
                    else Timber.warn { "Found no encounters for $patientId" }
                  }
                }
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
