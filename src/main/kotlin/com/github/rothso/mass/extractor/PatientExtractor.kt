package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkProvider

class PatientExtractor {
  private val athena = NetworkProvider.createAthenaClient()

  fun getSummaries() {
    val departmentId = getDepartmentId()

    athena.getAllPatients(departmentId) // (1) get all patients
        .flattenAsObservable { it.patients }
        .flatMapSingle { athena.getPatientEncounters(it.patientid, departmentId) } // (2) get patient encounters
        .flatMapIterable { it.encounters }
        .firstOrError()
        .flatMap { athena.getEncounterSummary(it.encounterId) } // (3) get HTML summary
        .map { it.html }
        .subscribe(::println, Throwable::printStackTrace)

    // TODO feed into faker, save to .html and .pdf (open .html in Word to convert to PDF)
  }

  private fun getDepartmentId(): Int {
    return athena.getDepartments() // query all departments
        .flattenAsObservable { it.departments }
        .map { it.departmentId }
        .blockingFirst() // pick the first department
  }
}
