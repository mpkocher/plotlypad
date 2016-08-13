package com.github.mpkocher.plotlypad

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Files, Path, Paths}

import com.github.mpkocher.plotlypad.Models.{BarPlot, Layout}
import com.github.mpkocher.plotlypad.SeleniumUtils.SeleniumHelper
import org.apache.commons.io.FileUtils

import scalatags.Text

//import org.apache.commons.io.FileUtils
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.support.ui.{ExpectedCondition, WebDriverWait}
import org.openqa.selenium._

import spray.json._
import DefaultJsonProtocol._

import breeze.stats.distributions._

object Models {

  trait BaseLayout {
    val xlabel: String
    val ylabel: String
    val title: String
  }

  trait LayoutAble[T <: BaseLayout] {
    val layout: T
  }

  trait Plot

  case class Layout(title: String, xlabel: String, ylabel: String) extends BaseLayout

  case class HistogramLayout(title: String, xlabel: String, ylabel: String, binWidth: Double, bargap: Double = 0.05) extends BaseLayout

  // There needs to be a NumberLike trait from breeze to support Int or Double. Using HistogramPlot[T] datum: Seq[T]
  // creates JSON seralization headaches
  case class HistogramPlot(datum: Seq[Double], layout: HistogramLayout) extends LayoutAble[HistogramLayout] with Plot
  case class BarPlot(x: Seq[Double], y: Seq[Double], layout: Layout) extends LayoutAble[Layout] with Plot
  case class ScatterPlot(x: Seq[Double], y: Seq[Double], layout: Layout) extends LayoutAble[Layout] with Plot

}

trait ModelJsonProtocols {
  import Models._

  implicit val histogramLayoutFormat = jsonFormat5(HistogramLayout)
  implicit val histogramFormat = jsonFormat2(HistogramPlot)
}

object ModelJsonProtocols extends ModelJsonProtocols


object SeleniumUtils {
  case class SeleniumHelper(timeOut: Int) {
    def waitFor(driver: WebDriver, f: (WebDriver) => WebElement): WebElement = {
      new WebDriverWait(driver, timeOut).until(
        new ExpectedCondition[WebElement] {
          override def apply(d: WebDriver) = f(d)
        })
    }

    def convertToPng(path: Path, outputPath: Path) = {
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


object HtmlConverters {

  import Models._
  import scalatags.Text.all._

  def writeToHtml(html: Text.TypedTag[String], output: Path): Path = {
    val bw = new BufferedWriter(new FileWriter(output.toFile))
    bw.write(html.toString())
    bw.close()
    output
  }

  def writeToPng(html: Path, png: Path)(implicit helper: SeleniumHelper): Path = {
    helper.convertToPng(html, png)
    png
  }


  trait BaseConverter[T <: Plot] {

    val PLOT_TYPE: String

    val DEFAULT_PLOT_DIV_ID = "pbplot"
    val DEFAULT_PLOT_WIDTH = 1200
    val DEFAULT_PLOT_HEIGHT = 900
    val DEFAULT_PLOTLY_JS = "https://cdn.plot.ly/plotly-latest.min.js"

    def toHtml(p: T, width: Int = DEFAULT_PLOT_WIDTH, height: Int = DEFAULT_PLOT_HEIGHT): Text.TypedTag[String]

    def writeHtml(p: T, output: Path): Path = {
      writeToHtml(toHtml(p), output)
    }

    def writePng(p: T, output: Path)(implicit helper: SeleniumHelper): Path = {
      // FIXME
      val htmlPath = Paths.get(output.toAbsolutePath.toString + ".html")
      writeHtml(p, htmlPath)
      writeToPng(htmlPath, output)
      output
    }
  }

  object ConvertHistogramPlot extends BaseConverter[HistogramPlot] {
    val PLOT_TYPE = "histogram"

    def toHtml(p: HistogramPlot, width: Int = DEFAULT_PLOT_WIDTH, height: Int = DEFAULT_PLOT_HEIGHT) = {
      // Generate String rep of datum of the form [1,2,3,4]
      val xs = "[" + p.datum.map(_.toString).reduceLeft[String] {(acc, n) => acc + "," + n.toString } + "]"

      html(
        head(
          script(src := s"$DEFAULT_PLOTLY_JS")
        ),
        body(
          div(id := s"$DEFAULT_PLOT_DIV_ID", style := s"width:${width}px;height:${height}px;"),
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
               |                size: ${p.layout.binWidth}
               |                },
               |            opacity: 0.75,
               |            x: xs,
               |            type: '$PLOT_TYPE'
               |        }
               |    ];
               |
               |// Plotly Layout
               |var simpleLayout = {
               |        title: '${p.layout.title}',
               |        xaxis: {title: '${p.layout.xlabel}'},
               |        yaxis: {title: '${p.layout.ylabel}'},
               |        bargap: ${p.layout.bargap}
               |    };
               |
            |// Plot the histogram
               |Plotly.newPlot('$DEFAULT_PLOT_DIV_ID', datum, simpleLayout);
          """.stripMargin)
        )
      )
    }
  }

  object ConvertBarPlot extends BaseConverter[BarPlot] {
    val PLOT_TYPE = "bar"
    def toHtml(p: BarPlot,width: Int = DEFAULT_PLOT_WIDTH, height: Int = DEFAULT_PLOT_HEIGHT) = {

      def toS(xs: Seq[Double]) =  "[" + xs.map(_.toString).reduceLeft[String] {(acc, n) => acc + "," + n.toString } + "]"

      val xs = toS(p.x)
      val ys = toS(p.y)

      html(
        head(
          script(src := s"$DEFAULT_PLOTLY_JS")
        ),
        body(
          div(id := s"$DEFAULT_PLOT_DIV_ID", style := s"width:${width}px;height:${height}px;"),
          script(
            s"""
               |// This contains the custom Plot description
               |
               |// Histogram Data
               |var xs = $xs;
               |var ys = $ys;
               |
               |// Custom plotly
               |var datum = [
               |        {
               |            name: 'control',
               |            opacity: 0.75,
               |            x: xs,
               |            y: ys,
               |            type: '$PLOT_TYPE'
               |        }
               |    ];
               |
               |// Plotly Layout
               |var simpleLayout = {
               |        title: '${p.layout.title}',
               |        xaxis: {title: '${p.layout.xlabel}'},
               |        yaxis: {title: '${p.layout.ylabel}'}
               |    };
               |
               |// Plot the histogram
               |Plotly.newPlot('$DEFAULT_PLOT_DIV_ID', datum, simpleLayout);
          """.stripMargin)
        )
      )
    }
  }

  object ConvertScatterPlot extends BaseConverter[ScatterPlot] {
    val PLOT_TYPE = "scatter"
    def toHtml(p: ScatterPlot, width: Int = DEFAULT_PLOT_WIDTH, height: Int = DEFAULT_PLOT_HEIGHT) = {

      def toS(xs: Seq[Double]) =  "[" + xs.map(_.toString).reduceLeft[String] {(acc, n) => acc + "," + n.toString } + "]"

      val xs = toS(p.x)
      val ys = toS(p.y)

      html(
        head(
          script(src := s"$DEFAULT_PLOTLY_JS")
        ),
        body(
          div(id := s"$DEFAULT_PLOT_DIV_ID", style := s"width:${width}px;height:${height}px;"),
          script(
            s"""
               |// This contains the custom Plot description
               |
               |// Histogram Data
               |var xs = $xs;
               |var ys = $ys;
               |
               |// Custom plotly
               |var datum = [
               |        {
               |            name: 'control',
               |            opacity: 0.75,
               |            x: xs,
               |            y: ys,
               |            type: '$PLOT_TYPE'
               |        }
               |    ];
               |
               |// Plotly Layout
               |var simpleLayout = {
               |        title: '${p.layout.title}',
               |        xaxis: {title: '${p.layout.xlabel}'},
               |        yaxis: {title: '${p.layout.ylabel}'}
               |    };
               |
               |// Plot the histogram
               |Plotly.newPlot('$DEFAULT_PLOT_DIV_ID', datum, simpleLayout);
          """.stripMargin)
        )
      )
    }
  }
}


object ExamplePlots {

  import Models._
  import HtmlConverters._


  private def toPath(rootOutputPath: Path, fname: String) =
    rootOutputPath.toAbsolutePath.resolve(fname)


  /**
    * BarPlot Demo
    *
    * @param helper
    * @param outputDir
    */
  def barPlotDemo(helper: SeleniumHelper, outputDir: Path) = {

    val barOutputHtml = toPath(outputDir, "bar-plot.html")
    val barOutputPng = toPath(outputDir, "bar-plot.png")

    println(s"Generating BarChart to $barOutputHtml")

    val barLayout = Layout("Bar Chart", "X-label", "Y-label")
    val xs = (0 to 5).map(_ * 1.0)
    val ys = xs.map(i => i * i)
    val barPlot = BarPlot(xs, ys, barLayout)

    val barHtml = ConvertBarPlot.writeHtml(barPlot, barOutputHtml)
    writeToPng(barOutputHtml, barOutputPng)(helper)

    println(s"Wrote $barHtml")
  }

  /**
    * Scatter Plot Demo
    *
    * @param helper
    * @param outputDir
    */
  def scatterPlotDemo(helper: SeleniumHelper, outputDir: Path) = {

    val layout = Layout("Scatter Plot Demo", "X-Label", "Y-Label")
    println("Generating Scatter Plot")

    val xs = (0 to 5).map(_ * 1.0)
    val ys = xs.map(i => i * i)
    val scatterPlot = ScatterPlot(xs, ys, layout)

    val scatterOutputHtml =  toPath(outputDir, "scatter-plot.html")
    val scatterOutputPng = toPath(outputDir, "scatter-plot.png")

    ConvertScatterPlot.writeHtml(scatterPlot, scatterOutputHtml)
    writeToPng(scatterOutputHtml, scatterOutputPng)(helper)
    println(s"Wrote scatter plot to $scatterOutputHtml")

  }

  def histogramPlotDemo(helper: SeleniumHelper, outputDir: Path) = {

    val nvalues = 100

    val nx = new Gaussian(0, 0.5)

    val exampleDatum = nx.sample(nvalues)

    val outputHtml = toPath(outputDir, "histogram-plot.html")
    val outputPng = toPath(outputDir, "histogram-plot.png")

    println(s"Generating HistogramPlot $outputHtml")
    val histogramLayout = HistogramLayout("Histogram Plot Demo", "X-label", "Y-label", 0.05)
    val histogram = HistogramPlot(exampleDatum, histogramLayout)

    ConvertHistogramPlot.writeHtml(histogram, outputHtml)
    writeToPng(outputHtml, outputPng)(helper)

  }


  /*
  Example Demo of Testing
   */
  def demo(rootOutputPath: Path) = {

    val sx = SeleniumHelper(100)

    histogramPlotDemo(sx, rootOutputPath)

    barPlotDemo(sx, rootOutputPath)

    scatterPlotDemo(sx, rootOutputPath)

    0
  }

}


object Example extends App {
  ExamplePlots.demo(Paths.get("extras").toAbsolutePath)
  println("Exiting Main.")
}
