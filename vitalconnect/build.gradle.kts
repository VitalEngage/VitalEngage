plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
//    id("com.google.devtools.ksp")
}

android {
    namespace = "com.eemphasys.vitalconnect"
    compileSdk = 34

    defaultConfig {
        //applicationId = "com.eemphasys.vitalconnect"
        minSdk = 24
        targetSdk = 34
        /*versionCode = 1
        versionName = "1.0"*/

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.2"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

version = "1.0.0009"

dependencies {

    implementation(project(":commonmobilelib2.6"))
    //implementation ("com.twilio:conversations-android-with-symbols:6.0.3")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.room:room-common:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.emoji:emoji:1.1.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.firebase:firebase-messaging-ktx:23.4.1")
    kapt ("androidx.room:room-compiler:2.6.1")
//    ksp ("androidx.room:room-compiler:2.6.1")
    //implementation("androidx.paging:paging-common-ktx:3.2.1")
    implementation("androidx.paging:paging-runtime-ktx:2.1.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // retrofit

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")

// GSON
    // Android
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.1")
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    //Twilio
    implementation("com.twilio:conversations-android:6.0.3")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    //Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")
    //implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinx_coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
// coroutine

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.2")

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    implementation ("androidx.recyclerview:recyclerview:1.2.0-alpha04")
    implementation ("com.intuit.sdp:sdp-android:1.0.6")
    implementation ("com.intuit.ssp:ssp-android:1.0.6")

    //datechecker
    implementation ("com.googlecode.libphonenumber:libphonenumber:8.13.42")
    implementation ("com.google.android.gms:play-services-auth:21.2.0")
}