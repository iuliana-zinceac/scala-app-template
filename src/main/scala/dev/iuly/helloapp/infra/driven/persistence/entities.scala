package dev.iuly.helloapp.infra.driven.persistence

import doobie.postgres.implicits.*
import doobie.{Meta, *}

case class GreetingsRecord(
    name: String,
    greetings: Int
)

object GreetingsRecord {

  given Write[GreetingsRecord] = Write[(String, Int)].contramap { record =>
    (record.name, record.greetings)
  }

  given Read[GreetingsRecord] = Read[(String, Int)].map { case (name, greetings) =>
    GreetingsRecord(name, greetings)
  }
}
