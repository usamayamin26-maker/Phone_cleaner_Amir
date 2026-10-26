import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.room)
    alias(libs.plugins.ksp)

    // Firebase (google-services, crashlytics) is added together with google-services.json.
}

android {
    namespace = "com.phone.cleaner"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.phone.cleaner"

        minSdk = 24
        targetSdk = 36

        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all { test ->
            test.inputs.files(fileTree("src") { include("*/res/**/*.xml") })
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }
    }

    buildTypes {
        release {
            // R8 shrinks and obfuscates the release build. A minified build breaks in ways a debug
            // build never shows, so after any dependency or keep-rule change read
            // app/build/outputs/mapping/release/usage.txt for anything reached by name and test a
            // release install. Keep rules live in proguard-rules.pro and in each library's
            // consumer-rules.pro. Never widen one to silence a warning.
            isMinifyEnabled = true
            // A resource looked up by name (getIdentifier) must be listed in res/raw/keep.xml.
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // The release build exactly (same R8, resource shrinking and rules), but signed with the
        // debug key and installed beside the debug app, so a minified build can be tested on a
        // device without the release keystore. The library modules have no staging type and
        // fall back to their release variant.
        create("staging") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
}

// Room writes each database version's schema here, so a migration can be checked against it.
room {
    schemaDirectory("$projectDir/schemas")
}

// Phone_Cleaner_Vcode_1_Vname_1.0.0-debug.apk.
base {
    archivesName = "Phone_Cleaner_Vcode_${android.defaultConfig.versionCode}_Vname_${android.defaultConfig.versionName}"
}

dependencies {
    implementation(project(":storage"))
    implementation(project(":junkcleaner"))
    implementation(project(":aicleaner"))
    implementation(project(":whatsapp"))
    implementation(project(":duplicates"))
    implementation(project(":appmanager"))
    implementation(project(":deviceinfo"))
    implementation(project(":battery"))
    implementation(project(":appusage"))
    implementation(project(":compressor"))

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.core.splashscreen)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    // room - local cache (data/local)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Scalable sizes (sdp) and scalable text sizes (ssp)
    implementation(libs.intuit.sdp)
    implementation(libs.intuit.ssp)

    // lottie-anim
    implementation(libs.lottie)

    // Coil 3: image thumbnails, with video frames through coil-video
    implementation(libs.coil)
    implementation(libs.coil.video)
}
