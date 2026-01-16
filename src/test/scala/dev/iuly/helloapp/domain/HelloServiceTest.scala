package dev.iuly.helloapp.domain

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
}
