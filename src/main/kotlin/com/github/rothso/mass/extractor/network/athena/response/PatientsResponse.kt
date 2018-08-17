package com.github.rothso.mass.extractor.network.athena.response

data class PatientsResponse(
    val next: String, // paginated
    val patients: List<Patient>,
    val totalcount: Int
)