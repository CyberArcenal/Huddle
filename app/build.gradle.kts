plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    id("org.openapi.generator")
    id("kotlin-kapt")
}

android {
    namespace = "com.cyberarcenal.huddle"
    compileSdk = 36

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
        "library" to "jvm-retrofit2", // Gumamit ng retrofit2 para sa Android
        "serializationLibrary" to "gson",
        "useCoroutines" to "true",
        "enumPropertyNaming" to "UPPERCASE",
        "enumUnknownDefaultCase" to "true",
        "collectionType" to "list",
    ))

    additionalProperties.set(mapOf(
        "nonPublicApi" to "false"
    ))

    // TAMA NA SYNTAX PARA SA KOTLIN DSL (.kts):
    typeMappings.set(mapOf(
        "file" to "MultipartBody.Part",
        "binary" to "MultipartBody.Part"
    ))

    importMappings.set(mapOf(
        "MultipartBody.Part" to "okhttp3.MultipartBody.Part"
    ))

    instantiationTypes.set(mapOf(
        "array" to "kotlin.collections.ArrayList",
        "map" to "kotlin.collections.HashMap"
    ))


    skipValidateSpec.set(true)
}


dependencies {
    implementation("com.google.accompanist:accompanist-pager:0.36.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)

    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.converter.scalars)
    implementation(libs.logging.interceptor)

    implementation(libs.coil.compose)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.activity.ktx)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.accompanist.swiperefresh)

    implementation(libs.ucrop)

    implementation(libs.reaction.picker)


    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer.hls)

    implementation(libs.generativeai)

    implementation(libs.identity.jvm)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.graphics)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.compose.foundation.v178)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.paging)
}
