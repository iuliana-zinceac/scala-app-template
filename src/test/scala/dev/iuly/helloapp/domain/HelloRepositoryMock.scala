package dev.iuly.helloapp.domain

import java.time.Instant

import cats.effect.{Ref, Sync}
import cats.implicits.*

trait HelloRepositoryMock[F[_]] extends HelloRepository[F] {
  def initMap(initial: Map[Name, Greetings]): F[Unit]
}

object HelloRepositoryMock {

  def apply[F[_]: Sync]: F[HelloRepositoryMock[F]] =
    Ref.of[F, Map[Name, Greetings]](Map.empty).map { ref =>
      new HelloRepositoryMock[F] {
        override def initMap(initial: Map[Name, Greetings]): F[Unit] =
          ref.set(initial)

        override def recordGreeting(name: Name, timestamp: Instant): F[Unit] =
          ref.update { greetingsMap =>
            val updatedGreetings = greetingsMap.get(name) match {
              case Some(greetings) => greetings.copy(totalTimes = greetings.totalTimes + 1)
              case None            => Greetings(name, 1)
            }
            greetingsMap.updated(name, updatedGreetings)
          }

        override def allGreetings(name: Name): F[Greetings] =
          ref.get.map { greetingsMap =>
            greetingsMap.getOrElse(name, Greetings(name, 0))
          }
      }
    }

}
