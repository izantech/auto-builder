@file:Suppress("UnstableApiUsage")

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.MavenPublishPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.maven.publish) apply false
}

allprojects {
    // Configure the JVM target version
    val javaVersion = "17"
    tasks.withType<KotlinCompile> {
        compilerOptions {
            suppressWarnings = true
            jvmTarget = JvmTarget.fromTarget(javaVersion)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.contracts.ExperimentalContracts",
                "-opt-in=kotlin.time.ExperimentalTime",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-Xcontext-receivers",
                "-Xmulti-dollar-interpolation",
            )
        }
    }
    tasks.withType<JavaCompile> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

subprojects {
    group = requireNotNull(project.findProperty("GROUP"))
    version = requireNotNull(project.findProperty("VERSION_NAME"))

    // Configure the Maven publishing
    plugins.withType<MavenPublishPlugin> {
        configure<MavenPublishBaseExtension> {
            coordinates(group.toString(), "autobuilder-${project.name}", version.toString())
            configure(KotlinJvm(JavadocJar.Javadoc(), sourcesJar = true))
        }
    }
}
