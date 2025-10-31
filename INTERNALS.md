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
