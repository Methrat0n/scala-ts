inThisBuild(
  Seq(
    organization := "io.github.scala-ts",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.12.20"
  )
)

lazy val typescript = (project in file("typescript"))
  .enablePlugins(ScalatsGeneratorPlugin)
  .settings(
    name := "prelude-isolation-ts"
  )

lazy val python = (project in file("python"))
  .enablePlugins(ScalatsPythonPlugin)
  .settings(
    name := "prelude-isolation-py"
  )

lazy val root = (project in file("."))
  .settings(
    name := "prelude-isolation",
    publish := ({}),
    publishTo := None
  )
  .aggregate(typescript, python)
