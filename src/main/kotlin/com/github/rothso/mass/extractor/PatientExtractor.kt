package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkProvider

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
        .map { it.patientid }
        .distinct()
        .doOnNext { println(it) }
        .onBackpressureBuffer()
        .flatMap({ patientid ->
          athena.getPatientEncounters(patientid) // (2)
              .doOnSubscribe { println("Requesting patient encounters for $patientid") }
              .doOnSuccess { println("Got encounters") }
              .flattenAsFlowable { it.encounters }
              .onBackpressureBuffer()
              .flatMapSingle({ encounter ->
                athena.getEncounterSummary(encounter.encounterId) // (3)
                    .doOnSubscribe { println("Requesting summaries for $patientid") }
                    .doOnSuccess { println("Got summary") }
              }, false, 1)
              .doOnComplete { println("Finished $patientid") }
        }, false, 3)
        .blockingSubscribe({}, Throwable::printStackTrace)

    // TODO Faker search/replace; save to .html; open .html in Word to convert to .pdf
  }
}
