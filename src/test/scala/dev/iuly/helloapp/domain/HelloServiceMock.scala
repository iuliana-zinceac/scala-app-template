package dev.iuly.helloapp.domain

import java.time.Instant

import cats.effect.{Ref, Sync}
import cats.implicits.*
import dev.iuly.helloapp.domain.{Greetings, Name}

trait HelloServiceMock[F[_]] extends HelloService[F] {

  def getGreetings(name: Name): F[Greetings]
}

object HelloServiceMock {
  def apply[F[_]: Sync]: F[HelloServiceMock[F]] =
    Ref.of[F, Map[Name, Greetings]](Map.empty).map { ref =>
      new HelloServiceMock[F] {
        override def sayHello(name: Name): F[GreetingResponse] =
          ref
            .update { greetingsMap =>
              val updatedGreetings = greetingsMap.get(name) match {
                case Some(greetings) => greetings.copy(totalTimes = greetings.totalTimes + 1)
                case None            => Greetings(name, 1)
              }
              greetingsMap.updated(name, updatedGreetings)
            }
            .as(GreetingResponse(s"Hello, ${name.value}!"))

        override def getGreetings(name: Name): F[Greetings] =
          ref.get.map(_.getOrElse(name, Greetings(name, 0)))
      }
    }
}
