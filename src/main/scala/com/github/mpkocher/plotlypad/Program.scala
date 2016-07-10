package com.github.mpkocher.plotlypad

import co.theasi.plotly
import co.theasi.plotly.{Plot, writer, draw}
import co.theasi.plotly.writer.PlotWriter

import util.Random


/**
  * Example of using the official plotly API (requires their services)
  */
object Program extends App {

  // Generate uniformly distributed x
  val xs = (0 until 100)

  // Generate random y
  val ys = (0 until 100).map { i => i + 5.0 * Random.nextDouble }


  def run = {
    val p = Plot().withScatter(xs, ys)
    println(s"Plot $p")
    //draw(p, "stuff.file")

    //PlotWriter.plotAsJson

    //draw(p, "basic-scatter", writer.FileOptions(overwrite = true))
    // returns  PlotFile(pbugnion:173,basic-scatter)
  }

  run
  println("Exiting Example")
}
