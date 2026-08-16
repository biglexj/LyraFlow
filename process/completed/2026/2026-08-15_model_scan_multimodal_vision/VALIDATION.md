# Validación — Escaneo Dinámico de Modelos y Drag & Drop Multimodal

## Comprobaciones realizadas
1. **Pruebas Unitarias**:
   - `ModelDiscoveryServiceTest`: Validado exitosamente el filtrado de modelos de Gemini (`generateContent`) y OpenAI (`chat/multimodal`), descartando embeddings, imagen pura y TTS.
   - `DesktopPreferencesStoreTest`: Validada exitosamente la persistencia de `discoveredModels` por proveedor.
2. **Pruebas de Compilación y Calidad**:
   - `.\gradlew check`: Compilación y suite completa de tests ejecutados con `BUILD SUCCESSFUL`.
3. **Pruebas Funcionales**:
   - Persistencia de modelos descubiertos tras reiniciar la aplicación verificada mediante tests de unidad de DesktopPreferencesStore.
   - Receptor de Drag & Drop ampliado para archivos de audio (`.wav`, `.mp3`, `.m4a`, `.ogg`, `.flac`) y archivos de imagen (`.png`, `.jpg`, `.jpeg`, `.webp`, `.bmp`, `.gif`) con prompts contextuales de OCR y transcripción multimodal.
