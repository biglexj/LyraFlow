# 🏛️ Guía Estándar de Arquitectura para Aplicaciones de Escritorio (Desktop Apps)

> **Proyectos de Referencia**: LyraFlow, LunaFetch, Ely-Tesia  
> **Ámbito**: Convenciones de UI/UX, Single-Instance Lock, In-App Auto-Update y Gestión Inteligente de Credenciales para Aplicaciones Desktop (Compose Multiplatform / WPF / Rust).

---

## 1. 🔒 Garantía de Instancia Única (Single Instance Lock) & Reactivación [CRÍTICO]

> [!IMPORTANT]
> **Estándar Core**: Conforme a `Docs/features/single-instance/README.md` y `Docs/platforms/windows/single_instance_lock.md`.
> Toda aplicación de escritorio instalada DEBE reutilizar su instancia viva por defecto cuando el usuario vuelve a iniciarla desde Inicio, el buscador, la barra de tareas, un acceso directo o la bandeja del sistema.

Para prevenir la duplicación de procesos, colisión de recursos o iconos duplicados en la bandeja del sistema (System Tray):

- **Aislamiento por Canales**:
  - Canal de Producción (`stable`): Puerto por defecto `49281`.
  - Canal de Desarrollo (`dev`): Puerto `49283`.
  - Ambos canales aplican instancia única de forma predeterminada, permitiendo que una ejecución local en desarrollo coexista con una versión estable instalada sin interferencias.
- **Despacho IPC de Activación**:
  - Si una segunda instancia intenta iniciar, detecta el bloqueo ocupado y envía una señal `ACTIVATE` (o los argumentos pasados) a la instancia primaria por IPC local antes de finalizar inmediatamente con código `0` (`exitProcess(0)`).
  - La instancia secundaria **NUNCA debe inicializar UI, bandeja del sistema, listeners, captura de audio, atajos globales ni servicios en segundo plano**.
- **Restauración y Foco en la Instancia Primaria**:
  - La instancia primaria recibe el mensaje en el hilo de interfaz, desminimiza la ventana si estaba minimizada o en bandeja (`windowVisible = true`), y fuerza el foco en primer plano en Windows mediante `ShowWindow(hwnd, SW_RESTORE)` y `SetForegroundWindow(hwnd)`.
- **Configuración Limpia en Gradle**:
  - El flag de canal de desarrollo (`lyraflow.channel=dev`) se inyecta exclusivamente en la tarea `run` (`tasks.withType<JavaExec>`).
  - **Queda estrictamente prohibido** declarar flags de desarrollo en `compose.desktop.application.jvmArgs`, ya que `jpackage` los empaqueta permanentemente en el archivo `.cfg` del launcher de producción.
- **Bypass Explícito para Pruebas**:
  - La ejecución simultánea de múltiples instancias en desarrollo solo se permite cuando se declara explícitamente `-Dlyraflow.allowMultipleInstances=true`.
- **Limpieza de Recursos**:
  - La instancia primaria libera el socket de bloqueo de forma segura durante el cierre ordenado (`SingleInstanceLock.release()`).

---

## 2. ⚡ Actualizaciones In-App de Fricción Cero (Patrón LunaFetch Auto-Updater)
Las actualizaciones de versión DEBEN ofrecer una experiencia sin fricción operativa:

- **Verificación Silenciosa**: Comprobación background al iniciar sin interrumpir al usuario.
- **Flujo In-App**:
  1. El usuario hace clic en *"Actualizar ahora"*.
  2. La app descarga el binario ejecutable (`.exe` / `.msi`) en segundo plano mostrando una barra de progreso en tiempo real.
  3. Una vez completada la descarga, la app ofrece el botón *"Instalar y Reiniciar"*.
  4. Al pulsar *"Instalar y Reiniciar"*, la aplicación ejecuta el instalador silencioso en modo pasivo (`/passive` o `/qn`), cierra la versión antigua, actualiza en caliente y **vuelve a abrir la nueva versión automáticamente**.
- **Cero Fricción**: **El usuario NUNCA debe verse obligado a buscar en la carpeta de Descargas, mover archivos o ejecutar manualmente el ejecutable.**

---

## 3. 🧠 Migración Reactiva a Modo Inteligente al Ingresar API Keys
Para garantizar la mejor experiencia con Inteligencia Artificial:

- **Estado Sin API Key / Modo Local**: Por defecto, la app opera en modo offline / literal (*Voz original* / transcripción local) con los modos IA (*Inteligente* / *Personalizado*) bloqueados reactivamente (`🔒`).
- **Auto-migración al ingresar API Key**: Tan pronto como el usuario pega o ingresa su clave de API por primera vez (cambiando de estado vacío a no vacío), la aplicación **migra automáticamente de "Voz original" a "Inteligente"**.
- **Preservación de Preferencias**: Si el usuario cambia manualmente el modo posteriormente, su preferencia se respeta íntegramente.

---

## 4. 🎨 Integración con el Sistema Operativo y Bandeja (System Tray)
- **Minimización a la Bandeja**: El botón de cerrar (`X`) o minimizar debe ocultar la ventana en el tray manteniendo activo el atajo global (*Global Hotkey*).
- **Menú Contextual Nativo**: Menú de bandeja con atajos de teclado, opción para abrir/restaurar ventana y opción de salida definitiva.

---

## 5. 📐 Persistencia Obligatoria del Estado y Dimensiones de la Ventana (Window State Persistence) [CRÍTICO]
Toda aplicación de escritorio DEBE recordar automáticamente su tamaño (ancho, alto), posición en pantalla y estado de maximizado (`isMaximized` / `WindowPlacement.Maximized`) entre sesiones:

- **Restauración al Iniciar**: Al arrancar la aplicación, se leen los valores guardados en la configuración local de usuario (`window_state`). Si el usuario previamente maximizó la ventana o cambió su tamaño, la app **DEBE abrirse exactamente con las mismas dimensiones y estado de maximizado que tenía antes de cerrarse**.
- **Guardado Continuo o al Cerrar**: Al cambiar las dimensiones de la ventana, al maximizar/restaurar o al ejecutar `onCloseRequested`, la aplicación guarda las propiedades de `WindowState` de forma transparente.
- **Prohibición**: Queda strictly prohibido forzar que la ventana se reinicie siempre en un tamaño fijo predeterminado o en modo flotante sin recordar si el usuario la maximizó o personalizó en su sesión previa.
