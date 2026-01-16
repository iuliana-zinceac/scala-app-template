package dev.iuly.helloapp.domain

opaque type Name = String

object Name {
  def apply(name: String): Name = name
  extension (name: Name) {
    def value: String = name
  }
}

case class Greetings(
    to: Name,
    totalTimes: Int
)

case class GreetingResponse(message: String)
