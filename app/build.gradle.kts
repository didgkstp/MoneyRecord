plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.hanse.SoftBrain"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.hanse.SoftBrain"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
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

    // 로컬 DB (Room) + 화면 자동 갱신용 LiveData
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.lifecycle.livedata)

    testImplementation(libs.junit)
}