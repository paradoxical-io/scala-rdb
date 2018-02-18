import sbt._
import sbt.Keys._

object BuildConfig {
  object versions {
    val mockito = "1.10.19"
    val scalatest = "3.0.1"
    val mySqlDriverVersion = "5.1.39"
    val h2DriverVersion = "1.4.193"

    val slickVersion = "3.2.0"
    val hikariVersion = "2.5.1"
    val squerylVersion = "0.9.8"

    val slf4j ="1.7.25"

    val paradoxGlobal = "1.1"

    val flyway = "4.0"
    val guice = "4.1.0"
    val scalaGuice = "4.1.0"
  }

  object Dependencies {
    val slick = "com.typesafe.slick" %% "slick" % versions.slickVersion
    val h2Driver = "com.h2database" % "h2" % versions.h2DriverVersion
    val mysqlDriver = "mysql" % "mysql-connector-java" % versions.mySqlDriverVersion
    val hikariCP = "com.zaxxer" % "HikariCP" % versions.hikariVersion
    val squeryl = "org.squeryl" %% "squeryl" % versions.squerylVersion

    val inject = "javax.inject" % "javax.inject" % "1"

    val jodaTime = "joda-time" % "joda-time" % "2.9.7" % "provided"
    val jodaConvert = "org.joda" % "joda-convert" % "1.8.1" % "provided"

    val jodaDeps = Seq(jodaTime, jodaConvert)

    val paradoxGlobal = "io.paradoxical" %% "paradox-scala-global" % versions.paradoxGlobal

    val h2DriverTest = h2Driver.withConfigurations(configurations = Some("test"))

    val mysqlDriverTest = mysqlDriver.withConfigurations(configurations = Some("test"))
    
    val scalatest = "org.scalatest" %% "scalatest" % "3.0.1" % "test"

    val loggingApi = "org.slf4j" % "slf4j-api" % versions.slf4j

    val slf4jSimple = "org.slf4j" % "slf4j-simple" % versions.slf4j % "test"

    val guice = "com.google.inject" % "guice" % versions.guice % "test"

    val guiceAssisted = "com.google.inject.extensions" % "guice-assistedinject" % versions.guice % "test"

    val scalaGuice = "net.codingwell" %% "scala-guice" % versions.scalaGuice % "test"

    val flywayCore = "org.flywaydb" % "flyway-core" % versions.flyway % "test"

    val dockerDeps = Seq(
      "com.google.guava" % "guava" % "21.0",
      "io.paradoxical" % "docker-client" % "1.24"
    )

    val testDeps = Seq(
      loggingApi,
      scalatest,
      mysqlDriverTest,
      h2DriverTest,
      guice,
      guiceAssisted,
      scalaGuice,
      flywayCore,
      slf4jSimple
    )
  }

  object Revision {
    lazy val version = System.getProperty("version", "1.0-SNAPSHOT")
  }

  object Testing {
    lazy val defaultSettings = Seq(
      fork in test := true,
      javaOptions in Test ++= Seq("-Djava.awt.headless=true")
    )

    lazy val itDefaultSettings = Seq(
      sourceDirectory in IntegrationTest := (sourceDirectory in Test).value,
      sourceDirectories in IntegrationTest ++= (sourceDirectories in Test).value
    )

    def includeTests(tags: String*) = tags.map(Tests.Argument("-n", _))

    def excludeTests(tags: String*) = tags.map(Tests.Argument("-l", _))
  }

  def commonSettings() = {
    Seq(
      organization := "io.paradoxical",

      version := BuildConfig.Revision.version,

      resolvers += Resolver.sonatypeRepo("releases"),

      scalaVersion := "2.12.4",

      crossScalaVersions := Seq("2.11.8", scalaVersion.value),

      scalacOptions ++= Seq(
        "-deprecation",
        "-encoding", "UTF-8",
        "-feature",
        "-language:existentials",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-language:postfixOps",
        "-language:experimental.macros",
        "-unchecked",
        "-Ywarn-nullary-unit",
        "-Xfatal-warnings",
        "-Ywarn-dead-code",
        "-Xfuture"
      ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) => Seq("-Xlint:-unused")
        case _ => Seq("-Xlint")
      }),

      scalacOptions in (Compile, doc) := scalacOptions.value.filterNot(_ == "-Xfatal-warnings"),
      scalacOptions in (Compile, doc) += "-no-java-comments"
    ) ++ Publishing.publishSettings
  }
}
