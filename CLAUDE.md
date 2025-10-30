# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AutoBuilder is a Kotlin Symbol Processor (KSP) library that generates builder classes from interface definitions, inspired by Kotlin data classes but without binary compatibility issues. The library allows developers to create immutable objects with builder pattern support through simple annotations.

## Architecture

The project consists of three main modules:

1. **`:annotations`** - Contains the annotation definitions (`@AutoBuilder`, `@DefaultValue`, `@Lateinit`, `@UseBuilderSetter`)
   - Package: `app.izantech.plugin.autobuilder.annotation`

2. **`:processor`** - The KSP processor that generates builder classes
   - Main processor: `AutoBuilderProcessor`
   - Service provider: `AutoBuilderProcessorProvider` (registered via META-INF/services)
   - Model generation: `ModelGenerator` handles code generation using KotlinPoet
   - Package: `app.izantech.plugin.autobuilder.processor`

3. **`:sample`** - Example usage demonstrating various features
   - Shows inherited properties, default values, lateinit properties, and complex types
   - Generated files are placed in `build/generated/ksp/main/kotlin/`

## Build Commands

```bash
# Build the entire project
gradle build

# Run tests (processor module tests with JUnit 5)
gradle :processor:test

# Run a specific test class
gradle :processor:test --tests "app.izantech.plugin.autobuilder.processor.AutoBuilderProcessorTests"

# Build and run sample module
gradle :sample:build

# Publish to Maven Local for testing
gradle publishToMavenLocal --no-configuration-cache

# Publish to Maven Central
gradle publish --no-configuration-cache

# Clean build
gradle clean
```

## Testing

The processor uses compile-testing framework (`dev.zacsweers.kctfork`) with JUnit 5 for testing KSP compilation. Tests verify:
- Error conditions (non-interface classes, internal interfaces, missing defaults)
- Generated builder code correctness
- Default value inference for various types
- Property inheritance from super interfaces

To run a single test:
```bash
gradle :processor:test --tests "AutoBuilderProcessorTests.test name here"
```

## Code Generation

The processor generates two files for each `@AutoBuilder` annotated interface:

1. **`InterfaceName.builder.kt`** - Contains:
   - Private implementation class with equals/hashCode/toString
   - Public builder class with property setters
   - Kotlin DSL extension functions for construction and copying

2. **`InterfaceName.defaults.kt`** - Contains:
   - Object implementing the interface with default values for all properties

## Key Technical Details

- **Kotlin Version**: 2.2.21 with KSP 2.3.0
- **Gradle Version**: 9.2.0
- **Java Target**: JVM 17
- **KotlinPoet**: 2.2.0 for code generation
- **Testing**: JUnit 5 (6.0.0) with AssertJ (3.27.2)
- **Publishing**: Maven Central via Sonatype OSSRH using `com.vanniktech.maven.publish` plugin
- **Group ID**: `io.github.izanrodrigo`
- **Artifacts**: `autobuilder-annotations` and `autobuilder-processor`

## Development Guidelines

When modifying the processor:

1. **Error Messages**: All processor errors are defined in `AutoBuilderErrors.kt` as formatted strings
2. **KSP Extensions**: Utility extensions for KSP types are in `util/KspExtensions.kt`
3. **KotlinPoet Helpers**: Code generation helpers are in `util/KotlinPoetExtensions.kt`
4. **Default Values**: Inferred default values for common types are handled in `ModelGenerator`
5. **Service Registration**: The processor provider must be registered in `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`

## Publishing Configuration

- Uses Sonatype Central Portal for Maven Central publishing
- Signing is enabled for releases (`RELEASE_SIGNING_ENABLED=true`)
- Version is managed in `gradle.properties` (currently 0.0.8)
- Artifacts use coordinates: `io.github.izanrodrigo:autobuilder-{annotations|processor}:VERSION`