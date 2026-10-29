pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // AdMob mediation: two partner SDKs are not published to Maven Central or Google's
        // repository, so the adapters pulled in by :app cannot resolve without these. Both are
        // scoped to their own group, so nothing else is ever looked up against them.
        maven("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") {
            content { includeGroupByRegex("com\\.mbridge\\.msdk.*") }
        }
        maven("https://artifact.bytedance.com/repository/pangle/") {
            content { includeGroup("com.pangle.global") }
        }
    }
}

rootProject.name = "PhoneCleaner"
include(":app")

// Feature library modules - one per feature, package ai.jhw.<feature>. No UI in any of them.
include(":storage")      // shared file layer: MediaStore queries, sizes, delete, large files
include(":junkcleaner")  // Smart Scan: cache, residual, empty folders, APK, trash
include(":aicleaner")    // AI Junk Cleaner: LiteRT (TensorFlow Lite) image classifier
include(":whatsapp")     // WA Cleaner: WhatsApp / WhatsApp Business media
include(":duplicates")   // Delete Duplicates: hash matching, keep best one
include(":appmanager")   // App Cleaner: installed apps, uninstall
include(":deviceinfo")   // Device, Processor, WiFi, Display, RAM, ROM, Camera
include(":battery")      // Battery Info, power saving, last full charge
include(":appusage")     // App Usage Report and Power Usage (UsageStatsManager)
include(":compressor")   // Photo Compressor
