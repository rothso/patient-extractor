package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkProvider
import com.github.rothso.mass.extractor.network.athena.response.Patient
import io.reactivex.schedulers.Schedulers

class PatientExtractor {
  private val athena = NetworkProvider.createAthenaClient()

  /**
   * Get the HTML summaries for redaction and storage. This requires three API calls:
   *  1. Get all patientids for the clinic
   *  2. Get every encounterid for each patient
   *  3. Get the HTML summary for every encounter
   */
  fun redactSummaries() {
    athena.getAllPatients() // (1)
        .flattenAsFlowable { it.patients }
        .distinct { it.patientid }
        .onBackpressureBuffer()
        .flatMap({ patient ->
          val id = patient.patientid
          athena.getPatientEncounters(id) // (2)
              .doOnSubscribe { println("Requesting patient encounters for $id") }
              .doOnSuccess { println("Got encounters") }
              .flattenAsFlowable { it.encounters }
              .onBackpressureBuffer()
              .flatMapSingle({ encounter ->
                athena.getEncounterSummary(encounter.encounterId) // (3)
                    .doOnSubscribe { println("Requesting summaries for $id") }
                    .doOnSuccess { println("Got summary") }
              }, false, 1)
              .doOnComplete { println("Finished $id") }
              .map { Pair(patient, it) }
        }, false, 3) // TODO may need to tweak maxConcurrency in prod build
        .observeOn(Schedulers.computation())
        .map { (patient, summary) -> fake(patient, summary.html) }
        .blockingSubscribe({ html ->
          saveAsHtml(html)
          saveAsPdf(html)
        }, Throwable::printStackTrace)
  }

  private fun fake(patient: Patient, html: String): String {
    TODO()
  }

  private fun saveAsHtml(html: String) {
    TODO()
  }

  private fun saveAsPdf(html: String) {
    TODO()
  }
}
