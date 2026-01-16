package dev.iuly.helloapp.domain

import cats.Monad
import cats.effect.Clock
import cats.implicits.*
import org.typelevel.log4cats.Logger

trait HelloService[F[_]] {
  def sayHello(name: Name): F[GreetingResponse]
}

object HelloService {
  def apply[F[_]: Monad: Clock: Logger](
      repository: HelloRepository[F]
  ): HelloService[F] = {
    new HelloService[F]:
      override def sayHello(name: Name): F[GreetingResponse] = {
        for {
          time      <- Clock[F].realTimeInstant
          greetings <- repository.allGreetings(name)
          _         <- Logger[F].info(
            s"Said hello to $name at $time. Total number of greetings ${greetings.totalTimes + 1}"
          )
          _ <- repository.recordGreeting(name, time)
        } yield GreetingResponse(
          s"Hello, ${name.value}! You've been greeted ${greetings.totalTimes + 1} times."
        )
      }
  }
}
