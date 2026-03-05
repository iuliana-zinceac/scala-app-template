package dev.iuly.helloapp.domain

import java.time.Instant

import cats.effect.IO
import munit.CatsEffectSuite
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger

class HelloServiceTest extends CatsEffectSuite {

  given Logger[IO] = NoOpLogger[IO]

  test("sayHello should record greeting in repository") {
    val name = Name("Alice")
    val initialGreetings = Greetings(name, 2)

    for {
      helloRepository <- HelloRepositoryMock[IO]
      _               <- helloRepository.initMap(Map(name -> initialGreetings))
      helloService = HelloService[IO](helloRepository)
      greetingResponse <- helloService.sayHello(name)
      result           <- helloRepository.allGreetings(name)
    } yield {
      assertEquals(
        obtained = result,
        expected = Greetings(name, initialGreetings.totalTimes + 1)
      )
      assertEquals(
        obtained = greetingResponse.message,
        expected = s"Hello, ${name.value}! You've been greeted ${initialGreetings.totalTimes + 1} times."
      )
    }
  }

  test("sayHello should greet a first-time user with count 1") {
    val name = Name("Bob")

    for {
      helloRepository <- HelloRepositoryMock[IO]
      helloService = HelloService[IO](helloRepository)
      greetingResponse <- helloService.sayHello(name)
      result           <- helloRepository.allGreetings(name)
    } yield {
      assertEquals(obtained = result, expected = Greetings(name, 1))
      assertEquals(
        obtained = greetingResponse.message,
        expected = s"Hello, ${name.value}! You've been greeted 1 times."
      )
    }
  }

  test("sayHello should propagate error raised by repository") {
    val name = Name("Charlie")
    val failingRepo = new HelloRepository[IO] {
      def allGreetings(to: Name): IO[Greetings] = IO.raiseError(HelloError("db down"))
      def recordGreeting(to: Name, at: Instant): IO[Unit] = IO.unit
    }
    val helloService = HelloService[IO](failingRepo)

    interceptMessageIO[HelloError]("db down") {
      helloService.sayHello(name)
    }
  }
}
