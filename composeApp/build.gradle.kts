plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val appVersion = providers.gradleProperty("lyraflow.versionName").get()
val appVersionCode = providers.gradleProperty("lyraflow.versionCode").get().toInt()

// JDK completo para la imagen de aplicación; el instalador usa Inno Setup.
private fun jdkIsUsable(home: String?): Boolean {
    if (home.isNullOrBlank()) return false
    if (!File(home, "bin/jpackage.exe").exists()) return false
    return true
}

val jpackageJdkHome: String? = run {
    val envJavaHome = System.getenv("JAVA_HOME")
    if (jdkIsUsable(envJavaHome)) return@run envJavaHome
    val sysJavaHome = System.getProperty("java.home")
    if (jdkIsUsable(sysJavaHome)) return@run sysJavaHome

    val candidates = listOf(
        File("C:/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot"),
        File("C:/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot"),
    )
    candidates.firstOrNull { jdkIsUsable(it.absolutePath) }?.absolutePath
        ?: listOf(File("C:/Program Files/Eclipse Adoptium"), File("C:/Program Files/Microsoft"), File("C:/Program Files/Java")).asSequence()
            .filter { it.isDirectory }
            .flatMap { it.listFiles()?.asSequence() ?: emptySequence() }
            .map { it.absolutePath }
            .firstOrNull { jdkIsUsable(it) }
}


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
        jpackageJdkHome?.let {
            javaHome = it
        }
        nativeDistributions {
            modules("java.net.http")
            // Solo imagen de aplicación: los instaladores los genera Inno Setup.
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
        // Alias dev:isolated: permite varias instancias simultaneas.
        // La propiedad la fija la tarea raiz devIsolated (ver build.gradle.kts).
        if (project.findProperty("lyraflow.multiInstance") == "true") {
            systemProperty("lyraflow.allowMultipleInstances", "true")
        }
    }
}

// Alias compatible: el único instalador oficial usa Inno Setup.
tasks.register<Exec>("packageLyraFlowInstaller") {
    group = "distribution"
    description = "Genera el instalador EXE con Inno Setup."
    dependsOn("createDistributable")
    workingDir = rootDir
    commandLine("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
        "-File", "scripts/packaging/Build-Installer.ps1", "-SkipBuild")
}

// Impide regenerar el EXE anterior que encapsulaba un MSI.
tasks.matching { it.name in setOf("packageExe", "packageMsi", "packageReleaseExe", "packageReleaseMsi") }.configureEach {
    doFirst { error("Usa releaseLocal o packageLyraFlowInstaller: el instalador oficial usa Inno Setup.") }
}
