# LyraFlow 🎙️✨

LyraFlow es una aplicación de dictado inteligente construida con Kotlin Multiplatform y Compose Multiplatform. Captura tu voz mediante un atajo global, la convierte en texto contextualizado y la inserta en la aplicación activa.

## Funciones actuales

- Atajo global `Ctrl + Espacio` en Windows, personalizable con combinaciones de 2 o 3 teclas.
- Captura WAV mono a 16 kHz con nivel y duración visibles.
- Transcripción y corrección contextual mediante Gemini.
- Compatibilidad prevista con Whisper local mediante sidecar.
- Inserción directa usando portapapeles y simulación de teclado.
- Temas automático, claro y oscuro con Material 3 Expressive.
- Preferencias persistentes para tema, modelo, atajo e inyección automática.
- Escritorio Windows/Linux y base compartida para Android.

## Tecnologías

- Kotlin Multiplatform 2.3.10.
- Compose Multiplatform 1.11.1.
- Gradle 9.4.1.
- Ktor para Gemini.
- JNA para integraciones de escritorio.

## Ejecutar en escritorio

Requiere JDK 17 o posterior. El proyecto está verificado con Temurin 25.

```powershell
.\gradlew.bat :composeApp:run
```

La clave puede definirse mediante `GEMINI_API_KEY` o introducirse desde Ajustes. En Windows se conserva cifrada con DPAPI para el usuario actual; borrar el campo elimina también la copia persistida.

La tarjeta `Whisper local` instala bajo demanda el binario x64 desde la última release oficial de `ggml-org/whisper.cpp` y el modelo base en los datos locales del usuario. La interfaz muestra el progreso y permite reintentar si la descarga falla.

## Verificación

```powershell
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA "Android\Sdk"
.\gradlew.bat :composeApp:desktopTest :composeApp:compileDebugKotlinAndroid
```

## Distribución

```powershell
.\build-release.ps1 -LocalOnly
```

Genera y valida EXE, MSI y MSIX firmados sin modificar Git ni GitHub. Para publicar la versión activa desde `main`:

```powershell
.\build-release.ps1
```

El flujo automático crea el commit de release, el tag, realiza un push atómico y publica los artefactos junto con `SHA256SUMS.txt` en GitHub Releases.

## Estado de plataforma

- Windows: hotkey, audio, Gemini e inyección disponibles.
- Linux: paquetes configurados; hotkey e inyección nativos aún en desarrollo.
- Android: interfaz compartida compilable; captura y permisos aún pendientes.

Licencia MIT · Biglex J, 2026.
