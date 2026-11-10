import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}
dependencies {
    implementation(project(":app:shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "online.lds.nos"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "online.lds.nos"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Same as ldsonline / Element: secrets from global ~/.gradle/gradle.properties
    // (KEYSTORE, KEYSTORE_PASSWORD, KEY_ALIAS_LDS, KEY_PASSWORD) — also used as
    // signing.element.* for lds-chat-element-android release.
    signingConfigs {
        create("release") {
            if (project.hasProperty("KEYSTORE")) {
                storeFile = file(project.properties["KEYSTORE"] as String)
                storePassword = project.properties["KEYSTORE_PASSWORD"] as String
                keyAlias = project.properties["KEY_ALIAS_LDS"] as String
                keyPassword = project.properties["KEY_PASSWORD"] as String
            }
        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
}