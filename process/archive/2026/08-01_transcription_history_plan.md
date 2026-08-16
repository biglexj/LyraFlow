# Plan de Implementación: Pestaña "Historial Temporal de Transcripciones" (LyraFlow v1.0.4)

Fecha: 2026-08-01
Módulo Principal: `LyraFlow / composeApp`
Convención de Almacenamiento de Plan: `plan/08-01_transcription_history_plan.md`

---

## 🎯 Objetivo y Alcance

Implementar una sección de **Historial Temporal de Transcripciones** en LyraFlow, orientada a la recuperación rápida de dictados recientes y copia al portapapeles sin añadir complejidad de gestión de archivos pesados.

### Principios de Diseño
1. **Fricción Cero:** Guardado automático y transparente de cada transcripción procesada.
2. **Retención Configurable (24h a 72h):** Limpieza automática periódica para evitar la acumulación de datos innecesarios.
3. **Copiado al Portapapeles con 1-Clic:** Permitir re-copiar transcripciones crudas o refinadas con un solo toque.

---

## 🏛️ Estructura del Dominio y Capas

### 1. Modelo de Datos (`commonMain/kotlin/com/biglexj/lyraflow/core/model/`)
- **[NEW] [TranscriptionHistoryEntry.kt](file:///d:/Proyectos/biglexj/LyraFlow/composeApp/src/commonMain/kotlin/com/biglexj/lyraflow/core/model/TranscriptionHistoryEntry.kt)**
  - `id`: String (UUID)
  - `timestamp`: Long (Epoch Milliseconds)
  - `rawTranscript`: String
  - `refinedText`: String
  - `providerName`: String (ej. "Gemini Flash", "Whisper Local")
  - `audioDurationMs`: Long

### 2. Capa de Datos y Persistencia (`commonMain/kotlin/com/biglexj/lyraflow/data/history/`)
- **[NEW] [TranscriptionHistoryRepository.kt](file:///d:/Proyectos/biglexj/LyraFlow/composeApp/src/commonMain/kotlin/com/biglexj/lyraflow/data/history/TranscriptionHistoryRepository.kt)**
  - Interfaz y repositorio local respaldado por almacenamiento seguro o base de datos ligera / JSON rotativo local.
  - Método `saveEntry(entry: TranscriptionHistoryEntry)`
  - Método `getHistory(retentionWindowMs: Long): Flow<List<TranscriptionHistoryEntry>>`
  - Método `purgeExpiredEntries(expirationCutoffMs: Long)`

### 3. Configuración y Retención (`commonMain/kotlin/com/biglexj/lyraflow/core/config/`)
- **[MODIFY] [SettingsRepository.kt](file:///d:/Proyectos/biglexj/LyraFlow/composeApp/src/commonMain/kotlin/com/biglexj/lyraflow/data/settings/SettingsRepository.kt)**
  - Nueva opción `retentionPeriodHours` (Opciones: `24` horas [Default], `48` horas, `72` horas).

### 4. Interfaz de Usuario (`commonMain/kotlin/com/biglexj/lyraflow/feature/history/`)
- **[NEW] [HistoryScreen.kt](file:///d:/Proyectos/biglexj/LyraFlow/composeApp/src/commonMain/kotlin/com/biglexj/lyraflow/feature/history/HistoryScreen.kt)**
  - Vista adaptativa en Material 3 Expressive.
  - Lista cronológica inversa (más reciente arriba) con tarjetas tonales.
  - Botón prominente *"Copiar al Portapapeles"* en cada elemento.
  - Visualización del texto crudo y texto refinado en pestañas/toggles secundarios.
  - Vacíos interactivos con ilustración/mensaje descriptivo cuando el historial esté limpio.

---

## 🧪 Plan de Verificación

### Pruebas Unitarias (`commonTest`)
- Pruebas del repositorio `TranscriptionHistoryRepositoryTest`:
  - Guardado e inserción de nuevas transcripciones.
  - Purga automática de elementos más antiguos que el umbral de retención (ej. > 24 horas).
  - Recuperación ordenada por fecha descendente.

### Pruebas de Interfaz (Desktop/Android)
- Validación del copiado al portapapeles (`ClipboardManager`).
- Comprobación de selección de retención desde el panel de Ajustes.
