package dev.iuly.main

import cats.effect.{ExitCode, IO, IOApp, Resource}
import dev.iuly.helloapp.infra.driven.persistence.config.Database
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val program =
      for {
        config           <- Resource.eval(Config.load[IO, Config])
        dbTransactor     <- Database[IO](config.db)
        given Logger[IO] <- Resource.eval(Slf4jLogger.create[IO])
        service          <- AppModule[IO](config, dbTransactor).wireUp()
      } yield service

    program.useForever.as(ExitCode.Success)
  }
}
