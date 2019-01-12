package com.github.rothso.mass.extractor

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.TermColors
import timber.log.Timber
import timber.log.error
import timber.log.info
import java.io.File

fun main(args: Array<String>) = MainCommand().main(args)

class MainCommand : CliktCommand(name = "java -jar ${getJarName()}") {
  private val file by argument().file(exists = true).optional() // TODO: document args
  private val verbose by option("-v", "--verbose").flag("-s", "--silent", default = false)

  override fun run() {
    Application.enableConsoleLogs(verbose)

    val app = try {
      Application()
    } catch (e: IllegalStateException) {
      return Timber.error { e.message!! }
    }

    val extractor = if (file == null) {
      // We are downloading summaries based on patient IDs
      app.getAllPatientsExtractor()
    } else {
      // We are downloading all patient summaries
      app.getByPatientIdExtractor(try {
        parsePatientIdsFile(file!!)
      } catch (e: NumberFormatException) {
        return Timber.error { e.message!! }
      })
    }

    // Run the extractor tool
    val tc = TermColors()
    extractor.getSummaries().blockingSubscribe({ (encounterId, patient, html) ->
      val (firstName, _, lastName, _, id) = patient

      // Save each file under an easy-to-identify name
      val fileName = "%s/%04d_%s_%s_%d.html".format(app.outputDir, id, lastName, firstName, encounterId)
      File(fileName).apply { parentFile.mkdirs() }.writeText(html)

      // Print a message so we know the request succeeded
      val info = with(tc) { "%s  %-9d %s(%d)".format(green("\u2713"), encounterId, bold(patient.name), id) }
      Timber.info { info }
    }, Throwable::printStackTrace)
  }

  private fun parsePatientIdsFile(file: File): List<Int> {
    return file.readLines().mapIndexed { i, line ->
      line.toIntOrNull() ?: throw NumberFormatException(
          "Parse error: $line (line $i) of ${file.name} is not a number")
    }
  }
}

private fun getJarName(): String? {
  // https://stackoverflow.com/a/11159435
  return File(MainCommand::class.java.protectionDomain.codeSource.location.path).name
}