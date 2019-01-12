package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkContext
import com.github.rothso.mass.extractor.network.NetworkProvider
import io.reactivex.Flowable
import timber.log.Timber
import timber.log.error
import timber.log.info
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

interface PatientExtractor {
  fun getSummaries(): Flowable<RedactedSummary>

  class Factory(env: Environment, private val patientFaker: PatientFaker) {
    private val maxConcurrency = if (env.previewMode) 5 else 10
    private val networkCtx = NetworkContext(AtomicInteger(maxConcurrency))
    private val athena = NetworkProvider(env).createAthenaClient {
      if (it == 403) {
        // On retry, log a message so we can see how often we are hitting rate limits
        Timber.error { "Hit API rate limit, retrying" }
        // Automatically lower the concurrency so this doesn't happen as often
        networkCtx.maxConcurrency.updateAndGet { i -> max(i - 1, 2) }
      }
    }

    fun createByPatientIdExtractor(patientIds: List<Int>): PatientExtractor {
      Timber.info { "Downloading summaries for ${patientIds.size} patients" }
      return ByPatientIdExtractor(athena, networkCtx, patientFaker, patientIds)
    }

    fun createAllPatientsExtractor(): PatientExtractor {
      Timber.info { "Downloading summaries for all patients" }
      return AllPatientsExtractor(athena, networkCtx, patientFaker)
    }
  }
}