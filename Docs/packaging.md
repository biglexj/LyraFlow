# Empaquetado y Distribución de LyraFlow

LyraFlow se distribuye en Windows Desktop **exclusivamente mediante instalador ejecutable EXE** (`LyraFlow-Windows-X.Y.Z.exe`). La versión se centraliza en `gradle.properties` y el formato se define en `composeApp/build.gradle.kts` con `TargetFormat.Exe`.

## Build local

```powershell
.\scripts\release\build-release.ps1 -LocalOnly
```

El script utiliza el JDK completo indicado por `JAVA_HOME`, ejecuta las pruebas unitarias y genera:

- Instalador EXE (`LyraFlow-Windows-X.Y.Z.exe`) mediante Compose Desktop (`:composeApp:packageExe`).
- Firma digital del ejecutable con el certificado oficial `CN=biglexj`.
- `SHA256SUMS.txt` con la huella criptográfica SHA-256 del instalador.

La salida se genera de forma limpia en `release/`.

## Publicación oficial en GitHub Releases

```powershell
.\scripts\release\build-release.ps1
```

La ejecución exige `main` sincronizada con `origin/main`, GitHub CLI autenticado y una versión superior a la última tag publicada. El proceso:
1. Compila y verifica el instalador EXE.
2. Firma y calcula el hash SHA-256.
3. Crea el commit `release: LyraFlow vX.Y.Z` y tag anotado `vX.Y.Z`.
4. Realiza push atómico de rama y tag.
5. Publica la release en GitHub con `LyraFlow-Windows-X.Y.Z.exe` y `SHA256SUMS.txt` adjuntos.

Parámetros disponibles:

- `-Version X.Y.Z`: Especifica la versión exacta (avanza `versionCode`).
- `-ReleaseNotesFile archivo.md`: Archivo de notas (por defecto `RELEASE_MESSAGE.md`).
- `-SkipTests`: Omite la suite de pruebas unitarias.
- `-SkipBuild`: Reutiliza el binario existente en `build/`.
- `-SkipSigning`: Permitido únicamente con `-LocalOnly`.
