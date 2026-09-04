# Validación del Proceso: Reubicación de Scripts y Estandarización de Iconos

## 1. Verificación de Transparencia de Iconos
- [x] `assets/branding/icons/icon.png`: Fondo 100% transparente (Alpha(0,0)=0, Centro=0,199,178).
- [x] `composeApp/src/desktopMain/resources/icon.png`: Fondo 100% transparente (Alpha(0,0)=0, Centro=0,199,178).
- [x] `composeApp/src/desktopMain/resources/app_icon.ico`: Resoluciones 16, 24, 32, 48, 64, 128, 256 con canal alfa puro y sin esquinas blancas (todos los frames tienen `Corner(0,0) = srgba(0,0,0,0)`).
- [x] `Image/Square44x44Logo.png`: Esquinas transparentes (`Alpha == 0`, Centro=2,196,176).
- [x] `Image/Square150x150Logo.png`: Esquinas transparentes (`Alpha == 0`, Centro=0,202,181).
- [x] `Image/StoreLogo.png`: Esquinas transparentes (`Alpha == 0`, Centro=0,197,176).
- [x] `Image/Wide310x150Logo.png`: Esquinas transparentes (`Alpha == 0`, Centro=0,202,181).

## 2. Pruebas Automatizadas
- [x] `./gradlew :composeApp:desktopTest` exitoso en 5s con todas las pruebas y verificación de recursos en verde.

## 3. Verificación de Scripts
- [x] `scripts/release/build-release.ps1` localiza `$root`, `ReleaseTools.ps1` y `gradle.properties` correctamente.
- [x] `.\build-release.ps1` (raíz) delega limpiamente hacia `scripts/release/build-release.ps1` manteniendo switches y parámetros intactos.
- [x] `.gitignore` actualizado con `!scripts/release/` para garantizar seguimiento completo en Git.
