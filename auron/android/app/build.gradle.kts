plugins {
    id("com.android.application")
}

android {
    namespace = "com.auron.assistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.auron.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
