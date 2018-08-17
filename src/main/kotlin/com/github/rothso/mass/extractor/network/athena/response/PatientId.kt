package com.github.rothso.mass.extractor.network.athena.response

import com.squareup.moshi.Json

data class PatientId(
    @Json(name = "patientid") val id: Int
)