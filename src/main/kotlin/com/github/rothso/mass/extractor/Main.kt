package com.github.rothso.mass.extractor

import com.github.rothso.mass.BuildConfig
import com.github.rothso.mass.extractor.network.NetworkProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Okio
import java.io.File

/**
 * File location to serialize/deserialize the [PatientFaker].
 */
const val FAKER_CACHE = "faker.json"

fun main(args: Array<String>) {
  // Required arguments: API key and secret
  val athenaKey = args.getOrNull(0) ?: BuildConfig.ATHENA_KEY
  val athenaSecret = args.getOrNull(1) ?: BuildConfig.ATHENA_SECRET

  // Optional values (no practiceId will use the preview endpoint)
  val practiceId = args.getOrNull(2)?.toInt()
  val maxConcurrency = if (practiceId == null) 2 else args.getOrNull(3)?.toInt() ?: 10

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
  val athena = NetworkProvider(athenaKey, athenaSecret, practiceId)
  val extractor = PatientExtractor(athena, faker, maxConcurrency)
  extractor.redactSummaries()
}