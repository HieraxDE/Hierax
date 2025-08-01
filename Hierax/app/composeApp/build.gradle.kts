@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

kotlin {
    jvm()
    wasmJs() {
        browser {
            commonWebpackConfig {
                outputFileName = "app.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            api(compose.materialIconsExtended)

            implementation ("io.github.bonede:tree-sitter:0.25.3")
            implementation ("io.github.bonede:tree-sitter-kotlin:0.3.8.1")


            implementation(project(":logic"))
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)

            implementation("com.github.zahid4kh:deskit:1.2.0")

            implementation ("io.github.bonede:tree-sitter:0.25.3")
            implementation ("io.github.bonede:tree-sitter-kotlin:0.3.8.1")
        }

    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "app"
            packageVersion = "1.0.0"
        }
    }
}
