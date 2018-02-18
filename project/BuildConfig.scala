import sbt._
import sbt.Keys._

object BuildConfig {
  object Dependencies {
    val testDeps = Seq(
      "org.scalatest" %% "scalatest" % versions.scalatest,
      "org.mockito" % "mockito-all" % versions.mockito
    ).map(_ % "test")
  }

  object Revision {
    lazy val version = System.getProperty("version", "1.0-SNAPSHOT")
  }

  object versions {
    val mockito = "1.10.19"
    val scalatest = "3.0.1"
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
