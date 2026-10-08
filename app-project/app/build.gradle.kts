import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing: NOTHING secret lives in this repository.
// Point SHARKDROID_KEYSTORE_PROPS (env) or -PkeystoreProps=... at an untracked properties file
// containing storeFile / storePassword / keyAlias / keyPassword. Default: ../keys/keystore.properties
// (outside the project). Without it, release builds are produced unsigned.
val keystorePropsFile: File = run {
    val p = (findProperty("keystoreProps") as String?) ?: System.getenv("SHARKDROID_KEYSTORE_PROPS")
    if (p != null) file(p) else rootProject.file("../keys/keystore.properties")
}
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "org.sharkdroid"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.sharkdroid"
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0-ws4.6.9"
        ndk { abiFilters += "arm64-v8a" }
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = keystorePropsFile.parentFile.resolve(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    androidResources {
        // Only ship the languages the UI is translated into.
        localeFilters += listOf("en", "zh")
    }

    packaging {
        jniLibs {
            // Executables are shipped as lib*.so and must be extracted to nativeLibraryDir
            useLegacyPackaging = true
            // Already stripped by our own build; don't let AGP touch them.
            keepDebugSymbols += "**/*.so"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("roborazzi.test.record", "true")
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
                it.systemProperty("sharkdroid.screenshotDir",
                    (findProperty("screenshotDir") as String?) ?: rootProject.file("../out/screenshots").absolutePath)
                it.maxHeapSize = "3g"
            }
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // JVM screenshot tests (Robolectric native graphics + Roborazzi); test-only, never shipped.
    testImplementation(composeBom)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.76.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.76.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
