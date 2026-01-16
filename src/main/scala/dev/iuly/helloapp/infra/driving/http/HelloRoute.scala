package dev.iuly.helloapp.infra.driving.http

import cats.effect.Async
import cats.implicits.*
import dev.iuly.helloapp.domain.{HelloService, Name}
import dev.iuly.helloapp.infra.driving.http.HelloEndpoint.sayHelloEndpoint
import org.http4s.HttpRoutes
import org.typelevel.log4cats.Logger
import sttp.tapir.server.http4s.Http4sServerInterpreter

object HelloRoute {

  def routes[F[_]: Async: Logger](
      helloService: HelloService[F]
  ): HttpRoutes[F] =
    Http4sServerInterpreter[F]()
      .toRoutes(sayHelloEndpoint.serverLogic { name =>
        helloService
          .sayHello(Name(name))
          .map(response => Right(Response(response.message)))
          .handleErrorWith { error =>
            Logger[F].error(error)(s"Error occurred in sayHello endpoint") *>
            Left(ErrorResponse("Can not say hello", error.getMessage.nn)).pure[F]
          }
      })

}
