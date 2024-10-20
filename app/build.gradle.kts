plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "gr.xmp.torstream"
    compileSdk = 34
    // Link Gradle



    defaultConfig {
        applicationId = "gr.xmp.torstream"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures{viewBinding = true}

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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.github.bumptech.glide:glide:5.0.0-rc01")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.github.ybq:Android-SpinKit:1.4.0")
    implementation("org.videolan.android:libvlc-all:3.5.4-eap4")

    implementation("org.libtorrent4j:libtorrent4j:2.1.0-31")
    implementation("org.libtorrent4j:libtorrent4j-android-x86_64:2.1.0-31")
    implementation ("org.libtorrent4j:libtorrent4j-android-arm64:2.1.0-31")
    implementation ("org.libtorrent4j:libtorrent4j-android-arm:2.1.0-31")
    implementation ("org.libtorrent4j:libtorrent4j-android-x86_64:2.1.0-31")
    implementation ("org.libtorrent4j:libtorrent4j-android-x86:2.1.0-31")


    implementation("androidx.recyclerview:recyclerview:1.3.2")
}