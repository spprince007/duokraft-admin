plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "se.duokraft.admin"
    compileSdk = 35

    defaultConfig {
        applicationId = "se.duokraft.admin"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // The website this app manages. Not a secret: it is the public site address.
        // Change it here (or per build type) to point the app at another site.
        buildConfigField("String", "SITE_URL", "\"https://sketch-mcp-duokraft.staging2.tokig.site/\"")
        // Identifies this app to WordPress' "Authorize application" screen. Fixed per app, not a secret.
        buildConfigField("String", "WP_APP_ID", "\"6f1c2a4e-8d3b-4f5a-9c7e-2b1d0e9a7c35\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.exifinterface)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
