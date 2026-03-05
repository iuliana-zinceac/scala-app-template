package dev.iuly.helloapp.infra.driven.persistence

import java.time.Instant

import cats.arrow.FunctionK
import cats.effect.IO
import cats.implicits.*
import dev.iuly.helloapp.domain.*
import munit.CatsEffectSuite

class HelloRepositoryTest extends CatsEffectSuite {

  test("recordGreeting should persist a greeting record in storage") {
    for {
      storage <- HelloStorageMock[IO]()
      time = Instant.parse("2024-01-01T12:00:00Z").nn
      repo = HelloRepository[IO, IO](FunctionK.id, storage)
      _      <- repo.recordGreeting(Name("John"), time)
      result <- storage.getGreetings("John")
    } yield assertEquals(obtained = result, expected = Some(GreetingsRecord("John", 1)))
  }

  test("allGreetings should return Greetings when a storage record exists") {
    for {
      storage <- HelloStorageMock[IO]()
      _       <- storage.seed(Map("Alice" -> GreetingsRecord("Alice", 5)))
      repo = HelloRepository[IO, IO](FunctionK.id, storage)
      result <- repo.allGreetings(Name("Alice"))
    } yield assertEquals(obtained = result, expected = Greetings(Name("Alice"), 5))
  }

  test("allGreetings should return zero greetings when no storage record exists") {
    for {
      storage <- HelloStorageMock[IO]()
      repo = HelloRepository[IO, IO](FunctionK.id, storage)
      result <- repo.allGreetings(Name("NewUser"))
    } yield assertEquals(obtained = result, expected = Greetings(Name("NewUser"), 0))
  }

  test("allGreetings should wrap storage error in HelloError") {
    val failingStorage = new HelloStorage[IO] {
      def getGreetings(name: String): IO[Option[GreetingsRecord]] =
        IO.raiseError(new RuntimeException("db down"))
      def recordGreeting(name: String, time: Instant): IO[Unit] = IO.unit
    }
    val repo = HelloRepository[IO, IO](FunctionK.id, failingStorage)

    interceptIO[HelloError] {
      repo.allGreetings(Name("Alice"))
    }
  }

  test("recordGreeting should wrap storage error in HelloError") {
    val failingStorage = new HelloStorage[IO] {
      def getGreetings(name: String): IO[Option[GreetingsRecord]] = IO.none
      def recordGreeting(name: String, time: Instant): IO[Unit] =
        IO.raiseError(new RuntimeException("db down"))
    }
    val repo = HelloRepository[IO, IO](FunctionK.id, failingStorage)
    val time = Instant.parse("2024-01-01T12:00:00Z").nn

    interceptIO[HelloError] {
      repo.recordGreeting(Name("Alice"), time)
    }
  }
}
