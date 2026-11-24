plugins {
    id("com.android.application")
}

android {
    namespace = "co.pazmental"
    compileSdk = 36

    defaultConfig {
        applicationId = "co.pazmental"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
