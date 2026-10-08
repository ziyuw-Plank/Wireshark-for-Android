plugins {
    id("com.android.application") version "9.4.1" apply false
    // AGP 9 has built-in Kotlin; this pins a newer Kotlin Gradle plugin for the Compose compiler.
    id("org.jetbrains.kotlin.android") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}
