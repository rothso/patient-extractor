package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkProvider

class PatientExtractor {
  private val athena = NetworkProvider.createAthenaClient()

  fun quickstart() {
    athena.getPracticeInfo(1) // get all available practices
        .flattenAsObservable { it.practiceInfos }
        .map { it.practiceId } // we only care about the ID
        .firstOrError() // get the first practice ID
        .flatMap { practiceId ->
          athena.getDepartments(practiceId) // query all departments
              .flattenAsObservable { it.departments }
              .map { dept -> Pair(practiceId, dept.departmentId) }
              .elementAtOrError(4) // pick an arbitrary department
        }
        .flatMap { (practiceId, departmentId) ->
          // Add a new patient to the chosen department
          athena.createPatient(practiceId, mapOf(
              "departmentid" to departmentId.toString(),
              "dob" to "09/10/1999",
              "firstname" to "David",
              "lastname" to "Smith",
              "mobilephone" to "(906) 214-1313"
          ))
        } // On success, print the new patient ID
        .subscribe(::println, Throwable::printStackTrace)
  }
}
