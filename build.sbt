import sbt._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.10"
ThisBuild / organization := "com.github.moust"

ThisBuild / scalafixDependencies += Dependencies.organizeImports

lazy val root = (project in file("."))
  .settings(
    name := "kafka-example",
    testFrameworks += new TestFramework("munit.Framework"),
    Dependencies.compileDependencies,
    Dependencies.testDependencies,
  )

addCommandAlias(
  "lint",
  "scalafmt ; Test / scalafmt ; scalafixAll --rules OrganizeImports",
)

addCommandAlias(
  "lintCheck",
  "scalafmtCheck ; Test / scalafmtCheck",
)