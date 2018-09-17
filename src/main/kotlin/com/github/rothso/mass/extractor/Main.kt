package com.github.rothso.mass.extractor

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Okio
import java.io.File

/**
 * File location to serialize/deserialize the [PatientFaker].
 */
const val FAKER_CACHE = "faker.json"

fun main(args: Array<String>) {
  // TODO args: practiceId, key, secret, max_concurrency

  // Deserialize the PatientFaker and reuse any existing fake associations
  val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
  val adapter = moshi.adapter<PatientFaker>(PatientFaker::class.java).lenient()
  val mapFile = File(FAKER_CACHE)
  val faker = when {
    mapFile.exists() -> adapter.fromJson(Okio.buffer(Okio.source(mapFile))) ?: PatientFaker()
    else -> PatientFaker()
  }

  // Serialize the PatientFaker before exiting in case the program crashed and needs to be restarted
  Runtime.getRuntime().addShutdownHook(Thread {
    Okio.buffer(Okio.sink(mapFile)).use { adapter.toJson(it, faker) }
  })

  // Run the extractor tool
  val extractor = PatientExtractor(faker)
  extractor.redactSummaries()
}