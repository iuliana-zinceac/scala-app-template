package dev.iuly.helloapp.domain

import java.time.Instant

trait HelloRepository[F[_]] {
  def allGreetings(to: Name): F[Greetings]
  def recordGreeting(to: Name, at: Instant): F[Unit]
}
