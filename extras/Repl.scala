// Load via Ammonite 0.6.2
/*
 * Load this using `amm -f Repl.scala`
 *
 */
import ammonite.ops._
import sys.process._
import collection.mutable

println("Loading plotlypad library")

import $ivy.`com.github.mpkocher::plotlypad:0.1.5-SNAPSHOT`

import java.nio.file.Paths
import com.github.mpkocher.plotlypad._
import Models._
import HtmlConvertersImplicits._
import SeleniumUtils._

// Define Test data for demo

val xs = (0 to 10).map(_.toDouble)
val ys = xs.map(i => 2 * i * i)
val basicLayout = Layout("Ammonite Demo", "X-label", "Y-Label")

val plot = ScatterPlot(xs, ys, basicLayout)

println(s"Default plot $plot")

val px = Paths.get("repl-test.html").toAbsolutePath

//writeToHtml(plot, px)
//println(s"Wrote demo scatter plot to $px")

// Open up the Html page in the default browsers
// val outputHtml = showPlot(plot)

// Only required for saving png(s)
val helper = new SeleniumHelper(100)

// Write to Png requires Selenium Driver
//writeToPng(plot, px, helper)


println("Finished loading. Run `showPlot(plot)` to open up an example plot in the default browser")
