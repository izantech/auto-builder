@file:OptIn(ExperimentalCompilerApi::class)

package app.izantech.plugin.autobuilder.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import org.assertj.core.api.Assertions.assertThat
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
