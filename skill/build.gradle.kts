import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.com.android.library)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.compose)
    id("maven-publish")
}

android {
    namespace = "org.dicio.skill"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        // Flag to enable support for the new language APIs (needed for dicio-numbers LocalDateTime)
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(libs.versions.java.get())
        }
    }

    buildFeatures {
        compose = true
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    // Desugaring (needed for dicio-numbers LocalDateTime)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // dicio-numbers is needed to bring ParserFormatter into the classpath
    implementation(libs.dicio.numbers)

    // Compose (check out https://developer.android.com/jetpack/compose/bom/bom-mapping)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)

    // Testing
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
}
