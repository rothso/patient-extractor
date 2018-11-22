package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.athena.response.Patient

data class RedactedSummary(
    val encounterId: Int,
    val alias: Patient,
    val reportHtml: String
)