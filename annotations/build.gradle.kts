@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
}

kotlin {
    // JVM
    jvm()

    // JS
    js(IR) {
        browser()
        nodejs()
    }

    // Wasm
    wasmJs {
        browser()
        nodejs()
    }

    // Native - iOS
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    // Native - macOS
    macosArm64()

    // Native - Linux
    linuxX64()
    linuxArm64()

    // Native - Windows
    mingwX64()

    // Native - tvOS
    tvosArm64()
    tvosSimulatorArm64()

    // Native - watchOS
    watchosArm32()
    watchosArm64()
    watchosSimulatorArm64()
    watchosDeviceArm64()
}
