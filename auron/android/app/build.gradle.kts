plugins {
    id("com.android.application")
}

android {
    namespace = "com.auron.assistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.auron.assistant.testsafe"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "0.9.1-test-safe"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
