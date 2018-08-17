package com.github.rothso.mass.extractor.network.athena.response

import com.squareup.moshi.Json

data class PracticeResponse(
    @Json(name = "practiceinfo") val practiceInfos: List<PracticeInfo>
)