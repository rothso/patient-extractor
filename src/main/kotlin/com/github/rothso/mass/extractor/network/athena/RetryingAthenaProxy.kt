package com.github.rothso.mass.extractor.network.athena

import com.github.rothso.mass.extractor.network.athena.response.Encounters
import com.github.rothso.mass.extractor.network.athena.response.Patient
import com.github.rothso.mass.extractor.network.athena.response.Patients
import com.github.rothso.mass.extractor.network.athena.response.Summary
import io.reactivex.Maybe
import io.reactivex.Single
import retrofit2.HttpException
import java.util.concurrent.TimeUnit

class RetryingAthenaProxy(
    private val athena: AthenaService,
    private val onRetry: (Int) -> Unit = {}
) : AthenaService {

  companion object {
    const val DELAY_MS = 500L
  }

  override fun getAllPatients(offset: Int): Single<Patients> {
    return athena.getAllPatients(offset)
        .onErrorResumeNext { t: Throwable ->
          if (t is HttpException && t.code() in setOf(403, 503)) {
            onRetry(t.code())
            getAllPatients(offset).delaySubscription(DELAY_MS, TimeUnit.MILLISECONDS)
          } else Single.error(t)
        }
  }

  override fun getPatientById(patientId: Int): Single<List<Patient>> {
    return athena.getPatientById(patientId)
        .onErrorResumeNext { t: Throwable ->
          if (t is HttpException && t.code() in setOf(403, 503)) {
            onRetry(t.code())
            getPatientById(patientId).delaySubscription(DELAY_MS, TimeUnit.MILLISECONDS)
          } else Single.error(t)
        }
  }

  override fun getPatientEncounters(patientId: Int): Maybe<Encounters> {
    return athena.getPatientEncounters(patientId)
        .onErrorResumeNext { t: Throwable ->
          if (t is HttpException && t.code() in setOf(403, 503)) {
            onRetry(t.code())
            getPatientEncounters(patientId).delaySubscription(DELAY_MS, TimeUnit.MILLISECONDS)
          } else Maybe.error(t)
        }
  }

  override fun getEncounterSummary(encounterId: Int): Single<Summary> {
    return athena.getEncounterSummary(encounterId)
        .onErrorResumeNext { t: Throwable ->
          if (t is HttpException && t.code() in setOf(403, 503)) {
            onRetry(t.code())
            getEncounterSummary(encounterId).delaySubscription(DELAY_MS, TimeUnit.MILLISECONDS)
          } else Single.error(t)
        }
  }
}