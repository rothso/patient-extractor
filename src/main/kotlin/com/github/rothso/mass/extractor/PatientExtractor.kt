package com.github.rothso.mass.extractor

import com.github.ajalt.mordant.TermColors
import com.github.rothso.mass.extractor.network.NetworkContext
import com.github.rothso.mass.extractor.network.NetworkProvider
import com.github.rothso.mass.extractor.persist.Marshallable
import io.reactivex.Flowable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

interface PatientExtractor : Marshallable {
  fun getSummaries(): Flowable<RedactedSummary>

  class Factory(
      athenaKey: String,
      athenaSecret: String,
      practiceId: Int?,
      maxConcurrency: Int,
      private val patientFaker: PatientFaker
  ) {
    private val networkCtx = NetworkContext(AtomicInteger(maxConcurrency))
    private val athena = NetworkProvider(athenaKey, athenaSecret, practiceId).createAthenaClient {
      // On retry, log a message so we can see how often we are hitting rate limits
      println(TermColors().run { red + bold }("\u2718 Hit API rate limit, retrying"))
      // Automatically lower the concurrency so this doesn't happen as often
      networkCtx.maxConcurrency.updateAndGet { i -> max(i - 1, 2) }
    }

    fun createByPatientIdExtractor(patientIds: List<Int>): PatientExtractor {
      return ByPatientIdExtractor(athena, networkCtx, patientFaker, patientIds)
    }

    fun createAllPatientsExtractor(): PatientExtractor {
      return AllPatientsExtractor(athena, networkCtx, patientFaker)
    }
  }
}