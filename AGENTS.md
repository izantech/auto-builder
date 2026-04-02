# Repository Guidelines

## Project Structure & Architecture
AutoBuilder is a Kotlin Symbol Processor that turns annotated interfaces into builder DSLs. Modules: `annotations` (Kotlin Multiplatform — public annotations in `src/commonMain/kotlin/.../annotation`, targets JVM, JS, Wasm, iOS, macOS, Linux, Windows, tvOS, watchOS), `processor` (JVM-only KSP processor — AutoBuilderProcessor, ModelGenerator, `model/` and `util/` helpers, provider entry in `META-INF/services`), `sample` (JVM feature demos with generated code in `build/generated/ksp/main/kotlin/`), and `integration-test` (KMP module that verifies generated code compiles on JVM, iOS, wasmJs, macOS, and Linux). Tests live in `processor/src/test/java`.

## Build, Test, and Development Commands
- `./gradlew build` – compiles all modules (annotations for all KMP targets, processor and sample on JVM 17), runs tests.
- `./gradlew :annotations:build` – compiles annotations for all KMP targets (JVM, JS, Wasm, native).
- `./gradlew :processor:test` (add `--tests "…AutoBuilderProcessorTests"`) – executes the compile-testing suite or a targeted case.
- `./gradlew :sample:run` or `./gradlew :sample:build` – exercises the sample DSL and ensures generated builders compile.
- `./gradlew :integration-test:build` – verifies generated code compiles on all KMP targets (JVM, iOS, wasmJs, macOS, Linux).
- `./gradlew publishToMavenLocal` / `./gradlew publish --no-configuration-cache` – publishes `autobuilder-annotations` (17 platform artifacts) and `autobuilder-processor` locally or to Sonatype.
- `./gradlew clean dependencyUpdates --no-parallel` – clears outputs and reports dependency drift.

## Coding Style & Processor Conventions
We follow `kotlin.code.style=official` with four-space indentation. Public APIs live in `app.izantech.plugin.autobuilder.annotation`; processor internals stay in `app.izantech.plugin.autobuilder.processor` and its `model`/`util` subpackages. Keep diagnostics in `AutoBuilderErrors.kt`: compile-time errors return plain message strings (KSP's logger attaches source location from the `KSNode` argument), while runtime errors (e.g. `uninitializedLateinit`) embed `file:line` in the message via `runtimeErrorMessage()` since generated `error()` calls lack KSP location tracking, KSP helpers in `util/KspExtensions.kt`, and KotlinPoet extensions in `util/KotlinPoetExtensions.kt`. Preserve `<Interface>.builder.kt` / `<Interface>.defaults.kt` naming, prefer immutable structures, and conditionally add `@JvmSynthetic` only on JVM targets (via `isJvm` flag from `environment.platforms`). Processor uses context parameters (Kotlin 2.3+ syntax: `context(resolver: Resolver)`) and KSP-native `KSAnnotation` API (never `getAnnotationsByType`). Tooling baseline: Kotlin 2.3.20, KSP 2.3.6, KotlinPoet 2.3.0, Gradle 9.2, JVM 17.

## Code Generation Notes
Each `@AutoBuilder` interface generates a DSL file (`*.builder.kt` with the private `Impl`, public `Builder`, and DSL helpers) and a defaults file (`*.defaults.kt`). When changing behaviour, keep both outputs consistent and adjust `ModelGenerator` plus the provider entry in `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`. Empty interfaces are rejected by default; use `@AutoBuilder(allowEmpty = true)` to generate valid empty implementations (equals, hashCode returning 0, toString returning "Name()").

## Testing Guidelines
Tests rely on `dev.zacsweers.kctfork` plus AssertJ. Add cases beside `AutoBuilderProcessorTests` with backticked Given/When/Then names, covering error diagnostics, generated KotlinPoet output, inheritance, and default inference. KMP compatibility tests inspect generated source via `findGeneratedSource()` to assert no JVM-only patterns (`javaClass`, `Objects.hash`, `UninitializedPropertyAccessException`). The `:integration-test` module provides end-to-end KMP verification — it compiles generated code on JVM, iOS, wasmJs, macOS, and Linux. Run `./gradlew :processor:test` and `./gradlew :integration-test:build` before pushing. Use the `sample` module for manual DSL smoke tests.

## Publishing & Release
Artifacts publish as `io.github.izanrodrigo:autobuilder-{annotations|processor}:<VERSION_NAME>`. The annotations module publishes 17 per-platform artifacts (e.g., `autobuilder-annotations-jvm`, `autobuilder-annotations-iosarm64`, `autobuilder-annotations-wasm-js`). Publishing uses vanniktech maven-publish with conditional `KotlinMultiplatform()` / `KotlinJvm()` config. Manage the version (currently 0.1.0), Sonatype host, and signing in `gradle.properties`, and validate `./gradlew publish --no-configuration-cache` against the staging portal when preparing releases.

## Commit & Pull Request Guidelines
Commits keep short, imperative subjects (e.g., `Fix critical bugs`, `Update dependencies`) and stay reviewable on their own. PRs should outline behavioural changes, include relevant generated snippets (`build/generated/ksp/main/kotlin/`), link issues, and note the commands run (`./gradlew build` at minimum). Call out configuration updates such as Sonatype credentials and coordinate version bumps with release planning.
