package dev.iuly.helloapp

import org.scalacheck.Gen

object DataGenerator {

  def generateName: Gen[String] = for {
    firstName <- Gen.oneOf("Alice", "Bob", "Charlie", "Diana", "Eve")
    lastName  <- Gen.oneOf("Smith", "Johnson", "Williams", "Brown", "Jones")
  } yield s"$firstName $lastName"

}
