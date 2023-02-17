import sbt._
import sbt.Keys._

object Versions {
  val circe = "0.14.4"
  val enumeratum = "1.7.2"
  val kafka = "3.4.0"
  val logback = "1.4.5"
  val logbackContrib = "0.1.5"
  val munit = "0.7.29"
}

object Dependencies {

  lazy val compileDependencies = Seq(
    resolvers += "Confluent" at "https://packages.confluent.io/maven/",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % Versions.logback % Runtime,
      "ch.qos.logback.contrib" % "logback-jackson" % Versions.logbackContrib,
      "ch.qos.logback.contrib" % "logback-json-classic" % Versions.logbackContrib,
      "com.beachape" %% "enumeratum" % Versions.enumeratum,
      "com.beachape" %% "enumeratum-circe" % Versions.enumeratum,
      "org.apache.kafka" % "kafka-clients" % Versions.kafka,
      "org.apache.kafka" % "kafka-streams" % Versions.kafka,
      "org.apache.kafka" %% "kafka-streams-scala" % Versions.kafka,
      "io.circe" %% "circe-core" % Versions.circe,
      "io.circe" %% "circe-generic" % Versions.circe,
      "io.circe" %% "circe-parser" % Versions.circe,
    )
  )

  lazy val testDependencies = Seq(
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.15.1" % Test,
      "org.scalameta" %% "munit" % Versions.munit % Test,
      // "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test,
      "org.scalameta" %% "munit-scalacheck" % Versions.munit % Test,
    )
  )

  // Scalafix rules
  lazy val organizeImports = "com.github.liancheng" %% "organize-imports" % "0.6.0"

}
