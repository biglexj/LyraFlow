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
                implementation(libs.ktor.client.mock)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.ktor.client.okhttp)
            }
        }
        val desktopMain by getting {
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
        val validJpackageJdk = run {
            val envJavaHome = System.getenv("JAVA_HOME")
            if (!envJavaHome.isNullOrBlank() && File(envJavaHome, "bin/jpackage.exe").exists()) return@run envJavaHome
            val sysJavaHome = System.getProperty("java.home")
            if (!sysJavaHome.isNullOrBlank() && File(sysJavaHome, "bin/jpackage.exe").exists()) return@run sysJavaHome

            val candidates = listOf(
                File("C:/Program Files/Microsoft/jdk-17.0.19.10-hotspot"),
                File("C:/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot"),
            )
            candidates.firstOrNull { File(it, "bin/jpackage.exe").exists() }?.absolutePath
                ?: listOf(File("C:/Program Files/Microsoft"), File("C:/Program Files/Eclipse Adoptium"), File("C:/Program Files/Java")).asSequence()
                    .filter { it.isDirectory }
                    .flatMap { it.listFiles()?.asSequence() ?: emptySequence() }
                    .firstOrNull { File(it, "bin/jpackage.exe").exists() }
                    ?.absolutePath
        }
        validJpackageJdk?.let {
            javaHome = it
        }
        nativeDistributions {
            modules("java.net.http")
            targetFormats(
                TargetFormat.Exe,
            )
            packageName = "LyraFlow"
            packageVersion = appVersion
            vendor = "biglexj"
            description = "Dictado inteligente multiplataforma"

            windows {
                iconFile.set(project.file("src/desktopMain/resources/app_icon.ico"))
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

tasks.withType<JavaExec>().configureEach {
    if (name == "run") {
        systemProperty("lyraflow.channel", "dev")
    }
}

val pkgWinDir = file("../package/windows")
tasks.matching { it.name.startsWith("package") }.configureEach {
    doFirst {
        val resDir = file("build/compose/tmp/resources")
        resDir.mkdirs()
        if (pkgWinDir.exists()) {
            pkgWinDir.copyRecursively(resDir, overwrite = true)
        }
    }
}

