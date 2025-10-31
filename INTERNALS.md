# AutoBuilder Internals

## `User.builder.kt`

This file will contain the implementation class of your definition interface, the builder class and some extensions to make the builder more idiomatic in Kotlin.

```kotlin
private class UserImpl(
    override val age: Int,
    override val name: String,
) : User {
    // Generated `equals`, `hashCode` and `toString` methods...
}

public class UserBuilder @PublishedApi internal constructor(
    source: User? = null,
) {
    // Generated properties and Java setters...

    public fun build(): User = UserImpl(...)
}

@JvmSynthetic
public inline fun User(`init`: UserBuilder.() -> Unit = {}): User = UserBuilder().apply(init).build()

@JvmSynthetic
public inline fun User.copy(`init`: UserBuilder.() -> Unit = {}): User = UserBuilder(this).apply(init).build()

```

## `User.defaults.kt`

This file will contain the default values for the properties that don't have a default value defined in the interface.

```kotlin
public object UserDefaults : User {
  override val age: Int = 0

  override val name: String = ""
}
```

## Default Resolution Internals

### Baseline Metrics (Phase 1)
Current strategy (after initial simplification) uses a single `resolve(name)` with a `when(name)` dispatch and inline anonymous context objects for nullable custom default properties.

Baseline (sample `FullModel`):
* Generated builder lines: ~888 (previous multiline context version was ~1193).
* Custom default branches with context objects: 5.
* Removed `computations` map (older design) to reduce indirection.
* Cycle detection: `resolving` set; memoization: `resolvedValues` map.

Upcoming optimization phases will target:
1. Shared proxy context (single reusable implementation with delegated `resolve`).
2. Selective overrides (only referenced properties emitted for context).
3. Cached context instance (avoid multiple anonymous allocations).
4. Optional branch compression for very large interfaces.
5. Lightweight performance harness (timing multiple `build()` calls) to measure allocation/latency improvements.
6. Feature flag for rollback to legacy lambda-map approach.

Rollback Plan: Introduce Gradle property `autobuilder.legacyResolution=true` to restore previous lambda-based generation if issues arise.

Metrics recorded here provide a baseline for tracking improvements in subsequent phases.
