# Fase 0 Kotlin Multiplatform — resultados iniciales

Fecha: 2026-07-16

## Resultado

Se añadió la implementación Kotlin Multiplatform y, después de validar el flujo principal en Windows, se retiró el cliente WPF heredado por decisión del usuario.

El prototipo compila con:

- Kotlin 2.3.10.
- Compose Multiplatform 1.11.1.
- Gradle 9.0.
- Java 17 como destino y runtime de packaging.
- Android SDK 36.

## Validaciones completadas

- `commonMain`, `desktopMain`, `androidMain`, `commonTest` y `desktopTest` configurados.
- Pipeline compartido con `DictationCoordinator` y `StateFlow`.
- Cliente Gemini Audio con WAV en `inlineData`.
- Modelos `gemini-3.1-flash-lite` y `gemini-3.5-flash` centralizados.
- Captura PCM 16 kHz mono mediante Java Sound.
- Encapsulado WAV validado por prueba automatizada.
- Hotkey Windows configurable implementado con JNA/Win32.
- Inyección de escritorio mediante portapapeles y pegado simulado.
- Adaptador sidecar para `whisper.cpp` con descubrimiento por variable de entorno.
- Diagnóstico inicial de sesión X11/Wayland.
- APK debug y release compilados.
- MSI y EXE generados con `jpackage`.
- Tests JVM ejecutados correctamente.
- Distribución de escritorio iniciada durante cinco segundos sin cierre inesperado y finalizada después de la prueba.
- APK inspeccionado con `applicationId=com.biglexj.lyraflow`, `versionName=1.1.0`, `versionCode=2` y `minSdk=24`.

Comando verificado:

```powershell
$jdk = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat "-Dorg.gradle.java.home=$jdk" `
  :composeApp:desktopTest `
  :composeApp:packageMsi `
  :composeApp:packageExe `
  :composeApp:assembleRelease
```

## Pendiente de validación manual

- Transcripción real con Gemini: no había `GEMINI_API_KEY` disponible en el entorno de compilación.
- Whisper real: no hay un `whisper-cli` instalado en `tools/whisper` ni configurado mediante `LYRAFLOW_WHISPER_BIN`.
- Captura desde el micrófono, hotkey e inyección deben probarse ejecutando la ventana en una sesión interactiva.
- X11 solo tiene diagnóstico; falta implementar registro e inyección nativos.
- Wayland solo detecta la sesión; falta integrar XDG GlobalShortcuts y decidir el camino de inyección.
- Android muestra la UI compartida, pero la captura y los permisos interactivos pertenecen a la siguiente iteración.
- El APK release de esta fase no tiene todavía la identidad de firma permanente.

## Decisión técnica

La estructura KMP es la implementación única de LyraFlow y genera artefactos Windows y Android desde el mismo módulo. El siguiente paso es completar los adaptadores Whisper y Linux.

## Compatibilidad del compilador

LyraFlow sigue generando bytecode compatible con Java 17, pero Gradle ya no exige encontrar una instalación separada de JDK 17. Se adoptó el patrón operativo de Ely-Tesia: Gradle 9.4.1 puede ejecutarse con el JDK moderno disponible en el equipo y Kotlin mantiene `JVM_17` como destino. El script de release prioriza ahora `JAVA_HOME`, por lo que utiliza el JDK 25 instalado cuando incluye `jpackage`.

## Interfaz Material 3 Expressive

La UI provisional fue reemplazada por un shell adaptativo con navegación lateral en escritorio y navegación inferior en tamaños compactos. Inicio reúne el estado del dictado, un visualizador de voz animado, el último resultado y la salud de Gemini/Whisper. Ajustes permite elegir tema automático, claro u oscuro, seleccionar el modelo Gemini, introducir una API key solo para la sesión y controlar la inyección automática.

Los esquemas claro y oscuro usan roles semánticos de Material 3, tipografía propia y contraste accesible. Las formas se limitaron a radios de 6–20 dp; la navegación se implementó de forma propia para evitar el indicador completamente redondo del componente predeterminado. Tema, modelo e inyección se guardan en preferencias de escritorio, mientras la API key no se persiste.

Verificación realizada con Temurin 25 y Gradle 9.4.1:

- `compileKotlinDesktop`: correcto.
- `compileDebugKotlinAndroid`: correcto.
- `desktopTest`: correcto.
- Revisión visual manual de la ventana de escritorio en modo oscuro: correcta.

## Dictado contextual y telemetría de captura

El ciclo del atajo quedó explicitado como alternancia: la primera pulsación inicia el micrófono y la segunda detiene el WAV y comienza el procesamiento. El valor predeterminado es `Ctrl + Espacio`; Ajustes permite grabar combinaciones de 2 o 3 teclas, persistirlas y volver a registrarlas sin reiniciar. `Windows + Espacio` se rechaza porque Windows lo reserva para cambiar el idioma o la distribución del teclado. El valor inicial se comprobó directamente con Win32 y estaba disponible en el equipo de prueba. Durante la captura, la pantalla muestra duración y barras alimentadas por el nivel RMS real del micrófono; durante el procesamiento conserva la duración del audio recibido.

Gemini ahora recibe instrucciones para producir texto final contextualizado: corrige puntuación, concordancia, repeticiones y falsos comienzos; crea párrafos o listas cuando corresponda e interpreta órdenes habladas de formato. La política prohíbe resumir, inventar o alterar nombres, cifras, rutas y código. La ventana inicial de escritorio se amplió a `1200 × 840 dp`.

El estado de procesamiento ya no reutiliza el visualizador detenido: muestra una onda continua que viaja entre los colores primario y secundario, texto de progreso y un indicador circular dentro del botón. La acción de grabar permanece bloqueada hasta que Gemini termina para impedir capturas superpuestas.

## Release oficial 1.0.1

La versión se unificó como `1.0.1` y se generaron instaladores EXE, MSI y MSIX x64 desde la misma distribución Compose Desktop. Los tres artefactos están firmados por `CN=biglexj` con la huella `D7B495DD421C479062A3BF22FFC38E6ED2C16102`; sus hashes coinciden con `SHA256SUMS.txt`. El manifiesto MSIX fue inspeccionado y declara la identidad `biglexj.LyraFlow`, versión `1.0.1.0`.

El símbolo superior de la navegación lateral funciona como selector rápido de tema y recorre `Automático → Claro → Oscuro`, persistiendo la preferencia. Para instalar el MSIX fuera de Microsoft Store, Windows debe confiar en la parte pública del certificado local utilizado para firmarlo.

## Publicación automatizada

`build-release.ps1` combina el preflight estricto de WinTTS con el packaging Gradle de Ely-Tesia. `-LocalOnly` ejecuta pruebas, genera EXE/MSI/MSIX, firma y calcula hashes sin tocar Git; todos los archivos quedan directamente en `release/`, sin subcarpetas. Sin parámetros exige `main` sincronizada, valida GitHub CLI, crea commit y tag anotado, realiza push atómico y publica cuatro assets usando `RELEASE_MESSAGE.md`. Las utilidades de detección de JDK/SDK, versión, firma y estado remoto viven separadas en `scripts/release/ReleaseTools.ps1`.

La versión oficial `v1.0.1` quedó publicada en GitHub con los tres instaladores y `SHA256SUMS.txt`: https://github.com/biglexj/LyraFlow/releases/tag/v1.0.1

## Parche 1.0.2 — bandeja del sistema

Cerrar la ventana ya no termina LyraFlow: la aplicación se oculta, desaparece de la barra de tareas y mantiene registrado el atajo global. El icono nativo de la bandeja permite restaurar la misma ventana con clic o elegir `Salir` para liberar el atajo, detener una grabación activa y finalizar el proceso.

La validación en Windows confirmó que, tras cerrar la ventana, el proceso permanece activo sin `MainWindowHandle` y `Ctrl + Espacio` sigue reservado por LyraFlow. Las pruebas automatizadas cubren además las acciones de abrir, salir y retirar correctamente el icono de bandeja.

La corrección quedó publicada oficialmente en https://github.com/biglexj/LyraFlow/releases/tag/v1.0.2 con EXE, MSI, MSIX y hashes verificados.

La actualización `v1.0.3` quedó publicada oficialmente en https://github.com/biglexj/LyraFlow/releases/tag/v1.0.3 con EXE, MSI, MSIX y hashes SHA-256 verificados contra los artefactos locales.

## Parche 1.0.3 — configuración persistente

La clave de Gemini se almacena cifrada con DPAPI para el usuario actual de Windows. Cuando falta, la tarjeta Gemini abre un diálogo compacto para guardarla sin navegar a Ajustes; vaciar el campo de configuración elimina la copia persistente.

La tarjeta Whisper local instala ahora el binario x64 de la última release oficial de `whisper.cpp` y el modelo base dentro de `%LOCALAPPDATA%`. La descarga muestra progreso y el runtime de escritorio declara explícitamente `java.net.http`, necesario para que la distribución empaquetada pueda ejecutar este flujo.

El menú de bandeja dejó de usar el aspecto genérico de Windows: ahora es un popover oscuro con acento turquesa, hover y esquinas de 18 px. La validación manual confirmó el diálogo Gemini, la persistencia cifrada, la descarga completa de Whisper y el cambio de estado a `Whisper base listo`.
