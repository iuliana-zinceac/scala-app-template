package dev.iuly.helloapp.domain

case class HelloError(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.orNull)
