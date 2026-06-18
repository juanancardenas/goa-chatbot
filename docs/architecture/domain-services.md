# Dominio, servicios y puertos

Reglas detalladas para el modelo de dominio, servicios, puertos de entrada, puertos de salida y ciclo conversacional.

Este documento conserva reglas extraidas de `AGENTS.md`. Mantiene el mismo caracter normativo: `DEBE`, `DEBERIA` y `PUEDE`.

## Dominio (`domain`)

- DEBE ubicar el modelo de negocio en `domain.model`.
- DEBE ubicar modelos de IA en `domain.model.ai`.
- DEBE ubicar comandos internos de chatbot en `domain.model.chatbot.command`.
- DEBE ubicar decisiones internas de respuesta en `domain.model.chatbot.reply`.
- DEBE ubicar resultados internos de chatbot en `domain.model.chatbot.result`.
- DEBE ubicar modelos internos de metricas conversacionales en `domain.model.metrics`.
- DEBE ubicar modelos internos de seguridad conversacional, moderacion y deteccion PII en `domain.model.safety`.
- DEBE ubicar modelos auxiliares de paginacion en `domain.model.pagination`.
- DEBE ubicar contexto de usuario autenticado en `domain.model.security`.
- DEBE ubicar snapshots/contexto de otros microservicios en `domain.model.platform`.
- DEBE ubicar enumerados de negocio en `domain.enums`.
- DEBE ubicar mensajes constantes reutilizables en `domain.common`.
- DEBE ubicar excepciones propias en `domain.exceptions`.
- DEBE ubicar puertos de entrada de casos de uso en `domain.ports.in`.
- DEBE ubicar puertos de salida en `domain.ports.out`.
- DEBE ubicar casos de uso y servicios de dominio en `domain.services`.

Modelos principales:
- `Conversation`
- `Message`
- `Escalation`

Modelos IA:
- `ChatbotAiRequest`
- `ChatbotAiResponse`

Modelos internos de comando actuales:
- `ChatbotMessageCommand`
- `ChatbotContextualConversationCommand`

Modelos internos de decision actuales:
- `ChatbotAiReplyResult`
- `ChatbotReplyDecision`

Modelos internos de resultado actuales:
- `ChatbotMessageResult`
- `ChatbotContextualConversationResult`
- `ChatbotConfigurationResult`
- `ChatbotConversationHistoryResult`
- `ChatbotConversationSummaryResult`
- `ChatbotHistoryMessageResult`

Modelos internos de metricas actuales:
- `ChatbotMessageMetric`
- `ChatbotAiMetric`
- `ChatbotEscalationMetric`
- `ChatbotFallbackMetric`
- `ChatbotModerationMetric`

Modelos internos de seguridad conversacional actuales:
- `ChatbotModerationAction`
- `ChatbotModerationDecision`
- `ChatbotModerationReason`
- `ChatbotPiiDetectionResult`

Puertos de entrada actuales:
- `CloseConversationUseCase`
- `DeleteConversationUseCase`
- `EscalateConversationUseCase`
- `ReadChatbotConfigurationUseCase`
- `ReadConversationHistoryListUseCase`
- `ReadConversationHistoryUseCase`
- `ReopenConversationUseCase`
- `SendChatbotMessageUseCase`
- `StartContextualConversationUseCase`
- `StartGeneralConversationUseCase`

Modelos auxiliares internos actuales:
- `PageResult`
- `AuthenticatedUserContext`

Enumerados de dominio actuales:
- `ChatbotAiProvider`
- `ChatbotResponseMode`
- `ChatbotScopeViolationReason`
- `ConversationProfileType`
- `ConversationStatus`
- `ConversationType`
- `MessageSenderType`
- `MessageType`
- `PlatformQuestionType`

Modelos de plataforma/contexto:
- `ChatbotPlatformContext`
- `ChatbotDocumentContext`
- `EngagementLetterSummary`
- `EngagementEventPage`
- `EngagementEventSummary`
- `LegalProcedureSummary`
- `UserSummary`

Nota:
- `UserSummary` es el snapshot de usuario usado por `UserClient` y los servicios de dominio que necesitan datos basicos de contacto; no debe reintroducirse un modelo redundante en `domain.model`.

## Servicios (`domain.services`)

- Los componentes de dominio gestionados por Spring DEBEN usar `@Service`.
- DEBEN usar un sufijo acorde a su responsabilidad: `Service`, `Builder`, `Policy`, `Classifier`, `Orchestrator` o equivalente ya establecido en el paquete.
- DEBE trabajar con modelos de dominio, comandos y resultados internos; no con DTOs HTTP.
- DEBE implementar puertos de entrada `domain.ports.in` cuando actue como caso de uso invocado desde resources.
- DEBE acceder a adaptadores externos exclusivamente mediante puertos de `domain.ports.out`.
- NO DEBE importar clases de `adapter.*`.
- NO DEBE acceder directamente a `MongoRepository`, Feign clients concretos o `ChatClient` de Spring AI.
- DEBE encapsular reglas conversacionales en metodos privados cuando no sean triviales.
- DEBE validar ownership de conversaciones antes de leer, modificar, borrar, cerrar, reabrir o escalar.
- DEBE validar estado de conversacion antes de permitir mensajes o escalado.
- DEBE mantener separadas conversaciones `GENERAL` y `CONTEXTUAL`.
- DEBE persistir tanto mensajes de usuario como respuestas del asistente.
- DEBE mantener `sequenceNumber` y `parentMessageId` para trazabilidad de mensajes.
- DEBE reservar `sequenceNumber` mediante el contador atomico `Conversation.lastSequenceNumber`; NO DEBE calcular el siguiente valor leyendo el ultimo mensaje.
- DEBE usar respuestas seguras y restringidas cuando el usuario pregunte fuera del scope del encargo.
- DEBE representar los modos de respuesta internos con `ChatbotResponseMode`, no con literales `String`.
- DEBE evitar inventar informacion de plataforma cuando no hay contexto disponible.

Servicios actuales:
- `ChatbotService`
- `reply.ChatbotReplyOrchestrator`
- `reply.ai.ChatbotAiReplyService`
- `reply.ai.prompt.ChatbotAiRequestBuilder`
- `reply.base.ChatbotBaseReplyBuilder`
- `reply.base.ChatbotContextualFallbackReplyBuilder`
- `reply.base.ChatbotCourtesyReplyBuilder`
- `reply.base.ChatbotGeneralReplyBuilder`
- `reply.base.ChatbotPlatformReplyBuilder`
- `reply.context.ChatbotPlatformContextService`
- `reply.context.ChatbotDocumentContextService`
- `classification.ChatbotQuestionClassifier`
- `classification.ChatbotQuestionTypes`
- `conversation.ChatbotConversationService`
- `conversation.ChatbotEscalationService`
- `conversation.ChatbotHistoryService`
- `conversation.ChatbotMessageService`
- `conversation.ChatbotResponseSanitizer`
- `policies.ChatbotScopePolicy`
- `prompt.ChatbotPromptBuilder`
- `safety.ChatbotModerationService`
- `safety.ChatbotPiiDetector`
- `safety.ChatbotModerationPolicy`

Reglas de organizacion:
- `ChatbotService` DEBE actuar como orquestador fino de caso de uso HTTP/dominio e implementar los puertos de entrada de `domain.ports.in`; NO DEBE concentrar reglas que ya pertenezcan a servicios especializados.
- `reply` DEBE contener la decision y composicion de respuestas del asistente.
- `reply.ai` DEBE contener la orquestacion de respuesta con IA: llamada al puerto `ChatbotAiClient`, activacion por propiedades y fallback seguro ante errores de IA.
- `reply.ai.prompt` DEBE contener la preparacion de `ChatbotAiRequest`, incluyendo mensaje de usuario para IA, contexto de plataforma serializado y mensajes recientes para prompt.
- `reply.base` DEBE contener respuestas base seguras, cortesia, respuesta general, fallback contextual y composicion determinista previa a la IA.
- `reply.context` DEBE contener carga/preparacion de contexto de plataforma y contexto documental.
- `classification` DEBE contener clasificadores de intencion o tipo de pregunta.
- `conversation` DEBE contener ciclo de vida de conversaciones, historial, mensajes, escalado y normalizacion de respuestas para frontend.
- `reply.ChatbotReplyOrchestrator` DEBE decidir la respuesta asistente para un mensaje ya validado: cortesia, restricciones de scope, cambio de encargo, contexto de plataforma, fallback contextual y respuesta general. NO DEBE persistir mensajes ni modificar conversaciones.
- `policies` DEBE contener decisiones de permisos, alcance o restricciones funcionales.
- `prompt` DEBE contener construccion de prompts e instrucciones para la IA.
- `safety` DEBE contener deteccion PII, politica de moderacion, decisiones de seguridad conversacional y registro seguro de metricas de moderacion.
- Los servicios especializados PUEDEN depender entre si dentro de `domain.services` cuando la responsabilidad sea clara; DEBEN seguir dependiendo de adaptadores solo mediante puertos.

## Puertos de entrada (`domain.ports.in`)

- DEBE definir contratos de casos de uso invocados por adaptadores de entrada, principalmente resources HTTP.
- DEBE permanecer libre de anotaciones Spring MVC, persistencia, Feign o detalles de adaptador.
- DEBE usar modelos de dominio, comandos internos, resultados internos y `AuthenticatedUserContext`; NO DEBE usar DTOs HTTP.
- DEBE tener nombres orientados al caso de uso y sufijo `UseCase`.
- DEBE ser implementado por servicios de dominio, actualmente `ChatbotService`.
- NO DEBE contener logica de implementacion.
- NO DEBE depender de `domain.ports.out`; esa dependencia pertenece a los servicios que implementan el caso de uso.

Puertos actuales:
- `CloseConversationUseCase`
- `DeleteConversationUseCase`
- `EscalateConversationUseCase`
- `ReadChatbotConfigurationUseCase`
- `ReadConversationHistoryListUseCase`
- `ReadConversationHistoryUseCase`
- `ReopenConversationUseCase`
- `SendChatbotMessageUseCase`
- `StartContextualConversationUseCase`
- `StartGeneralConversationUseCase`

Regla de direccion:

```text
adapter.in.rest.ChatbotResource
  -> domain.ports.in.*UseCase
      <- domain.services.ChatbotService
```

Regla recomendada:

```text
ChatbotResource -> ReadConversationHistoryListUseCase
ChatbotResource -> StartContextualConversationUseCase
ChatbotResource -> StartGeneralConversationUseCase
ChatbotResource -> SendChatbotMessageUseCase
ChatbotResource -> ReadChatbotConfigurationUseCase
ChatbotResource -> ReadConversationHistoryUseCase
ChatbotResource -> DeleteConversationUseCase
ChatbotResource -> CloseConversationUseCase
ChatbotResource -> ReopenConversationUseCase
ChatbotResource -> EscalateConversationUseCase

ChatbotService <- todos los puertos anteriores
```

## Puertos de salida (`domain.ports.out`)

- DEBE definir contratos que el dominio necesita para salir a adaptadores externos.
- DEBE permanecer libre de anotaciones de persistencia, Feign, HTTP o Spring MVC.
- DEBERIA evitar tipos especificos de frameworks en firmas de puertos.
- NO DEBE contener logica de implementacion.

Puertos actuales:
- `ChatbotAiClient`
- `ChatbotAiSettings`
- `ChatbotMetricsRecorder`
- `ConversationGateway`
- `MessageGateway`
- `EscalationGateway`
- `EngagementClient`
- `UserClient`

Regla recomendada:

```text
ChatbotService -> conversation.ChatbotConversationService
ChatbotService -> conversation.ChatbotMessageService
ChatbotService -> conversation.ChatbotHistoryService
ChatbotService -> conversation.ChatbotEscalationService
ChatbotService -> conversation.ChatbotResponseSanitizer
ChatbotService -> reply.ChatbotReplyOrchestrator
ChatbotService -> safety.ChatbotModerationService
ChatbotService -> ChatbotAiSettings
ChatbotService -> ChatbotMetricsRecorder

conversation.ChatbotConversationService -> ConversationGateway
conversation.ChatbotConversationService -> MessageGateway
conversation.ChatbotMessageService -> MessageGateway
conversation.ChatbotMessageService -> ConversationGateway
conversation.ChatbotHistoryService -> ConversationGateway
conversation.ChatbotHistoryService -> MessageGateway
conversation.ChatbotEscalationService -> EscalationGateway
conversation.ChatbotEscalationService -> ChatbotMetricsRecorder
conversation.ChatbotEscalationService -> UserClient
reply.ChatbotReplyOrchestrator -> reply.base.ChatbotBaseReplyBuilder
reply.ChatbotReplyOrchestrator -> reply.context.ChatbotPlatformContextService
reply.ChatbotReplyOrchestrator -> reply.ai.ChatbotAiReplyService
reply.ChatbotReplyOrchestrator -> policies.ChatbotScopePolicy

reply.base.ChatbotBaseReplyBuilder -> reply.base.ChatbotCourtesyReplyBuilder
reply.base.ChatbotBaseReplyBuilder -> reply.base.ChatbotGeneralReplyBuilder
reply.base.ChatbotBaseReplyBuilder -> reply.base.ChatbotContextualFallbackReplyBuilder
reply.base.ChatbotBaseReplyBuilder -> reply.base.ChatbotPlatformReplyBuilder
reply.base.ChatbotPlatformReplyBuilder -> reply.context.ChatbotDocumentContextService
reply.context.ChatbotPlatformContextService -> EngagementClient
reply.ai.ChatbotAiReplyService -> ChatbotAiClient
reply.ai.ChatbotAiReplyService -> ChatbotAiSettings
reply.ai.ChatbotAiReplyService -> ChatbotMetricsRecorder
reply.ai.ChatbotAiReplyService -> reply.ai.prompt.ChatbotAiRequestBuilder
reply.ai.prompt.ChatbotAiRequestBuilder -> ChatbotAiSettings
reply.ai.prompt.ChatbotAiRequestBuilder -> conversation.ChatbotMessageService
prompt.ChatbotPromptBuilder -> ChatbotAiSettings
safety.ChatbotModerationService -> safety.ChatbotPiiDetector
safety.ChatbotModerationService -> safety.ChatbotModerationPolicy
safety.ChatbotModerationService -> ChatbotMetricsRecorder
```

Deuda tecnica conocida:
- Mantener `ChatbotService` como orquestador fino; la decision de respuesta de `sendMessage` debe permanecer en `reply.ChatbotReplyOrchestrator`.

## Clasificacion y scope conversacional

- `ChatbotQuestionClassifier` DEBE clasificar preguntas de plataforma usando `PlatformQuestionType`.
- `ChatbotScopePolicy` DEBE decidir si una pregunta esta permitida dentro del scope de la conversacion.
- `ChatbotScopeDecision` DEBE representar el resultado de la evaluacion de scope.
- DEBE tratar con especial cuidado referencias a otro encargo, expediente, caso u hoja de encargo.
- DEBE usar respuestas seguras de `ChatbotResponseMessages` cuando la pregunta exceda el alcance permitido.
- DEBERIA evitar hard-code disperso de expresiones: si hay patrones reutilizables, centralizarlos en clasificadores o politicas.

## Conversaciones y mensajes

- DEBE asociar toda conversacion a `userId`.
- DEBE permitir conversaciones de tipo `GENERAL` y `CONTEXTUAL`.
- DEBE asociar conversaciones contextuales a `engagementLetterId`.
- DEBE mantener estados de conversacion mediante `ConversationStatus`.
- `ChatbotConversationService` DEBE centralizar creacion, ownership, cierre, borrado y reapertura de conversaciones.
- `ChatbotHistoryService` DEBE centralizar listado de conversaciones, lectura paginada de mensajes y normalizacion de paginacion.
- `ChatbotMessageService` DEBE centralizar persistencia de mensajes, calculo de secuencia y transformacion a historial/prompt.
- `ChatbotModerationService` DEBE centralizar la moderacion previa de mensajes de usuario, deteccion PII y registro seguro de `ChatbotModerationMetric`.
- `ChatbotReplyOrchestrator` DEBE centralizar la decision de respuesta asistente y devolver `ChatbotReplyDecision` sin persistir mensajes.
- `ChatbotReplyDecision` DEBE incluir el modo de respuesta, uso de datos de plataforma, fuentes resumidas y si la respuesta final uso IA real.
- `ChatbotEscalationService` DEBE centralizar escalado, archivado de conversacion, persistencia de `Escalation` y registro seguro de `ChatbotEscalationMetric`.
- `ChatbotResponseSanitizer` DEBE centralizar transformaciones de salida necesarias para que el frontend renderice respuestas de forma segura.
- DEBE impedir envio de mensajes a conversaciones no activas.
- DEBE moderar mensajes de usuario antes de persistirlos o invocar IA.
- DEBE permitir que advertencias de moderacion continuen el flujo conversacional normal sin asumir uso de IA en la metrica de moderacion.
- DEBE bloquear mensajes con accion `BLOCK` mediante respuesta segura del asistente, sin guardar el mensaje original del usuario ni llamar a IA.
- DEBE impedir reapertura de conversaciones archivadas.
- DEBE borrar mensajes asociados al borrar una conversacion.
- DEBE archivar conversacion cuando se escala.
- DEBE crear la traza de `Escalation` antes de archivar la conversacion; si falla la traza, la conversacion no debe archivarse.
- DEBE tolerar reintentos de escalado tras fallo parcial usando una traza unica por `conversationId`; si la traza existe pero el archivado falla, debe quedar registro del intento y registrarse log de error.
- DEBE persistir escalado en `Escalation` con datos de contacto disponibles.
- DEBE registrar `ChatbotEscalationMetric` en cada intento de escalado, usando `success=true` solo cuando el escalado se completa y codigos controlados en `errorType` cuando falla.
- Un fallo al registrar la metrica de escalado NO DEBE impedir el escalado ni ocultar la excepcion funcional o tecnica original.
- DEBE usar `MessageSenderType` para distinguir `USER` y `ASSISTANT`.
- DEBE usar `MessageType` para distinguir `REQUEST` y `RESPONSE`.
- DEBE usar `ChatbotResponseMode` para distinguir respuestas `GENERAL`, `CONTEXTUAL_PLATFORM_DATA` y `CONTEXTUAL_RESTRICTED`.
- DEBE conservar orden conversacional con `sequenceNumber`.
- DEBE impedir duplicados de mensajes mediante indice unico por `conversationId` y `sequenceNumber`.
- DEBE enlazar respuesta con peticion mediante `parentMessageId` cuando aplique.
- En respuestas bloqueadas por moderacion, `parentMessageId` PUEDE ser `null` porque no se persiste peticion de usuario asociada.
