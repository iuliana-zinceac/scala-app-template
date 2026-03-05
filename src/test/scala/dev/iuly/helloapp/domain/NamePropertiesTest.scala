package dev.iuly.helloapp.domain

import munit.ScalaCheckSuite
import org.scalacheck.Prop.forAll

class NamePropertiesTest extends ScalaCheckSuite {

  property("Name.value round-trips through apply") {
    forAll { (s: String) =>
      Name(s).value == s
    }
  }
}
