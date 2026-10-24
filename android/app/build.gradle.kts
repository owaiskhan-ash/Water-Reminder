import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/**
 * Signing material is supplied through environment variables so that no
 * keystore ever has to live inside the repository.
 *
 * The names deliberately differ from the SigningConfig property names: inside
 * `signingConfigs { create("release") { ... } }` the receiver's members win
 * name resolution, so `keyPassword = keyPassword` would assign the property to
 * itself and silently sign with nothing.
 */
val keystoreFilePath: String = System.getenv("ANDROID_KEYSTORE_FILE").orEmpty()
val keystoreStorePass: String = System.getenv("ANDROID_KEYSTORE_PASSWORD").orEmpty()
val keystoreAliasName: String = System.getenv("ANDROID_KEY_ALIAS").orEmpty()
val keystoreKeyPass: String = System.getenv("ANDROID_KEY_PASSWORD").orEmpty()

android {
    namespace = "com.owaiskhan.waterreminder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.owaiskhan.waterreminder"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreFilePath.isNotBlank()) {
            require(
                keystoreStorePass.isNotBlank() &&
                    keystoreAliasName.isNotBlank() &&
                    keystoreKeyPass.isNotBlank(),
            ) {
                "Signing material is incomplete: ANDROID_KEYSTORE_PASSWORD, " +
                    "ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD must all be set."
            }
            create("release") {
                storeFile = file(keystoreFilePath)
                storePassword = keystoreStorePass
                keyAlias = keystoreAliasName
                keyPassword = keystoreKeyPass
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystoreFilePath.isNotBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        viewBinding = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/*.version",
            "DebugProbesKt.bin",
        )
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("com.google.android.material:material:1.12.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core-ktx:1.6.1")
}
