package dev.iuly.helloapp.infra.driven.persistence

import cats.effect.{Ref, Sync}
import cats.implicits.*
import dev.iuly.helloapp.domain.{Greetings, Name}

trait HelloStorageMock[F[_]] extends HelloStorage[F] {
  def seed(data: Map[String, GreetingsRecord]): F[Unit]
}

object HelloStorageMock {
  def apply[F[_]: Sync](): F[HelloStorageMock[F]] =
    Ref.of[F, Map[String, GreetingsRecord]](Map.empty).map { ref =>
      new HelloStorageMock[F] {
        override def seed(data: Map[String, GreetingsRecord]): F[Unit] = ref.set(data)
        override def getGreetings(name: String): F[Option[GreetingsRecord]] =
          ref.get.map(_.get(name))

        override def recordGreeting(name: String, time: java.time.Instant): F[Unit] =
          ref.update { greetingsMap =>
            val currentCount = greetingsMap.get(name).map(_.greetings).getOrElse(0)
            greetingsMap.updated(name, GreetingsRecord(name, currentCount + 1))
          }
      }
    }
}
