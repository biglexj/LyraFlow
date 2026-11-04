# Instalador de Whisper local — Plan

- Estado: `IN_PROGRESS`
- Fecha: `2026-10-30`
- Proyecto: `LyraFlow`

## Objetivo

Que el botón «Instalar Whisper local» de la app descargue el binario `whisper-cli.exe` sin fallar con el error «Collection contains no element».

## Alcance

- Incluye: resolución del paquete `whisper-bin-x64.zip` de `ggml-org/whisper.cpp`, mensajes de error comprensibles, respaldo cuando la API de GitHub no responde y prueba unitaria del selector de assets.
- No incluye: cambios en el proveedor de transcripción, en los modelos de HuggingFace ni en el empaquetado del instalador de Windows.

## Enfoque

1. Diagnosticar el origen del error (`.first {}` sobre una lista de assets vacía).
2. Sustituir `/releases/latest` por el listado completo de releases y quedarse con la primera que incluya el paquete de Windows.
3. Añadir respaldo con URL fijadas y mensajes en español cuando no se pueda resolver el paquete.
4. Cubrir el selector con pruebas unitarias.

## Criterios de finalización

- [x] La instalación resuelve una URL de descarga válida del paquete de Windows.
- [x] Ningún camino del instalador lanza `NoSuchElementException` (`Collection contains no element`).
- [x] Existen pruebas unitarias del selector de assets en verde.
- [ ] Validación manual del flujo completo en la interfaz de Windows.

## Autorización

- [ ] Plan aprobado para ejecución.
