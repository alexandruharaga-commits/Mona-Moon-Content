plugins {
    id("com.android.application")
}

android {
    namespace = "com.john.executive"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.john.executive"
        minSdk = 26
        targetSdk = 35
        versionCode = 56
        versionName = "5.6"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
