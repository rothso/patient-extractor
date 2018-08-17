package com.github.rothso.mass.extractor

import com.github.rothso.mass.extractor.network.NetworkProvider
import io.reactivex.Observable

class PatientExtractor {
  private val athena = NetworkProvider.createAthenaClient()

  fun showAvailablePractices() {
    athena.getPracticeInfo(1)
        .flatMap { Observable.fromIterable(it.practiceInfos) }
        .subscribe(::println, Throwable::printStackTrace)
  }
}
