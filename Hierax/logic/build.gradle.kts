@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    id("convention.publication")
}

group = "hierax"
version = "1.0.0"

kotlin {
    jvm()
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.multiplatformSettings)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }

    }

}
