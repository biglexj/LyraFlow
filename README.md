# LyraFlow 🎙️✨

**LyraFlow** es una herramienta de dictado inteligente de "Inyección Directa" diseñada para transformar tu voz en texto refinado y profesional al instante. LyraFlow elimina la fricción de escribir, permitiéndote dictar tus ideas y verlas aparecer perfectamente puntuadas y estructuradas en cualquier aplicación de Windows (WhatsApp, Slack, Word, VS Code, etc.).

## 🚀 Características Principales

- **Inyección Silenciosa Directa**: El texto refinado aparece directamente donde está tu cursor, sin simular la tecla `ENTER` para evitar envíos accidentales.
- **Panel de Configuración Inteligente**: Gestiona tus API Keys de **Gemini** y **Groq** directamente desde la interfaz, con persistencia local segura.
- **Doble Motor de Transcripción**:
  - **Local (Whisper.net)**: Privacidad total con procesamiento local.
  - **Nube (Groq API)**: Transcripción ultra-rápida casi instantánea.
- **Refinamiento con Gemini AI**: Corrige gramática, estilo y estructura automáticamente sin perder ni una palabra del mensaje original.
- **DISEÑO PREMIUM (Turquesa)**: Interfaz minimalista con modo oscuro/claro dinámico y estética de alta gama.
- **Diálogo de Salida Inteligente**: Panel de confirmación centrado para elegir entre **Ocultar** (Bandeja) o **Salir** (Cierre total), con soporte para cerrar haciendo clic fuera o pulsando `Esc`.
- **Atajo Global**: Inicia y detén la grabación con una combinación de teclas (Ej: `Ctrl+Shift+Espacio`).

## 🛠️ Requisitos
- **OS**: Windows 10 o Superior.
- **Framework**: .NET 10.0 (WPF).
- **API Keys**: Para el modo en la nube y refinamiento, necesitas llaves de [Google Gemini](https://aistudio.google.com/) y [Groq](https://console.groq.com/).

## ⚙️ Configuración y Uso

1. **Compilar y Ejecutar**:
   ```ps1
   dotnet run
   ```
2. **API Keys**: Al abrir la ventana de ajustes, introduce tus llaves de Gemini y Groq. Se guardarán localmente en `%LOCALAPPDATA%\LyraFlow\config.json`.
3. **Contexto (`context.md`)**: Este archivo es el "cerebro" del refinamiento. Añade aquí tus reglas personalizadas, glosario técnico o ejemplos de formato para que la IA escriba exactamente como tú quieres.
4. **Dictado**: 
   - Pulsa tu atajo (Ej: `Ctrl+Shift+Espacio`) para empezar a grabar.
   - LyraFlow silenciará el sistema automáticamente.
   - Vuelve a pulsar el atajo para procesar e inyectar.

## 📄 Notas de Versión
- **v3.0**: Panel de API integrado, inyección silenciosa, diálogo de salida personalizado y rediseño visual completo en Turquesa Premium.

---
*Desarrollado para maximizar tu productividad vocal.*
