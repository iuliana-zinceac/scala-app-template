package dev.iuly.helloapp.infra.driven.persistence

import java.time.Instant

import munit.CatsEffectSuite

class HelloRepositoryIT extends CatsEffectSuite with DatabaseMunitFixture {

  test("Check that HelloRepository correctly interacts with the database") {
    val time = Instant.parse("2024-01-01T12:00:00Z").nn
    val storage = HelloStorage()

    transactAndRollback {
      for {
        _     <- storage.recordGreeting("John", time)
        count <- storage.getGreetings("John")
      } yield {       
        assertEquals(obtained = count, expected = Some(GreetingsRecord("John", 1)))
      }
    }
  }
}
