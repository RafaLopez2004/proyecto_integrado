plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.prueba"
    compileSdk = 34

    compileOptions {
        // For AGP 4.1+
        isCoreLibraryDesugaringEnabled = true
    }

    defaultConfig {
        applicationId = "com.example.prueba"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Importar Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    // Importar GSON para poder leer JSON
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // Location
    implementation("com.google.android.gms:play-services-location:19.0.1")
    implementation(libs.androidx.cardview)
    //Enables use of stuff wich requires API level 25+
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    // Import material design
    implementation("com.google.android.material:material:1.11.0")
    // Allows for easier management of permisions
    implementation("com.vmadalin:easypermissions-ktx:1.0.0")
    // Allows the app to interact with our firebase DB
    implementation("com.google.firebase:firebase-bom:32.8.0")
    implementation(libs.firebase.common.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    //
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}