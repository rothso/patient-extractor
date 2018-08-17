package com.github.rothso.mass.extractor.network.athena.response

import com.squareup.moshi.Json

data class Department(
    val state: String,
    @Json(name = "departmentid") val departmentId: Int,
    val address: String,
    val clinicals: String,
    val name: String,
    @Json(name = "patientdepartmentname") val patientDepartmentName: String,
    val zip: String,
    @Json(name = "portalurl") val portalUrl: String,
    val city: String,
    val fax: String?,
    val phone: String?
)