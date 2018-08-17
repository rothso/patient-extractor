package com.github.rothso.mass.extractor.network.athena.response

data class Patient(
  val firstname: String, // Abigail
  val middlename: String? = null, // Jamie
  val lastname: String, // Smith
  val homephone: String? = null, // 2038036135
  val address1: String? = null, // 1015 Washington Ave
  val address2: String? = null, // Unit 509
  val lastemail: String? = null, // demo@athenahealth.com
  val zip: String? = null, // 29681
  val city: String? = null, // SAINT LOUIS
  val firstappointment: String? = null, // 08\/03\/2018 18:00
  val lastappointment: String? = null, // 08\/03\/2018 18:00
  val sex: String? = null, // F
  val state: String? = null, // MO
  val patientid: Int, // 33
  val dob: String, // 06\/17\/1990
  val countrycode: String, // USA
  val countrycode3166: String // US
)