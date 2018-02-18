import BuildConfig.{Dependencies, Testing}
import sbt._

lazy val commonSettings = BuildConfig.commonSettings()

name := "paradoxical-rdb"

lazy val rdb_config = (project in file("config")).
  settings(commonSettings).
  settings(
    name := "rdb-config",
    libraryDependencies ++= Seq()
  )

lazy val `rdb-test` = (project in file("rdb-test")).
  settings(commonSettings).
  settings(
    name := "rdb-test",
    libraryDependencies ++= {
      Dependencies.testDeps ++
      Dependencies.jodaDeps ++
      Dependencies.dockerDeps :+
      Dependencies.mysqlDriver
    },
    testOptions in Test ++= Testing.excludeTests("tag.Integration", "tag.RequiresInternet"),
    testOptions in IntegrationTest ++= Testing.includeTests("tag.Integration", "tag.RequiresInternet"),
    fork in test := true,
    javaOptions in test ++= Seq("-Dlog.jdbc.bench=debug")
  ).
  dependsOn(slick, hikari).
  settings(Testing.defaultSettings ++ Testing.itDefaultSettings).
  configs(IntegrationTest)

lazy val slick = (project in file("slick")).
  settings(commonSettings).
  settings(
    name := "slick",
    libraryDependencies ++= Seq(
      Dependencies.slick,
      Dependencies.h2Driver,
      Dependencies.mysqlDriver,
      Dependencies.inject,
      Dependencies.paradoxGlobal
    ) ++ Dependencies.jodaDeps
  ).dependsOn(hikari, rdb_config)

lazy val hikari = (project in file("hikari")).
  settings(commonSettings).
  settings(
    name := "hikari",
    libraryDependencies ++= Seq(
      Dependencies.hikariCP
    ) ++ BuildConfig.Dependencies.testDeps
  ).dependsOn(rdb_config)

lazy val root = (project in file(".")).
  settings(commonSettings).
  settings(
    aggregate in update := false
  ).
  aggregate(
    slick,
    hikari,
    `rdb-test`,
    rdb_config
  )


lazy val showVersion = taskKey[Unit]("Show version")

showVersion := {
  println(version.value)
}

// custom alias to hook in any other custom commands
addCommandAlias("build", "; compile")
