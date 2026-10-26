plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ramirez.yourpeak"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ramirez.yourpeak"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Dirección de Spring Boot. 10.0.2.2 es el "localhost" de la PC visto desde el emulador.
        // En un celular físico usa la IP de la PC: ./gradlew installDebug -Pyourpeak.apiUrl=http://192.168.1.50:8080/
        val apiUrl = providers.gradleProperty("yourpeak.apiUrl").getOrElse("http://10.0.2.2:8080/")
        buildConfigField("String", "API_BASE_URL", "\"${apiUrl.trimEnd('/')}/\"")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // --- RED Y COMUNICACIÓN CON SPRING BOOT ---
    // Retrofit: Cliente HTTP para realizar peticiones REST a Spring Boot
    implementation(libs.retrofit)
    // Converter Gson: Convierte automáticamente las respuestas JSON a objetos Kotlin
    implementation(libs.retrofit.converter.gson)
    // Logging Interceptor: Muestra en Logcat las peticiones y respuestas HTTP
    implementation(libs.okhttp.logging.interceptor)

    // --- ARQUITECTURA Y NAVEGACIÓN DE UI ---
    // Navigation Compose: Permite cambiar entre pantallas (Splash, Login, Registro)
    implementation(libs.androidx.navigation.compose)
    // ViewModel Compose: Maneja la lógica de estado y llamadas asíncronas
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Runtime Compose: collectAsStateWithLifecycle para observar el estado de los ViewModel
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}