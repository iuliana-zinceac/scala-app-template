## What

<!-- Brief description of the change. Link to spec if this is a speckit feature branch. -->

Closes #

## Type

- [ ] feat — new feature
- [ ] fix — bug fix
- [ ] refactor — no behaviour change
- [ ] test — tests only
- [ ] chore — build, deps, tooling, docs

## Layer changed

- [ ] domain
- [ ] infra/driven/persistence
- [ ] infra/driven/kafka
- [ ] infra/driving/http
- [ ] infra/driving/kafka
- [ ] config / build
- [ ] `.claude/` — Claude commands or settings
- [ ] `.specify/` — spec templates or constitution
- [ ] `CLAUDE.md` — coding conventions

## Checklist

- [ ] `sbt ci` passes locally
- [ ] Domain layer has no infra imports (`io.circe`, `doobie`, `org.http4s`)
- [ ] New env vars added to `application.conf`, `.envrc.example`, and `Config.scala`
- [ ] Flyway migration added and versioned (`V{n}__description.sql`)
- [ ] Tests cover the happy path and at least one error case
- [ ] No `null` introduced — used `Option` or `.nn` for Java interop
- [ ] If `.claude/` or `.specify/` changed: commands tested manually and README updated if needed
