<!--
SYNC IMPACT REPORT
==================
Version change: N/A → 1.0.0 (initial ratification)
Modified principles: N/A (first version)
Added sections:
  - I. Hexagonal Architecture
  - II. Domain Layer Purity
  - III. Test-First with MUnit + Cats Effect
  - IV. Hand-rolled Ref-based Mocks
  - V. Configuration & Environment Management
  - VI. Code Style & Tooling Discipline
  - Technology Stack & Constraints
  - Development Workflow & Quality Gates
  - Governance
Removed sections: N/A
Templates reviewed:
  - .specify/templates/plan-template.md          ✅ aligned (Constitution Check section present)
  - .specify/templates/spec-template.md          ✅ aligned (no constitution-specific gaps)
  - .specify/templates/tasks-template.md         ✅ aligned (test discipline reflected in task phases)
  - .claude/commands/*.md                        ✅ reviewed (no outdated agent-specific references)
Deferred TODOs: none
-->

# Scala App Template Constitution

## Core Principles

### I. Hexagonal Architecture

The codebase MUST follow ports-and-adapters (hexagonal) architecture with three distinct layers:

- **Domain** (`domain/`): pure business logic, no framework dependencies, exposes traits (ports)
- **Driven adapters** (`infra/driven/`): implement domain ports for storage (Doobie + PostgreSQL)
- **Driving adapters** (`infra/driving/`): expose domain functionality to callers (Http4s + Tapir)

`Main.scala` is the composition root. `AppModule` wires all dependencies. No layer may bypass the domain
port; adapters MUST depend on domain traits, not on each other directly.

**Rationale**: Ensures the business logic can be tested, reasoned about, and evolved independently of
frameworks and infrastructure choices.

### II. Domain Layer Purity

The `domain/` package MUST have zero knowledge of how it is called or how it stores data.

**Forbidden in `domain/`:**
- Any `io.circe` import (JSON codecs belong in the HTTP adapter)
- Any `doobie` import (SQL belongs in the persistence adapter)
- Any `org.http4s` import (HTTP types belong in the driving adapter)
- Any derived `Codec`, `Encoder`, `Decoder`, `Read`, or `Write` instances from infra libraries
- Any reference to `infra` packages or database entity types

**Allowed in `domain/`:**
- Pure Scala / Scala standard library
- Cats and Cats Effect typeclasses (`cats.Monad`, `cats.effect.Sync`, `Clock`, etc.)
- `log4cats` — `Logger[F]` is an abstract effect, not an infra dependency
- Own domain models, errors, and traits

The rule of thumb: if `domain/` cannot compile without the database driver or http4s on the classpath,
something has leaked.

**Rationale**: Prevents accidental coupling that makes the domain untestable in isolation and collapses
the architectural boundaries.

### III. Test-First with MUnit + Cats Effect

All tests MUST use MUnit + Cats Effect (`CatsEffectSuite`). Plain `FunSuite` is forbidden.

- Tests MUST use `for` comprehensions returning `IO[Assertions]`, not side-effecting `assert()` calls
- `assertEquals` MUST use named args in order: `assertEquals(obtained = actual, expected = expected)`
- Test names MUST describe behavior, not structure: "should record greeting" not "test sayHello"
- Error scenarios MUST use `interceptIO[E]` or `interceptMessageIO[E]("msg")` — never `assertThrows`
  or `attempt.map(...)`
- When the SUT requires a `Logger`, add `given Logger[IO] = NoOpLogger[IO]` at the top of the test class
- ScalaCheck property tests (`ScalaCheckSuite`) MUST be used for pure functions with wide input spaces
  (opaque types, codec round-trips, pure transformations); MUST NOT be used for effectful/stateful code

**Rationale**: Consistent test style makes the suite readable and ensures error assertions are
type-safe and composable within the IO monad.

### IV. Hand-rolled Ref-based Mocks

ScalaMock MUST NOT be used for repository or service mocks. Use in-memory `Ref`-based implementations.

Structure:
- A `FooRepositoryMock[F[_]]` trait extends the real trait and adds test helpers (`seed`, `initMap`, etc.)
- A companion `object` builds the mock via `Ref.of[F, State](empty).map { ref => new ... }`
- The mock MUST live in `src/test/scala/` in the same package as the real trait

```scala
object FooRepositoryMock {
  def apply[F[_]: Sync]: F[FooRepositoryMock[F]] =
    Ref.of[F, Map[Key, Value]](Map.empty).map { ref =>
      new FooRepositoryMock[F] {
        override def seed(data: Map[Key, Value]): F[Unit] = ref.set(data)
        // ... implement real trait methods via ref
      }
    }
}
```

**Rationale**: Ref-based mocks compose naturally with Cats Effect, are deterministic, and express
domain invariants better than framework mocks.

### V. Configuration & Environment Management

When adding any new environment variable, ALL FOUR locations MUST be updated in this order:

1. `application.conf` — bind env var using Typesafe Config substitution (`key = ${ENV_VAR}`)
2. `.envrc.example` — add with a safe dummy value (committed to VCS)
3. `.envrc` — add with real local value (gitignored)
4. `Config.scala` — add field to the relevant PureConfig case class (`derives ConfigReader`)

Environment variable names MUST use `SCREAMING_SNAKE_CASE` prefixed by the feature/service name
(e.g., `DATABASE_HOST`, `NEW_FEATURE_API_KEY`). PureConfig maps kebab-case config keys to camelCase
field names automatically.

**Rationale**: Prevents configuration drift between environments and broken builds due to missing
env vars.

### VI. Code Style & Tooling Discipline

- MUST prefer `for` comprehensions over `.flatMap` chains
- MUST use opaque types for domain primitives
- All domain errors MUST be defined in `domain/errors.scala`, extending a sealed hierarchy
- No `null` — `-Yexplicit-nulls` is enabled; use `Option` or `.nn` for Java interop
- `sbt fmt` MUST be run after writing or modifying any Scala or `.sbt` file — never leave formatting
  to the user
- Before committing or pushing, `sbt ci` MUST pass (`clean` + `fmtCheck` + `test`)
- Integration tests (`sbt integration/test`) MUST NOT be run unless explicitly requested; they require
  Docker and a running PostgreSQL container

**Rationale**: Consistent formatting and strict compile options reduce cognitive load and prevent
entire classes of bugs at compile time.

## Technology Stack & Constraints

**Language**: Scala 3.3 with strict compiler options (`-Yexplicit-nulls`, unused import warnings)

**Core libraries**:
- Cats Effect 3 for purely functional IO
- Http4s + Ember for the HTTP server
- Tapir for type-safe endpoint definitions (auto-generates OpenAPI/Swagger)
- Doobie + HikariCP for database access and connection pooling
- Flyway for database migrations
- PureConfig for type-safe configuration
- Log4cats + Logback (JSON structured logging via logstash encoder)

**Build**: SBT 1.12 with multi-project build. Subprojects: `integration` (TestContainers + PostgreSQL),
`performance` (Gatling, Scala 2.13).

**Testing libraries**: MUnit + Cats Effect (unit), TestContainers + PostgreSQL (integration),
Gatling (performance).

New dependencies MUST be justified against existing stack capabilities before introduction.

## Development Workflow & Quality Gates

**Feature development** follows the speckit workflow:

| Step | Command | Output |
|------|---------|--------|
| Define principles | `/speckit.constitution` | `.specify/memory/constitution.md` |
| Specify feature | `/speckit.specify` | `specs/###-feature/spec.md` |
| Resolve ambiguities | `/speckit.clarify` | updated `spec.md` |
| Plan implementation | `/speckit.plan` | `plan.md`, `research.md`, `data-model.md` |
| Generate tasks | `/speckit.tasks` | `tasks.md` |
| Validate consistency | `/speckit.analyze` | analysis report |
| Implement | `/speckit.implement` | working code |

**File placement** MUST follow the hexagonal layout:

| Artifact | Location |
|----------|----------|
| Domain trait (port) | `src/main/scala/.../domain/` |
| Domain service | `src/main/scala/.../domain/` |
| Domain models/errors | `src/main/scala/.../domain/` |
| Persistence adapter | `src/main/scala/.../infra/driven/persistence/` |
| HTTP adapter | `src/main/scala/.../infra/driving/http/` |
| Unit test | `src/test/scala/` mirroring main package |
| Mock for test | `src/test/scala/` same package as real trait |
| Integration test | `integration/src/test/scala/` |

## Governance

This constitution supersedes all other architectural guidance. When CLAUDE.md and this constitution
conflict, raise the conflict before proceeding — do not silently resolve it.

**Amendment procedure**:
1. Propose the change with rationale in a PR description or conversation
2. Determine version bump: MAJOR (principle removed/redefined), MINOR (principle added/expanded),
   PATCH (clarification/wording fix)
3. Update this file, run consistency propagation across all templates, and update `LAST_AMENDED_DATE`
4. Record the change in the Sync Impact Report comment at the top of this file

**Compliance review**: Every plan (`/speckit.plan`) MUST include a Constitution Check gate that verifies
the proposed design against all six principles before Phase 0 research proceeds.

**Versioning policy**: Semantic versioning — `MAJOR.MINOR.PATCH`. Version line below is the
authoritative version.

**Version**: 1.0.0 | **Ratified**: 2026-03-05 | **Last Amended**: 2026-03-05
