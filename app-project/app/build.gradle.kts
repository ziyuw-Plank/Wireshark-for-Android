import java.util.Properties

plugins {
    id("com.android.application")
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
    compileSdk = 36

    defaultConfig {
        applicationId = "org.sharkdroid"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-ws4.6.9"
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
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}
