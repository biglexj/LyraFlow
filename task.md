# Fase 0 — Prototipo Kotlin Multiplatform

- [x] Verificar JDK, Gradle y Android SDK disponibles.
- [x] Crear el esqueleto `composeApp` con `commonMain`, `desktopMain`, `androidMain` y `commonTest`.
- [x] Centralizar versiones y metadatos de la aplicación.
- [x] Crear el estado y coordinador compartido del flujo de dictado.
- [x] Implementar el cliente Gemini Audio con modelos configurables.
- [x] Crear contrato y adaptador sidecar para Whisper local.
- [ ] Validar captura de audio, hotkey e inyección en una sesión manual de Windows.
- [x] Añadir diagnóstico/camino de compatibilidad para X11 y Wayland.
- [x] Crear UI mínima Compose para escritorio y Android.
- [x] Configurar paquetes mínimos de escritorio y Android.
- [x] Ejecutar pruebas y builds disponibles.
- [x] Documentar resultados, límites y siguiente decisión en `walkthrough_kotlin_phase0.md`.
- [x] Alinear el arranque Gradle con Ely-Tesia para aceptar el JDK moderno instalado sin descargar un JDK 17 adicional.

## Material 3 Expressive

- [x] Crear tokens de color, forma y tipografía para temas claro/oscuro/sistema.
- [x] Implementar shell adaptativo y navegación principal.
- [x] Rediseñar el centro de dictado y los estados del pipeline.
- [x] Crear configuración de apariencia, modelo, API de sesión e inyección.
- [x] Persistir preferencias no sensibles en escritorio.
- [x] Verificar escritorio, Android y pruebas.
- [x] Mostrar nivel y duración reales durante la captura activada por hotkey.
- [x] Cambiar Gemini de transcripción literal a escritura contextual fiel.
- [x] Ampliar la ventana inicial de escritorio.
- [x] Añadir un estado animado y bloqueante mientras Gemini procesa el audio.
- [x] Retirar el cliente heredado .NET/WPF y sus scripts de empaquetado.
- [x] Actualizar documentación para Kotlin Multiplatform como implementación única.

## Release oficial 1.0.1

- [x] Establecer la versión única 1.0.1.
- [x] Configurar identidad, icono y Upgrade UUID de Windows.
- [x] Crear manifiesto y generador MSIX para Compose Desktop.
- [x] Generar y firmar EXE, MSI y MSIX.
- [x] Validar artefactos y hashes de la release.
- [x] Convertir el símbolo lateral en selector rápido Automático → Claro → Oscuro.

## Atajos configurables

- [x] Cambiar el atajo predeterminado a Ctrl + Espacio.
- [x] Permitir combinaciones personalizadas de 2 o 3 teclas desde Ajustes.
- [x] Rechazar Windows + Espacio por estar reservado por el sistema.
- [x] Persistir y volver a registrar el atajo sin reiniciar LyraFlow.
- [x] Mostrar la combinación activa en Inicio.
- [x] Verificar escritorio, Android y pruebas.
- [x] Comprobar mediante Win32 que el atajo predeterminado está disponible.

## Publicación automática 1.0.1

- [x] Adaptar `build-release.ps1` al flujo automático WinTTS/Ely-Tesia.
- [x] Añadir preflight de rama, remoto, autenticación, tag y release.
- [x] Verificar build local, pruebas, firma y hashes.
- [x] Documentar el comando de publicación y sus opciones seguras.
- [x] Aplanar la salida para dejar todos los artefactos directamente en `release/`.
- [x] Crear commit y tag `v1.0.1` con push atómico.
- [x] Publicar GitHub Release con EXE, MSI, MSIX y hashes.
- [x] Verificar remotamente la release y sus assets.

## Parche 1.0.2 — bandeja del sistema

- [x] Confirmar la regresión respecto de LyraFlow 1.0.0.
- [x] Separar ocultar la ventana de salir de la aplicación.
- [x] Añadir icono de bandeja con acciones de abrir y salir.
- [x] Mantener el atajo activo mientras la ventana está oculta.
- [x] Actualizar versión y notas a 1.0.2.
- [x] Ejecutar pruebas y compilar escritorio.
- [x] Validar manualmente el ciclo de bandeja.
- [x] Generar y verificar EXE, MSI, MSIX y hashes.
- [ ] Publicar y verificar GitHub Release v1.0.2.
