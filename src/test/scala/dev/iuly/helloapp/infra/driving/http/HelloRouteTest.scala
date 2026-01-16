package dev.iuly.helloapp.infra.driving.http

import cats.effect.IO
import dev.iuly.helloapp.domain.{HelloService, HelloServiceMock}
import dev.iuly.helloapp.infra.driving.http.{HelloRoute, Response}
import munit.CatsEffectSuite
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.implicits.*
import org.http4s.{Method, Request, Uri}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger

class HelloRouteTest extends CatsEffectSuite {
  given Logger[IO] = NoOpLogger[IO]

  test("HelloRoute should return a greeting message") {
    for {
      helloService: HelloService[IO] <- HelloServiceMock[IO]
      httpApp = HelloRoute.routes[IO](helloService).orNotFound
      request = Request[IO](Method.GET, uri"/hello/John")
      response <- httpApp.run(request)
      body     <- response.as[Response]
    } yield {
      assertEquals(response.status.code, 200)
      assertEquals(body.message, "Hello, John!")
    }
  }

}
