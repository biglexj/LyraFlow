# Plan de Ejecución — Escaneo Dinámico de Modelos y Drag & Drop Multimodal

## Objetivo
Implementar el descubrimiento dinámico de modelos desde las APIs de Gemini y OpenAI, almacenamiento persistente en caché local de modelos disponibles, y soporte de Drag & Drop para imágenes (OCR/reconocimiento) además de audio.

## Alcance
1. **Model Discovery Service**: `ModelDiscoveryService` y DTOs para consultar y filtrar modelos disponibles en Gemini y OpenAI-compatible.
2. **Caché y Persistencia**: Guardar y cargar modelos descubiertos en `DesktopPreferencesStore`.
3. **UI Reactiva**: Botón de escaneo con spinner en `SettingsScreen` y `ModelSelectorDialog`, mostrando chips dinámicos de modelos disponibles.
4. **Auto-escaneo**: Disparar escaneo automático al ingresar una API Key por primera vez.
5. **Drag & Drop de Imágenes**: Soporte para soltar imágenes (`.png`, `.jpg`, `.webp`, etc.) para reconocimiento visual y OCR multimodal.
6. **Pruebas y Verificación**: Suite de pruebas unitarias y compilación completa del proyecto.
