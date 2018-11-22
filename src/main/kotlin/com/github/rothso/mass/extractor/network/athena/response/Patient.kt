package com.github.rothso.mass.extractor.network.athena.response

data class Patient(
    val firstname: String, // Abigail
    val middlename: String? = null, // Jamie
    val lastname: String, // Smith
    val sex: String? = null, // F
    val patientid: Int, // 33
    val dob: String // 06/17/1990
) {
  val name = when (middlename) {
    null -> "$firstname $lastname"
    else -> "$firstname $middlename $lastname"
  }
}