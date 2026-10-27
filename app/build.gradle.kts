import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Configuração da API lida de local.properties (fora do controle de versão)
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "br.unip.tmb"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.sophiacostameuapptesteunip"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String", "API_BASE_URL",
            "\"${localProps.getProperty("API_BASE_URL", "http://10.0.2.2:8000")}\""
        )
        buildConfigField(
            "String", "API_TRANSPORT_KEY",
            "\"${localProps.getProperty("API_TRANSPORT_KEY", "")}\""
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}