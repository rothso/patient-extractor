package com.github.rothso.mass.extractor.persist

import okio.BufferedSink
import okio.BufferedSource

interface Marshallable {
  fun onHydrate(source: BufferedSource)
  fun onHibernate(sink: BufferedSink)
}