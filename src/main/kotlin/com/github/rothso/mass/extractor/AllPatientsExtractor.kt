package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkContext
import com.github.rothso.mass.extractor.network.athena.AthenaService
import io.reactivex.Flowable

internal class AllPatientsExtractor(
    private val athena: AthenaService,
    networkCtx: NetworkContext,
    patientFaker: PatientFaker
) : BasePatientExtractor(athena, networkCtx, patientFaker) {

  /**
   * Get the HTML summaries for redaction and storage. This requires three API calls:
   *  1. Get all patientids for the clinic
   *  2. Get every encounterid for each patient
   *  3. Get the HTML summary for every encounter
   */
  override fun getSummaries(): Flowable<RedactedSummary> {
    return Flowable.range(0, Integer.MAX_VALUE)
        .concatMap { offset ->
          athena.getAllPatients(offset * 10) // (1)
              .flatMapPublisher { results ->
                Flowable.fromIterable(results.patients)
                    .distinct { it.patientid }
                    .compose(PatientToSummary())
                    .map { ctx -> Pair(results.next, ctx) }
              }
        }
        .takeUntil { it.first == null } // stop when there are no pages left
        .map { it.second }
  }
}