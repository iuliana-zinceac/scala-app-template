package dev.iuly.helloapp.infra.driving.http

import sttp.model.StatusCode.*
import sttp.tapir.*
import sttp.tapir.json.circe.*

object HelloEndpoint {

  val sayHelloEndpoint: PublicEndpoint[String, ErrorResponse, Response, Any] =
    endpoint.get
      .in("hello")
      .in(path[String]("name"))
      .out(statusCode(Ok))
      .out(customCodecJsonBody[Response])
      .errorOut(statusCode(InternalServerError))
      .errorOut {
        customCodecJsonBody[ErrorResponse]
      }
      .tag("Say Hello endpoint")
}
