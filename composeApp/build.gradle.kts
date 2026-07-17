import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val appVersion = providers.gradleProperty("lyraflow.versionName").get()
val appVersionCode = providers.gradleProperty("lyraflow.versionCode").get().toInt()

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
                implementation("org.jetbrains.compose.foundation:foundation:1.11.1")
                implementation("org.jetbrains.compose.material3:material3:1.11.0-alpha07")
                implementation("org.jetbrains.compose.ui:ui:1.11.1")
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.okhttp)
            }
        }
        val desktopMain by getting {
            resources.srcDir(rootProject.file("Image"))
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.cio)
                implementation(libs.jna)
                implementation(libs.jna.platform)
            }
        }
    }
}

android {
    namespace = "com.biglexj.lyraflow"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.biglexj.lyraflow"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "com.biglexj.lyraflow.MainKt"
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")
        nativeDistributions {
            modules("java.net.http")
            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Exe,
                TargetFormat.Deb,
                TargetFormat.Rpm,
            )
            packageName = "LyraFlow"
            packageVersion = appVersion
            vendor = "biglexj"
            description = "Dictado inteligente multiplataforma"

            windows {
                iconFile.set(rootProject.file("Icon/app_icon.ico"))
                msiPackageVersion = appVersion
                exePackageVersion = appVersion
                upgradeUuid = "55ee1e98-1bd5-4d89-990f-27756b85820c"
                shortcut = true
                menu = true
                menuGroup = "LyraFlow"
                dirChooser = true
                perUserInstall = true
            }

            linux {
                shortcut = true
                menuGroup = "Utility"
                appCategory = "Utility"
            }
        }
    }
}
