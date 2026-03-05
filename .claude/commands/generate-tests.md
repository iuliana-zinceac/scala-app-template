# Generate Tests

Generate tests for the Scala file(s) provided or currently open in the editor.

Follow the rules in CLAUDE.md exactly. Here is the workflow:

## Step 1 — Identify the target

If the user specified a file, use that. Otherwise, ask which file or layer they want tests for.

## Step 2 — Read and analyse the source file

Read the source file. Identify:
- **Layer**: domain service / domain repository trait / infra persistence / infra HTTP route
- **Dependencies**: what traits/services does it depend on? Are mocks already available?
- **Methods**: list every public method and its signature
- **Error cases**: look at the error types defined in `domain/errors.scala`

## Step 3 — Check what already exists

Search for an existing test file and mock files for this class. If they exist, read them — you will extend, not duplicate.

## Step 4 — Decide test type

| Layer | Test class | Mock needed? |
|---|---|---|
| Domain service | `CatsEffectSuite` | Yes — mock for each repository it uses |
| Domain repository trait | N/A — test the impl | Yes — mock for storage |
| Infra persistence (repository impl) | `CatsEffectSuite` | Yes — storage mock |
| Infra HTTP route | `CatsEffectSuite` | Yes — service mock |
| Pure domain logic with wide input space | `ScalaCheckSuite` | No |

## Step 5 — Generate mock(s) if missing

For each dependency that lacks a mock, create a `Ref`-based mock following the CLAUDE.md pattern:
- File: `src/test/scala/<same-package-as-real-trait>/<TraitName>Mock.scala`
- Trait extends the real trait and adds `seed` / `init` helpers
- Object builds it with `Ref.of(...).map { ref => new ... }`

## Step 6 — Generate the test file

File: `src/test/scala/<same-package>/<ClassName>Test.scala`

Requirements:
- Extend `CatsEffectSuite` (or `ScalaCheckSuite` for property tests)
- One `test(...)` block per scenario, named as behavior not method
- `given Logger[IO] = NoOpLogger[IO]` if the SUT needs a logger
- `assertEquals(obtained = ..., expected = ...)` with named args
- Cover at minimum: happy path, empty/first-time case, each error path

## Step 7 — Report

List every file created or modified, and what each test covers. If any test scenario is left out intentionally, say why.
