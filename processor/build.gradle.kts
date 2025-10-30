plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

dependencies {
    implementation(projects.annotations)
    implementation(libs.google.ksp.processor.api)
    implementation(libs.kotlinpoet.lib)
    implementation(libs.kotlinpoet.ksp)

    // Testing
    testImplementation(libs.androidX.annotations)
    testImplementation(platform(libs.androidX.compose.bom))
    testImplementation(libs.androidX.compose.runtime)
    testImplementation(libs.androidX.compose.ui)
    testImplementation(libs.test.assertj)
    testImplementation(libs.test.junit.api)
    testImplementation(libs.test.processors.core)
    testImplementation(libs.test.processors.ksp)
    testImplementation(platform(libs.test.junit.bom))
    testRuntimeOnly(libs.test.junit.platform.launcher)
    testRuntimeOnly(libs.test.junit.engine)
}

// Configure JUnit
tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
    jvmArgs(
        "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
    )
}
