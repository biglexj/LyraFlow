# Migración de LyraFlow a Kotlin Multiplatform

Fecha: 2026-07-16
Estado: Fase 0 aprobada y prototipo inicial compilado
Referencia estructural: `D:\Proyectos\biglexj\Ely-Tesia`

## Decisión propuesta

Reescribir LyraFlow con **Kotlin Multiplatform + Compose Multiplatform**, compartiendo lógica e interfaz entre escritorio y Android, pero priorizando este orden:

1. Windows con paridad funcional.
2. Linux X11.
3. Linux Wayland.
4. Android como cliente de dictado, sin exigir inicialmente inyección global.

La implementación WPF fue retirada el 2026-07-16 por decisión del usuario. Kotlin Multiplatform es desde entonces la única implementación activa y no conserva dependencias con el cliente C#.

## Alcance de proveedores

Groq se elimina completamente del producto, la configuración, la UI y la documentación.

Proveedores iniciales:

- **Gemini Transcription**: proveedor cloud predeterminado.
- **Whisper Local**: proveedor offline para escritorio.
- **OpenAI Transcription**: posible proveedor cloud posterior, fuera del primer hito.

No se introducirá una abstracción llamada Groq ni un fallback silencioso hacia Groq.

## Modelo Gemini correcto

Aurora no utiliza un modelo STT con un nombre independiente. Envía el WAV como `inlineData` a un modelo Gemini Flash multimodal y solicita una transcripción de texto.

El selector actual de Aurora usa:

1. `gemini-3.1-flash-lite` como primera opción rápida.
2. `gemini-3.5-flash` como segunda opción/inteligente.

Para LyraFlow se propone invertir el sentido según el perfil:

- **Rápido**: `gemini-3.1-flash-lite`.
- **Inteligente**: `gemini-3.5-flash`.
- **Automático**: medir latencia y calidad, con fallback configurable.

`gemini-3.1-flash-tts-preview` no es un transcriptor; genera audio desde texto y no formará parte de la lista STT.

Los identificadores no se escribirán directamente dentro de composables. Vivirán en un catálogo configurable para poder retirar previews o cambiar fallbacks sin modificar la interfaz.

## Estructura del proyecto

Se toma de Ely-Tesia el uso de un módulo `composeApp`, version catalog y source sets, pero se evita concentrar toda la aplicación en un composable gigante.

```text
LyraFlow/
├── composeApp/
│   └── src/
│       ├── commonMain/
│       │   └── kotlin/com/biglexj/lyraflow/
│       │       ├── app/
│       │       ├── core/
│       │       │   ├── audio/
│       │       │   ├── config/
│       │       │   ├── model/
│       │       │   ├── result/
│       │       │   └── security/
│       │       ├── domain/
│       │       │   ├── dictation/
│       │       │   ├── refinement/
│       │       │   └── transcription/
│       │       ├── data/
│       │       │   ├── gemini/
│       │       │   ├── models/
│       │       │   └── settings/
│       │       └── feature/
│       │           ├── overlay/
│       │           ├── settings/
│       │           ├── history/
│       │           └── modelmanager/
│       ├── desktopMain/
│       │   └── kotlin/com/biglexj/lyraflow/platform/
│       │       ├── audio/
│       │       ├── hotkey/
│       │       ├── injection/
│       │       ├── secrets/
│       │       ├── tray/
│       │       ├── windows/
│       │       └── linux/
│       ├── androidMain/
│       │   └── kotlin/com/biglexj/lyraflow/platform/
│       │       ├── audio/
│       │       ├── permissions/
│       │       ├── secrets/
│       │       └── share/
│       └── commonTest/
├── gradle/libs.versions.toml
├── packaging/
│   ├── msix/
│   ├── winget/
│   ├── linux/
│   └── android/
├── scripts/release/
├── gradle.properties
└── build-release.ps1
```

Se mantendrá un solo módulo Gradle al comienzo. Solo se crearán módulos adicionales si los tiempos de compilación o la dependencia entre dominios lo justifican.

## Reglas de tamaño y responsabilidad

- Objetivo normal: **60–180 líneas por archivo Kotlin**.
- Revisión obligatoria al superar **220 líneas**.
- Límite excepcional: **300 líneas**, con justificación.
- Composables de pantalla: máximo recomendado de 150 líneas.
- Funciones: máximo recomendado de 40 líneas.
- Un archivo define una responsabilidad principal.
- Los composables renderizan estado y emiten eventos; no llaman directamente a HTTP, archivos, micrófono ni APIs del sistema.
- Cada pantalla se divide en `Screen`, `State`, `Event`, `Presenter/ViewModel` y componentes visuales cuando corresponda.
- `App.kt` solo ensambla tema, navegación y dependencias.
- No crear equivalentes a un `AppContent.kt` de miles de líneas.
- Los adaptadores de Windows/Linux se mantienen fuera de `commonMain`.

Estas cifras son guardas de diseño, no una excusa para fragmentar clases cohesionadas en archivos artificiales.

## Flujo funcional

```text
Global shortcut
    -> DictationCoordinator
    -> AudioCapture
    -> AudioNormalizer / VAD
    -> TranscriptionProvider
       -> GeminiTranscriptionProvider
       -> WhisperLocalProvider
    -> TextRefinementPolicy
    -> ReviewPolicy
    -> TextInjector
    -> DictationHistory
```

Contratos principales:

- `AudioCapture`
- `GlobalShortcut`
- `TranscriptionProvider`
- `TextRefiner`
- `TextInjector`
- `TrayController`
- `AutostartController`
- `SecretStore`
- `ModelRepository`

`DictationCoordinator` será el único orquestador del flujo. La UI le enviará eventos y observará un `StateFlow<DictationState>`.

Estados mínimos:

- Idle
- Listening
- Transcribing
- Refining
- Reviewing
- Injecting
- Completed
- Failed

## Transcripción y refinamiento

### Gemini cloud

- Capturar PCM mono de 16 bits a 16 kHz.
- Construir WAV en memoria.
- Enviar audio como parte multimodal.
- Pedir solo texto plano y cadena vacía cuando no exista voz clara.
- Aplicar timeout, cancelación y rotación controlada de claves si se habilita.
- Conservar métricas de modelo, tiempo y error sin registrar la API key ni el audio.

Gemini puede ofrecer un modo de una sola llamada que transcriba y limpie, pero el modo predeterminado mantendrá dos resultados:

- `rawTranscript`: fidelidad al audio.
- `refinedText`: corrección contextual.

Esto permite deshacer, comparar y evitar que una mejora estilística elimine información.

### Whisper local

Primera implementación de escritorio:

- Empaquetar `whisper.cpp` como sidecar por sistema operativo.
- Invocarlo mediante un adaptador con timeout y cancelación.
- Mantener descargables los modelos `base`, `small` y `large-v3-turbo`.
- Verificar hashes de modelos.

Esta opción reduce el riesgo inicial frente a mantener JNI propio. Cuando el producto sea estable se evaluará JNI para streaming y menor latencia de arranque.

En Android, Whisper local queda para una fase posterior porque requiere binarios nativos por ABI y aumenta tamaño, complejidad y consumo. El MVP Android usará Gemini cloud.

### Contextualización

- Diccionario personal para nombres, marcas, rutas y vocabulario técnico.
- Perfiles: chat, correo, código y texto largo.
- Modos literal, equilibrado e inteligente.
- Reglas explícitas de preservación: no resumir, no inventar, no cambiar números ni nombres.
- Corrección de repeticiones involuntarias y frases como “puntos por puntos” -> “punto por punto”.
- Creación de listas solo cuando el dictado enumere realmente.
- Vista previa opcional y acción para recuperar el texto crudo.

## Plataforma de escritorio

### Windows

- Hotkey global mediante Win32/JNA.
- Captura de audio mediante una biblioteca JVM compatible o adaptador nativo.
- Inyección mediante `SendInput`, con portapapeles como fallback.
- System tray de Compose Desktop.
- Autoinicio y almacenamiento seguro con adaptadores Windows.
- Overlay transparente, always-on-top y sin robar foco.

### Linux X11

- Hotkey e inyección mediante X11/JNA.
- Audio compatible con PipeWire/PulseAudio.
- Autoinicio XDG.
- Paquetes `.deb` y `.rpm`; AppImage se evaluará después.

### Linux Wayland

- Hotkey mediante XDG GlobalShortcuts cuando esté disponible.
- Prototipo de inyección mediante portales/libei.
- Fallback universal: copiar el resultado y mostrar una indicación breve para pegar.
- Validación separada en GNOME y KDE.

No se prometerá paridad de inyección en Wayland hasta completar el prototipo técnico.

## Android

El mismo `commonMain` compartirá:

- UI principal y tema.
- Configuración y catálogo de modelos.
- Cliente Gemini.
- Diccionario, perfiles y refinamiento.
- Historial de dictados.

El MVP Android permitirá grabar, transcribir, copiar y compartir. La inserción global requiere implementar un teclado/IME, que será una decisión separada y no debe bloquear escritorio.

## Release y packaging

Se mantiene el patrón probado en Ely-Tesia:

- `gradle.properties` como fuente única de `versionName` y `versionCode`.
- `libs.versions.toml` para Kotlin, Compose y dependencias.
- `compose.desktop.nativeDistributions` para paquetes nativos.
- APK y AAB release firmados con keystore permanente.
- MSI y EXE para Windows.
- MSIX firmado y manifiestos Winget como paquetes opcionales.
- DEB y RPM generados en Linux.
- `SHA256SUMS.txt` para todos los artefactos.
- Carpeta `release/` con nombres uniformes.

Artefactos previstos:

```text
LyraFlow-Windows-X.Y.Z.msi
LyraFlow-Windows-X.Y.Z.exe
LyraFlow-Windows-X.Y.Z.msix
LyraFlow-Linux-X.Y.Z.deb
LyraFlow-Linux-X.Y.Z.rpm
LyraFlow-Android-X.Y.Z.apk
LyraFlow-Android-X.Y.Z.aab
SHA256SUMS.txt
```

Todos los artefactos locales se guardarán directamente en `release/`, sin carpetas por versión o plataforma. El build local y la publicación se separarán:

- `build-release.ps1 -LocalOnly`: compila, valida y calcula hashes.
- Publicación: exige árbol Git limpio, versión confirmada y credenciales válidas.
- El script no ejecutará `git add -A` sobre cambios arbitrarios.
- Una matriz CI Windows/Linux generará los paquetes propios de cada SO.

## Fases y estimación

Estimación para una persona trabajando de forma concentrada. Incluye implementación y pruebas, no tiempos de espera de tiendas.

### Fase 0 — Esqueleto y pruebas de riesgo: 3–5 días

- Crear el proyecto KMP siguiendo Ely-Tesia.
- Compose Desktop Windows/Linux y Android vacío.
- Overlay mínimo.
- Prueba de micrófono.
- Prueba Gemini Audio.
- Prueba Whisper sidecar.
- Hotkey e inyección Windows/X11/Wayland.

Salida: confirmar las bibliotecas y el alcance real de Wayland antes de construir la UI completa.

### Fase 1 — Dominio y pipeline compartido: 4–6 días

- Contratos, estados y `DictationCoordinator`.
- Configuración, errores y logging.
- Gemini Transcription.
- Texto crudo/refinado e historial.
- Pruebas unitarias del pipeline.

### Fase 2 — Windows con paridad funcional: 7–10 días

- UI de configuración y overlay.
- Hotkey, captura, tray, autoinicio e inyección.
- Gestor de modelos Whisper.
- Whisper local sidecar.
- Migración de preferencias.
- Pruebas en chats, navegadores, Office y editores.

Hito: primera versión Kotlin utilizable diariamente en Windows.

### Fase 3 — Linux X11 y packaging: 7–10 días

- Adaptadores Linux.
- Inyección X11.
- DEB/RPM y matriz de release.
- Pruebas en Ubuntu y una segunda distribución.

### Fase 4 — Wayland: 5–15 días

- GlobalShortcuts.
- Inyección autorizada o fallback de portapapeles.
- GNOME/KDE, multimonitor y overlay.

La variación depende del soporte real de cada compositor.

### Fase 5 — Android MVP: 5–8 días

- Permisos y captura.
- UI adaptativa.
- Gemini cloud.
- Copiar, compartir e historial.
- APK/AAB firmados.

### Estimación total

- **Windows Kotlin utilizable**: 3–4 semanas.
- **Windows + Linux X11**: 4–6 semanas.
- **Windows + Linux X11/Wayland + Android MVP**: 6–9 semanas.
- **Producto pulido con instaladores, corpus de calidad y correcciones de plataforma**: 8–10 semanas.

La lógica de negocio es pequeña; la mayor parte del tiempo está en hotkeys, audio, inyección, binarios Whisper, permisos y packaging.

## Validación de calidad

- Corpus de audio en español latinoamericano.
- Nombres propios, tecnicismos, rutas, código, listas y autocorrecciones habladas.
- Medir latencia p50/p95 por proveedor y modelo.
- Medir pérdida o invención de contenido, no solo ortografía.
- Pruebas unitarias en `commonTest`.
- Pruebas de integración por adaptador de plataforma.
- Prueba manual de inyección en aplicaciones representativas.
- Verificación de firma, versión interna y SHA-256 de cada release.

## Estado de la primera aprobación

La **Fase 0** fue aprobada el 2026-07-16. La reescritura completa comenzará después de demostrar:

1. Gemini Audio desde Kotlin.
2. Whisper local desde Compose Desktop.
3. Hotkey e inyección en Windows.
4. Camino viable para X11 y Wayland.
5. Build mínimo en Android.

## Iteración visual Material 3 Expressive — aprobada

Autorizada por el usuario el 2026-07-16 con libertad de diseño y una restricción: evitar radios completamente redondos y mantener esquinas moderadas, visualmente entre 15–20%.

Alcance de esta iteración:

- Sustituir la UI provisional por un shell adaptativo con navegación, inicio y configuración.
- Añadir temas claro, oscuro y automático con esquemas semánticos accesibles.
- Aplicar una identidad Material 3 Expressive propia de LyraFlow, con jerarquía tipográfica, contenedores tonales, movimiento y estados interactivos.
- Incorporar configuración persistente no sensible para tema, modelo y comportamiento de inyección.
- Permitir una API key de Gemini durante la sesión sin persistirla como texto plano.
- Mantener composables pequeños y separados por responsabilidad.
- Verificar compilación JVM/Android y pruebas existentes.

## Release oficial 1.0.1 — aprobada

- Unificar la versión `1.0.1` en Gradle y Compose Desktop.
- Generar EXE y MSI mediante `jpackage`/Compose Desktop.
- Construir MSIX Full Trust desde la imagen de aplicación con Windows SDK.
- Firmar los tres artefactos con el certificado cuyo publicador es `CN=biglexj`.
- Publicar hashes SHA-256 en una carpeta de release versionada.

## Publicación automática estilo WinTTS/Ely-Tesia — aprobada

Objetivo: convertir `build-release.ps1` en el único punto de entrada para construir y publicar LyraFlow 1.0.1 en `biglexj/LyraFlow`.

1. Adaptar el preflight de WinTTS al proyecto Gradle: exigir `main`, remoto `biglexj/LyraFlow`, autenticación de GitHub CLI, tag inexistente y GitHub Release inexistente.
2. Mantener `gradle.properties` como fuente de versión y validar la regla de parche `.9`.
3. Ejecutar pruebas de escritorio, generar EXE/MSI/MSIX, firmar con `CN=biglexj`, verificar firmas y producir `SHA256SUMS.txt`.
4. Conservar `-LocalOnly`, `-SkipTests`, `-SkipBuild` y `-SkipSigning` para desarrollo y diagnóstico; la ejecución sin parámetros publicará.
5. Crear un commit de release con la migración Kotlin aprobada, tag anotado `v1.0.1` y push atómico de `main` + tag.
6. Crear GitHub Release `LyraFlow v1.0.1` con `RELEASE_MESSAGE.md` y adjuntar EXE, MSI, MSIX y hashes.
7. Verificar en GitHub el tag, la release y sus cuatro assets; documentar el flujo en README, packaging y walkthrough.

Estado comprobado antes de ejecutar: rama `main`, remoto `https://github.com/biglexj/LyraFlow.git`, sesión `gh` autenticada como `biglexj`, último tag/release `v1.0.0`; `v1.0.1` está disponible.

## Parche 1.0.2 — ejecución en bandeja del sistema

Objetivo: recuperar el comportamiento residente de LyraFlow en Windows sin mantener una ventana ocupando espacio en la barra de tareas.

1. Separar “cerrar ventana” de “salir de la aplicación”: la X ocultará la ventana y conservará vivos el proceso, el atajo global y el coordinador de dictado.
2. Añadir un icono de bandeja con acciones para abrir LyraFlow y salir completamente. La acción de salida liberará el atajo global, detendrá cualquier captura activa y cerrará el proceso de forma limpia.
3. Hacer que abrir desde la bandeja restaure y enfoque la ventana, sin crear instancias adicionales.
4. Mantener el icono de la aplicación fuera de la barra de tareas mientras la ventana esté oculta; cuando esté abierta, conservar el comportamiento normal de una ventana de escritorio.
5. Actualizar la versión a `1.0.2`, las notas y el mensaje de release con una descripción breve del parche.
6. Ejecutar pruebas y compilación de escritorio; realizar una comprobación manual del ciclo abrir → cerrar a bandeja → usar atajo → restaurar → salir.
7. Generar EXE, MSI y MSIX directamente en `release/`, verificar firmas y hashes, y publicar `v1.0.2` cuando la corrección quede validada.
