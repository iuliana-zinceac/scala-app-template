package dev.iuly.main

import cats.effect.Sync
import dev.iuly.helloapp.infra.driven.persistence.config.Config as DbConfig
import pureconfig.{ConfigReader, ConfigSource}

case class Config(
    serviceName: String,
    serverPort: Int,
    db: DbConfig
) derives ConfigReader

object Config {

  def load[F[_]: Sync, A: ConfigReader]: F[A] =
    Sync[F].fromEither(ConfigSource.default.load[A].left.map(err => new RuntimeException(err.toString)))
}
