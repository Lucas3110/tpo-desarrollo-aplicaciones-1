
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}

/**
 * Direccion del backend.
 *
 * Por defecto apunta al emulador: 10.0.2.2 es el alias que redirige al
 * localhost de la PC ("localhost" NO sirve, desde el emulador apunta al
 * propio emulador).
 *
 * Para probar en un celular fisico hay que poner la IP de la PC en
 * `local.properties`, que NO se commitea:
 *
 *     ronda.baseUrl=http://192.168.0.153:3000/
 *
 * La IP la imprime el backend al arrancar, en la linea "Celular (WiFi) -> ...".
 *
 * Esta como propiedad y no como constante en el codigo a proposito: antes la
 * IP vivia en NetworkModule.java, y cada vez que alguien la cambiaba para
 * probar en su celular se la llevaba puesta en un commit y rompia el emulador
 * de todos los demas.
 */
val urlDelBackend: String =
    (project.findProperty("ronda.baseUrl") as String?) ?: "http://10.0.2.2:3000/"

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

        buildConfigField("String", "BASE_URL", "\"$urlDelBackend\"")
    }

    // Desde AGP 8 hay que pedirlo explicitamente para que se genere
    // BuildConfig con nuestros campos.
    buildFeatures {
        buildConfig = true
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
    // Glide: descarga, cachea y muestra la fotoPrincipal (una URL) de cada
    // publicacion en el listado. Sin annotationProcessor: no hace falta.
    implementation(libs.glide)
    // Biometria: desbloqueo de la sesion con la huella del dispositivo (Punto 1)
    implementation(libs.biometric)
    // EncryptedSharedPreferences: guarda la copia del token con una llave del
    // Keystore, que es lo que la huella desbloquea.
    implementation(libs.security.crypto)
    // Room: la cache local del modo sin conexion (Punto 6). Como el proyecto
    // es Java va con annotationProcessor, no con kapt.
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}

