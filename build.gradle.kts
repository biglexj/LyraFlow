plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
}

// --- Alias de comandos -------------------------------------------------------
// Estandar global de scripts de compilacion, ejecucion y alias de comandos:
// Docs/global/architecture/command-scripts-standard.md, seccion E (KMP).
//
// Los alias con dos puntos (dev:isolated, build:desktop, release:local) viven
// en package.json, porque Gradle no admite ':' en el nombre de una tarea. Aqui
// se registran los equivalentes sin caracteres prohibidos, de modo que ambos
// caminos (npm/bun/pnpm y gradlew) llegan al mismo sitio.

tasks.register("dev") {
    group = "application"
    description = "Ejecuta la aplicacion de escritorio en modo desarrollo"
    dependsOn(":composeApp:run")
}

tasks.register("devIsolated") {
    group = "application"
    description = "Ejecuta la aplicacion en modo desarrollo permitiendo varias instancias simultaneas"
    dependsOn(":composeApp:run")
}

tasks.register("devAndroid") {
    group = "application"
    description = "Compila e instala la app en el dispositivo Android conectado"
    dependsOn(":composeApp:installDebug")
}

tasks.register("test") {
    group = "verification"
    description = "Ejecuta las pruebas de escritorio (desktopTest)"
    dependsOn(":composeApp:desktopTest")
}

tasks.register("check") {
    group = "verification"
    description = "Verifica la compilacion del codigo de escritorio y de Android"
    dependsOn(":composeApp:compileKotlinDesktop", ":composeApp:compileDebugKotlinAndroid")
}

tasks.register("build") {
    group = "build"
    description = "Genera la imagen de aplicacion de escritorio lista para ejecutar"
    dependsOn(":composeApp:createDistributable")
}

// Gradle hace coincidir 'build' con la tarea homonima de cada subproyecto, asi
// que este alias existe para compilar solo el escritorio, sin assemble de
// Android ni suite de pruebas.
tasks.register("buildDesktop") {
    group = "build"
    description = "Genera solo la imagen de aplicacion de escritorio, sin Android ni pruebas"
    dependsOn(":composeApp:createDistributable")
}

tasks.register<Exec>("releaseLocal") {
    group = "publishing"
    description = "Compila, empaqueta y firma el instalador EXE local sin publicar en GitHub"
    workingDir = rootDir
    commandLine("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "./scripts/release/build-release.ps1", "-LocalOnly")
}

tasks.register<Exec>("releasePublish") {
    group = "publishing"
    description = "Pipeline completo de release oficial (Git tag, GitHub Release, SHA256)"
    workingDir = rootDir
    commandLine("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "./scripts/release/build-release.ps1", "-Publish")
}

// El alias dev:isolated activa el modo multiinstancia del SingleInstanceLock.
// La propiedad se propaga al subproyecto durante la configuracion, antes de que
// se evalue su script de construccion.
if (gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "devIsolated" }) {
    project(":composeApp").extensions.extraProperties["lyraflow.multiInstance"] = "true"
}
