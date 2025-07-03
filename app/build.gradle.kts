plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.checkmark"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.checkmark"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.scenecore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.android.material:material:1.8.0")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.google.android.material:material:1.6.0")
    implementation("com.prolificinteractive:material-calendarview:1.4.3")
    implementation("com.google.android.material:material:1.6.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.prolificinteractive:material-calendarview:1.4.3")

}