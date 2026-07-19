plugins {
    id("com.android.application")
}

android {
    namespace = "com.woojung.jerusalemmix"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.woojung.jerusalemmix"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.3.1-layout-fix"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main").assets.directories.add("../docs")
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        // AGP 9.3 knows API 37, but the public SDK repository does not yet
        // provide platforms;android-37. API 36 is the latest installable SDK.
        disable += setOf("OldTargetApi", "GradleDependency")
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
