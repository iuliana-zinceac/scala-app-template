package dev.iuly.helloapp.infra.driving.http

import io.circe.Codec
import sttp.tapir.Schema

case class Response(
    message: String
)

object Response {
  given Codec[Response] = Codec.derived
  given Schema[Response] = Schema.derived[Response]
}

case class ErrorResponse(
    message: String,
    cause: String
)

object ErrorResponse {
  given Codec[ErrorResponse] = Codec.derived
  given Schema[ErrorResponse] = Schema.derived[ErrorResponse]
}
