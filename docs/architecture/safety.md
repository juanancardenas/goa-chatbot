# Seguridad conversacional y moderacion

Reglas detalladas para moderacion previa a persistencia, deteccion PII, politicas de seguridad conversacional y metricas asociadas.

Este documento conserva reglas extraidas de `AGENTS.md`. Mantiene el mismo caracter normativo: `DEBE`, `DEBERIA` y `PUEDE`.

## Seguridad conversacional y moderacion (`domain.model.safety` y `domain.services.safety`)

- `ChatbotModerationService` DEBE moderar el mensaje de usuario antes de persistirlo y antes de invocar `ChatbotReplyOrchestrator` o IA.
- `ChatbotModerationService` DEBE devolver `ChatbotModerationDecision` con `action`, `reason`, `safeReply` y `containsPii`.
- `ChatbotModerationAction` DEBE limitarse a `ALLOW`, `WARN` y `BLOCK`.
- `ChatbotModerationReason` DEBE limitar las razones a codigos controlados como `NONE`, razones PII (`PII_EMAIL`, `PII_PHONE`, `PII_DNI_NIE`, `PII_CARD`, `PII_IBAN`) y razones de seguridad (`UNSAFE_REQUEST`, `OUT_OF_POLICY`).
- `ChatbotPiiDetector` DEBE encapsular los patrones de deteccion PII y devolver `ChatbotPiiDetectionResult`; NO DEBE decidir si el mensaje se bloquea o continua.
- `ChatbotPiiDetectionResult` DEBE exponer flags de deteccion y un conjunto inmutable de `ChatbotModerationReason`.
- `ChatbotPiiDetector` DEBE tratar mensajes `null`, vacios o blank como resultado sin PII.
- `ChatbotPiiDetector` DEBE detectar email, telefono espanol, DNI/NIE, tarjeta y IBAN espanol; para evitar falsos positivos, DEBE excluir IBAN espanol antes de evaluar patron de tarjeta.
- `ChatbotModerationPolicy` DEBE centralizar la decision de permitir, advertir o bloquear a partir del resultado PII y senales de seguridad.
- `ChatbotModerationPolicy` DEBE bloquear tarjeta (`PII_CARD`) y solicitudes inseguras o fuera de politica cuando se indiquen.
- `ChatbotModerationPolicy` DEBE advertir, sin bloquear, ante IBAN, DNI/NIE, telefono o email cuando no haya una condicion de bloqueo.
- `ChatbotModerationService` DEBE degradar de forma segura ante errores de deteccion o politica, devolviendo `BLOCK` con `OUT_OF_POLICY` y una respuesta segura.
- `ChatbotModerationService` DEBE registrar `ChatbotModerationMetric` de forma segura mediante `ChatbotMetricsRecorder.recordModeration(...)` sin propagar errores del recorder.
- `ChatbotModerationService` y `LoggingChatbotMetricsRecorder` NO DEBEN registrar contenido completo de mensajes, emails, telefonos, DNI/NIE, tarjetas, IBAN, prompts ni respuestas completas.
- `ChatbotService` DEBE aplicar la moderacion dentro de `handleConversationMessage(...)` despues de validar longitud y ownership/estado de la conversacion, y antes de reservar secuencias para el flujo normal.
- Si la moderacion devuelve `BLOCK`, `ChatbotService` DEBE persistir solo una respuesta segura del asistente, con `usedAi=false`, `usedPlatformData=false`, `sourcesSummary` vacio y `responseMode` `GENERAL` o `CONTEXTUAL_RESTRICTED` segun la conversacion.
- Si la moderacion devuelve `BLOCK`, `ChatbotService` NO DEBE persistir el mensaje original del usuario ni invocar `ChatbotReplyOrchestrator`, `ChatbotAiReplyService` o proveedores externos.
- Si la moderacion devuelve `WARN`, el flujo conversacional PUEDE continuar; la advertencia queda trazada como metrica de moderacion y el uso final de IA se decide posteriormente en el flujo de respuesta.
