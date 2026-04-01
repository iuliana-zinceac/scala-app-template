package dev.iuly.helloapp

import scala.concurrent.duration._

import cats.effect.unsafe.implicits.{global => catsRuntime}
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class HelloSimulation extends Simulation {

  def httpProtocol(baseUrl: String) =
    http
      .baseUrl(baseUrl)
      .acceptHeader("application/json")

  private val dataFeeder = Iterator
    .continually(DataGenerator.generateName.sample.getOrElse("Unknown"))
    .map(name => Map("name" -> name))

  private val sayHelloScenario =
    scenario("Say Hello")
      .feed(dataFeeder)
      .exec(
        http("GET /hello/{name}")
          .get(session => s"/hello/${session("name").as[String]}")
          .check(status.is(200))
      )

  val simulationSetupo = for {
    baseUrl <- TestConfig.baseUrl
    protocol = httpProtocol(baseUrl)
    _ = println(s"Running simulation against base URL: $baseUrl")
  } yield {
    setUp(
      sayHelloScenario
        .inject(
          rampUsersPerSec(1).to(50).during(30.seconds),
          constantUsersPerSec(50).during(60.seconds)
        )
    ).protocols(protocol)
      .assertions(
        global.responseTime.percentile(95).lt(500),
        global.responseTime.mean.lt(200),
        global.successfulRequests.percent.gt(99)
      )
  }

  simulationSetupo.unsafeRunSync()
}
