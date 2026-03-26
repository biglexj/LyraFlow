# Política de Privacidad de LyraFlow 🛡️

**Última actualización: 26 de marzo de 2026**

En **LyraFlow**, nos tomamos muy en serio tu privacidad. Esta política describe cómo manejamos tus datos cuando utilizas nuestra aplicación de dictado inteligente.

## 1. Datos que Procesamos

### Transcripción de Audio
LyraFlow procesa audio capturado por tu micrófono para convertirlo en texto. Dependiendo de tu configuración, el procesamiento se realiza de dos maneras:
- **Modo Local (Whisper.net)**: El audio se procesa íntegramente en tu dispositivo. **Ningún dato de audio sale de tu ordenador.**
- **Modo Nube (Groq API)**: Si seleccionas esta opción, el audio se envía a los servidores de **Groq** para una transcripción ultra-rápida. Consulta la [Política de Privacidad de Groq](https://groq.com/privacy-policy/) para más detalles.

### Refinamiento de Texto (IA)
Si el refinamiento está activado, el texto transcripto se envía a los servidores de **Google Gemini** para corregir gramática y estilo basado en el archivo `context.md`. Consulta la [Política de Privacidad de Google](https://policies.google.com/privacy) para más detalles.

## 2. Almacenamiento de Información

- **Configuración Local**: Tus claves de API (Gemini/Groq) y preferencias se almacenan localmente en tu dispositivo en `%LOCALAPPDATA%\LyraFlow\config.json`. **No almacenamos tus claves en nuestros servidores.**
- **Modelos de Whisper**: Los modelos descargados se guardan localmente en la carpeta de la aplicación.

## 3. Seguimiento y Telemetría
LyraFlow **no incluye** herramientas de seguimiento, análisis de comportamiento ni telemetría oculta. No recopilamos estadísticas de uso ni información personal identificable (PII) sin tu consentimiento explícito.

## 4. Servicios de Terceros
La aplicación utiliza las siguientes APIs opcionales:
- **Google Gemini**: Para el refinamiento de texto.
- **Groq Cloud**: Para la transcripción acelerada.
El uso de estas funciones está sujeto a sus respectivos términos de servicio.

## 5. Tus Derechos
Como usuario, tienes control total sobre tus datos:
- Puedes borrar tus API Keys en cualquier momento desde los ajustes.
- Puedes optar por usar únicamente el modelo local para asegurar que ningún dato salga de tu red.

## 6. Contacto
Si tienes dudas sobre esta política, puedes contactarnos a través del repositorio oficial de GitHub.

---
*Al usar LyraFlow, aceptas los términos descritos en esta política.*
