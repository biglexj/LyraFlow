# 🎯 LyraFlow — Roadmap

Plan de trabajo, objetivos y prioridades del proyecto.

## 🔴 Urgente / Importante (Prioridad Alta)
- [ ] Validar manualmente endpoints multimodales de terceros configurados por el usuario en Windows.
- [ ] Añadir adaptadores nativos únicamente cuando un proveedor no exponga audio mediante OpenAI-compatible.

## 🟡 Intermedio (Prioridad Media/Baja)
- [ ] Integración avanzada con el Centro de Feedback oficial (`https://www.biglexj.com/feedback`) enviando metadatos completos por API JSON.
- [ ] Añadir perfiles de proveedor para conservar también modelos y endpoints recientes por servicio.
- [ ] Incorporar una prueba de integración opcional contra un endpoint configurado por el usuario.

## 🟢 Completado
- [x] **v1.1.0** (2026-08-02) — Dictado inteligente & transcripción multimodal con Drag & Drop, Pestañas de System Prompt (*Voz Original* vs *Inteligente*), Single-Instance Lock con listener socket ping y bypass dev (`isDev`), Auto-actualizador In-App silencioso con barra de progreso y reinicio en caliente, Persistencia de estado de ventana (Sección 5), Catálogo GPT-5.6 (`gpt-5.6-luna`, `gpt-5.6-terra`, `gpt-5.6-sol`) y Feedback Center.
- [x] In-App Silent Auto-Update con barra de progreso, descarga en segundo plano e instalación pasiva silenciosa ("Instalar y Reiniciar" sin tocar descargas).
- [x] Persistencia de Estado de Ventana (Sección 5: ancho, alto, posición y estado de maximizado entre sesiones).
- [x] Single-Instance Lock inteligente con listener socket ping y bypass automático en entorno de desarrollo (`-Dlyraflow.dev=true`).
- [x] Pestañas de System Prompt (*Voz Original* / literal vs *Inteligente*) con migración reactiva al ingresar API Key por primera vez.
- [x] Catálogo actualizado de OpenAI a la familia GPT-5.6 (`gpt-5.6-luna`, `gpt-5.6-terra`, `gpt-5.6-sol`, `gpt-audio-5.6`) con auto-migración de claves guardadas.
- [x] Botón de Feedback Center e integración directa con GitHub Issues en modal "Acerca de".
- [x] **v1.0.9** (2026-07-27) — Detección dinámica de tema oscuro nativo en tiempo real y rediseño de apariencia Material 3 Expressive.
- [x] Hito inicial del proyecto
- [x] Actualizar iconos con fondo y esquinas redondeadas para la aplicación y la bandeja de sistema.
- [x] Separar el dominio de dictado de Gemini mediante un catálogo de proveedores multimodales.
- [x] Añadir adaptador OpenAI-compatible con endpoint y modelo editables e indicativo de compatibilidad experimental.
- [x] Migrar claves cifradas y preferencias para Gemini y proveedores compatibles.
