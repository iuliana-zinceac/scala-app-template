package dev.iuly.helloapp.infra.driven.persistence.config

import pureconfig.ConfigReader

case class Config(
    host: String,
    port: Int,
    database: String,
    username: String,
    password: String,
    numThreads: Int
) derives ConfigReader
