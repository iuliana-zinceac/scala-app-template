ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.7"

lazy val root = (project in file("."))
  .settings(
    name := "scala-app-template",
    resolvers ++= dependencyResolvers,
    libraryDependencies ++= dependencies,
    scalacOptions ++= scalaOptions,
    semanticdbEnabled := true, // for scalafix
    addCommandAlias("fmt", "scalafmtAll; scalafmtSbt"),
    addCommandAlias("fmtCheck", "scalafmtCheckAll; r"),
    addCommandAlias("ci", "clean; fmtCheck; test")
  )

lazy val scalaOptions = Seq(
  "-Yexplicit-nulls",
  "-Wconf:msg=unused:info"
)

lazy val dependencyResolvers = Seq(
  "Confluent Platform" at "https://packages.confluent.io/maven/"
)

lazy val dependencies =
  Dependencies.core.all ++
    Dependencies.config ++
    Dependencies.logging ++
    Dependencies.http.main ++
    Dependencies.db.all
