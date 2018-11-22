package com.github.rothso.mass.extractor

import com.github.rothso.mass.BuildConfig
import com.github.rothso.mass.extractor.network.NetworkProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Okio
import java.io.File
import java.io.PrintWriter

const val FAKER_CACHE = "faker.json"
const val PAGE_RECORD = "page.json"
const val OUTPUT_FOLDER = "encounters/"

fun main(args: Array<String>) {
  val mapFile = File(FAKER_CACHE)
  val pageFile = File(PAGE_RECORD)

  // Create an adapter for serializing/deserializing the Faker
  val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  val adapter = moshi.adapter<PatientFaker>(PatientFaker::class.java).lenient()

  // Required arguments: API key and secret
  val athenaKey = args.getOrNull(0) ?: BuildConfig.ATHENA_KEY
  val athenaSecret = args.getOrNull(1) ?: BuildConfig.ATHENA_SECRET

  // Optional values (no practiceId = use the preview endpoint)
  val practiceId = args.getOrNull(2)?.toInt()
  val maxConcurrency = if (practiceId == null) 2 else args.getOrNull(3)?.toInt() ?: 10

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
  val lastPage = if (pageFile.exists()) pageFile.readText().toInt() else 0

  // Run the extractor tool
  extractor.getSummaries(maxConcurrency, lastPage)
      .blockingSubscribe({ (encounterId, patient, html) ->
        saveAsHtml(encounterId, html)
        println("\u2713  $encounterId \t ${patient.name}")
      }, Throwable::printStackTrace)

  // TODO: Auto restart if it crashes due to a 403
}

private fun saveAsHtml(encounterId: Int, html: String) {
  val file = File("$OUTPUT_FOLDER$encounterId.html").apply {
    parentFile.mkdirs()
  }

  PrintWriter(file).use { pw -> pw.print(html) }
}