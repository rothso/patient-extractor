package com.github.rothso.mass.extractor

import com.github.javafaker.Faker
import com.github.rothso.mass.extractor.network.athena.response.Patient

class PatientFaker(private val mappings: MutableMap<Int, Patient> = mutableMapOf()) {

  fun fake(patient: Patient, text: String): Pair<Patient, String> {
    val alias = getAlias(patient)
    val replacements = mapOf(
        // Middle names don't appear in the report, so we don't have to worry about replacing them
        "${patient.firstname} ${patient.lastname}" to "${alias.firstname} ${alias.lastname}",
        "${patient.lastname}, ${patient.firstname}" to "${alias.lastname}, ${alias.firstname}",
        patient.dob to alias.dob
    )

    return alias to replacements.asIterable().fold(text) { str, (old, new) ->
      str.replace(old, new, true)
    }
  }

  private fun getAlias(target: Patient): Patient {
    return mappings.getOrPut(target.patientid) {
      val faker = Faker()
      val name = faker.name()

      Patient(
          patientid = target.patientid,
          firstname = name.firstName(),
          lastname = name.lastName(),
          dob = let {
            // Keep the year; replace month and day
            val date = faker.date().birthday()
            val month = date.month
            val day = date.day
            val year = target.dob.split("/")[2]
            "$month/$day/$year"
          }
      )
    }
  }
}