plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.ronda"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.ronda"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    // Navigation Component: una sola Activity + Fragments, como en las practicas
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    // Retrofit + Gson: cliente HTTP contra la API de Ronda (clase 3)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    // Hilt: inyeccion de dependencias. En Java se usa annotationProcessor,
    // no kapt (kapt es para proyectos Kotlin).
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
