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

  // It might be cleaner to encapsulate the entire plotly model (for each plot type), then convert to JSON.
  // This would reduce the js writing layer
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

object HtmlConvertersImplicits {
  import Models._
  import HtmlConverters._

  implicit val convertBarPlotter = HtmlConverters.ConvertBarPlot
  implicit val convertHistogramPlotter = HtmlConverters.ConvertHistogramPlot
  implicit val convertScatterPlotter = HtmlConverters.ConvertScatterPlot

    def convertToHtml[T <: Plot](p: T)(implicit converter: BaseConverter[T]): Text.TypedTag[String] =
      converter.toHtml(p)

    def writeToHtml[T <: Plot](p: T, output: Path)(implicit converter: BaseConverter[T]): Path =
      converter.writeHtml(p, output)

    // FIXME. There can only be one implicit, helper must explicit for now
    def writeToPng[T <: Plot](p: T, output: Path, helper: SeleniumHelper)(implicit converter: BaseConverter[T]): Path =
      converter.writePng(p, output)(helper)


}


object ExamplePlots {

  import Models._
  import HtmlConverters._


  private def toPath(rootOutputPath: Path, fname: String) =
    rootOutputPath.toAbsolutePath.resolve(fname)


  /**
    * BarPlot Demo
    *
    * @param helper    Selenium Helper
    * @param outputDir Root Output Directory of output html and png
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
    * @param helper    Selenium Helper
    * @param outputDir Root Output Directory of output html and png
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

  /**
    * Histogram Plot Demo
    *
    * @param helper    Selenium Helper
    * @param outputDir Root Output Directory of output html and png
    */
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

  def barPlotImplicitDemo(helper: SeleniumHelper, outputDir: Path) = {
    import HtmlConvertersImplicits._

    val barOutputHtml = toPath(outputDir, "bar-plot2.html")
    val barOutputPng = toPath(outputDir, "bar-plot2.png")

    println(s"Generating BarChart to $barOutputHtml")

    val barLayout = Layout("Bar Chart", "X-label", "Y-label")
    val xs = (0 to 30).map(_.toDouble)
    val ys = xs.map(i => i * i)
    val barPlot = BarPlot(xs, ys, barLayout)

    // Implicits will convert the types
    writeToHtml(barPlot, barOutputHtml)
    writeToPng(barPlot, barOutputPng, helper)


  }




  /*
  Example Demo for Testing
   */
  def demo(rootOutputPath: Path) = {

    val sx = SeleniumHelper(100)

    histogramPlotDemo(sx, rootOutputPath)

    barPlotDemo(sx, rootOutputPath)

    scatterPlotDemo(sx, rootOutputPath)

    barPlotImplicitDemo(sx, rootOutputPath)

    0
  }

}


object Example extends App {
  ExamplePlots.demo(Paths.get("extras").toAbsolutePath)
  println("Exiting Main.")
}
