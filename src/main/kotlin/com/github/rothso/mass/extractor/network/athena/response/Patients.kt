package com.github.rothso.mass.extractor.network.athena.response

import com.squareup.moshi.Json

data class Patients(
    val next: String, // paginated
    val patients: List<Patient>,
    @Json(name = "totalcount") val totalCount: Int
)