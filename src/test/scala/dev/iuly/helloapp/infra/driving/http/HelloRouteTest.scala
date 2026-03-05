package dev.iuly.helloapp.infra.driving.http

import cats.effect.IO
import dev.iuly.helloapp.domain.{GreetingResponse, HelloError, HelloService, HelloServiceMock, Name}
import dev.iuly.helloapp.infra.driving.http.{ErrorResponse, HelloRoute, Response}
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
      assertEquals(obtained = response.status.code, expected = 200)
      assertEquals(obtained = body.message, expected = "Hello, John!")
    }
  }

  test("HelloRoute should return 500 with error body when service fails") {
    val failingService = new HelloService[IO] {
      def sayHello(name: Name): IO[GreetingResponse] =
        IO.raiseError(HelloError("something went wrong"))
    }
    val httpApp = HelloRoute.routes[IO](failingService).orNotFound
    val request = Request[IO](Method.GET, uri"/hello/John")

    for {
      response <- httpApp.run(request)
      body     <- response.as[ErrorResponse]
    } yield {
      assertEquals(obtained = response.status.code, expected = 500)
      assertEquals(obtained = body.message, expected = "Can not say hello")
      assertEquals(obtained = body.cause, expected = "something went wrong")
    }
  }
}
