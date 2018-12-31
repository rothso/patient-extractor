package com.github.rothso.mass.extractor

import com.github.ajalt.mordant.TermColors
import com.github.rothso.mass.extractor.Constants.FAKER_FILE
import com.github.rothso.mass.extractor.Constants.LAST_PAGE_FILE
import com.github.rothso.mass.extractor.Constants.LAST_PATIENT_FILE
import com.github.rothso.mass.extractor.Constants.OUTPUT_FOLDER
import com.github.rothso.mass.extractor.persist.Marshaller
import io.github.cdimascio.dotenv.dotenv
import java.io.File
import java.io.PrintWriter

private val dotenv = dotenv()
private val tc = TermColors()

// No practiceId = use the preview Athena endpoint
private val previewMode = dotenv["PRACTICE_ID"] == null

private object Constants {
  // Isolate the state between preview and production so the extractor
  // doesn't resume from the last page or patient id of the wrong mode.
  val FAKER_FILE = prefix("faker.json")
  val LAST_PAGE_FILE = prefix("page.txt")
  val LAST_PATIENT_FILE = prefix("patient.txt")

  // Use different preview and production output locations so preview
  // summaries don't accidentally get mixed in with production summaries.
  val OUTPUT_FOLDER = suffix("encounters")

  private fun prefix(name: String): String {
    return if (previewMode) "preview-$name" else name
  }

  private fun suffix(name: String): String {
    return if (previewMode) "$name-preview" else name
  }
}

fun main(args: Array<String>) {
  // Required arguments: API key and API secret
  val athenaKey = dotenv["ATHENA_KEY"]
      ?: return println(tc.red("Missing ATHENA_KEY in .env"))
  val athenaSecret = dotenv["ATHENA_SECRET"]
      ?: return println(tc.red("Missing ATHENA_SECRET in .env"))
  val practiceId = dotenv["PRACTICE_ID"]?.toInt()

  // We want a number as high as possible without triggering rate limiting
  val maxConcurrency = if (previewMode) 5 else 10

  with(tc) {
    if (practiceId == null) {
      println((yellow + bold)("\n\t\u26A0 Using practice API"))
      println(yellow("\t  Specify the PRACTICE_ID in .env file to use the production API\n"))
    } else {
      println((green + bold)("\n\t\u2713 Using production API"))
      println(green("\t  Practice ID: ${bold(practiceId.toString())}\n"))
    }
  }

  // Create the faker
  val faker = PatientFaker()

  // Create the extractor
  val factory = PatientExtractor.Factory(athenaKey, athenaSecret, practiceId, maxConcurrency, faker)
  val extractor: PatientExtractor
  val extractorSaveFile: String

  val inputFileName = args.getOrNull(0)
  if (inputFileName != null) {
    // We are downloading summaries based on patient IDs
    println(tc.green("Downloading summaries for patient IDs listed in ${tc.bold(inputFileName)}"))
    val file = File(args[0])
    if (!file.exists())
      return println(tc.run { red + bold }("Input file does not exist: ${file.absolutePath}"))
    val patientIds = File(args[0]).readLines().map {
      it.toIntOrNull() ?: return@main println(tc.red("Parser error: $it is not a number"))
    }
    extractor = factory.createByPatientIdExtractor(patientIds)
    extractorSaveFile = LAST_PATIENT_FILE
  } else {
    // No input file, so we are downloading all patient summaries
    println(tc.green("Downloading summaries for all patients"))
    extractor = factory.createAllPatientsExtractor()
    extractorSaveFile = LAST_PAGE_FILE
  }

  // Restore any state from previous runs
  Marshaller.unmarshall(faker, FAKER_FILE)
  Marshaller.unmarshall(extractor, extractorSaveFile)

  // Save state before exiting in case the program crashed and needs to be restarted
  Runtime.getRuntime().addShutdownHook(Thread {
    println("\nCleaning up...")
    Marshaller.marshall(faker, FAKER_FILE)
    Marshaller.marshall(extractor, extractorSaveFile)
    println("Summaries are located at ${tc.green(File(OUTPUT_FOLDER).absolutePath)}")
  })

  // Run the extractor tool
  extractor.getSummaries()
      .blockingSubscribe({ (eId, patient, html) ->
        // Save each file under an easy-to-identify name
        val paddedId = String.format("%04d", patient.patientid)
        val fileName = paddedId + "_${patient.lastname}_${patient.firstname}_$eId"
        saveAsHtml("$OUTPUT_FOLDER/$fileName.html", html)

        // Print a message so we know the request succeeded
        val msg = String.format("  %-10d", eId) + tc.bold(patient.name) + " (${patient.patientid})"
        println(tc.green("\u2713") + msg)
      }, Throwable::printStackTrace)
}

private fun saveAsHtml(name: String, html: String) {
  val file = File(name)
  file.parentFile.mkdirs()
  PrintWriter(file).use { pw -> pw.print(html) }
}