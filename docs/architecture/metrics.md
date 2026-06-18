# Metricas conversacionales

Reglas detalladas para modelos de metricas, puerto `ChatbotMetricsRecorder`, trazabilidad de IA, fallback, escalado y logging estructurado.

Este documento conserva reglas extraidas de `AGENTS.md`. Mantiene el mismo caracter normativo: `DEBE`, `DEBERIA` y `PUEDE`.

## Metricas conversacionales (`domain.model.metrics` y `ChatbotMetricsRecorder`)

- DEBE ubicar eventos/modelos internos de metricas conversacionales en `domain.model.metrics`.
- DEBE publicar metricas desde el dominio mediante el puerto de salida `domain.ports.out.ChatbotMetricsRecorder`.
- `ChatbotMetricsRecorder` DEBE permanecer libre de detalles de Micrometer, Prometheus, MongoDB, HTTP, Feign o cualquier adaptador concreto.
- Los modelos de metricas DEBEN representar hechos de dominio observables, no contratos HTTP ni entidades de persistencia.
- Los servicios de dominio NO DEBEN depender de implementaciones concretas de metricas; DEBEN depender solo de `ChatbotMetricsRecorder`.
- Los servicios de dominio DEBEN registrar metricas de forma segura: un fallo del recorder NO DEBE ocultar ni sustituir la excepcion funcional original del caso de uso.
- La implementacion tecnica de metricas, cuando exista, DEBE vivir en `adapter.out` o en un subpaquete tecnico equivalente y mapear desde los modelos de `domain.model.metrics`.
- La implementacion actual de metricas vive en `adapter.out.metrics.LoggingChatbotMetricsRecorder` y DEBE limitarse a registrar logs estructurados como salida tecnica inicial.
- `LoggingChatbotMetricsRecorder` DEBE implementar `ChatbotMetricsRecorder`, estar registrado como componente Spring y no introducir dependencias de observabilidad externa, persistencia, HTTP, Feign ni MongoDB.
- Los logs de metricas DEBEN incluir el campo comun `chatbot_metric_type` con valores identificables como `message_handled`, `ai_call`, `escalation`, `fallback` y `moderation`.
- `LoggingChatbotMetricsRecorder` NO DEBE registrar contenido completo de mensajes, respuestas de IA, prompts, documentos legales, tokens, secretos ni trazas completas de excepciones.
- `LoggingChatbotMetricsRecorder` DEBE ignorar metricas `null` de forma controlada y capturar internamente errores de logging para no interrumpir el flujo principal del chatbot.
- Las consultas operativas para AWS CloudWatch Logs Insights DEBEN mantenerse documentadas en `docs/cloudwatch-logs-insights-metrics.md` cuando cambie el formato de logs de metricas.
- DEBERIA evitar usar identificadores de alta cardinalidad o datos sensibles como tags de sistemas de metricas agregadas; si se necesitan para trazabilidad, deben tratarse como evento/log estructurado segun el adaptador.
- `ChatbotMessageMetric.usedAi` DEBE indicar que la respuesta final enviada al usuario uso contenido real devuelto por el proveedor IA.
- `ChatbotMessageMetric.usedAi` NO DEBE usarse para indicar que la IA estaba habilitada o que se intento llamar al proveedor; los intentos, fallos y fallback de IA deben modelarse con `ChatbotAiMetric`.
- `ChatbotAiMetric` DEBE registrar los intentos de uso de IA realizados por `ChatbotAiReplyService`, incluyendo `provider`, `model`, `durationMs`, `success`, `fallback`, `errorType` y `createdAt`.
- `ChatbotAiMetric.success` DEBE ser `true` solo cuando se acepta contenido no vacio del proveedor IA como respuesta final.
- `ChatbotAiMetric.fallback` DEBE ser `true` cuando el flujo de IA termina usando la respuesta base por respuesta nula, error del proveedor, contenido vacio o excepcion tecnica.
- `ChatbotEscalationMetric` DEBE registrarse desde `ChatbotEscalationService` al finalizar un intento de escalado, tanto si el escalado termina correctamente como si falla con una excepcion funcional o tecnica.
- `ChatbotEscalationMetric.success` DEBE ser `true` solo cuando se ha creado la traza de `Escalation` y se ha solicitado correctamente el archivado atomico de la conversacion mediante `EscalationGateway.createAndArchiveConversation(...)`.
- `ChatbotEscalationMetric.errorType` DEBE ser `null` en escalados correctos y DEBE usar codigos controlados en fallos, actualmente `CONVERSATION_NOT_FOUND`, `CONVERSATION_FORBIDDEN`, `CONVERSATION_NOT_ACTIVE` o `ESCALATION_ERROR`.
- `ChatbotEscalationService` DEBE capturar internamente fallos de `ChatbotMetricsRecorder.recordEscalation(...)` para que la trazabilidad no oculte ni sustituya `NotFoundException`, `ForbiddenException`, `ConflictException` ni errores de persistencia.
- `ChatbotFallbackMetric` DEBE registrarse cuando un fallo de IA obliga a usar fallback; su `fallbackType` y `reason` DEBEN usar codigos controlados, no mensajes completos de usuario, prompts, respuestas IA ni trazas.
- Los errores de registro de `ChatbotAiMetric` o `ChatbotFallbackMetric` NO DEBEN interrumpir la generacion de la respuesta del asistente.
- `ChatbotModerationMetric` DEBE registrarse desde `ChatbotModerationService` al evaluar un mensaje, tanto si se permite, se advierte o se bloquea.
- `ChatbotModerationMetric` DEBE incluir `conversationId`, `userId`, `action`, `reason`, `containsPii`, `blocked`, `usedAi` y `createdAt`.
- `ChatbotModerationMetric.action` DEBE usar `ChatbotModerationAction` con valores controlados `ALLOW`, `WARN` o `BLOCK`.
- `ChatbotModerationMetric.reason` DEBE usar `ChatbotModerationReason` con valores controlados; NO DEBE contener mensajes completos de usuario, prompts, respuestas IA, documentos, identificadores personales o trazas.
- `ChatbotModerationMetric.usedAi` DEBE ser `false` cuando la moderacion bloquea el mensaje antes de invocar IA; DEBE ser `null` cuando la moderacion solo advierte o permite continuar y el uso final de IA aun no esta decidido por ese evento.
- Los errores de registro de `ChatbotModerationMetric` NO DEBEN interrumpir la moderacion ni el flujo conversacional.

Modelos de metricas actuales:
- `ChatbotMessageMetric`: evento de mensaje gestionado por conversacion; resume duracion, exito, tipo de conversacion, modo de respuesta, uso de datos de plataforma y uso real de IA en la respuesta final.
- `ChatbotAiMetric`: evento de llamada o intento de uso de IA, con resultado de exito/fallback y tipo de error controlado.
- `ChatbotEscalationMetric`: evento de intento de escalado a intervencion humana; resume conversacion, usuario, exito, tipo de error controlado y fecha de creacion.
- `ChatbotFallbackMetric`: evento de uso de fallback conversacional o fallback de IA.
- `ChatbotModerationMetric`: evento de moderacion de mensaje; resume accion, razon controlada, presencia de PII, bloqueo, uso de IA cuando sea determinable y fecha de creacion.

Puerto actual:
- `ChatbotMetricsRecorder`

Adaptador actual:
- `LoggingChatbotMetricsRecorder`

Regla de direccion:

```text
domain.ports.out.ChatbotMetricsRecorder
  <- adapter.out.metrics.LoggingChatbotMetricsRecorder
```
