package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkProvider
import com.github.rothso.mass.extractor.network.athena.response.Patient
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import java.io.File
import java.io.PrintWriter

class PatientExtractor(private val patientFaker: PatientFaker) {
  private val athena = NetworkProvider.createAthenaClient()

  private data class Context(
      val nextPage: String?,
      val patient: Patient,
      val encounterId: Int,
      val summaryHtml: String
  )

  /**
   * Get the HTML summaries for redaction and storage. This requires three API calls:
   *  1. Get all patientids for the clinic
   *  2. Get every encounterid for each patient
   *  3. Get the HTML summary for every encounter
   */
  fun redactSummaries() {
    Flowable.range(0, Integer.MAX_VALUE)
        .concatMap { offset ->
          val pageObservable = athena.getAllPatients(offset * 10) // (1)
              .doOnSuccess { println("[HIT] Page") }
              .cache()
          val nextPage = pageObservable.blockingGet().next

          pageObservable
              .flattenAsFlowable { it.patients }
              .distinct { it.patientid }
              .onBackpressureBuffer()
              .flatMap({ patient ->
                println("\t[HIT] Patient")
                athena.getPatientEncounters(patient.patientid) // (2)
                    .flattenAsFlowable { it.encounters }
                    .onBackpressureBuffer()
                    .onErrorResumeNext { t: Throwable ->
                      // Silently ignore "400: bad request" errors
                      if (t is HttpException && t.code() == 400) Flowable.empty()
                      else Flowable.error(t)
                    }
                    .map { it.encounterId }
                    // .doOnComplete { println("Finished patient ${patient.patientid}") }
                    .flatMapSingle({ eId ->
                      println("\t\t[HIT] Encounter")
                      athena.getEncounterSummary(eId) // (3)
                          .map { Context(nextPage, patient, eId, it.html) }
                    }, false, 3)
              }, 1) // TODO may need to tweak maxConcurrency in prod API
        }
        .takeUntil { it.nextPage == null } // stop when there are no pages left
        .observeOn(Schedulers.computation())
        .map { (_, patient, eId, html) -> Pair(eId, fake(patient, html)) }
        .blockingSubscribe({ (eId, html) -> saveAsHtml(eId, html) }, Throwable::printStackTrace)
  }

  private fun fake(patient: Patient, html: String): String {
    val alias = patientFaker.getAlias(patient)
    val replacements = mapOf(
        "${patient.firstname} ${patient.lastname}" to "${alias.firstname} ${alias.lastname}",
        "${patient.lastname}, ${patient.firstname}" to "${alias.lastname}, ${alias.firstname}",
        patient.dob to alias.dob
    )

    return replacements.asIterable().fold(html) { str, (old, new) ->
      str.replace(old, new, true)
    }
  }

  private fun saveAsHtml(encounterId: Int, html: String) {
    val file = File("encounters/$encounterId.html").apply {
      parentFile.mkdirs()
    }

    PrintWriter(file).use { pw -> pw.print(html) }
    println("Saved $encounterId")
  }
}
