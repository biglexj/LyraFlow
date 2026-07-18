# Política de privacidad de LyraFlow

Última actualización: 16 de julio de 2026.

LyraFlow captura audio únicamente cuando el usuario inicia un dictado. Con Gemini, el WAV se envía a Google para generar el texto contextualizado y queda sujeto a la política y términos de Google. Cuando Whisper local esté configurado, el procesamiento se realizará en el dispositivo y el audio no se enviará a un proveedor cloud.

Las preferencias de tema, modelo, inicio con Windows e inyección automática se almacenan localmente. En Windows, la API key introducida en la interfaz se conserva cifrada con DPAPI para el usuario actual; también puede proporcionarse mediante la variable de entorno `GEMINI_API_KEY`.

LyraFlow no incluye analítica, seguimiento de comportamiento ni telemetría oculta. La aplicación puede copiar el resultado al portapapeles e insertarlo en la ventana activa cuando el usuario habilita la inyección automática.

El usuario puede evitar el procesamiento cloud usando Whisper local cuando el adaptador y el modelo estén instalados. Para consultas sobre esta política, utiliza el repositorio oficial del proyecto.
