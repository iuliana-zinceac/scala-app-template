import Versions.*
import sbt.*

object Versions {
  val catsEffect = "3.6.3"
  val pureconfig = "0.17.10"
  val log4cats = "2.8.0"
  val logback = "1.5.32"
  val logbackEncoder = "9.0"
  val http4s = "0.23.33"
  val tapir = "1.13.14"
  val flyway = "11.20.3"
  val doobie = "1.0.0-RC12"
  val postgres = "42.7.10"

  val gatling = "3.14.9"
  val testContainers = "0.44.1"
  val scalaMock = "7.5.3"
  val munit = "1.2.4"
  val munitCatsEffect = "2.1.0"
  val scalaCheckEffect = "2.0.0-M2"
}

object Dependencies {

  object core {
    val main: Seq[ModuleID] = Seq(
      "org.typelevel" %% "cats-effect" % catsEffect
    )

    val test: Seq[ModuleID] = Seq(
      "org.scalameta" %% "munit"                   % munit            % Test,
      "org.typelevel" %% "munit-cats-effect"       % munitCatsEffect  % Test,
      "org.typelevel" %% "scalacheck-effect"       % scalaCheckEffect % Test,
      "org.typelevel" %% "scalacheck-effect-munit" % scalaCheckEffect % Test,
      // only stubs are supported
      "org.scalamock" %% "scalamock"             % scalaMock % Test,
      "org.scalamock" %% "scalamock-cats-effect" % scalaMock % Test
    )

    val all: Seq[ModuleID] = main ++ test
  }

  val config: Seq[ModuleID] = Seq(
    "com.github.pureconfig" %% "pureconfig-core"        % pureconfig,
    "com.github.pureconfig" %% "pureconfig-cats-effect" % pureconfig
  )

  val logging: Seq[ModuleID] = Seq(
    "org.typelevel"       %% "log4cats-slf4j"           % log4cats,
    "ch.qos.logback"       % "logback-classic"          % logback % Runtime,
    "net.logstash.logback" % "logstash-logback-encoder" % logbackEncoder
  )

  object http {
    val main: Seq[ModuleID] = Seq(
      "org.http4s"                    %% "http4s-core"         % http4s,
      "org.http4s"                    %% "http4s-dsl"          % http4s,
      "org.http4s"                    %% "http4s-client"       % http4s,
      "org.http4s"                    %% "http4s-circe"        % http4s,
      "org.http4s"                    %% "http4s-ember-server" % http4s,
      "com.softwaremill.sttp.tapir"   %% "tapir-core"          % tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-http4s-server" % tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"    % tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-openapi-docs"  % tapir,
      "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui"    % tapir,
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml"  % "0.11.10"
    )

    val all: Seq[ModuleID] = main
  }

  object db {
    val main: Seq[ModuleID] = Seq(
      "org.flywaydb"   % "flyway-core"                % flyway,
      "org.flywaydb"   % "flyway-database-postgresql" % flyway,
      "org.tpolecat"  %% "doobie-hikari"              % doobie,
      "org.tpolecat"  %% "doobie-core"                % doobie,
      "org.tpolecat"  %% "doobie-postgres"            % doobie,
      "org.postgresql" % "postgresql"                 % postgres
    )

    val all: Seq[ModuleID] = main
  }

  val integration: Seq[ModuleID] = Seq(
    "com.dimafeng" %% "testcontainers-scala-munit"      % testContainers % Test,
    "com.dimafeng" %% "testcontainers-scala-postgresql" % testContainers % Test,
    "org.tpolecat" %% "doobie-munit"                    % doobie         % Test
  )

  val performance: Seq[ModuleID] = Seq(
    "io.gatling.highcharts" % "gatling-charts-highcharts" % gatling    % Test,
    "io.gatling"            % "gatling-test-framework"    % gatling    % Test,
    "org.scalacheck"       %% "scalacheck"                % "1.17.0"   % Test,
    "org.typelevel"        %% "cats-effect"               % catsEffect % Test
  )
}
