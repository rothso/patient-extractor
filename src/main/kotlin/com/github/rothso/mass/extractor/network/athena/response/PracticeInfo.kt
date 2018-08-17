package com.github.rothso.mass.extractor.network.athena.response

import com.squareup.moshi.Json

data class PracticeInfo(
    @Json(name = "iscoordinatorsender") val isCoordinatorSender: Boolean,
    @Json(name = "hasclinicals") val hasClinicals: Boolean,
    @Json(name = "name") val name: String,
    @Json(name = "golivedate") val goLiveDate: String, // MM/DD/YYYY
    @Json(name = "experiencemode") val experienceMode: String,
    @Json(name = "hascommunicator") val hasCommunicator: Boolean,
    @Json(name = "iscoordinatorreceiver") val isCoordinatorReceiver: Boolean,
    @Json(name = "hascollector") val hasCollector: Boolean,
    @Json(name = "practiceid") val practiceId: Int
)