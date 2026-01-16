package dev.iuly.helloapp.infra.driven.persistence.config

import javax.sql.DataSource

import cats.effect.{Async, Resource}
import com.zaxxer.hikari.HikariDataSource
import dev.iuly.helloapp.infra.driven.persistence.config.Config
import doobie.Transactor
import doobie.util.ExecutionContexts
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

object Database {

  def apply[F[_]: Async](config: Config): Resource[F, Transactor[F]] = {
    for
      ec         <- ExecutionContexts.fixedThreadPool(config.numThreads)
      dataSource <- initDataSource(config)
      _          <- Resource.eval(migrate(dataSource))
    yield Transactor.fromDataSource[F](dataSource, ec)
  }

  private def initDataSource[F[_]: Async](config: Config): Resource[F, DataSource] = {
    val init = Async[F].delay {
      val datasource = new HikariDataSource()
      datasource.setDriverClassName("org.postgresql.Driver")
      datasource.setJdbcUrl(s"jdbc:postgresql://${config.host}:${config.port}/${config.database}")
      datasource.setUsername(config.username)
      datasource.setPassword(config.password)
      datasource
    }

    Resource.make(init)(db => Async[F].delay(db.close().nn))
  }

  private def migrate[F[_]: Async](dataSource: DataSource): F[MigrateResult] =
    Async[F].delay(
      Flyway
        .configure()
        .nn
        .dataSource(dataSource)
        .nn
        .load
        .nn
        .migrate
        .nn
    )
}
