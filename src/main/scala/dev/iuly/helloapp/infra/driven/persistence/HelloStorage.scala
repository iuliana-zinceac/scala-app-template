package dev.iuly.helloapp.infra.driven.persistence

import java.time.Instant

import cats.implicits.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.{ConnectionIO, *}

trait HelloStorage[F[_]] {

  def getGreetings(name: String): F[Option[GreetingsRecord]]

  def recordGreeting(name: String, time: Instant): F[Unit]
}

object HelloStorage {

  def apply(): HelloStorage[ConnectionIO] = {
    new HelloStorage[ConnectionIO] {
      override def getGreetings(name: String): ConnectionIO[Option[GreetingsRecord]] =
        sql"SELECT name, greetings FROM greetings WHERE name = $name"
          .query[GreetingsRecord]
          .option

      override def recordGreeting(name: String, time: Instant): ConnectionIO[Unit] =
        sql"""
            INSERT INTO greetings (name, greetings) VALUES ($name, 1)
            ON CONFLICT (name) DO UPDATE SET greetings = greetings.greetings + 1""".update.run.void
    }
  }
}
