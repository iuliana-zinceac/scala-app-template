package dev.iuly.main

import cats.effect.{Async, Resource}
import cats.syntax.all.*
import com.comcast.ip4s.*
import doobie.Transactor
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import dev.iuly.helloapp.domain.*
import dev.iuly.helloapp.infra.driven.persistence.HelloRepository as HelloRepositoryImpl
import dev.iuly.helloapp.infra.driving.http.{HelloEndpoint, HelloRoute}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.SwaggerUI
import sttp.apispec.openapi.circe.yaml.*

trait AppModule[F[_]] {
  def wireUp(): Resource[F, Unit]
}

object AppModule {

  def apply[F[_]: Async: Logger](config: Config, dbTransactor: Transactor[F]): AppModule[F] =
    new AppModule[F]:
      override def wireUp(): Resource[F, Unit] =
        val helloRepository = HelloRepositoryImpl[F](dbTransactor)
        val helloService    = HelloService[F](helloRepository)

        // API routes from Tapir endpoint + server logic
        val apiRoutes = HelloRoute.routes[F](helloService)

        // Swagger UI routes from Tapir endpoint definition
        val openApiDocs = OpenAPIDocsInterpreter().toOpenAPI(
          HelloEndpoint.sayHelloEndpoint,
          config.serviceName,
          "1.0"
        )
        val swaggerRoutes = Http4sServerInterpreter[F]().toRoutes(
          SwaggerUI[F](openApiDocs.toYaml)
        )

        val httpApp = Router("/" -> (apiRoutes <+> swaggerRoutes)).orNotFound

        for
          logger <- Resource.eval(Slf4jLogger.create[F])
          _      <- Resource.eval(logger.info("Starting server..."))
          _ <- EmberServerBuilder
            .default[F]
            .withHost(host"0.0.0.0")
            .withPort(Port.fromInt(config.serverPort).getOrElse(port"8080"))
            .withHttpApp(httpApp)
            .build
          _ <- Resource.eval(logger.info(s"Server started on port ${config.serverPort}"))
        yield ()
}
