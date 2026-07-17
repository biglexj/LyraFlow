# Empaquetado de LyraFlow

LyraFlow se distribuye desde el proyecto Kotlin Multiplatform. La versión vive en `gradle.properties` y los formatos se configuran en `composeApp/build.gradle.kts`.

## Build local completo

```powershell
.\build-release.ps1 -LocalOnly
```

El script usa el JDK completo indicado por `JAVA_HOME`, ejecuta pruebas y genera:

- MSI y EXE mediante Compose Desktop.
- MSIX de aplicación Full Trust mediante Windows SDK.
- Firma de los tres artefactos con el certificado local configurado.
- `SHA256SUMS.txt` con las huellas de los artefactos.

La salida se guarda directamente en `release/`, sin subcarpetas: todos los instaladores y `SHA256SUMS.txt` quedan juntos y fáciles de encontrar. Los paquetes DEB y RPM deben generarse desde Linux mediante las tareas de Compose Desktop.

## Publicación automática

```powershell
.\build-release.ps1
```

La ejecución sin parámetros exige `main` sincronizada con `origin/main`, GitHub CLI autenticado y un tag/release inexistente. Después de construir, firmar y verificar, crea el commit `release: LyraFlow vX.Y.Z`, un tag anotado, hace push atómico de rama y tag y publica EXE, MSI, MSIX y `SHA256SUMS.txt` en GitHub Releases usando `RELEASE_MESSAGE.md`.

Opciones disponibles:

- `-Version X.Y.Z`: actualiza `versionName` y aumenta `versionCode` cuando cambia la versión.
- `-ReleaseNotesFile archivo.md`: usa otras notas para GitHub.
- `-SkipTests`: omite pruebas, pero conserva la compilación.
- `-SkipBuild`: reutiliza binarios existentes y vuelve a empaquetar.
- `-SkipSigning`: solo está permitido junto con `-LocalOnly`.

## Build sin firma

```powershell
.\build-release.ps1 -LocalOnly -SkipSigning
```

Una publicación oficial nunca admite paquetes sin firma.

El MSIX usa la identidad `biglexj.LyraFlow` y el publicador `CN=biglexj`. Para instalar un paquete firmado con certificado local, Windows debe confiar previamente en la parte pública de ese certificado.
