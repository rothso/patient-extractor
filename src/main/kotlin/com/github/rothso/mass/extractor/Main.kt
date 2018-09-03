package com.github.rothso.mass.extractor

fun main(args: Array<String>) {
  // TODO args: practiceId, key, secret, max_concurrency
  val extractor = PatientExtractor()
  extractor.redactSummaries()
}