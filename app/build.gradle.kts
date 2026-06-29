plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    kotlin("kapt")
}

// google-services.json isn't always checked in; without it this plugin fails the build.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS-таргеты объявлены, но компилируются ТОЛЬКО на macOS (Kotlin/Native).
    // На Windows их сборка не запускается и не требуется (Фаза 0 проверяется через Android).
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        androidMain.dependencies {
            // Compose BOM (Android, androidx.compose — НЕ org.jetbrains.compose: см. Фаза 0)
            implementation(project.dependencies.platform(libs.compose.bom))
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.graphics)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.animation)
            implementation(libs.compose.animation.core)
            implementation(libs.activity.compose)

            // Navigation
            implementation(libs.navigation.compose)
            implementation(libs.hilt.navigation.compose)

            // Lifecycle
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)

            // Hilt DI (runtime)
            implementation(libs.hilt.android)

            // Retrofit + OkHttp
            implementation(libs.retrofit)
            implementation(libs.retrofit.converter.gson)
            implementation(libs.okhttp)
            implementation(libs.okhttp.logging.interceptor)

            // Gson
            implementation(libs.gson)

            // Coil (загрузка изображений)
            implementation(libs.coil.compose)

            // DataStore (хранение токенов)
            implementation(libs.datastore.preferences)

            // Accompanist (swipe refresh, permissions)
            implementation(libs.accompanist.swiperefresh)
            implementation(libs.accompanist.permissions)

            // Core
            implementation(libs.core.ktx)
            implementation(libs.core.splashscreen)
            implementation(libs.exifinterface)

            // Firebase Cloud Messaging (push notifications)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging.ktx)
            implementation(libs.kotlinx.coroutines.play.services)
        }

        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.mockk)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.robolectric)
            implementation(libs.androidx.test.core)
        }

        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.test.ext.junit)
            implementation(libs.androidx.test.runner)
        }

        iosMain.dependencies {
            // Только для iOS-stub (ComposeUIViewController). На Android не попадает.
            implementation(compose.runtime)
            implementation(compose.ui)
        }
    }
}

android {
    namespace = "ru.agromarket"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.agromarket"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"https://agro.assaru.space/api/v1/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// Hilt annotation processor (kapt). Единственный потребитель kapt — Hilt;
// будет полностью устранён в Фазе 1c (Hilt → Koin).
dependencies {
    add("kapt", libs.hilt.compiler)
}

kapt {
    correctErrorTypes = true
}
