# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

**Language/Version**: Scala 3.3.x
**Primary Dependencies**: Cats Effect 3, Http4s + Ember, Tapir, Doobie + HikariCP, Flyway, PureConfig, Log4cats + Logback
**Storage**: PostgreSQL (via Doobie); [add other storage if applicable or N/A]
**Messaging**: [e.g., Kafka via fs2-kafka, or N/A]
**Testing**: MUnit + Cats Effect (unit), TestContainers + PostgreSQL (integration), Gatling (performance)
**Target Platform**: JVM / Linux server
**Project Type**: web-service
**Performance Goals**: [feature-specific, e.g., p95 < 200ms at 500 req/s or NEEDS CLARIFICATION]
**Constraints**: [feature-specific, e.g., no blocking IO, pure FP, or NEEDS CLARIFICATION]
**Scale/Scope**: [feature-specific, e.g., expected load, data volume or NEEDS CLARIFICATION]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

[Gates determined based on constitution file]

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
src/main/scala/dev/iuly/
├── main/
│   ├── Main.scala              # Entry point (IOApp)
│   ├── Config.scala            # Top-level config loader
│   └── AppModule.scala         # DI container & HTTP server wiring
│
└── [feature]/                  # One package per bounded context
    ├── domain/                 # Pure business logic — NO framework deps
    │   ├── models.scala        # Opaque types, value objects, domain entities
    │   ├── errors.scala        # Domain error ADT
    │   ├── [Feature]Repository.scala  # Repository trait (port)
    │   └── [Feature]Service.scala     # Service implementation
    │
    └── infra/
        ├── driven/             # Outgoing adapters (app calls these)
        │   ├── persistence/    # Database adapter
        │   │   ├── config/
        │   │   │   ├── Config.scala    # DB config case class
        │   │   │   └── Database.scala  # HikariCP + Flyway init
        │   │   ├── [Feature]Repository.scala  # Repository impl (adapter)
        │   │   ├── [Feature]Storage.scala     # Doobie SQL queries
        │   │   └── entities.scala             # DB record types
        │   └── kafka/      # Outgoing message publishing (e.g. Kafka producer)
        │       ├── [Feature]Publisher.scala   # Publisher impl (adapter)
        │       └── entities.scala             # Message payload types
        └── driving/            # Incoming adapters (these call the app)
            ├── http/           # HTTP adapter
            │   ├── [Feature]Endpoint.scala    # Tapir endpoint definition
            │   ├── [Feature]Route.scala       # Route handler with error mapping
            │   └── entities.scala             # HTTP request/response types
            └── kafka/      # Incoming message consumption (e.g. Kafka consumer)
                └── [Feature]Consumer.scala    # Consumer handler
                └── entities.scala             # Message payload types

src/test/scala/dev/iuly/[feature]/
├── domain/                     # Unit tests for domain (pure, no DB/HTTP)
└── infra/                      # Unit tests for adapters (mocked dependencies)

integration/src/test/scala/dev/iuly/[feature]/
└── infra/                      # Integration tests (TestContainers + PostgreSQL)

src/main/resources/
├── application.conf            # PureConfig configuration
└── db/migration/               # Flyway SQL migrations (V1__*.sql)
```

**Structure Decision**: [Document which feature package(s) are added and any deviations from the standard hexagonal layout, with justification]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
