name := "plotlypad"

version := "0.1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scalanlp" % "breeze_2.11" % "0.12",
  "co.theasi" % "plotly_2.11" % "0.1",
  "org.seleniumhq.selenium" % "selenium-java" % "2.53.0",
  "io.spray" % "spray-json_2.11" % "1.3.2",
  "com.lihaoyi" % "scalatags_2.11" % "0.5.5",
  "com.lihaoyi" % "ammonite-repl" % "0.6.2" % "test" cross CrossVersion.full
)

initialCommands in (Test, console) := """ammonite.repl.Main().run()""".stripMargin

mainClass in (Compile, run) := Some("com.github.mpkocher.plotlypad.Example")


resolvers ++= Seq(
  // other resolvers here
  // for breeze
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)