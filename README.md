# Scala App Template

A Scala 3 application template with batteries included. Ships with a working example app (`helloapp`) that exercises every library in the stack, so you can verify the setup works end-to-end before building your own features on top.

## What's Inside

**Core stack:**
- **Scala 3.3.7** with strict compiler options (`-Yexplicit-nulls`, unused import warnings)
- **Cats Effect 3** for purely functional IO
- **Http4s + Ember** HTTP server
- **Tapir** for type-safe endpoint definitions with auto-generated OpenAPI/Swagger docs
- **Doobie** for database access with **HikariCP** connection pooling
- **Flyway** for database migrations
- **PureConfig** for type-safe configuration loading
- **Log4cats + Logback** with JSON structured logging (logstash encoder)

**Build tooling:**
- **SBT 1.12** with multi-project build
- **Scalafmt** for formatting, **Scalafix** for linting (organized imports, `DisableSyntax` rules)
- **direnv** for environment variable management

**Testing:**
- **MUnit + Cats Effect** for unit tests
- **ScalaMock** for mocking
- **TestContainers** with PostgreSQL for integration tests (separate subproject)
- **Gatling** for performance tests (separate subproject, Scala 2.13)

## Project Structure

```
scala-app-template/
|-- src/main/scala/dev/iuly/
|   |-- main/
|   |   |-- Main.scala              # Entry point (IOApp)
|   |   |-- Config.scala            # Configuration loader
|   |   +-- AppModule.scala         # DI container & HTTP server wiring
|   |
|   +-- helloapp/                   # Example application
|       |-- domain/                 # Business logic (no framework deps)
|       |   |-- models.scala        # Name (opaque type), Greetings, GreetingResponse
|       |   |-- HelloRepository.scala   # Repository trait (port)
|       |   |-- HelloService.scala      # Service implementation
|       |   +-- errors.scala
|       +-- infra/
|           |-- driven/persistence/ # Database adapter
|           |   |-- config/
|           |   |   |-- Config.scala    # DB config case class
|           |   |   +-- Database.scala  # HikariCP + Flyway init
|           |   |-- HelloRepository.scala   # Repository impl (adapter)
|           |   |-- HelloStorage.scala      # Doobie SQL queries
|           |   +-- entities.scala          # DB record types
|           +-- driving/http/       # HTTP adapter
|               |-- HelloEndpoint.scala     # Tapir endpoint definition
|               |-- HelloRoute.scala        # Route handler with error mapping
|               +-- entities.scala          # HTTP response types
|
|-- src/test/                       # Unit tests
|-- integration/src/test/           # Integration tests (TestContainers + PostgreSQL)
+-- performance/src/test/           # Gatling load tests
```

## Architecture

The template follows **hexagonal architecture** (ports & adapters):

```
         HTTP Request
              |
     [Driving Adapter]        HelloEndpoint (Tapir) + HelloRoute (Http4s)
              |
        [Domain Port]         HelloService -> HelloRepository trait
              |
     [Driven Adapter]         HelloRepository impl -> HelloStorage (Doobie)
              |
          PostgreSQL
```

**`Main`** is the composition root -- it loads config, initializes the database (with Flyway migrations), and delegates to **`AppModule`** which acts as a DI container: it wires all dependencies together and starts the HTTP server.

## Getting Started

### Prerequisites

- JDK 21+
- SBT 1.12+
- Docker (for PostgreSQL)
- [direnv](https://direnv.net/) (optional, for env vars)

### Setup

1. Copy the environment template and allow direnv:
   ```bash
   cp .envrc.example .envrc
   direnv allow
   ```

2. Start PostgreSQL:
   ```bash
   docker compose up -d
   ```

3. Run the application:
   ```bash
   sbt run
   ```

4. Try it out:
   ```bash
   curl http://localhost:8080/hello/World
   ```
   Swagger UI is available at `http://localhost:8080/docs`.

## SBT Commands

| Command | Description |
|---|---|
| `sbt run` | Start the application |
| `sbt test` | Run unit tests |
| `sbt integration/test` | Run integration tests (requires Docker) |
| `sbt Gatling/test` | Run performance tests |
| `sbt fmt` | Format code + organize imports |
| `sbt fmtCheck` | Check formatting (used in CI) |
| `sbt ci` | Full CI pipeline: clean, format check, test |

## Configuration

Configuration is loaded from `src/main/resources/application.conf` using PureConfig. Database settings are read from environment variables:

| Variable | Default | Description |
|---|---|---|
| `DATABASE_HOST` | `localhost` | PostgreSQL host |
| `DATABASE_PORT` | `5432` | PostgreSQL port |
| `DATABASE_USER` | `postgres` | Database user |
| `DATABASE_PASSWORD` | `postgres` | Database password |
| `DATABASE_NAME` | `helloapp` | Database name |
| `DATABASE_THREADS_NUM` | `4` | Connection pool size |

## Using as a Template

1. Replace the `helloapp` package with your own domain
2. Add new Tapir endpoints alongside `HelloEndpoint`
3. Add new repositories following the `HelloRepository` pattern (trait in domain, impl in infra)
4. Wire new dependencies in `AppModule`
5. Add Flyway migrations under `src/main/resources/db/migration/`
