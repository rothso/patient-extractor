package com.github.rothso.mass.extractor

import com.github.ajalt.mordant.TermColors
import com.github.rothso.mass.extractor.log.ConsoleTree
import com.github.rothso.mass.extractor.persist.Marshaller
import io.github.cdimascio.dotenv.dotenv
import timber.log.Timber
import timber.log.info

internal class Application {

  // We're reading the ATHENA_KEY, ATHENA_SECRET, and optional PRACTICE_ID secrets from
  // the .env file. If there is no practice ID specified, the program assumes you intend
  // to use the preview API and will use a special practice ID for ambulatory testing.
  private val environment = let {
    val dotenv = dotenv() // TODO: test missing .env
    Environment(
        dotenv["ATHENA_KEY"] ?: throw IllegalStateException("Missing ATHENA_KEY in .env"),
        dotenv["ATHENA_SECRET"] ?: throw IllegalStateException("Missing ATHENA_SECRET in .env"),
        dotenv["PRACTICE_ID"]?.toInt() ?: 195900,
        dotenv["PRACTICE_ID"] == null
    )
  }

  private val factory = PatientExtractor.Factory(environment, getFaker())
  val outputDir = if (environment.previewMode) "$OUTPUT_DIR-preview" else OUTPUT_DIR

  init {
    // Log whether we are in preview or production mode to help with troubleshooting.
    // These messages are logged at the INFO level, which will be displayed even if the
    // verbose (-v) flag is not enabled.
    with(TermColors()) {
      if (environment.previewMode) {
        Timber.info { (yellow + bold)("\n\t\u26A0 Using preview Athena API") }
        Timber.info { yellow("\t  To use the production API, specify the PRACTICE_ID in .env file") }
      } else {
        Timber.info { (green + bold)("\n\t\u2713 Using production Athena API") }
        Timber.info { green("\t  Practice ID: ${bold(environment.practiceId.toString())}") }
      }
      Timber.info { magenta("\t  Summaries will be saved at /${bold(outputDir)}\n") }
    }
  }

  companion object {
    private const val FAKER_FILE = "faker.json"
    private const val OUTPUT_DIR = "encounters"

    fun enableConsoleLogs(verbose: Boolean) {
      Timber.plant(ConsoleTree(if (verbose) Timber.VERBOSE else Timber.INFO))
    }
  }

  fun getAllPatientsExtractor(): PatientExtractor {
    return factory.createAllPatientsExtractor()
  }

  fun getByPatientIdExtractor(patientIds: List<Int>): PatientExtractor {
    return factory.createByPatientIdExtractor(patientIds)
  }

  private fun getFaker(): PatientFaker = PatientFaker().also {
    Marshaller.unmarshall(it, FAKER_FILE)
    Runtime.getRuntime().addShutdownHook(Thread {
      Marshaller.marshall(it, FAKER_FILE)
    })
  }
}