package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkProvider
import com.github.rothso.mass.extractor.network.athena.response.Patient
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.PrintWriter

class PatientExtractor {
  private val athena = NetworkProvider.createAthenaClient()
  private val patientFaker = PatientFaker()

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
              .flattenAsFlowable { it.encounters }
              .onBackpressureBuffer()
              .flatMapSingle({ encounter ->
                athena.getEncounterSummary(encounter.encounterId) // (3)
                    .map { summary -> Pair(encounter.encounterId, summary) }
              }, false, 1)
              .map { (encId, summary) -> Triple(patient, encId, summary.html) }
        }, 3) // TODO may need to tweak maxConcurrency in prod API
        .observeOn(Schedulers.computation())
        .map { (patient, encId, html) -> Pair(encId, fake(patient, html)) }
        .blockingSubscribe({ (encId, html) -> saveAsHtml(encId, html) }, Throwable::printStackTrace)
  }

  private fun fake(patient: Patient, html: String): String {
    val alias = patientFaker.getAlias(patient)
    val replacements = mapOf(
        "${patient.firstname} ${patient.lastname}" to "${alias.firstname} ${alias.lastname}",
        "${patient.lastname}, ${patient.firstname}" to "${alias.lastname}, ${alias.firstname}",
        patient.dob to alias.dob
    )

    return replacements.asIterable().fold(html) {
      str, (old, new) -> str.replace(old, new, true)
    }
  }

  private fun saveAsHtml(encounterId: Int, html: String) {
    val file = File("encounters/$encounterId.html").apply {
      getParentFile().mkdirs()
    }

    PrintWriter(file).use { pw -> pw.print(html) }
    println("Saved $encounterId")
  }
}
