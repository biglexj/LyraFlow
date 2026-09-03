---
trigger: always_on
---

# 🏛️ Guía Estándar de Arquitectura para Aplicaciones de Escritorio (Desktop Apps)

> **Proyectos de Referencia**: LyraFlow, LunaFetch, Ely-Tesia  
> **Ámbito**: Convenciones de UI/UX, Single-Instance Lock, In-App Auto-Update y Gestión Inteligente de Credenciales para Aplicaciones Desktop (Compose Multiplatform).

---

## 1. 🔒 Garantía de Instancia Única (Single Instance Lock) [CRÍTICO]

> [!IMPORTANT]
> **Condición de Aplicabilidad**: El **Single-Instance Lock** APLICA a toda aplicación que opere como servicio en segundo plano o que mantenga proceso en la bandeja del sistema (System Tray), tal como LyraFlow con su captura de atajos globales y ventana acoplada.

Para prevenir duplicación de procesos e iconos repetidos en la bandeja del sistema al relanzar en producción:

- **Mecanismo Obligatorio**: Adquisición de socket de bucle local (`ServerSocket(127.0.0.1:PORT)`) al iniciar el proceso principal.
- **Bypass en Modo Desarrollo (`isDev`) [OBLIGATORIO Y CRÍTICO]**: El Single-Instance Lock **NUNCA DEBE bloquear ni cerrar la aplicación cuando se ejecuta en desarrollo** (`./gradlew :composeApp:run`, IntelliJ/VSCode o flags `-Dlyraflow.channel=dev`, `-Dlyraflow.dev=true` o `idea.active`).
  - La aplicación DEBE detectar el flag dev y **retornar `true` sin bloquear ni llamar a `exitProcess(0)`**.
- **Comportamiento en Producción**: Si una segunda instancia del instalador distribuido intenta iniciar:
  1. Detecta que el socket está ocupado.
  2. Envía la orden `ACTIVATE` al socket de la primera instancia.
  3. La primera instancia restaura su ventana (desoculta de bandeja, `isMinimized = false`), llama a Win32 `ShowWindow(hwnd, SW_RESTORE)` y `SetForegroundWindow(hwnd)`.
  4. La nueva invocación finaliza inmediatamente con código `0`.
- **Limpieza de Recursos**: Invocar `SingleInstanceLock.release()` en el desecho de la aplicación (`onDispose`, `exitApplication`) y **especialmente antes de iniciar el instalador de auto-actualización**.

---

## 2. ⚡ Actualizaciones In-App de Fricción Cero (Distribución Exclusiva EXE)
Las actualizaciones de versión ofrecen una experiencia sin fricción operativa:

- **Verificación Silenciosa**: Comprobación background al iniciar sin interrumpir al usuario.
- **Distribución Exclusiva EXE**: LyraFlow compila, distribuye y actualiza **únicamente instaladores EXE** (`LyraFlow-Windows-X.Y.Z.exe`).
- **Flujo In-App**:
  1. El usuario hace clic en *"Actualizar ahora"*.
  2. La app descarga el instalador `.exe` en segundo plano mostrando barra de progreso (0-100%, MBs).
  3. Completada la descarga, ofrece el botón *"Instalar y Reiniciar 🚀"*.
  4. Al pulsar *"Instalar y Reiniciar"*, la aplicación:
     - Libera el bloqueo de instancia única (`SingleInstanceLock.release()`).
     - Lanza el proceso desasociado: `cmd.exe /c timeout /t 2 /nobreak > nul & start /wait "" "$absPath" /passive & start "" "$currentExePath"`.
     - Finaliza la instancia antigua (`exitProcess(0)`).
     - El instalador se ejecuta de forma pasiva (`/passive`), sustituye archivos y relanza LyraFlow automáticamente.
- **Cero Fricción**: **El usuario NUNCA debe verse obligado a buscar en la carpeta de Descargas, mover archivos o ejecutar manualmente el instalador.**

---

## 3. 🧠 Migración Reactiva a Modo Inteligente al Ingresar API Keys
- **Estado Sin API Key / Modo Local**: Por defecto, la app opera en modo offline / literal (*Voz original* / transcripción local) con los modos IA (*Inteligente* / *Personalizado*) bloqueados reactivamente (`🔒`).
- **Auto-migración al ingresar API Key**: Al pegar una clave de API por primera vez (de vacío a no vacío), la aplicación **migra automáticamente de "Voz original" a "Inteligente"**.
- **Preservación de Preferencias**: Si el usuario cambia manualmente el modo posteriormente, su preferencia se respeta íntegramente.

---

## 4. 🎨 Integración con el Sistema Operativo y Bandeja (System Tray)
- **Minimización a la Bandeja**: El botón de cerrar (`X`) u ocultar minimiza la ventana al tray manteniendo activo el atajo global.
- **Menú Contextual Nativo**: Menú de bandeja con atajos, opción para abrir/restaurar ventana y opción de salida definitiva.

---

## 5. 📐 Persistencia Obligatoria del Estado y Dimensiones de la Ventana [CRÍTICO]
Toda aplicación de escritorio DEBE recordar automáticamente su tamaño (ancho, alto), posición y estado de maximizado (`isMaximized`):
- **Restauración al Iniciar**: Se leen los valores guardados en `preferencesStore.loadWindowState()`.
- **Guardado Continuo o al Cerrar**: Al cambiar dimensiones o al cerrar la ventana, se guardan las propiedades transparentemente.
- **Prohibición**: Queda prohibido forzar tamaños fijos arbitrarios que ignoren el tamaño previo elegido por el usuario.

---

## 6. 🎨 Personalización y Localización del Instalador de Escritorio EXE
- **Localización al Español**: Instalador configurado en español (`es-PE` / `es-ES`), evitando pantallas o términos en inglés.
- **Branding e Identidad Visual**: Logotipo oficial, paleta de colores M3 Expressive, icono en ejecutable y en el panel de desinstalación de Windows.
- **Organización**: `biglexj`, `Copyright (c) 2026 Biglex J`, enlaces a `https://github.com/biglexj`.
- **Modo Pasivo**: Soportar `/passive` sin errores para garantizar la actualización in-app sin fricción.
