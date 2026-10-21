# Validación del Proceso: Reubicación de Scripts y Estandarización de Iconos

## 1. Verificación de Transparencia de Iconos
- [x] `assets/branding/icons/icon-transparent.png`: Fuente canónica vectorial/PNG con canal alfa puro.
- [x] `assets/branding/icons/icon.png`: Fondo 100% transparente (Alpha(0,0)=0, Centro=0,199,178).
- [x] `composeApp/src/desktopMain/resources/icon.png`: Fondo 100% transparente (Alpha(0,0)=0, Centro=0,199,178).
- [x] `composeApp/src/desktopMain/resources/app_icon.ico`: Resoluciones 16, 24, 32, 48, 64, 128, 256 con canal alfa puro y sin esquinas blancas (todos los frames tienen `Corner(0,0) = srgba(0,0,0,0)`).
- [x] `composeApp/src/desktopMain/resources/Square44x44Logo.png`: Esquinas transparentes (`Alpha == 0`, Centro=2,196,176).
- [x] `composeApp/src/desktopMain/resources/Square150x150Logo.png`: Esquinas transparentes (`Alpha == 0`, Centro=0,202,181).
- [x] `composeApp/src/desktopMain/resources/StoreLogo.png`: Esquinas transparentes (`Alpha == 0`, Centro=0,197,176).
- [x] `composeApp/src/desktopMain/resources/Wide310x150Logo.png`: Esquinas transparentes (`Alpha == 0`, Centro=0,202,181).

## 2. Limpieza de Raíz y Dependencias
- [x] Carpeta `Image/` en la raíz eliminada completamente sin impacto funcional.
- [x] `composeApp/build.gradle.kts` desacoplado de `resources.srcDir(rootProject.file("Image"))`.
- [x] Script `build-release.ps1` eliminado de la raíz del proyecto.

## 3. Pruebas Automatizadas
- [x] `./gradlew :composeApp:desktopTest`: 40/40 pruebas unitarias superadas exitosamente (0 fallos).
- [x] Pruebas de carga de recursos (`DesktopPreferencesStoreTest` y componentes de UI) funcionando contra `Square44x44Logo.png` en el classpath.

## 4. Empaquetado y Publicación Local de la Versión 1.1.7
- [x] `scripts/release/build-release.ps1` ejecutado con `-Version "1.1.7" -LocalOnly`.
- [x] Versión centralizada actualizada en `gradle.properties` (`versionName=1.1.7`, `versionCode=117`) y `AppVersion.kt`.
- [x] Instalador generado: `release/LyraFlow-Windows-1.1.7.exe` (72,436,080 bytes / ~69.08 MB).
- [x] Firma digital de autenticode verificada (`CN=biglexj`, SHA256) con tolerancia a bloqueos de E/S.
- [x] Catálogo de sumas de verificación `release/SHA256SUMS.txt` generado y coincidente (`833576919d54891e0425a2b2a850dd02b3f55154b09b5f94613609d7e59c5f1b`).
