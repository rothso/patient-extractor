package com.github.rothso.mass.extractor.network.athena.response

import com.squareup.moshi.Json

data class Summary(@Json(name = "summaryhtml") val html: String)