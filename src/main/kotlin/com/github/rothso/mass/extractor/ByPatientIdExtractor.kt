package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkContext
import com.github.rothso.mass.extractor.network.athena.AthenaService
import io.reactivex.Flowable
import timber.log.Timber
import timber.log.verbose

internal class ByPatientIdExtractor(
    private val athena: AthenaService,
    networkCtx: NetworkContext,
    patientFaker: PatientFaker,
    private val patientIds: List<Int>
) : BasePatientExtractor(athena, networkCtx, patientFaker) {

  override fun getSummaries(): Flowable<RedactedSummary> {
    return Flowable.fromIterable(patientIds)
        .concatMap { id ->
          Timber.verbose { "$id\t Getting patient by ID" }
          athena.getPatientById(id) // (1)
              // TODO: test a 404 error case (since it's possible to get a 404 response here)
              // TODO: print an error message on 404 (patient not found) and ensure we recover
              .map { it[0] }
              .doOnSuccess { Timber.verbose { "$id\t Got patient by ID (${it.name})" } }
              .toFlowable()
              .compose(PatientToSummary())
        }
  }
}