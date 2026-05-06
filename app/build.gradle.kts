plugins {
    id("com.android.application")
}

android {
    namespace = "com.wrdwarn.rewardads"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wrdwarn.rewardads"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:25.2.0")
}
