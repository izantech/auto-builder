@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.google.ksp)
}

kotlin {
    jvm()
    iosSimulatorArm64()
    wasmJs { nodejs() }
    macosArm64()
    linuxX64()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.annotations)
        }
    }
}

dependencies {
    add("kspJvm", projects.processor)
    add("kspIosSimulatorArm64", projects.processor)
    add("kspWasmJs", projects.processor)
    add("kspMacosArm64", projects.processor)
    add("kspLinuxX64", projects.processor)
}
