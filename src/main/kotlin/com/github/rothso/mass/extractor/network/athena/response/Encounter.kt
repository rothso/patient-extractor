package com.github.rothso.mass.extractor.network.athena.response

import com.squareup.moshi.Json

data class Encounter(
    @Json(name = "departmentid") val departmentId: Int, // 1
    @Json(name = "encounterdate") val encounterDate: String, // "02/10/2018"
    @Json(name = "encounterid") val encounterId: Int // 34934
)