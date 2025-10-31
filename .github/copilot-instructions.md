# Copilot Project Instructions

Concise, project-specific guidance for AI assistants working on AutoBuilder.

## 1. Architecture & Modules
- Multi-module Gradle project: `annotations` (public annotations), `processor` (KSP logic & generation), `sample` (smoke tests of generated code).
- Flow: KSP calls `AutoBuilderProcessor` -> scans `@AutoBuilder` interfaces -> builds an internal model (`model/`) -> `ModelGenerator` emits two files per interface in KSP output: `<Interface>.builder.kt` (private Impl + public Builder + DSL creators) and `<Interface>.defaults.kt` (singleton implementing the interface with defaults).
- Rationale: Separate annotations vs processor keeps published API lean and allows internal refactors without breaking consumers; dual-file output isolates default logic from builder ergonomics.
- Public API strictly in `app.izantech.plugin.autobuilder.annotation`; everything else is internal (`processor/...`).

## 2. Code Generation Conventions
- File naming is contract: never rename `*.builder.kt` / `*.defaults.kt` (golden tests assert them).
- Builder file layout (example `User.builder.kt`): `private class UserImpl(...) : User`, `public class UserBuilder(...)`, `@JvmSynthetic inline fun User { ... }`, `@JvmSynthetic inline fun User.copy { ... }`.
- Defaults file (`User.defaults.kt`): `object UserDefaults : User { override val age = 0 ... }` mixing inferred + explicit `@DefaultValue` getters.
- Extend inference: modify `ModelGenerator` where `when`/`if` chain decides defaults; mirror new logic in README list + add test.

## 3. Default Resolution Internals
- Two code paths in generated `build()`:
	1. No custom defaults -> direct constructor call, lateinit checks inline (fast path, no maps).
	2. Custom default(s) -> emit `resolvedValues`, `resolving`, and `computations` maps plus local `resolve(name)`; context object only for nullable custom default properties with dependencies.
- `resolve(name)` adds cycle detection (throws on recursion) and memoizes computed values; each property contributes a lambda in `computations` only when custom defaults are present.
- Avoid side-effects in custom default getters; they may be invoked lazily through `resolve`.
- Legacy always-lambda design deprecated; opt-in rollback may be guarded by a Gradle flag (see `INTERNALS.md`).

## 4. Adding / Modifying Features
- New annotation: add in `annotations/src/main/java/.../annotation`, keep package stable; parse in processor model; integrate in `ModelGenerator` emission.
- If processor entry changes, update `processor/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`.
- Structural changes: update both generated file templates + golden test expectations + sample module regeneration.
- Preserve Java interop: keep public setters (`setX`) on Builder + `@JvmSynthetic` for Kotlin-only DSL.

## 5. Testing Workflow
- Full suite: `./gradlew build` (JVM 17 baseline).
- Processor focus: `./gradlew :processor:test --tests "*AutoBuilderProcessorTests"` for targeted cases.
- Manual generation check: run `./gradlew :sample:build` then inspect `sample/build/generated/ksp/main/kotlin/` for expected file names & contents.
- Add tests: create interface in test sources + expected assertion; name tests with backticked descriptive titles.

## 6. Publishing & Versioning
- Version: bump in `gradle.properties`; snapshot vs release flows are manual.
- Local validation: `./gradlew publishToMavenLocal` then consume in another project.
- Release: `./gradlew publish --no-configuration-cache` (requires signing + Sonatype creds).

## 7. Style & PR Expectations
- Kotlin official style (4 spaces); keep generated code stable (avoid unnecessary reordering—affects golden tests).
- Prefer immutable collections; only use mutable if builder logic demands.
- Diagnostics live in `AutoBuilderErrors.kt`; follow existing message tone & ID schema.
- PR checklist: behavioral summary + generated snippet diff + tests updated + commands run (`./gradlew build`).

## 8. When Extending Default Inference
1. Add type logic in `ModelGenerator` default inference branch.
2. Add test interface + assertion referencing emitted default.
3. Update README list (keep ordering & formatting consistent).
4. Optionally add sample usage in `sample` for manual verification.

## 9. Safe Change Checklist (Copy in PR)
- [ ] Updated builder + defaults emission.
- [ ] Adjusted/added tests (golden + behavior).
- [ ] Regenerated sample & inspected generated sources.
- [ ] No internal packages leaked into public API.
- [ ] README / INTERNALS updated (if semantics changed).

## 10. Anti-Patterns to Avoid
- Reflection or runtime scanning (keep compile-time deterministic KSP path).
- Unstable file names/member order (breaks golden tests & diff readability).
- Side-effectful default getters (must be pure/idempotent).
- Adding stateful singletons beyond the defaults object.

Use this as the authoritative quick-reference; prefer updating here + AGENTS.md + README coherently when process changes.

## 11. Troubleshooting Tips
- Missing inferred default? Inspect generated `<Name>.defaults.kt` under `sample/build/generated/ksp/main/kotlin/`; if absent, extend inference in `ModelGenerator`.
- Array property comparisons rely on `contentEquals`/`contentHashCode`; ensure custom defaults for arrays are pure to keep equality predictable.
- Unexpected runtime error on build: check for uninitialized `@Lateinit` in thrown `UninitializedPropertyAccessException` message sourced from `AutoBuilderErrors`.
- Cyclic custom defaults throw during `resolve(name)`; search for property names with mutually referencing getters.