plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.kotlin.serialization) // 添加 Kotlin serialization 插件
}

android {
    namespace = "com.example.note"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.note"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        // 添加 freeCompilerArgs 以进行全局 Opt-in
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
            // 如果有其他需要全局 Opt-in 的 API，继续在此添加
        )
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.1" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // 添加 Material Icons (假设别名为 material.icons.core 和 material.icons.extended)
    implementation(libs.material.icons.core)
    implementation(libs.material.icons.extended)

    // 添加 Navigation Compose (假设别名为 navigation.compose)
    implementation(libs.navigation.compose)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto) // 保留，可用于 DataStore 或字段加密

    // 添加 Material Components for Android (用于 XML 主题等)
    implementation(libs.google.material)

    // 如果使用 SQLCipher (假设别名为 sqlcipher 和 sqlite.ktx)
    // implementation(libs.sqlcipher)
    // implementation(libs.sqlite.ktx)

    // 添加 Kotlinx Serialization JSON 库
    implementation(libs.kotlinx.serialization.json)

    // 添加 Coil for Compose
    implementation(libs.coil.compose)

    // 移除 Compose Markdown 库
    // implementation(libs.compose.markdown)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
