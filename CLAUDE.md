# Claude Instructions for scala-app-template

## Project Stack

Scala 3.3, Cats Effect 3, Http4s + Tapir, Doobie, PostgreSQL, MUnit + ScalaMock + ScalaCheck.
Hexagonal architecture: domain (pure), driven adapters (persistence), driving adapters (HTTP).

## Test Generation Rules

### Test framework — always MUnit + Cats Effect

```scala
class FooTest extends CatsEffectSuite {
  test("description of what it does, not what it is") {
    for {
      ...
    } yield assertEquals(obtained = actual, expected = expected)
  }
}
```

- Extend `CatsEffectSuite`, never plain `FunSuite`
- Use `for` comprehensions that return `IO[Assertions]`, not `assert()` calls
- Use named args in `assertEquals(obtained = ..., expected = ...)` — always in that order
- Test names describe behavior: "should record greeting in repository", not "test sayHello"

### Mocks — hand-rolled with Cats Effect Ref

Do NOT use ScalaMock for repository/service mocks. Use in-memory `Ref`-based implementations:

```scala
trait FooRepositoryMock[F[_]] extends FooRepository[F] {
  def seed(data: Map[Key, Value]): F[Unit]  // test helper for setup
}

object FooRepositoryMock {
  def apply[F[_]: Sync]: F[FooRepositoryMock[F]] =
    Ref.of[F, Map[Key, Value]](Map.empty).map { ref =>
      new FooRepositoryMock[F] {
        override def seed(data: Map[Key, Value]): F[Unit] = ref.set(data)
        override def find(key: Key): F[Option[Value]] = ref.get.map(_.get(key))
        // ... other trait methods
      }
    }
}
```

- Mock is a `trait` that extends the real trait and adds test helpers (`seed`, `initMap`, etc.)
- Companion `object` builds it via `Ref.of(...).map { ref => new ... }`
- Mock lives in `src/test/scala/...` in the same package as the real trait

### NoOpLogger for tests needing Logger

```scala
given Logger[IO] = NoOpLogger[IO]
```

Always add this as a `given` at the top of the test class body when the SUT requires a `Logger`.

### What to test per layer

**Domain (service) tests** — `src/test/scala/.../domain/`
- Test all service methods against the mock repository
- Cover: happy path, first-time user (no prior greetings), error cases from the repository

**Infra driven (storage/repository) tests** — `src/test/scala/.../infra/driven/`
- Test the repository adapter against the storage mock
- Verify mapping between domain types and persistence types

**HTTP route tests** — `src/test/scala/.../infra/driving/http/`
- Use Http4s `Request[IO]` + `httpApp.run(request)` pattern
- Assert response status code AND decoded body
- Mock the service layer with a `HelloServiceMock`-style in-memory implementation

```scala
val httpApp = FooRoute.routes[IO](fooService).orNotFound
val request = Request[IO](Method.GET, uri"/foo/bar")
val response <- httpApp.run(request)
val body     <- response.as[Response]
assertEquals(response.status.code, 200)
```

### Integration tests — `integration/src/test/scala/`
- Extend `DatabaseMunitFixture` (already in the project)
- Use TestContainers + PostgreSQL via the existing fixture
- Test the real repository impl with a live database, no mocks

### Error handling tests — always use interceptIO / interceptMessageIO

Never use `assertThrows` or `attempt.map(...)` for error scenarios. Use the MUnit Cats Effect helpers:

```scala
// Assert the error type only
interceptIO[HelloError] {
  service.doSomething(name)
}

// Assert both the error type AND the message
interceptMessageIO[HelloError]("db down") {
  service.doSomething(name)
}
```

Rules:
- Use `interceptIO[E]` when you only care that the specific error type is raised
- Use `interceptMessageIO[E]("exact message")` when the message content matters for the test
- These return `IO[E]` — chain `.map(e => assertEquals(...))` if you need to assert on fields beyond the message
- Always use the domain error type (e.g. `HelloError`), not `RuntimeException`, unless testing infrastructure-level failures

### ScalaCheck — use for pure domain logic with wide input space

When a function is pure and has many valid input combinations, add a ScalaCheck property test:

```scala
import org.scalacheck.Prop.forAll
import munit.ScalaCheckSuite

class FooPropertiesTest extends ScalaCheckSuite {
  property("Name round-trips through value") {
    forAll { (s: String) =>
      s.nonEmpty ==> (Name(s).value == s)
    }
  }
}
```

Use ScalaCheck for: opaque types, pure transformations, codec round-trips.
Do NOT use it for: effectful code, IO-based services, anything with Ref or state.

## Environment Variables

When adding a new environment variable, update **all three places** in this order:

**1. `application.conf`** — bind the env var using Typesafe Config substitution:
```hocon
new-feature {
  api-key = ${NEW_FEATURE_API_KEY}
  timeout = ${NEW_FEATURE_TIMEOUT_SECONDS}
}
```

**2. `.envrc.example`** — add with a safe dummy value (this file is committed):
```bash
export NEW_FEATURE_API_KEY=change-me
export NEW_FEATURE_TIMEOUT_SECONDS=30
```

**3. `.envrc`** — add with a real local value (this file is gitignored, safe for real values):
```bash
export NEW_FEATURE_API_KEY=actual-local-key
export NEW_FEATURE_TIMEOUT_SECONDS=30
```

**4. `Config.scala`** — add the field to the relevant PureConfig case class:
```scala
case class NewFeatureConfig(apiKey: String, timeoutSeconds: Int) derives ConfigReader
```

PureConfig maps kebab-case config keys to camelCase field names automatically (`api-key` → `apiKey`).

Naming convention for env vars: `SCREAMING_SNAKE_CASE`, prefixed by the feature/service name (e.g. `DATABASE_HOST`, `NEW_FEATURE_API_KEY`).

## Domain Layer Purity

The `domain/` package is the innermost layer — it must have **zero knowledge** of how it is called or how it stores data.

**Forbidden in `domain/`:**
- Any `io.circe` import (JSON codecs belong in the HTTP adapter)
- Any `doobie` import (SQL belongs in the persistence adapter)
- Any `org.http4s` import (HTTP types belong in the driving adapter)
- Any `io.circe` derived instances (`derives Codec`, `derives Encoder/Decoder`)
- Any `doobie.util.Read/Write` derived instances
- Any reference to `infra` packages
- Database entity types (e.g. `GreetingsRecord`) — those live in `infra/driven/persistence/`

**Allowed in `domain/`:**
- Pure Scala / Scala standard library
- Cats (`cats.Monad`, `cats.implicits.*`, etc.) and Cats Effect typeclasses (`Clock`, `Sync`, `Async`)
- `log4cats` — `Logger[F]` is an abstract effect, not an infra dependency
- Your own domain models, errors, and traits

**The rule of thumb:** if you can't compile `domain/` without the database driver or http4s on the classpath, something leaked.

When generating or modifying domain code, check imports first. If a forbidden import is needed, it means the logic belongs in an adapter, not the domain.

## SBT Commands

| Command | What it does | When to run |
|---|---|---|
| `sbt fmt` | Scalafmt + Scalafix (format + organize imports) | After editing any `.scala` or `.sbt` file |
| `sbt fmtCheck` | Same checks but fails on diff — does not modify files | Before committing, in CI |
| `sbt test` | Unit tests only | After changing domain or infra code |
| `sbt integration/test` | Integration tests (requires Docker + running PostgreSQL) | After changing persistence layer |
| `sbt ci` | `clean` + `fmtCheck` + `test` — the full local CI pipeline | Before committing or pushing |

**Rules:**
- Always run `sbt fmt` after writing or modifying Scala files — never leave formatting to the user
- When asked to verify code is correct, run `sbt ci`
- Do not run `sbt integration/test` unless the user explicitly asks — it requires Docker
- If `sbt fmtCheck` or `sbt ci` fails due to formatting, fix it by running `sbt fmt` first, then retry

## Code Style

- Prefer `for` comprehensions over `.flatMap` chains
- Use opaque types for domain primitives (see `Name` in `models.scala`)
- All errors in `domain/errors.scala`, extending a sealed hierarchy
- No `null` — `-Yexplicit-nulls` is enabled; use `Option` or `.nn` for Java interop
- Imports: organized by Scalafix (`OrganizeImports`), java imports first, then scala, then third-party

## File Placement

| What | Where |
|---|---|
| Domain trait (port) | `src/main/scala/.../domain/` |
| Domain service | `src/main/scala/.../domain/` |
| Domain models/errors | `src/main/scala/.../domain/` |
| Persistence adapter | `src/main/scala/.../infra/driven/persistence/` |
| HTTP adapter | `src/main/scala/.../infra/driving/http/` |
| Unit test | `src/test/scala/` mirroring main package |
| Mock for test | `src/test/scala/` same package as the real trait |
| Integration test | `integration/src/test/scala/` |
