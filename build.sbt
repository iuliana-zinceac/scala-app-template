ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.7"

lazy val root = (project in file("."))
  .settings(
    name := "scala-app-template",
    resolvers ++= dependencyResolvers,
    libraryDependencies ++= dependencies,
    scalacOptions ++= scalaOptions,
    semanticdbEnabled := true // for scalafix
  )

lazy val integration = (project in file("integration"))
  .dependsOn(root % "compile->compile;test->test")
  .settings(
    libraryDependencies ++= Dependencies.integration,
    scalacOptions ++= scalaOptions,
    semanticdbEnabled := true, // for scalafix
    Test / unmanagedResourceDirectories += (root / Compile / resourceDirectory).value
  )

lazy val performance = (project in file("performance"))
  .enablePlugins(GatlingPlugin)
  .settings(
    scalaVersion := "2.13.16",
    libraryDependencies ++= Dependencies.performance
  )

addCommandAlias("fmt", ";scalafmtAll ;scalafmtSbt ;scalafixAll")
addCommandAlias("fmtCheck", ";scalafmtCheckAll; scalafmtSbtCheck ;scalafixAll --check ")
addCommandAlias("ci", "clean; fmtCheck; test")

lazy val scalaOptions = Seq(
  "-Yexplicit-nulls",
  "-Ywarn-unused-import",
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
