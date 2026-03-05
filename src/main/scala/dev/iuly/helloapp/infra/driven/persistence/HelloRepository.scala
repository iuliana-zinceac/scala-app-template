package dev.iuly.helloapp.infra.driven.persistence

import cats.effect.MonadCancelThrow
import cats.implicits.*
import cats.{MonadThrow, ~>}
import dev.iuly.helloapp.domain.*
import doobie.Transactor

object HelloRepository {

  def apply[F[_]: MonadCancelThrow](transactor: Transactor[F]): HelloRepository[F] =
    apply(transactor.trans, HelloStorage())

  def apply[F[_]: MonadThrow, G[_]: MonadThrow](
      transactor: G ~> F,
      storage: HelloStorage[G]
  ): HelloRepository[F] = {
    new HelloRepository[F] {
      override def allGreetings(name: Name): F[Greetings] = {
        val program: G[Greetings] = for {
          record <- storage.getGreetings(name.value)
        } yield record match {
          case Some(greetingsRecord) =>
            Greetings(Name(greetingsRecord.name), greetingsRecord.greetings)
          case None => Greetings(name, 0)
        }

        transactor(program).adaptError { case e =>
          new HelloError(s"Failed to fetch greetings for ${name.value}", e.some)
        }
      }

      override def recordGreeting(name: Name, time: java.time.Instant): F[Unit] = {
        val program: G[Unit] = storage.recordGreeting(name.value, time)
        transactor(program).adaptError { case e =>
          new HelloError(s"Failed to record greeting for ${name.value}", e.some)
        }
      }
    }
  }
}
