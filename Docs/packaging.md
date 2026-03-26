# 📦 Guía de Empaquetado y Distribución - LyraFlow

Este documento resume los pasos para generar la versión distribuible de LyraFlow (v1.0).

## 🚀 Flujos de Compilación

Todos los scripts se encuentran en la carpeta `scripts/`.

### 1. Ejecutable Portable (.exe)
Genera un archivo único que incluye todo lo necesario (Self-Contained). No requiere instalación.
- **Script:** `.\scripts\build-exe.ps1`
- **Salida:** `publish\exe\LyraFlow.exe`

### 2. Instalador MSIX (.msix)
Genera un paquete de Windows firmado para una instalación limpia y profesional.
- **Paso A (Generar):** `.\scripts\build-msix.ps1`
- **Paso B (Firmar):** `sudo pwsh -File .\scripts\sign-msix.ps1` (Requiere Admin)
- **Salida:** `publish\msix\LyraFlow.msix`

---

## 🔐 Detalles de Firma (MSIX)

Para que el MSIX funcione sin errores de seguridad, el script de firma realiza lo siguiente automáticamene:
1. Crea un certificado auto-firmado (`LyraFlow_Dev.pfx`).
2. Lo instala en el almacén de **Entidades de certificación raíz de confianza** del equipo.
3. Firma el paquete usando `signtool.exe`.

> [!IMPORTANT]
> Si el script de firma falla con un error de "aplicación no asociada", ejecútalo siempre como:
> `sudo pwsh -File .\scripts\sign-msix.ps1`

---

## 🛠️ Notas Técnicas
- **ID de Aplicación:** `biglexj.LyraFlow`
- **Assets:** Los iconos y logos se gestionan en la carpeta `Image/`.
- **Manifest:** El archivo `Package.appxmanifest` define las capacidades (Full Trust) y la identidad visual.

🟢 **LyraFlow v1.0 - Listo para despegar.** 🚀🎙️✨
