# Repository Guidelines

## Project Structure & Architecture
AutoBuilder is a Kotlin Symbol Processor that turns annotated interfaces into builder DSLs. Modules: `annotations` (public annotations in `.../annotation`), `processor` (AutoBuilderProcessor, ModelGenerator, `model/` and `util/` helpers, provider entry in `META-INF/services`), and `sample` (feature demos with generated code in `build/generated/ksp/main/kotlin/`). Tests live in `processor/src/test/java`.

## Build, Test, and Development Commands
- `./gradlew build` – compiles all modules, runs tests on JVM 17.
- `./gradlew :processor:test` (add `--tests "…AutoBuilderProcessorTests"`) – executes the compile-testing suite or a targeted case.
- `./gradlew :sample:run` or `./gradlew :sample:build` – exercises the sample DSL and ensures generated builders compile.
- `./gradlew publishToMavenLocal` / `./gradlew publish --no-configuration-cache` – publishes `autobuilder-annotations` and `autobuilder-processor` locally or to Sonatype.
- `./gradlew clean dependencyUpdates` – clears outputs and reports dependency drift.

## Coding Style & Processor Conventions
We follow `kotlin.code.style=official` with four-space indentation. Public APIs live in `app.izantech.plugin.autobuilder.annotation`; processor internals stay in `app.izantech.plugin.autobuilder.processor` and its `model`/`util` subpackages. Keep diagnostics in `AutoBuilderErrors.kt`, KSP helpers in `util/KspExtensions.kt`, and KotlinPoet extensions in `util/KotlinPoetExtensions.kt`. Preserve `<Interface>.builder.kt` / `<Interface>.defaults.kt` naming, prefer immutable structures, and limit `@JvmSynthetic` helpers to DSL entry points. Tooling baseline: Kotlin 2.2.21, KSP 2.3.0, KotlinPoet 2.2.0, Gradle 9.2, JVM 17.

## Code Generation Notes
Each `@AutoBuilder` interface generates a DSL file (`*.builder.kt` with the private `Impl`, public `Builder`, and DSL helpers) and a defaults file (`*.defaults.kt`). When changing behaviour, keep both outputs consistent and adjust `ModelGenerator` plus the provider entry in `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`.

## Testing Guidelines
Tests rely on `dev.zacsweers.kctfork` plus AssertJ. Add cases beside `AutoBuilderProcessorTests` with backticked Given/When/Then names, covering error diagnostics, generated KotlinPoet output, inheritance, and default inference. Run `./gradlew :processor:test` before pushing and use the `sample` module for manual DSL smoke tests.

## Publishing & Release
Artifacts publish as `io.github.izanrodrigo:autobuilder-{annotations|processor}:<VERSION_NAME>`. Manage the version (currently 0.0.8), Sonatype host, and signing in `gradle.properties`, and validate `./gradlew publish --no-configuration-cache` against the staging portal when preparing releases.

## Commit & Pull Request Guidelines
Commits keep short, imperative subjects (e.g., `Fix critical bugs`, `Update dependencies`) and stay reviewable on their own. PRs should outline behavioural changes, include relevant generated snippets (`build/generated/ksp/main/kotlin/`), link issues, and note the commands run (`./gradlew build` at minimum). Call out configuration updates such as Sonatype credentials and coordinate version bumps with release planning.
