# Copilot Project Instructions

Concise, project-specific guidance for AI assistants working on AutoBuilder.

## 1. Architecture & Modules
- Multi-module Gradle project: `annotations` (public annotations), `processor` (KSP logic & generation), `sample` (usage & smoke tests of generated code).
- Core entry: `AutoBuilderProcessor` builds models then delegates to `ModelGenerator` to emit two files per `@AutoBuilder` interface: `<Interface>.builder.kt` (impl + builder + DSL functions) and `<Interface>.defaults.kt` (default object implementing the interface).
- Keep public API surface inside `app.izantech.plugin.autobuilder.annotation`. Processor internals remain under `app.izantech.plugin.autobuilder.processor` (plus `model` & `util`).

## 2. Code Generation Conventions
- File naming is contract: do not rename `*.builder.kt` / `*.defaults.kt` patterns—tests rely on them.
- Generated builder file sections: private `Impl`, public `Builder`, inline `@JvmSynthetic` DSL creators `Interface { ... }` and `copy`.
- Defaults file defines singleton `<Interface>Defaults : <Interface>` assigning concrete default values (inferred + custom `@DefaultValue`).
- Default inference list in README (e.g., primitives, collections, arrays). Extend via adjusting inference logic in `ModelGenerator` (look for when/if chains) and update README + tests.

## 3. Default Resolution Internals
- Current algorithm: single `resolve(name)` with `when` dispatch, cycle detection via `resolving` set, memoization in `resolvedValues` map (see implementation in generator logic). Avoid reintroducing per-property lambda maps (legacy approach) unless feature-flagged.
- Planned optimizations documented in `INTERNALS.md`; keep backward-compatible & optionally guard new strategies behind a Gradle property (e.g., `autobuilder.legacyResolution`).

## 4. Adding / Modifying Features
- Add new annotation => define in `annotations` module, expose under annotation package, then handle symbol in processor (parsing in processor model + usage in `ModelGenerator`). Update service registration if a new processor provider is introduced (`META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`).
- On structural change, adjust both emitted files + test golden outputs. Ensure Java interop (setter generation, `@JvmSynthetic` DSL) stays intact.

## 5. Testing Workflow
- Run full build & tests: `./gradlew build` (JVM 17).
- Faster processor-only cycle: `./gradlew :processor:test` (optionally `--tests "*AutoBuilderProcessorTests"`).
- Validate generation manually: `./gradlew :sample:build` then inspect `sample/build/generated/ksp/main/kotlin/`.
- Tests use `dev.zacsweers.kctfork` + AssertJ; add new scenario tests in `processor/src/test/java/...` with backticked descriptive names.

## 6. Publishing & Versioning
- Artifacts: `io.github.izanrodrigo:autobuilder-{annotations|processor}:<VERSION_NAME>`.
- Bump version in `gradle.properties`. Use `./gradlew publishToMavenLocal` for local checks; `publish --no-configuration-cache` for Sonatype (ensure signing & credentials).

## 7. Style & PR Expectations
- Kotlin official style, 4-space indent. Immutable prefs—avoid mutable collections in models/builders unless necessary.
- Keep diagnostics centralized (`AutoBuilderErrors.kt`); extend with consistent wording & ID pattern.
- PRs should include: summary of behavioral change, sample of new generated snippet, updated tests, commands executed (at least `./gradlew build`).

## 8. When Extending Default Inference
1. Add type handling in generation logic.
2. Add test interface exercising new type + expected file assertions.
3. Update README inference list.
4. Provide sample usage in `sample` if helpful.

## 9. Safe Change Checklist (Copy in PR)
- [ ] Updated both builder + defaults generation paths.
- [ ] Adjusted tests / added new coverage.
- [ ] Regenerated sample & manually inspected.
- [ ] No public API package leaks from processor internals.
- [ ] README / INTERNALS updated if semantics changed.

## 10. Anti-Patterns to Avoid
- Introducing reflection or runtime classpath scanning (keep compile-time deterministic).
- Emitting unstable file names or reordering members unnecessarily (breaks golden tests).
- Adding defaults that produce side-effects on access (keep pure & idempotent).

Use this as the authoritative quick-reference; prefer updating here + AGENTS.md + README coherently when process changes.