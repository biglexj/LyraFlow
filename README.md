# LyraFlow 🎙️✨

[![Versión](https://img.shields.io/badge/LyraFlow-v2.0.0-00C9B9)](https://github.com/biglexj/LyraFlow/releases)
[![Licencia](https://img.shields.io/badge/licencia-MIT-lightgrey)](LICENSE)
[![Plataforma](https://img.shields.io/badge/plataforma-Windows%20%7C%20Linux-informational)](https://github.com/biglexj/LyraFlow)

LyraFlow es una aplicación de dictado inteligente construida en **Rust + GPUI**. Captura tu voz mediante un atajo global, la convierte en texto claro con IA multimodal o Whisper local y la inserta en la aplicación activa.

> [!NOTE]
> **Ramas del repositorio:** **`main`** contiene la versión vigente **v2.0.0** (Rust + GPUI). La edición histórica en Kotlin Multiplatform / Compose (**v1.1.9**, descontinuada) se conserva en la rama **`v-kmp`**.

## Estructura del proyecto

```text
LyraFlow/
├── src/                  # Aplicación nativa Rust + GPUI (UI, audio, proveedores, atajos, historial)
├── assets/               # Iconos de branding y copia fijada de tokens Aurora
├── packaging/windows/    # Instalador Inno Setup y scripts .iss
├── scripts/              # Despachador Mise, empaquetado, branding y release
├── Docs/                 # Documentación local y prototipos de diseño
├── build.rs              # Incrustación del recurso de icono Windows
├── Cargo.toml            # Manifiesto principal (versión 2.0.0 y binario LyraFlow.exe)
├── mise.toml             # Tareas estándar (dev, build, check, test, release, publish)
└── package.json          # Alias y versión sincronizada
```

## Organización de recursos

El icono canónico de identidad se conserva en [`assets/branding/icons/icon-transparent.png`](assets/branding/icons/icon-transparent.png). Los recursos derivados (`icon.png`, `app_icon.ico`, `titlebar-icon-badge.png` y `packaging/windows/LyraFlow.ico`) se generan desde esa fuente mediante [`scripts/branding/Sync-Icons.ps1`](scripts/branding/Sync-Icons.ps1) conforme al [Asset Organization Standard](../Docs/global/architecture/asset-organization-standard.md).

## Funciones principales

- Atajo global `Ctrl + Espacio` en Windows (configurable con combinaciones de 2 o 3 teclas) y atajo de cancelación `Ctrl + Alt + Espacio`.
- Captura WAV mono a 16 kHz con nivel y duración visibles.
- Transcripción y refinamiento con **Gemini**, **OpenAI** (endpoint y modelo editables para servicios compatibles) y **MiniMax** (`asr-1.0` + refinamiento), con descubrimiento dinámico de modelos.
- Modos de transcripción **Inteligente**, **Voz original** y **Personalizado** (con ejemplo y copia rápida).
- Respaldo e instalación bajo demanda de **Whisper local** mediante sidecar (`ggml-org/whisper.cpp`), con opción experimental de refinamiento por LLM.
- Inserción directa en la aplicación activa y persistencia local de historial con retención configurable.
- Apariencia **Aurora** y **Clásico** en modos Automático, Claro y Oscuro.
- Comprobación de actualizaciones desde GitHub Releases y diálogo «Acerca de LyraFlow» alineado al ecosistema.

## Tecnologías

- Rust (edición 2024) + GPUI `0.2.2`.
- `cpal` + `hound` para captura y codificación WAV a 16 kHz.
- `zed-reqwest` (`rustls-tls`) para clientes HTTP/multimodales.
- APIs Win32 (`windows` `0.61`) para DPAPI, atajos globales, `SendInput` y desenfoque de fondo modal.

## Operación con Mise

Mise es la entrada principal del proyecto:

```powershell
mise dev
```

| Tarea | Propósito |
|---|---|
| `mise dev` | Ejecutar LyraFlow en desarrollo (`cargo run --locked`). |
| `mise check` | Verificar compilación de Rust + GPUI (`cargo check --locked`). |
| `mise test` | Ejecutar las pruebas unitarias de Rust. |
| `mise test scripts` | Verificar las herramientas de tareas, identidad de build y publicación. |
| `mise build` | Compilar `target/release/LyraFlow.exe` (`cargo build --locked --release`). |
| `mise release` | Generar el instalador oficial Inno Setup en `release/` sin publicar. |
| `mise publish` | Publicar en GitHub Releases y sincronizar ficha en Aurora (`--plan` y `--check` sin escritura). |

Opciones de inspección:
- `--plan`: muestra el comando o plan sin ejecutar ni modificar archivos.
- `--check`: (solo `publish`) valida requisitos locales y remotos en solo lectura.
- `mise publish github`: publica únicamente en GitHub Releases omitiendo Aurora.

## Configuración de proveedores

Desde **Ajustes → Modelos** puedes elegir **Gemini**, **OpenAI** o **MiniMax**, pegar tu clave o definir `LYRAFLOW_API_KEY`, `GEMINI_API_KEY`, `OPENAI_API_KEY` o `MINIMAX_API_KEY`. En Windows las claves se almacenan cifradas con DPAPI por proveedor en `%LOCALAPPDATA%\LyraFlow\secrets.json` (con compatibilidad de lectura desde `LyraFlow-gpui-demo`).

## Distribución

LyraFlow se distribuye en Windows mediante un único instalador ejecutable: `LyraFlow-Windows-X.Y.Z.exe` (`LyraFlow-Windows-2.0.0.exe`). La versión está centralizada en [`Cargo.toml`](Cargo.toml) (`version` y `version_code`) y se sincroniza con [`package.json`](package.json) y [`packaging/windows/version.iss`](packaging/windows/version.iss).

Licencia MIT · Biglex J, 2026.
