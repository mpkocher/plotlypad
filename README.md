# plotlypad

**Offline** Scala plotting library using [plotly.js](https://cdn.plot.ly/plotly-latest.min.js) and selenium for static plots.


**Note**: This experiment was used to evaluate plotly.js for offline plotting. While this experiment was useful, it was ultimately a dead end and no further development is planned. I believe building off of the visualization grammar of graphics library, [Vega](https://vega.github.io/vega-lite/) and the scala library, [Vegas-Viz](https://github.com/vegas-viz/Vegas) is a more principled foundation for data visualization. 

The original paper describing vega is [here](http://idl.cs.washington.edu/papers/vega-lite/).


### Run Demo

```bash
$> sbt run
```

Example HTML files and png images will be generated in `extras/`


# Ammonite REPL integration

[Ammonite](https://github.com/lihaoyi/Ammonite) is a amazing tool in the expanding Scala ecosystem.

See [expanded documentation here](http://www.lihaoyi.com/Ammonite/).
 
Get Demo working locally:
 
1. Install plotlypad locally via `sbt publish-local`
2. Install ammonite `brew install ammonite-repl`
3. Run `make repl` or `amm -f extras/Repl.scala` to get an example plot and some data
4. (Optional) For generating png images, `FireFox` must be installed 

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


