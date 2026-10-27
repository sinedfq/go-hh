plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services") apply false
}

if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")

// Reuse the web project's public JS API key; Gradle property can override it.
val frontendMapFile = rootProject.file("../frontend/src/components/common/YandexMap.jsx")
val frontendMapKey = if (frontendMapFile.exists()) Regex("YANDEX_MAPS_API_KEY = '([^']+)'").find(frontendMapFile.readText())?.groupValues?.get(1).orEmpty() else ""
val yandexMapsKey = providers.gradleProperty("YANDEX_MAPS_API_KEY").getOrElse(frontendMapKey)
val buildNumberFile = rootProject.file("../.cache/android-build-number.txt")
val appBuildNumber = providers.gradleProperty("BUILD_NUMBER").getOrElse(
    if (buildNumberFile.exists()) buildNumberFile.readText().trim() else "1"
).toInt()
require(appBuildNumber in 1..2100000000) { "BUILD_NUMBER must be between 1 and 2100000000" }

android {
    sourceSets.getByName("main").assets.srcDir(rootProject.file("../frontend/public/legal"))
    namespace = "com.example.trudomix"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.trudomix"
        minSdk = 26
        targetSdk = 35
        versionCode = appBuildNumber
        versionName = "1.0"
        buildConfigField("String", "YANDEX_MAPS_API_KEY", "\"${yandexMapsKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"${providers.gradleProperty("API_BASE_URL").getOrElse("http://10.0.2.2:8080/")}\"")
            manifestPlaceholders["cleartextTraffic"] = "true"
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"${providers.gradleProperty("PRODUCTION_API_BASE_URL").getOrElse("https://api.trudomix.example/")}\"")
            manifestPlaceholders["cleartextTraffic"] = "false"
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Исправляем устаревший kotlinOptions на новый синтаксис
    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("org.commonmark:commonmark:0.24.0")
    implementation("dev.chrisbanes.haze:haze:1.1.1")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // Основные библиотеки AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM и материалы
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    // Навигация
    implementation(libs.androidx.navigation.compose)

    // Сеть (Retrofit + OkHttp)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Сериализация JSON
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization)

    // Загрузка изображений
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation(libs.coil.compose)

    // Хранение данных
    implementation(libs.androidx.datastore.preferences)

    // Корутины
    implementation(libs.kotlinx.coroutines.android)

    // Тестовые зависимости (оставляем только базовые, если они есть в toml,
    // но так как их нет, мы их пока убираем, чтобы не было ошибок)
    // Если захочешь добавить тесты позже, нужно будет добавить их в libs.versions.toml
}
