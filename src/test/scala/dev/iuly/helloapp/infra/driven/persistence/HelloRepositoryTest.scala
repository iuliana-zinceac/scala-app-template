package dev.iuly.helloapp.infra.driven.persistence
import java.time.Instant

import cats.arrow.FunctionK
import cats.effect.IO
import cats.implicits.*
import dev.iuly.helloapp.domain.*
import munit.CatsEffectSuite

class HelloRepositoryTest extends CatsEffectSuite {

  test("HelloRepository should return a greeting message") {
    for {
      storage <- HelloStorageMock[IO]()
      time = Instant.parse("2024-01-01T12:00:00Z").nn
      repo = HelloRepository[IO, IO](FunctionK.id, storage)
      _      <- repo.recordGreeting(Name("John"), time)
      result <- storage.getGreetings("John")
    } yield assertEquals(result, Some(GreetingsRecord("John", 1)))
  }
}
