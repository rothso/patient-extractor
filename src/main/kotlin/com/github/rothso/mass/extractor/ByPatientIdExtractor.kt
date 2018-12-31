package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkContext
import com.github.rothso.mass.extractor.network.athena.AthenaService
import io.reactivex.Flowable
import okio.BufferedSink
import okio.BufferedSource

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
          athena.getPatientById(id) // (1)
              .map { it[0] }
              .toFlowable()
              .compose(PatientToSummary())
        }
  }

  override fun onHibernate(sink: BufferedSink) {
    sink.writeUtf8(currentId.toString())
  }
}