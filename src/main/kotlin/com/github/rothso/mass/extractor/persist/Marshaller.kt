package com.github.rothso.mass.extractor.persist

import okio.Okio
import java.io.File

object Marshaller {
  fun unmarshall(obj: Marshallable, fileName: String) {
    val file = File(fileName)
    if (file.exists() && file.length() != 0L) {
      val bufferedSource = Okio.buffer(Okio.source(file))
      obj.onHydrate(bufferedSource)
      bufferedSource.close()
    }
  }

  fun marshall(obj: Marshallable, fileName: String) {
    val file = File(fileName)
    val bufferedSink = Okio.buffer(Okio.sink(file))
    obj.onHibernate(bufferedSink)
    bufferedSink.close()
  }
}