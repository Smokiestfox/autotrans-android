plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.autotrans.android.core.testing"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    api(project(":domain"))
    api(project(":core:common"))
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.mockk)
    api(libs.google.truth)
    api(libs.junit5.api)
    api(libs.junit5.params)
    api(libs.junit4)
}
