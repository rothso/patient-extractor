package com.github.rothso.mass.extractor

import com.github.javafaker.Faker
import com.github.rothso.mass.extractor.persist.Marshallable
import com.github.rothso.mass.extractor.network.athena.response.Patient
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.BufferedSink
import okio.BufferedSource

class PatientFaker : Marshallable {
  private var mappings: MutableMap<Int, Patient> = mutableMapOf()

  // Create an adapter for serializing/deserializing the Faker
  private val adapter = let {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val type = Types.newParameterizedType(MutableMap::class.java, Int::class.javaObjectType, Patient::class.java)
    moshi.adapter<MutableMap<Int, Patient>>(type).lenient()
  }

  override fun onHydrate(source: BufferedSource) {
    mappings = adapter.fromJson(source) ?: mappings
  }

  override fun onHibernate(sink: BufferedSink) {
    adapter.toJson(sink, mappings)
  }

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