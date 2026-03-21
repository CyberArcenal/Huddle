plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.openapi.generator")
}

android {
    namespace = "com.cyberarcenal.huddle"
    compileSdk = 36 // FIXED: Inayos ang Release(36) patungong 36

    defaultConfig {
        applicationId = "com.cyberarcenal.huddle"
        minSdk = 26
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets {
        getByName("main") {
            java.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/kotlin").get().asFile)
        }
    }

    buildFeatures {
        compose = true
    }
}

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$projectDir/src/main/openapi/schema.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)
    packageName.set("com.cyberarcenal.huddle.api")
    configOptions.set(mapOf(
        "library" to "jvm-retrofit2",
        "serializationLibrary" to "gson",
        "useCoroutines" to "true",
        "enumPropertyNaming" to "UPPERCASE",
        "enumUnknownDefaultCase" to "true"
    ))
    additionalProperties.set(mapOf(
        "nonPublicApi" to "false"
    ))
    skipValidateSpec.set(true)
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.remote.creation.core)
    implementation(libs.androidx.compose.runtime)

    // REMOVED: libs.androidx.compose.remote.creation.compose (Source of SDK 29 error)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.retrofit2:converter-scalars:3.0.0")
    implementation("androidx.paging:paging-runtime-ktx:3.4.2")
    implementation("androidx.paging:paging-compose:3.3.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.36.0")

    implementation("com.github.yalantis:ucrop:2.2.8")
    implementation("androidx.activity:activity-ktx:1.13.0")
}
