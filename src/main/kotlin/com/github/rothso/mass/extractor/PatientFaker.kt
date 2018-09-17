package com.github.rothso.mass.extractor

import com.github.javafaker.Faker
import com.github.rothso.mass.extractor.network.athena.response.Patient

class PatientFaker(private val mappings: MutableMap<Int, Patient> = mutableMapOf()) {

  fun getAlias(target: Patient): Patient {
    return mappings.getOrPut(target.patientid) {
      val faker = Faker()
      val name = faker.name()

      Patient(
          patientid = target.patientid,
          firstname = name.firstName(),
          lastname = name.lastName(),
          // TODO handle middle names?
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