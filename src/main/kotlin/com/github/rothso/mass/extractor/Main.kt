package com.github.rothso.mass.extractor

import com.github.ajalt.mordant.TermColors
import com.github.rothso.mass.extractor.network.NetworkProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.cdimascio.dotenv.dotenv
import okio.Okio
import java.io.File
import java.io.PrintWriter

const val FAKER_CACHE = "faker.json"
const val PAGE_RECORD = "page.json"
const val OUTPUT_FOLDER = "encounters/"

fun main(args: Array<String>) {
  val dotenv = dotenv()
  val tc = TermColors()

  val mapFile = File(FAKER_CACHE)
  val pageFile = File(PAGE_RECORD)

  // Create an adapter for serializing/deserializing the Faker
  val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  val adapter = moshi.adapter<PatientFaker>(PatientFaker::class.java).lenient()

  // Required arguments: API key and secret
  val athenaKey = dotenv["ATHENA_KEY"] ?: return println(tc.red("Missing ATHENA_KEY in .env"))
  val athenaSecret = dotenv["ATHENA_SECRET"] ?: return println(tc.red("Missing ATHENA_SECRET in .env"))

  // Optional values (no practiceId = use the preview endpoint)
  val practiceId = dotenv["PRACTICE_ID"]?.toInt()
  val maxConcurrency = if (practiceId == null) 3 else args.getOrNull(0)?.toInt() ?: 10

  // Create a faker that remembers associations from previous runs
  val faker = when {
    mapFile.exists() -> adapter.fromJson(Okio.buffer(Okio.source(mapFile))) ?: PatientFaker()
    else -> PatientFaker()
  }

  val athena = NetworkProvider(athenaKey, athenaSecret, practiceId).createAthenaClient()
  val extractor = PatientExtractor(athena, faker)

  // Save state before exiting in case the program crashed and needs to be restarted
  Runtime.getRuntime().addShutdownHook(Thread {
    Okio.buffer(Okio.sink(mapFile)).use { adapter.toJson(it, faker) }
    Okio.buffer(Okio.sink(pageFile)).use { it.writeUtf8(extractor.currentPage.toString()) }
  })

  // Get the last page we were on
  val lastPage = if (pageFile.exists()) pageFile.readText().trim().toInt() else 0

  // Run the extractor tool
  extractor.getSummaries(maxConcurrency, lastPage)
      .blockingSubscribe({ (encounterId, patient, html) ->
        saveAsHtml(encounterId, html)
        println(with(tc) { green("\u2713") + String.format("  %-10d", encounterId) + bold(patient.name) })
      }, Throwable::printStackTrace)
}

private fun saveAsHtml(encounterId: Int, html: String) {
  val file = File("$OUTPUT_FOLDER$encounterId.html").apply {
    parentFile.mkdirs()
  }

  PrintWriter(file).use { pw -> pw.print(html) }
}