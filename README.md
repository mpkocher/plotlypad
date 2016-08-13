# plotlypad

Offline Scala plotting library using [plotly.js](https://cdn.plot.ly/plotly-latest.min.js) and selenium for static plots.


### Run Demo

```bash
$> sbt run
```

HTML files and png images will be generated in `extras/`


### Ammonite REPL integration

Install Ammnoite `brew install ammonite-repl`

Example/Demo:

```bash
$> /usr/local/bin/amm -f extras/Repl.scala
```

Running `showPlot(plot)` in the repl will open the plot in the default browser.

Example Predef.scala file for Ammonite

```scala
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

// Only required for saving png(s)
val helper = new SeleniumHelper(100)

// Define Test data for demo

val xs = (0 to 10).map(_.toDouble)
val ys = xs.map(i => 2 * i * i)
val basicLayout = Layout("Ammonite Demo", "X-label", "Y-Label")

val sp = ScatterPlot(xs, ys, basicLayout)

val px = Paths.get("repl-test.html").toAbsolutePath

//writeToHtml(sp, px)
//println(s"Wrote demo scatter plot to $px")

// Write to Png requires Selenium Driver
//writeToPng(sp, px, helper)

// Open up the Html page in the default browsers
// val outputHtml = showPlot(sp)

println("Finished loading. Run `showPlot(sp)` to open up an example plot in the default browser")

```


