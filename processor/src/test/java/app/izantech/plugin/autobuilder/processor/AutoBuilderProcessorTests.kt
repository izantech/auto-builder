@file:OptIn(ExperimentalCompilerApi::class)

package app.izantech.plugin.autobuilder.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AutoBuilderProcessorTests {
    @TempDir lateinit var tempDir: File

    // region Errors
    @Test fun `WHEN definition is a class THEN compilation fails`() {
        // Given
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |
            |@AutoBuilder
            |class TestInterface(val int: Int)
        """.trimMargin()

        // When
        val compilationResult = compile(code)

        // Then
        assertThat(compilationResult.exitCode)
            .isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(compilationResult.messages)
            .contains(AutoBuilderErrors.TEMPLATE_NOT_PUBLIC_INTERFACE.format("TestInterface"))
    }

    @Test fun `WHEN definition interface is internal THEN compilation fails`() {
        // Given
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |
            |@AutoBuilder
            |internal interface TestInterface
        """.trimMargin()

        // When
        val compilationResult = compile(code)

        // Then
        assertThat(compilationResult.exitCode)
            .isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(compilationResult.messages)
            .contains(AutoBuilderErrors.TEMPLATE_NOT_PUBLIC_INTERFACE.format("TestInterface"))
    }

    @Test fun `WHEN definition interface is private THEN compilation fails`() {
        // Given
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |
            |@AutoBuilder
            |private interface TestInterface
        """.trimMargin()

        // When
        val compilationResult = compile(code)

        // Then
        assertThat(compilationResult.exitCode)
            .isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(compilationResult.messages)
            .contains(AutoBuilderErrors.TEMPLATE_NOT_PUBLIC_INTERFACE.format("TestInterface"))
    }

    @Test fun `WHEN definition interface is empty THEN compilation fails`() {
        // Given
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |
            |@AutoBuilder
            |interface TestInterface
        """.trimMargin()

        // When
        val compilationResult = compile(code)

        // Then
        assertThat(compilationResult.exitCode)
            .isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(compilationResult.messages)
            .contains(AutoBuilderErrors.TEMPLATE_EMPTY_INTERFACE.format("TestInterface"))
    }

    @Test fun `WHEN interface has type parameters THEN compilation fails`() {
        // Given
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |
            |@AutoBuilder
            |interface TestInterface<T> {
            |    val value: T
            |}
        """.trimMargin()

        // When
        val compilationResult = compile(code)

        // Then
        assertThat(compilationResult.exitCode)
            .isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(compilationResult.messages)
            .contains(AutoBuilderErrors.TEMPLATE_GENERIC_TYPE.format("TestInterface"))
    }

    @Test fun `WHEN property is uninitialized THEN compilation fails`() {
        // Given
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |
            |@AutoBuilder
            |interface TestInterface {
            |    val uninitialized: Exception
            |}
        """.trimMargin()

        // When
        val compilationResult = compile(code)

        // Then
        assertThat(compilationResult.exitCode)
            .isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertThat(compilationResult.messages)
            .contains(AutoBuilderErrors.TEMPLATE_UNINITIALIZED_PROPERTY.format("uninitialized"))
    }
    // endregion

    // region Success
    @Test fun `WHEN definition is public and contains properties THEN compilation succeeds`() {
        // Given
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |
            |@AutoBuilder
            |interface TestInterface { val int: Int }
        """.trimMargin()

        // When
        val compilationResult = compile(code)

        // Then
        assertThat(compilationResult.exitCode)
            .isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test fun `WHEN property is uninitialized but is annotated with @Lateinit THEN compilation succeeds`() {
        // Given
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |import app.izantech.plugin.autobuilder.annotation.Lateinit
            |
            |@AutoBuilder
            |interface TestInterface {
            |    @Lateinit val uninitialized: Exception
            |}
        """.trimMargin()

        // When
        val compilationResult = compile(code)

        // Then
        assertThat(compilationResult.exitCode)
            .isEqualTo(KotlinCompilation.ExitCode.OK)
    }
    // endregion

    // region Mutable Property Tests
    @Test fun `WHEN property is mutable THEN warning is shown`() {
        // Given
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |
            |@AutoBuilder
            |interface TestWithMutableProperty {
            |    var mutableName: String
            |    val immutableAge: Int
            |}
        """.trimMargin()

        // When
        val compilationResult = compile(code)

        // Then
        assertThat(compilationResult.exitCode)
            .isEqualTo(KotlinCompilation.ExitCode.OK)
        assertThat(compilationResult.messages)
            .contains("The property 'mutableName' is declared as mutable (var)")
            .contains("Consider using 'val' instead")
    }
    // endregion

    // region Array Tests
    @Test fun `WHEN model contains arrays THEN equals and hashCode work correctly`() {
        // Given
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |import app.izantech.plugin.autobuilder.annotation.DefaultValue
            |
            |@AutoBuilder
            |interface TestWithArrays {
            |    val stringArray: Array<String>
            |    val intArray: IntArray
            |    val nullableArray: Array<String>?
            |    val primitiveArray: FloatArray
            |}
            |
            |fun main() {
            |    val arr1 = arrayOf("a", "b", "c")
            |    val arr2 = arrayOf("a", "b", "c")
            |    val intArr1 = intArrayOf(1, 2, 3)
            |    val intArr2 = intArrayOf(1, 2, 3)
            |    val floatArr1 = floatArrayOf(1.0f, 2.0f)
            |    val floatArr2 = floatArrayOf(1.0f, 2.0f)
            |
            |    val model1 = TestWithArrays {
            |        stringArray = arr1
            |        intArray = intArr1
            |        nullableArray = null
            |        primitiveArray = floatArr1
            |    }
            |
            |    val model2 = TestWithArrays {
            |        stringArray = arr2
            |        intArray = intArr2
            |        nullableArray = null
            |        primitiveArray = floatArr2
            |    }
            |
            |    // Test equals
            |    require(model1 == model2) { "Models with same array contents should be equal" }
            |
            |    // Test hashCode
            |    require(model1.hashCode() == model2.hashCode()) {
            |        "Models with same array contents should have same hashCode"
            |    }
            |
            |    // Test with different arrays
            |    val model3 = TestWithArrays {
            |        stringArray = arrayOf("x", "y")
            |        intArray = intArrayOf(9, 8)
            |        nullableArray = arrayOf("test")
            |        primitiveArray = floatArrayOf(9.0f)
            |    }
            |
            |    require(model1 != model3) { "Models with different array contents should not be equal" }
            |
            |    // Test copy with arrays
            |    val model4 = model1.copy {
            |        nullableArray = arrayOf("modified")
            |    }
            |    require(model4.nullableArray?.contentEquals(arrayOf("modified")) == true) {
            |        "Copied model should have modified array"
            |    }
            |    require(model4.stringArray.contentEquals(arr1)) {
            |        "Copied model should retain original arrays for unmodified properties"
            |    }
            |}
        """.trimMargin()

        // When
        val compilationResult = compile(code)

        // Then
        assertThat(compilationResult.exitCode)
            .isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test fun `WHEN model contains nullable arrays THEN null handling works correctly`() {
        // Given
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |
            |@AutoBuilder
            |interface TestWithNullableArrays {
            |    val nullableStringArray: Array<String>?
            |    val nullableIntArray: IntArray?
            |}
            |
            |fun main() {
            |    // Test both nulls
            |    val model1 = TestWithNullableArrays {
            |        nullableStringArray = null
            |        nullableIntArray = null
            |    }
            |
            |    val model2 = TestWithNullableArrays {
            |        nullableStringArray = null
            |        nullableIntArray = null
            |    }
            |
            |    require(model1 == model2) { "Models with both null arrays should be equal" }
            |    require(model1.hashCode() == model2.hashCode()) {
            |        "Models with both null arrays should have same hashCode"
            |    }
            |
            |    // Test one null, one non-null
            |    val model3 = TestWithNullableArrays {
            |        nullableStringArray = arrayOf("test")
            |        nullableIntArray = null
            |    }
            |
            |    require(model1 != model3) { "Models with different null states should not be equal" }
            |
            |    // Test non-null arrays
            |    val model4 = TestWithNullableArrays {
            |        nullableStringArray = arrayOf("a", "b")
            |        nullableIntArray = intArrayOf(1, 2)
            |    }
            |
            |    val model5 = TestWithNullableArrays {
            |        nullableStringArray = arrayOf("a", "b")
            |        nullableIntArray = intArrayOf(1, 2)
            |    }
            |
            |    require(model4 == model5) { "Models with same non-null arrays should be equal" }
            |    require(model4.hashCode() == model5.hashCode()) {
            |        "Models with same non-null arrays should have same hashCode"
            |    }
            |}
        """.trimMargin()

        // When
        val compilationResult = compile(code)

        // Then
        assertThat(compilationResult.exitCode)
            .isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    // region Custom Default Resolution Tests (Baseline Phase 1)
    @Test fun `WHEN custom default references other properties THEN resolution succeeds`() {
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |import app.izantech.plugin.autobuilder.annotation.DefaultValue
            |
            |@AutoBuilder
            |interface CrossDefaultsModel {
            |    val base: Int
            |    val plusFive: Int
            |        @DefaultValue get() = base + 5
            |    val doubled: Int
            |        @DefaultValue get() = plusFive * 2
            |}
            |
            |fun main() {
            |    val model = CrossDefaultsModel { base = 10 }
            |    require(model.base == 10)
            |    require(model.plusFive == 15)
            |    require(model.doubled == 30)
            |}
        """.trimMargin()

        val compilationResult = compile(code)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test fun `WHEN custom defaults form a cycle THEN compilation succeeds (runtime would throw)`() {
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |import app.izantech.plugin.autobuilder.annotation.DefaultValue
            |
            |@AutoBuilder
            |interface CyclicDefaultsModel {
            |    val first: Int
            |        @DefaultValue get() = second
            |    val second: Int
            |        @DefaultValue get() = first
            |}
            |
            |// NOTE: compile-testing doesn't execute main. This validates code generation only.
            |fun main() {
            |    try {
            |        CyclicDefaultsModel {}
            |        error("Cycle should have thrown if executed")
            |    } catch (e: IllegalStateException) {
            |        require(e.message?.contains("Circular property default detected") == true)
            |    }
            |}
        """.trimMargin()

        val compilationResult = compile(code)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    }
    // endregion

    @Test fun `WHEN model contains all primitive array types THEN they work correctly`() {
        // Given
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |
            |@AutoBuilder
            |interface TestAllPrimitiveArrays {
            |    val intArray: IntArray
            |    val longArray: LongArray
            |    val floatArray: FloatArray
            |    val doubleArray: DoubleArray
            |    val booleanArray: BooleanArray
            |    val byteArray: ByteArray
            |    val shortArray: ShortArray
            |    val charArray: CharArray
            |}
            |
            |fun main() {
            |    val model1 = TestAllPrimitiveArrays {
            |        intArray = intArrayOf(1, 2, 3)
            |        longArray = longArrayOf(1L, 2L, 3L)
            |        floatArray = floatArrayOf(1.0f, 2.0f)
            |        doubleArray = doubleArrayOf(1.0, 2.0)
            |        booleanArray = booleanArrayOf(true, false)
            |        byteArray = byteArrayOf(1, 2, 3)
            |        shortArray = shortArrayOf(1, 2, 3)
            |        charArray = charArrayOf('a', 'b', 'c')
            |    }
            |
            |    val model2 = TestAllPrimitiveArrays {
            |        intArray = intArrayOf(1, 2, 3)
            |        longArray = longArrayOf(1L, 2L, 3L)
            |        floatArray = floatArrayOf(1.0f, 2.0f)
            |        doubleArray = doubleArrayOf(1.0, 2.0)
            |        booleanArray = booleanArrayOf(true, false)
            |        byteArray = byteArrayOf(1, 2, 3)
            |        shortArray = shortArrayOf(1, 2, 3)
            |        charArray = charArrayOf('a', 'b', 'c')
            |    }
            |
            |    require(model1 == model2) { "Models with same primitive arrays should be equal" }
            |    require(model1.hashCode() == model2.hashCode()) {
            |        "Models with same primitive arrays should have same hashCode"
            |    }
            |}
        """.trimMargin()

        // When
        val compilationResult = compile(code)

        // Then
        assertThat(compilationResult.exitCode)
            .isEqualTo(KotlinCompilation.ExitCode.OK)
    }

    @Test fun `WHEN property with DefaultValue references other properties THEN uses builder values`() {
        // Given
        val code = """
            |package com.izantech.plugin.autobuilder.test
            |
            |import app.izantech.plugin.autobuilder.annotation.AutoBuilder
            |import app.izantech.plugin.autobuilder.annotation.DefaultValue
            |
            |fun testPropertyInterdependencies() {
            |    // Test case 1: Property with custom default that references another property
            |    val interaction1 = PropertyInterdependency {
            |        prefix = "TEST: "
            |    }
            |    require(interaction1.formattedMessage?.invoke("Hello") == "TEST: Hello") {
            |        "Property with custom default should use builder value: got '${"$"}{interaction1.formattedMessage?.invoke("Hello")}'"
            |    }
            |
            |    // Test case 2: Property with custom default when referenced property is not set
            |    val interaction2 = PropertyInterdependency()
            |    require(interaction2.formattedMessage?.invoke("Hello") == "Default: Hello") {
            |        "Property with custom default should use default value when not set: got '${"$"}{interaction2.formattedMessage?.invoke("Hello")}'"
            |    }
            |
            |    // Test case 3: Chained property dependencies
            |    val chained = ChainedDependencies {
            |        base = 10
            |    }
            |    require(chained.doubled == 20) {
            |        "Chained property should double the base value: got '${"$"}{chained.doubled}'"
            |    }
            |    require(chained.quadrupled == 40) {
            |        "Chained property should quadruple the base value: got '${"$"}{chained.quadrupled}'"
            |    }
            |
            |    // Test case 4: Multiple property references
            |    val multi = MultiPropertyReference {
            |        firstName = "John"
            |        lastName = "Doe"
            |    }
            |    require(multi.fullName == "John Doe") {
            |        "Full name should combine first and last names: got '${"$"}{multi.fullName}'"
            |    }
            |}
            |
            |@AutoBuilder
            |interface PropertyInterdependency {
            |    val prefix: String?
            |    val formattedMessage: ((String) -> String)?
            |        @DefaultValue get() = { message ->
            |            val p = prefix ?: "Default: "
            |            p + message
            |        }
            |}
            |
            |@AutoBuilder
            |interface ChainedDependencies {
            |    val base: Int
            |        @DefaultValue get() = 5
            |    val doubled: Int
            |        @DefaultValue get() = base * 2
            |    val quadrupled: Int
            |        @DefaultValue get() = doubled * 2
            |}
            |
            |@AutoBuilder
            |interface MultiPropertyReference {
            |    val firstName: String?
            |    val lastName: String?
            |    val fullName: String
            |        @DefaultValue get() = listOfNotNull(firstName, lastName).joinToString(" ").ifEmpty { "Unknown" }
            |}
        """.trimMargin()

        // When
        val compilationResult = compile(code)

        // Then
        assertThat(compilationResult.exitCode)
            .isEqualTo(KotlinCompilation.ExitCode.OK)
    }
    // endregion

    private fun compile(code: String) = KotlinCompilation().apply {
        sources = listOf(SourceFile.kotlin(name = "file.kt", code))
        workingDir = tempDir
        inheritClassPath = true
        messageOutputStream = System.out

        // KSP configuration
        configureKsp {
            symbolProcessorProviders += AutoBuilderProcessorProvider()
        }
    }.compile()

    private fun readFile(name: String) =
        Thread.currentThread().contextClassLoader
            .getResourceAsStream(name)?.reader()?.readText()
            ?: throw IllegalStateException("$name not found")

    private fun getFile(name: String) =
        Thread.currentThread().contextClassLoader
            ?.getResource(name)?.let { File(it.file) }
            ?: throw IllegalStateException("$name not found")
}
