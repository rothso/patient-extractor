package com.github.rothso.mass.extractor.log

import com.github.ajalt.mordant.TermColors
import timber.log.Timber
import timber.log.Tree

class ConsoleTree(private val minPriority: Int) : Tree() {
  private val tc = TermColors()

  override fun performLog(priority: Int, tag: String?, throwable: Throwable?, message: String?) {
    if (priority >= minPriority && message != null) {
      println(when (priority) {
        Timber.ERROR -> tc.red(tc.bold("\u2718 $message"))
        Timber.WARNING -> tc.yellow("\u26A0 $message")
        Timber.VERBOSE -> tc.gray("$message")
        else -> message
      })
    }
  }
}