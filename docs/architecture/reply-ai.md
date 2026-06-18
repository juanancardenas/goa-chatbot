# Respuestas, contexto e IA

Reglas detalladas para adaptadores de IA, prompt engineering, respuestas base, contexto de respuesta y fallback seguro.

Este documento conserva reglas extraidas de `AGENTS.md`. Mantiene el mismo caracter normativo: `DEBE`, `DEBERIA` y `PUEDE`.

## Adaptador IA (`adapter.out.ai`)

- DEBE ubicar integraciones reales con Spring AI, OpenAI, Ollama, Gemini u otros proveedores en `adapter.out.ai`.
- DEBE implementar el puerto `ChatbotAiClient`.
- `ChatbotAiSettingsAdapter` DEBE implementar el puerto `ChatbotAiSettings`.
- DEBE usar modelos internos `ChatbotAiRequest` y `ChatbotAiResponse`.
- DEBE aislar errores del proveedor y devolver una respuesta segura si la IA no esta disponible.
- DEBE usar `ChatbotAiProperties` solo en `configuration` y adaptadores de salida para configuracion de proveedor, modelo, limites y activacion.
- `domain.services` DEBE depender de `ChatbotAiSettings`, no de `ChatbotAiProperties`.
- DEBE recibir `ChatbotAiRequest` ya preparado desde `domain.services.reply.ai.ChatbotAiReplyService` y construir el system prompt mediante `domain.services.prompt.ChatbotPromptBuilder`.
- NO DEBE filtrar excepciones tecnicas del proveedor hacia los resources.
- NO DEBE meter reglas de negocio del chatbot en el adaptador, salvo las necesarias para traducir errores tecnicos.

Adaptadores actuales:
- `ChatbotAiSettingsAdapter`
- `SpringAiChatbotClient`

Regla de direccion:

```text
domain.ports.out.ChatbotAiClient
  <- adapter.out.ai.SpringAiChatbotClient

domain.ports.out.ChatbotAiSettings
  <- adapter.out.ai.ChatbotAiSettingsAdapter
      -> configuration.ChatbotAiProperties
```

## Prompt engineering (`domain.services.prompt`)

- DEBE ubicar la construccion del system prompt en `ChatbotPromptBuilder`.
- DEBE construir instrucciones de sistema a partir de `ChatbotAiRequest`, propiedades IA y contexto permitido.
- DEBE preservar restricciones funcionales: no inventar datos, no dar asesoramiento legal vinculante, respetar el contexto del encargo y fuentes disponibles.
- DEBE mantener separadas las instrucciones del sistema y el mensaje del usuario. `ChatbotPromptBuilder` NO DEBE construir el mensaje de usuario para IA.
- `ChatbotPromptBuilder` DEBE componer secciones explicitas para prompt base, tipo de conversacion, perfil del usuario, restriccion de ambito, disponibilidad documental, contexto de plataforma e historial reciente.
- `ChatbotPromptBuilder` DEBE diferenciar instrucciones de conversaciones `GENERAL` y `CONTEXTUAL`.
- En conversaciones contextuales, `ChatbotPromptBuilder` DEBE reforzar que solo se respondan datos del encargo activo y que no se inventen tareas legales, estados, hitos, documentos, eventos ni fechas.
- En conversaciones generales, `ChatbotPromptBuilder` DEBE indicar que no se asuma un encargo concreto y que los datos reales de un encargo especifico deben consultarse desde el asistente del encargo.
- `ChatbotPromptBuilder` DEBE tratar el historial reciente solo como continuidad conversacional; NO DEBE permitir que el historial sustituya el contexto de plataforma ni que mezcle identificadores personales o financieros de preguntas anteriores.
- `ChatbotPromptBuilder` DEBE usar `ChatbotAiSettings.maxContextMessages()` para limitar los mensajes recientes incluidos en el prompt.
- NO DEBE realizar llamadas al proveedor de IA.
- NO DEBE acceder a MongoDB, Feign ni recursos HTTP.

## Respuestas base, contexto e IA (`domain.services.reply.base`, `domain.services.reply.context` y `domain.services.reply.ai`)

- `ChatbotBaseReplyBuilder` DEBE actuar como fachada de respuestas base y delegar en builders especializados de `reply.base`.
- `ChatbotBaseReplyBuilder.contextualFallbackReply(...)` DEBE delegar el fallback contextual en `ChatbotContextualFallbackReplyBuilder`.
- Las respuestas deterministas y seguras para inicio, FAQ general, contexto de plataforma, contexto no disponible, documentos y cortesia DEBEN vivir en builders especializados de `reply.base`.
- `ChatbotContextualFallbackReplyBuilder` DEBE generar respuestas seguras cuando una conversacion contextual no tenga contexto de plataforma disponible.
- `ChatbotPlatformReplyBuilder` DEBE componer respuestas deterministas con contexto de plataforma y usar `ChatbotDocumentContextService` cuando la respuesta dependa de contexto documental.
- `ChatbotAiReplyService` DEBE enriquecer la respuesta base solo cuando `chatbot.ai.enabled=true`.
- `ChatbotAiReplyService` DEBE delegar la construccion de `ChatbotAiRequest` en `reply.ai.prompt.ChatbotAiRequestBuilder`.
- `ChatbotAiReplyService` DEBE devolver la respuesta base si el proveedor IA falla, devuelve error, devuelve contenido vacio o lanza excepcion.
- `ChatbotAiReplyService` DEBE devolver `ChatbotAiReplyResult`, no un `String` plano, para propagar tanto la respuesta final como si se uso IA real.
- `ChatbotAiReplyService` DEBE publicar `ChatbotAiMetric` cuando intenta usar IA y `ChatbotFallbackMetric` cuando degrada a respuesta base por fallo de IA.
- `ChatbotAiReplyService` DEBE medir la duracion del intento de IA con `durationMs` y debe registrar metricas tambien en respuestas nulas, errores del proveedor, contenido vacio y excepciones.
- `ChatbotAiReplyService` DEBE capturar internamente fallos de `ChatbotMetricsRecorder` para que la trazabilidad no rompa el flujo conversacional.
- `ChatbotAiReplyResult.usedAi` DEBE ser `true` solo cuando se acepta contenido no vacio devuelto por el proveedor IA como respuesta final.
- `ChatbotAiReplyResult.usedAi` DEBE ser `false` cuando la IA esta desactivada, cuando falla el proveedor, cuando la respuesta del proveedor es invalida o cuando se usa la respuesta base como fallback.
- `ChatbotAiReplyService` NO DEBE persistir mensajes ni modificar conversaciones; esa responsabilidad pertenece a los servicios de conversacion y al orquestador.
- `ChatbotReplyOrchestrator` DEBE propagar `ChatbotAiReplyResult.usedAi` hacia `ChatbotReplyDecision.usedAi`.
- `ChatbotReplyDecision.usedAi` DEBE ser la fuente usada por `ChatbotService` para poblar `ChatbotMessageMetric.usedAi`.
- `ChatbotAiRequestBuilder` DEBE construir `ChatbotAiRequest` con conversacion, perfil, respuesta base, contexto de plataforma permitido, mensajes recientes y propiedades IA.
- `ChatbotAiRequestBuilder` DEBE depender de `ChatbotMessageService` para leer mensajes recientes; `ChatbotAiReplyService` NO DEBE acceder directamente al historial.
- `ChatbotAiRequestBuilder` DEBE construir el mensaje de usuario para IA con la pregunta actual, la respuesta base segura como guardrail y reglas adicionales contextuales cuando la conversacion sea `CONTEXTUAL`.
- `ChatbotAiRequestBuilder` DEBE serializar el contexto de plataforma permitido con `engagementLetterId`, propietario visible, procedimientos, tareas legales, eventos recientes y fuentes internas disponibles.
- `ChatbotAiRequestBuilder` DEBE usar textos de no disponibilidad cuando falte contexto y NO DEBE fabricar valores ausentes.
- `ChatbotAiRequestBuilder` DEBE indicar que la pregunta actual prevalece sobre identificadores personales o financieros del historial y que no se mezclen IBAN, DNI/NIE u otros identificadores sensibles.
- `ChatbotAiRequestBuilder` DEBE evitar pedir a la IA tablas Markdown, pseudo-graficos, sintaxis `**texto**` o formatos que la interfaz no soporte; debe preferir listas claras.
- `ChatbotResponseSanitizer` DEBE normalizar la respuesta final para el frontend antes de persistirla cuando aplique.
