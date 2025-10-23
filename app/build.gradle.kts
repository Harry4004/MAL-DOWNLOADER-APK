plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.harry.maldownloader"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.harry.maldownloader"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "3.1"
        vectorDrawables { useSupportLibrary = true }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "MAL_CLIENT_ID", propertyOrEnv("MAL_CLIENT_ID", "\"aaf018d4c098158bd890089f32125add\"") )
        buildConfigField("String", "APP_VERSION", "\"3.1-Enhanced\"")
        buildConfigField("String", "JIKAN_BASE_URL", "\"https://api.jikan.moe/v4/\"")
        buildConfigField("String", "MAL_BASE_URL", "\"https://api.myanimelist.net/v2/\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug-enhanced"
            buildConfigField("boolean", "ENABLE_LOGGING", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "ENABLE_LOGGING", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE",
                "/META-INF/LICENSE.txt",
                "/META-INF/NOTICE",
                "/META-INF/NOTICE.txt"
            )
        }
    }
    
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // Enhanced Material Design 3
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.0")
    
    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.fragment:fragment-ktx:1.8.4")
    implementation("com.google.android.material:material:1.12.0")
    
    // Enhanced Lifecycle and ViewModels
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    
    // Enhanced Compose
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Enhanced HTTP, API, and JSON processing
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")
    
    implementation("org.json:json:20231013")
    
    // Enhanced Image Loading, Processing, and Metadata
    implementation("io.coil-kt:coil:2.7.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.drewnoakes:metadata-extractor:2.19.0") // XMP metadata writing
    implementation("org.apache.sanselan:sanselan:0.97-incubator") // Advanced image metadata
    
    // File operations and storage
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("commons-io:commons-io:2.15.1") // Enhanced file utilities
    
    // Enhanced Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Background processing and notifications
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.work:work-multiprocess:2.9.0")
    
    // Enhanced permissions and settings
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.karumi:dexter:6.2.3") // Simplified permissions handling
    
    // Progress and UI enhancements
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.airbnb.android:lottie-compose:6.0.1") // Animations
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose:compose-bom:2024.02.00")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    // Data storage and preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    implementation("com.google.code.gson:gson:2.11.0")
}

fun propertyOrEnv(key: String, fallback: String): String = if (project.hasProperty(key)) {
    "\"${project.property(key)}\""
} else {
    System.getenv(key)?.let { "\"$it\"" } ?: fallback
}

kotlin {
    jvmToolchain(17)
}