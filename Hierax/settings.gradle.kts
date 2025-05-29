rootProject.name = "hierax"

pluginManagement {
    repositories {
        google {
            content { 
              	includeGroupByRegex("com\\.android.*")
              	includeGroupByRegex("com\\.google.*")
              	includeGroupByRegex("androidx.*")
              	includeGroupByRegex("android.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content { 
              	includeGroupByRegex("com\\.android.*")
              	includeGroupByRegex("com\\.google.*")
              	includeGroupByRegex("androidx.*")
              	includeGroupByRegex("android.*")
            }
        }
        mavenCentral()

        // for com.github.zahid4kh:deskit:1.2.0
        maven { url = uri("https://jitpack.io") }
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
includeBuild("convention-plugins")
include(":logic")
include(":app:composeApp")

