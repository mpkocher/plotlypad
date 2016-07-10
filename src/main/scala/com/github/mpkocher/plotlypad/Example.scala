package com.github.mpkocher.plotlypad

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Files, Path, Paths}

import org.apache.commons.io.FileUtils

//import org.apache.commons.io.FileUtils
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.support.ui.{ExpectedCondition, WebDriverWait}
import org.openqa.selenium._

import spray.json._
import DefaultJsonProtocol._

import breeze.stats.distributions._

object Models {

  case class Histogram(datum: Seq[Double], title: String, xlabel: String, ylabel: String, binWidth: Double, bargap: Double = 0.05)

}

trait ModelJsonProtocols {
  import Models._

  implicit val histogramFormat = jsonFormat6(Histogram)
}

object ModelJsonProtocols extends ModelJsonProtocols


object HtmlUtils {

  import Models._

  import scalatags.Text.all._

  // FIXME
  val plotDivId = "pbplot"

  def toHtml(histogram: Histogram, width: Int = 1200, height: Int = 900) = {

    // Generate String rep of datum of the form [1,2,3,4]
    val xs = "[" + histogram.datum.map(_.toString).reduceLeft[String] {(acc, n) => acc + "," + n.toString } + "]"

    html(
      head(
        script(src := "https://cdn.plot.ly/plotly-latest.min.js")
      ),
      body(
        div(id := s"$plotDivId", style := s"width:${width}px;height:${height}px;"),
        script(
          s"""
            |// This contains the custom Plot description
            |
            |// Histogram Data
            |var xs = $xs;
            |
            |// Custom plotly
            |var datum = [
            |        {
            |            name: 'control',
            |            autobinx: true,
            |            xbins: {
            |                start: -3.2,
            |                end: 2.8,
            |                size: ${histogram.binWidth}
            |                },
            |            opacity: 0.75,
            |            x: xs,
            |            type: 'histogram'
            |        }
            |    ];
            |
            |// Plotly Layout
            |var simpleLayout = {
            |        title: '${histogram.title}',
            |        xaxis: {title: '${histogram.xlabel}'},
            |        yaxis: {title: '${histogram.ylabel}'},
            |        bargap: ${histogram.bargap}
            |    };
            |
            |// Plot the histogram
            |Plotly.newPlot('$plotDivId', datum, simpleLayout);
          """.stripMargin)
      )
    )
    }

  def writeHtml(sx: String, path: Path): Path = {
    val bw = new BufferedWriter(new FileWriter(path.toFile))
    bw.write(sx)
    bw.close()
    path
  }

  val nvalues = 100

  val nx = new Gaussian(0, 0.5)

  val exampleDatum = nx.sample(nvalues)

  def demo(path: Path) = {
    val histogram = Histogram(exampleDatum, "Example Title", "X Label", "Y Label", 0.05)
    val html = toHtml(histogram)
    writeHtml(html.toString, path)
    path
  }

}


object ExampleDriver {

  case class SeleniumHelper(timeOut: Int) {
    def waitFor(driver: WebDriver, f: (WebDriver) => WebElement) : WebElement = {
      new WebDriverWait(driver, timeOut).until(
        new ExpectedCondition[WebElement] {
          override def apply(d: WebDriver) = f(d)
        })
    }

    def test(path: Path, outputPath: Path) = {
      val driver: WebDriver = new FirefoxDriver

      val ux = path.toUri.toURL
      println(s"Getting $ux")

      driver.get(ux.toString)

      val screenShotFile: File = driver.asInstanceOf[TakesScreenshot].getScreenshotAs(OutputType.FILE)

      println(s"Wrote file to $outputPath")
      FileUtils.copyFile(screenShotFile, outputPath.toFile)

      //waitFor(driver, _.findElement(By.className("module module-Menu")))
    }
  }
}


object Example extends App {

  import ExampleDriver._

  val outputHtml = Paths.get("extras/demo.html")
  val outputPng = Paths.get("extras/demo.png")

  HtmlUtils.demo(outputHtml)

  val sx = SeleniumHelper(100)
  sx.test(outputHtml, outputPng)

  println("Exiting main")
}
