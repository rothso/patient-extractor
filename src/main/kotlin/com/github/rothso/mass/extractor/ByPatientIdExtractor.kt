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
//          println("$id\t Getting patient by ID")
          athena.getPatientById(id) // (1)
              // TODO: print an error message on 404 (patient not found)
//              .doOnSuccess { println("$id\t Got patient by ID") }
              .map { it[0] }
//              .doOnSuccess { println("$id\t Patient name is ${it.name}") }
              .toFlowable()
              .compose(PatientToSummary())
        }
  }

  override fun onHibernate(sink: BufferedSink) {
    sink.writeUtf8(currentId.toString()) // TODO don't write if null
  }
}