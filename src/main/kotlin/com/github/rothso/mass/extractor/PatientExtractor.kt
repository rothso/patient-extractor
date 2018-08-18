package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkProvider
import com.github.rothso.mass.extractor.network.athena.response.Summary
import io.reactivex.Observable

class PatientExtractor {
  private val athena = NetworkProvider.createAthenaClient()

  /**
   * Get the HTML summaries for redaction and storage. This requires three API calls:
   *  1. Get all patientids for the clinic
   *  2. Get every encounterid for each patient
   *  3. Get the HTML summary for every encounter
   */
  fun getSummaries(): Observable<Summary> {
    // TODO: throttle requests
    return athena.getAllPatients() // (1)
        .flattenAsObservable { it.patients }
        .flatMapSingle { athena.getPatientEncounters(it.patientid) } // (2)
        .flatMapIterable { it.encounters }
        .firstOrError() // temporary, until throttling is added
        .flatMap { athena.getEncounterSummary(it.encounterId) } // (3)
        .toObservable()
  }

  fun redactSummaries(summaries: Observable<Summary>) {
    summaries
        .map { it.html }
        .subscribe(::println, Throwable::printStackTrace)
    // TODO Faker search/replace; save to .html; open .html in Word to convert to .pdf
  }
}
