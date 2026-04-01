package dev.iuly.helloapp

import cats.effect.IO
import cats.effect.std.Env

object TestConfig {

  val baseUrl: IO[String] =
    Env[IO]
      .get("APP_BASE_URL")
      .map(_.getOrElse("http://localhost:8080"))

}
