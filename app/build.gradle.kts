import org.eclipse.jgit.api.Git
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.stypox.dicio.unicodeCldrPlugin.UnicodeCldrLanguagesTask

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.dicio.sentences.compiler.plugin)
        classpath(libs.dicio.unicode.cldr.plugin)
    }
}

plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.compose)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.parcelize)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.com.google.dagger.hilt.android)
    alias(libs.plugins.com.google.protobuf)
    alias(libs.plugins.dicio.sentences.compiler.plugin)
    alias(libs.plugins.dicio.unicode.cldr.plugin)
}

android {
    namespace = "org.stypox.dicio"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.stypox.dicio"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 17
        versionName = "4.0"
        testInstrumentationRunner = "org.stypox.dicio.CustomTestRunner"

        vectorDrawables.useSupportLibrary = true

        ndk {
            abiFilters += arrayOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        debug {
            var normalizedGitBranch = gitBranch().replaceFirst("^[^A-Za-z]+", "").replace(Regex("[^0-9A-Za-z]+"), "")
            applicationIdSuffix = ".$normalizedGitBranch"
            versionNameSuffix = "-$normalizedGitBranch"

            val isScreenshotTest = (project.findProperty("android.testInstrumentationRunnerArguments.class") as? String)
                ?.contains("creenshot") == true
            if (!isScreenshotTest) {
                // only change the app name if we are not taking screenshots
                resValue("string", "app_name", "Dicio-${gitBranch()}")
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get())
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(libs.versions.java.get())
            // Just to opt in to future behavior and fix a warning; remove once the future behavior
            // becomes the default behavior. This is about @Annotations on data class fields.
            freeCompilerArgs = listOf("-Xannotation-default-target=param-property")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    plugins {
        generateProtoTasks {
            all().forEach {
                it.builtins {
                    create("kotlin") {
                        option("lite")
                    }
                    create("java") {
                        option("lite")
                    }
                }
            }
        }
    }
}

// workaround for https://github.com/google/ksp/issues/1590
// remove when not needed anymore
val kspKotlinRegex = "^ksp(.*)Kotlin$".toRegex()
androidComponents {
    onVariants(selector().all()) { variant ->
        afterEvaluate {
            tasks.named(kspKotlinRegex::matches).configureEach {
                val capName = kspKotlinRegex.find(name)!!.groupValues[1]
                dependsOn(tasks.named("generate${capName}Proto"))
            }
        }
    }
}

tasks.withType(UnicodeCldrLanguagesTask::class) {
    // tell the UnicodeCldrLanguagesTask plugin which git commit of the
    // https://github.com/unicode-org/cldr repo to use as a source of data
    unicodeCldrGitCommit = libs.versions.unicodeCldrGitCommit
}

dependencies {
    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Dicio own libraries
    implementation(libs.dicio.numbers)
    implementation(project(":skill"))

    // Android
    implementation(libs.appcompat)

    // Compose (check out https://developer.android.com/jetpack/compose/bom/bom-mapping)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.test.android.compose.ui.test.junit4)
    debugImplementation(libs.debug.compose.ui.tooling)
    debugImplementation(libs.debug.compose.ui.test.manifest)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)
    androidTestImplementation(libs.hilt.android.testing)
    //androidTestAnnotationProcessor(libs.hilt.android.compiler)
    testImplementation(libs.hilt.android.testing)
    testAnnotationProcessor(libs.hilt.android.compiler)

    // Protobuf and Datastore
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.protobuf.java.lite)
    implementation(libs.datastore)

    // Navigation
    implementation(libs.kotlin.serialization)
    implementation(libs.navigation)

    // Vosk
    implementation(libs.jna) { artifact { type = "aar" } }
    implementation(libs.vosk.android)

    // ONNX Runtime for Android (runs the Parakeet encoder and TDT decoder)
    implementation(libs.onnxruntime.android)

    // LiteRT / Tensorflow Lite (used by OpenWakeWord)
    implementation(libs.litert)

    // OkHttp
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.accompanist.drawablepainter)

    // Permission Flow https://github.com/PatilShreyas/permission-flow-android
    implementation(libs.permission.flow.android)
    implementation(libs.permission.flow.compose)

    // Miscellaneous
    implementation(libs.unbescape)
    implementation(libs.jsoup)

    // Used by skills
    implementation(libs.exp4j)

    // Testing
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.ui.automator)
}

// this is required to avoid NoClassDefFoundError for ActivityInvoker during androidTest
// https://github.com/android/android-test/issues/2247#issuecomment-2194435444
configurations.configureEach {
    resolutionStrategy {
        force(libs.test.core)
    }
}

fun gitBranch(): String {
    return Git.open(rootDir).use { it.repository.branch }
}
