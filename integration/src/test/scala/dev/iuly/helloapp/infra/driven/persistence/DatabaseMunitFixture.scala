package dev.iuly.helloapp.infra.driven.persistence

import javax.sql.DataSource

import cats.Applicative
import cats.effect.{IO, Resource}
import cats.implicits.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.munit.fixtures.TestContainersFixtures
import com.zaxxer.hikari.HikariDataSource
import dev.iuly.helloapp.infra.driven.persistence.TestDatabaseConfig.*
import doobie.ConnectionIO
import doobie.implicits.*
import doobie.util.transactor.Transactor
import doobie.util.update.Update
import munit.catseffect.IOFixture
import munit.{AnyFixture, CatsEffectSuite, catseffect}
import org.flywaydb.core.Flyway.configure
import org.flywaydb.core.api.output.MigrateResult
import org.testcontainers.containers
import org.testcontainers.postgresql.PostgreSQLContainer as JavaPostgreSQLContainer
import org.testcontainers.utility.*

object TestDatabaseConfig {
  val DB_USERNAME = "test"
  val DB_PASSWORD = "test"
  val DATABASE_NAME = "test"
  val DB_POOL_SIZE = 2
  val DOCKER_IMAGE_NAME = "postgres:16"
}

trait DatabaseMunitFixture extends CatsEffectSuite with TestContainersFixtures {

  val postgresMunitFixture: ForAllContainerFixture[PostgreSQLContainer] = ForAllContainerFixture(
    PostgreSQLContainer(
      dockerImageNameOverride = DockerImageName.parse(DOCKER_IMAGE_NAME).nn,
      databaseName = DATABASE_NAME,
      username = DB_USERNAME,
      password = DB_PASSWORD
    )
  )

  val dbTransactor: IOFixture[Transactor[IO]] =
    ResourceSuiteLocalFixture("transactor", TestDatabase.migrate())

  def postgresContainer: JavaPostgreSQLContainer = postgresMunitFixture().container

  override def munitFixtures: Seq[AnyFixture[?]] = List(postgresMunitFixture, dbTransactor)

  def transactor: doobie.Transactor[IO] =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      postgresContainer.getJdbcUrl.nn,
      postgresContainer.getUsername.nn,
      postgresContainer.getPassword.nn,
      None
    )

  def truncateTable(table: String): IO[Int] =
    val program = Update(s"TRUNCATE TABLE $table CASCADE").run(())
    program.transact(transactor)

  def transactAndRollback[T](connIO: ConnectionIO[T]): IO[T] =
    connIO.flatTap(_ => doobie.free.connection.rollback).transact(transactor)

  def transact[T](connIO: ConnectionIO[T]): IO[T] =
    connIO.transact(transactor)

  object TestDatabase {

    def makeDataSource(jdbcUrl: String): Resource[IO, DataSource] = {
      val dataSourceIO = IO {
        val datasource = new HikariDataSource()
        datasource.setJdbcUrl(jdbcUrl)
        datasource.setUsername(DB_USERNAME)
        datasource.setPassword(DB_PASSWORD)
        datasource.setDriverClassName("org.postgresql.Driver")
        datasource.setMaximumPoolSize(DB_POOL_SIZE)
        datasource.setConnectionTimeout(0)
        datasource
      }

      Resource.make(dataSourceIO)(dataSource => IO(dataSource.close()))
    }

    def migrate(): Resource[IO, Transactor[IO]] = {
      for {
        _          <- Resource.eval(IO(postgresMunitFixture.container.start()))
        dataSource <- makeDataSource(postgresContainer.getJdbcUrl.nn)
        _          <- Resource.eval(runMigration(dataSource))
        transactor <- Resource.pure(transactor)
      } yield transactor
    }

    def runMigration(dataSource: DataSource): IO[MigrateResult] = IO {
      configure().nn
        .dataSource(dataSource)
        .nn
        .load
        .nn
        .migrate
        .nn
    }
  }
}
