plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false // Set to 1.9.25
    id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false // KSP for Moshi codegen
    id("com.google.dagger.hilt.android") version "2.51.1" apply false // Added Hilt plugin
}