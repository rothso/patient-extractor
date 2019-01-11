package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkContext
import com.github.rothso.mass.extractor.network.athena.AthenaService
import io.reactivex.Flowable
import okio.BufferedSink
import okio.BufferedSource
import timber.log.Timber
import timber.log.verbose

internal class ByPatientIdExtractor(
    private val athena: AthenaService,
    networkCtx: NetworkContext,
    patientFaker: PatientFaker,
    private val patientIds: List<Int>
) : BasePatientExtractor(athena, networkCtx, patientFaker) {
  private var currentId: Int? = null
  private var startId: Int? = null

  override fun onHydrate(source: BufferedSource) {
    startId = source.readUtf8().trim().toInt()
  }

  override fun getSummaries(): Flowable<RedactedSummary> {
    return Flowable.fromIterable(patientIds)
        .skipWhile { startId != null && it != startId }
        .doOnNext { currentId = it }
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

  override fun onHibernate(sink: BufferedSink) {
    sink.writeUtf8(currentId.toString()) // TODO don't write if null
  }
}