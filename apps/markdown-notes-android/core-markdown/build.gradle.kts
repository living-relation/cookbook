plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.livingrelation.markdownnotes.core.markdown"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("org.commonmark:commonmark:0.24.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.24.0")
    testImplementation("junit:junit:4.13.2")
}
