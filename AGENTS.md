# Guia de estilo y arquitectura - GOA Chatbot (v2)

Documento normativo para contribuir en `goa-chatbot`.
Esta version refleja el estado real del codigo tras el refactor de servicios de conversacion, respuestas base, contexto de respuesta e IA bajo `domain.services.reply`, la unificacion del resumen de usuario en `UserSummary`, la tipificacion de modos de respuesta con `ChatbotResponseMode`, el refuerzo de consistencia conversacional de **2026-05-17**, la introduccion de puertos de entrada `domain.ports.in` para los casos de uso HTTP de **2026-05-18**, el refactor de adaptadores bajo `adapter` y configuracion bajo `configuration`, la introduccion de modelos de metricas conversacionales bajo `domain.model.metrics` con el puerto de salida `ChatbotMetricsRecorder` y el adaptador inicial `LoggingChatbotMetricsRecorder`, la trazabilidad de uso real de IA en respuestas mediante `ChatbotAiReplyResult`, `ChatbotReplyDecision.usedAi`, `ChatbotMessageMetric.usedAi`, `ChatbotAiMetric` y `ChatbotFallbackMetric` incorporada en la issue 41, la metrica de escalado emitida desde `ChatbotEscalationService` mediante `ChatbotEscalationMetric`, y la capa de seguridad conversacional bajo `domain.model.safety` y `domain.services.safety` con deteccion PII, moderacion previa a persistencia y `ChatbotModerationMetric`.

## Niveles de regla

- `DEBE`: obligatorio.
- `DEBERIA`: recomendado, salvo razon tecnica explicita.
- `PUEDE`: opcional.

## Arquitectura general

Microservicio Spring Boot del ecosistema GOA encargado de exponer un chatbot conversacional con soporte de IA, persistencia de conversaciones, conversaciones contextuales asociadas a hojas de encargo, consulta de contexto de plataforma y escalado a intervencion humana.

```text
go a-front / gateway
  ->
go a-chatbot
  -> MongoDB
  -> proveedor IA via Spring AI
  -> goa-engagement via OpenFeign
  -> goa-user via OpenFeign
```

El proyecto sigue una aproximacion de **arquitectura hexagonal / puertos y adaptadores**:

```text
resources HTTP
  -> domain.ports.in
      <- domain.services
          -> domain.ports.out
              <- adapter.out.mongodb
              <- adapter.out.ai
              <- adapter.out.metrics
              <- adapter.out.webclient
```

Regla principal:

```text
domain NO DEBE depender de adapter.
adapter PUEDE depender de domain.
```

## Estructura real del microservicio

```text
es.upm.api/
  configuration/
  adapter/
    in/
      rest/
        dto/
        error/
        security/
    out/
      ai/
      metrics/
      mongodb/
        adapter/
        entity/
        repository/
      webclient/
        engagement/
          dto/
        user/
          dto/
  domain/
    common/
    enums/
    exceptions/
    model/
      ai/
      chatbot/
        command/
        reply/
        result/
      metrics/
      pagination/
      platform/
      safety/
      security/
    ports/
      in/
      out/
    services/
      classification/
      conversation/
      policies/
      prompt/
      reply/
        ai/
          prompt/
        base/
        context/
      safety/
```

Notas:
- `domain` contiene modelo, reglas, casos de uso y puertos.
- `adapter` contiene adaptadores de entrada y salida: REST, MongoDB, IA, metricas por logging, Feign y DTOs de entrada/salida.
- `configuration` contiene configuracion tecnica de Spring, seguridad, Feign, OpenAPI, propiedades e inicializacion de indices.
- Los DTOs HTTP estan actualmente en `adapter.in.rest.dto`.
- La resolucion del usuario autenticado vive en `adapter.in.rest.security` y entrega `AuthenticatedUserContext` al dominio.
- Los comandos/resultados internos usados para desacoplar DTOs viven actualmente en `domain.model.chatbot.command`, `domain.model.chatbot.reply` y `domain.model.chatbot.result`.
- Los modelos internos de metricas conversacionales viven actualmente en `domain.model.metrics`.
- Los modelos internos de seguridad conversacional y moderacion viven actualmente en `domain.model.safety`.
- El contexto de usuario autenticado vive en `domain.model.security`.
- Los modelos auxiliares de paginacion viven en `domain.model.pagination`.
- Los puertos de entrada de casos de uso viven en `domain.ports.in`; `ChatbotResource` depende de ellos y `ChatbotService` los implementa.

## Resources HTTP (`adapter.in.rest`)

- DEBE usar `@RestController` y sufijo `Resource`.
- DEBE definir rutas base y subrutas como constantes `public static final String`.
- DEBE delegar la logica de negocio en puertos de entrada de `domain.ports.in`.
- DEBE aplicar seguridad con `@PreAuthorize` y constantes de `adapter.in.rest.Security`.
- NO DEBE usar expresiones SpEL literales directamente en `@PreAuthorize` si ya existe una constante reutilizable.
- DEBE usar `@Valid` en cuerpos de entrada cuando aplique.
- DEBE convertir DTOs HTTP de entrada a comandos internos antes de llamar al puerto de entrada.
- DEBE convertir resultados internos de dominio a DTOs HTTP de salida antes de devolver respuesta.
- DEBE resolver el usuario autenticado con `AuthenticatedUserContextResolver` y pasar `AuthenticatedUserContext` al puerto de entrada.
- DEBE inyectar interfaces de `domain.ports.in`, no implementaciones concretas de servicios de dominio.
- NO DEBE acceder a repositorios, clientes Feign, Spring AI ni MongoDB directamente.
- NO DEBE contener reglas de negocio conversacional, reglas de scope, prompt engineering ni logica de persistencia.

Resources actuales:
- `ChatbotResource`
- `SystemResource`

Ejemplo correcto:

```java
@PostMapping(MESSAGES)
public ChatbotMessageResponseDto sendMessage(
        @Valid @RequestBody ChatbotMessageRequestDto requestDto,
        Authentication authentication
) {
    return ChatbotMessageResponseDto.fromDomain(
            this.sendChatbotMessageUseCase.sendMessage(
                    this.authenticatedUserContextResolver.resolve(authentication),
                    requestDto.toCommand()
            )
    );
}
```

## DTOs y contratos HTTP (`adapter.in.rest.dto`)

- DEBE ubicar los contratos HTTP en `adapter.in.rest.dto`.
- DEBE mantener DTOs separados de modelos de dominio cuando representen entrada/salida HTTP.
- DEBE convertir entrada HTTP a comandos internos mediante metodos `toCommand()` cuando exista caso de uso asociado.
- DEBE convertir modelos internos a DTOs mediante metodos `fromDomain(...)`.
- NO DEBE introducir logica de negocio en DTOs.
- PUEDE contener normalizaciones simples de entrada, por ejemplo convertir strings blank a `null`, si esa normalizacion es propia del contrato HTTP.
- DEBE mantener `ChatbotMessageResponseDto.responseMode` como `String` por compatibilidad del contrato REST, mapeado desde `ChatbotResponseMode.name()`.
- NO DEBE devolver entidades Mongo ni modelos internos directamente desde resources si el endpoint tiene un contrato publico propio.

DTOs HTTP actuales:
- `ChatbotConfigurationStatusDto`
- `ChatbotContextualConversationRequestDto`
- `ChatbotContextualConversationResponseDto`
- `ChatbotConversationHistoryResponseDto`
- `ChatbotConversationSummaryDto`
- `ChatbotHistoryMessageDto`
- `ChatbotMessageRequestDto`
- `ChatbotMessageResponseDto`

Regla de direccion de dependencias:

```text
adapter.in.rest.dto -> domain.model.chatbot.command/result  OK
domain.services -> adapter.in.rest.dto                      PROHIBIDO
adapter.in.rest -> domain.ports.in                          OK
adapter.in.rest -> domain.services                          EVITAR
```

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

## Clasificacion y scope conversacional

- `ChatbotQuestionClassifier` DEBE clasificar preguntas de plataforma usando `PlatformQuestionType`.
- `ChatbotScopePolicy` DEBE decidir si una pregunta esta permitida dentro del scope de la conversacion.
- `ChatbotScopeDecision` DEBE representar el resultado de la evaluacion de scope.
- DEBE tratar con especial cuidado referencias a otro encargo, expediente, caso u hoja de encargo.
- DEBE usar respuestas seguras de `ChatbotResponseMessages` cuando la pregunta exceda el alcance permitido.
- DEBERIA evitar hard-code disperso de expresiones: si hay patrones reutilizables, centralizarlos en clasificadores o politicas.

## Persistencia MongoDB (`adapter.out.mongodb`)

- DEBE ubicar repositories Spring Data en `adapter.out.mongodb.repository`.
- DEBE ubicar entidades Mongo en `adapter.out.mongodb.entity`.
- DEBE ubicar implementaciones de puertos en `adapter.out.mongodb.adapter`.
- DEBE usar sufijo `Entity` para documentos Mongo.
- DEBE marcar colecciones con `@Document` e IDs con `@Id`.
- DEBERIA usar `@Indexed` y `@CompoundIndex` cuando haya consultas frecuentes o restricciones de ordenacion.
- DEBE mapear `Entity <-> Domain` dentro de la propia entidad o el adaptador de persistencia.
- DEBE implementar puertos de `domain.ports.out`.
- NO DEBE exponer entidades Mongo hacia `domain.services` ni hacia resources.
- NO DEBE inyectar repositories directamente en servicios de dominio.

Repositories actuales:
- `ConversationRepository`
- `MessageRepository`
- `EscalationRepository`

Entidades actuales:
- `ConversationEntity`
- `MessageEntity`
- `EscalationEntity`

Adaptadores actuales:
- `ConversationAdapter`
- `MessageAdapter`
- `EscalationAdapter`

## Integraciones externas (`adapter.out.webclient`)

- DEBE aislar llamadas a otros microservicios en clientes Feign dentro de `adapter.out.webclient`.
- DEBE usar `FeignConfig` para propagar `Authorization`.
- DEBE ubicar DTOs propios de respuestas Feign en subpaquetes del adaptador, por ejemplo `adapter.out.webclient.user.dto` y `adapter.out.webclient.engagement.dto`.
- Los Feign clients NO DEBEN devolver modelos de dominio; DEBEN devolver DTOs del adaptador.
- Los adaptadores de webclient DEBEN implementar puertos de `domain.ports.out` y mapear DTOs Feign a modelos internos de dominio/plataforma.
- Los mappers de adaptador DEBEN mantener explicito el mapeo entre contrato externo y modelo interno.
- NO DEBE inyectar clientes Feign concretos en `domain.services`; los servicios deben depender de puertos.

Clientes y adaptadores actuales:
- `engagement.EngagementFeignClient`, devuelve DTOs de `adapter.out.webclient.engagement.dto`.
- `engagement.EngagementClientAdapter`, implementa `EngagementClient`.
- `engagement.EngagementFeignMapper`, mapea DTOs de engagement a `domain.model.platform`.
- `user.UserFeignClient`, devuelve DTOs de `adapter.out.webclient.user.dto`.
- `user.UserClientAdapter`, implementa `UserClient`.
- `user.UserFeignMapper`, mapea DTOs de user a `domain.model.platform.UserSummary`.

Regla actual:

```text
domain.ports.out.UserClient
  <- adapter.out.webclient.user.UserClientAdapter
      -> adapter.out.webclient.user.UserFeignClient
          -> adapter.out.webclient.user.dto.UserResponseDto
```

```text
domain.ports.out.EngagementClient
  <- adapter.out.webclient.engagement.EngagementClientAdapter
      -> adapter.out.webclient.engagement.EngagementFeignClient
          -> adapter.out.webclient.engagement.dto.*
```

```text
adapter.out.webclient.*.dto -> domain.model  PROHIBIDO
domain.ports.out -> adapter.out.webclient.*  PROHIBIDO
```

## Seguridad

- DEBE mantener seguridad stateless.
- DEBE mantener `@EnableMethodSecurity` activo.
- DEBE mantener coherencia entre `SecurityFilterChain` y `@PreAuthorize`.
- DEBE permitir publicamente solo endpoints explicitamente definidos:
  - `GET /system/**`
  - `/actuator/health`
  - `/v3/api-docs/**`
  - `/swagger-ui/**`
  - `/swagger-ui.html`
  - `OPTIONS /**`
- DEBE limitar los endpoints publicos al metodo HTTP minimo necesario; `SystemResource` solo DEBE exponerse publicamente mediante `GET`.
- DEBE mantener CSRF activado por defecto en cadenas publicas o de endpoints que no necesiten desactivarlo.
- PUEDE desactivar CSRF solo en cadenas stateless autenticadas mediante `Authorization Bearer` JWT, sin cookies ni sesiones server-side, y DEBE dejar justificacion explicita junto a la configuracion.
- DEBE usar constantes de `adapter.in.rest.Security` para autorizacion de metodos.
- NO DEBE introducir rutas publicas sin revisar `ResourceServerConfig` y el metodo resource correspondiente.
- DEBE propagar JWT en llamadas Feign cuando exista autenticacion de usuario.
- DEBE usar token tecnico desde `TokenManager` cuando no exista JWT de usuario.
- DEBE resolver el usuario autenticado en `adapter.in.rest.security.AuthenticatedUserContextResolver` y pasar `AuthenticatedUserContext` al dominio.
- `domain.services` NO DEBE leer directamente `SecurityContextHolder`.

Roles actualmente considerados en resources:
- `admin`
- `manager`
- `operator`
- `customer`

Deuda tecnica conocida:
- La propagacion de JWT para Feign sigue leyendo `SecurityContextHolder` en configuracion tecnica; no debe moverse a dominio.

## Configuracion y perfiles

- DEBE ubicar configuracion tecnica en `configuration`.
- DEBE usar `@ConfigurationProperties` para configuracion estructurada.
- DEBE validar propiedades criticas con Bean Validation cuando aplique.
- DEBE mantener configuracion sensible fuera del codigo fuente.
- DEBE usar perfiles separados para `dev`, `test` y `prod`.
- DEBE mantener `LoggingFilter` limitado a `dev`.
- DEBE mantener `DatabaseSeederDev` limitado a perfiles no productivos si se usa para datos iniciales.
- DEBE exponer un `Clock` comun desde `configuration` usando UTC para fechas tecnicas internas.
- DEBE inyectar `Clock` en servicios y componentes que generen timestamps; NO DEBE usar `LocalDateTime.now()`, `Instant.now()` o equivalentes sin `Clock` o `ZoneId` explicito.

Configuraciones actuales:
- `ChatbotAiProperties`
- `ChatbotMongoIndexInitializer`
- `FeignConfig`
- `LoggingFilter`
- `OpenApiConfig`
- `ResourceServerConfig`
- `TokenManager`

Propiedades IA relevantes:
- `chatbot.ai.enabled`
- `chatbot.ai.provider`
- `chatbot.ai.model`
- `chatbot.ai.max-input-characters`
- `chatbot.ai.max-output-tokens`
- `chatbot.ai.max-context-messages`
- `chatbot.ai.timeout-seconds`
- `chatbot.ai.documents-available`
- `chatbot.ai.base-prompt`
- `chatbot.ai.temperature`

Proveedores IA soportados por configuracion:
- `ollama`
- `openai`
- `gemini`

## Excepciones y manejo de errores

- DEBE usar excepciones propias de `domain.exceptions` para errores funcionales.
- DEBE centralizar el mapeo HTTP en `adapter.in.rest.error.ApiExceptionHandler`.
- NO DEBE usar `printStackTrace()`, `System.out` ni `System.err` para diagnostico de errores; DEBE usar logging configurado con el nivel adecuado y sin exponer datos sensibles.
- NO DEBE manejar errores de forma ad hoc en cada resource salvo casos tecnicamente justificados.
- DEBE traducir errores de validacion a `400 BAD_REQUEST`.
- DEBE traducir errores de ownership/acceso funcional a `403 FORBIDDEN`.
- DEBE traducir entidades inexistentes a `404 NOT_FOUND`.
- DEBE traducir conflictos de estado a `409 CONFLICT`.
- DEBE traducir fallos de integraciones externas a `502 BAD_GATEWAY` cuando aplique.

Excepciones actuales:
- `BadGatewayException`
- `BadRequestException`
- `ConflictException`
- `ForbiddenException`
- `NotFoundException`

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

## Contexto de plataforma

- `reply.context.ChatbotPlatformContextService` DEBE cargar contexto asociado al `engagementLetterId` usando `EngagementClient`.
- DEBE tratar el contexto de plataforma como snapshot de lectura.
- DEBE devolver fuentes/resumen mediante `sourcesSummary` cuando se use informacion de plataforma.
- DEBE degradar con seguridad si no hay contexto disponible.
- NO DEBE permitir que una conversacion contextual consulte informacion de otro encargo.

## Documentos y contexto documental

- `reply.context.ChatbotDocumentContextService` DEBE encapsular la disponibilidad y preparacion de contexto documental.
- DEBE respetar `chatbot.ai.documents-available`.
- DEBE evitar simular documentos inexistentes como si fueran fuente real.
- PUEDE devolver contexto vacio o no disponible si aun no hay integracion documental implementada.

## Testing

Convencion actual observada:
- Unitarios: `*Test`.
- Funcionales HTTP: `*FT`.
- Integracion puntual: `*IT`.

Directorios actuales:
- `src/test/java/es/upm/api/domain/...`
- `src/test/java/es/upm/api/adapter/...`
- `src/test/java/es/upm/api/configuration/...`
- `src/test/java/es/upm/api/functionaltests/...`
- `src/test/java/es/upm/api/integrationtests/...`

Reglas:
- DEBE cubrir casos felices y casos de error.
- DEBE cubrir seguridad/autorizacion en endpoints sensibles.
- DEBE probar reglas de scope conversacional.
- DEBE probar deteccion PII, decisiones de moderacion, bloqueo sin persistir mensaje de usuario y registro seguro de metricas de moderacion.
- DEBE probar persistencia y mapeos `Entity <-> Domain`.
- DEBE probar configuracion critica de IA.
- DEBERIA mockear proveedores externos y clientes Feign en tests de servicio/resource.
- DEBERIA usar fechas fijas, `Clock.fixed(...)` o fixtures temporales controladas en tests; DEBERIA evitar dependencias fragiles de `LocalDateTime.now()` salvo que el comportamiento temporal sea parte del caso probado.
- DEBERIA usar constantes de `java.time.Month` en tests al crear `LocalDate` o `LocalDateTime`, evitando literales numericos para meses.
- DEBERIA usar assertions expresivas de AssertJ en tests: `isZero()` para contadores a cero, `isEmpty()` para colecciones vacias, `hasToString(...)` para validar representacion textual, y `satisfies(...)` o `extracting(...)` para agrupar comprobaciones del mismo objeto cuando mejore la legibilidad.
- DEBE incluir al menos una assertion o verificacion explicita por test; en Mockito, DEBERIA evitar `eq(...)` cuando todos los argumentos son valores directos y reservarlo para combinaciones con `any(...)`, captors u otros matchers.
- DEBERIA actualizar paquetes de test cuando se muevan clases en `main`; los tests de servicios de respuesta deben seguir la estructura fisica de `domain.services.reply`.

Tests actuales destacados:
- `ChatbotServiceTest`
- `ChatbotResourceTest`
- `ChatbotAiReplyResultTest`
- `ChatbotAiReplyServiceTest`
- `ChatbotAiRequestBuilderTest`
- `ChatbotPromptBuilderTest`
- `ChatbotModerationDecisionTest`
- `ChatbotPiiDetectionResultTest`
- `ChatbotModerationMetricTest`
- `ChatbotMetricsRecorderTest`
- `ChatbotModerationPolicyTest`
- `ChatbotModerationServiceTest`
- `ChatbotPiiDetectorTest`
- `ChatbotBaseReplyBuilderTest`
- `ChatbotContextualFallbackReplyBuilderTest`
- `ChatbotCourtesyReplyBuilderTest`
- `ChatbotGeneralReplyBuilderTest`
- `ChatbotPlatformReplyBuilderTest`
- `ChatbotConversationServiceTest`
- `ChatbotEscalationServiceTest`
- `ChatbotHistoryServiceTest`
- `ChatbotMessageServiceTest`
- `ChatbotReplyOrchestratorTest`
- `ChatbotResponseSanitizerTest`
- `ChatbotScopePolicyTest`
- `ChatbotQuestionClassifierTest`
- `ChatbotPlatformContextServiceTest`
- `ChatbotDocumentContextServiceTest`
- `SpringAiChatbotClientTest`
- `LoggingChatbotMetricsRecorderTest`
- `ChatbotResourceFT`
- `SystemResourceFT`
- `ConversationAdapterTest`
- `MessageAdapterTest`
- `EscalationAdapterTest`

## Tecnologia y build

- Lenguaje: Java.
- Framework principal: Spring Boot.
- Seguridad: Spring Security Resource Server JWT.
- Persistencia: MongoDB / Spring Data MongoDB.
- Integraciones HTTP internas: OpenFeign.
- IA: Spring AI.
- Documentacion API: springdoc OpenAPI.
- Validacion: Jakarta Validation.
- Utilidades: Lombok.

Reglas:
- DEBE compilar con la version de Java definida por el proyecto.
- DEBE mantener annotation processing de Lombok habilitado en IDE y CI.
- DEBE evitar introducir dependencias nuevas sin necesidad clara.
- DEBERIA mantener imports ordenados y sin comodines.

## Antipatrones prohibidos

- Logica de negocio en resources.
- Resources HTTP inyectando implementaciones concretas de `domain.services` cuando exista un puerto de entrada aplicable.
- Servicios de dominio importando `adapter.*`.
- DTOs HTTP usados como parametros o retornos de `domain.services`.
- Acceso directo a `MongoRepository` desde servicios de dominio.
- Entidades Mongo expuestas fuera de `adapter.out.mongodb`.
- Clientes Feign concretos inyectados en servicios de dominio.
- Uso directo de `ChatClient` de Spring AI fuera de `adapter.out.ai`.
- SpEL literal disperso en `@PreAuthorize`.
- Mezclar contratos HTTP externos con reglas de dominio sin modelo intermedio.
- Inventar datos de plataforma, documentos o encargo cuando no hay fuente disponible.
- Permitir que una conversacion contextual responda sobre otro encargo.
- Saltarse `ChatbotModerationService` antes de persistir mensajes de usuario o invocar IA.
- Registrar en metricas o logs contenido completo de mensajes moderados, emails, telefonos, DNI/NIE, tarjetas, IBAN o prompts.
- Registrar en logs tokens, secretos, prompts con datos sensibles o respuestas completas con informacion confidencial en produccion.
- Crear rutas publicas sin revisar seguridad en filtro y en metodo.

## Deuda tecnica priorizada

1. Mantener `ChatbotService` como orquestador fino; si `sendMessage` sigue creciendo, extraer un caso de uso dedicado para flujo de envio.
2. Revisar si `ChatbotPromptBuilder` y `ChatbotAiRequestBuilder` deben compartir algun helper de normalizacion para evitar duplicar reglas como `safeText` y textos de valor no disponible.

## Checklist para agentes de IA antes de modificar codigo

- Identificar si el cambio pertenece a `domain`, `adapter` o `configuration`.
- Verificar que ninguna clase de `domain` importe `adapter.*`.
- Si se toca un endpoint, actualizar DTO, mapper y resource, no el modelo Mongo.
- Si se toca un endpoint de chatbot, inyectar y llamar al puerto de entrada `domain.ports.in` correspondiente, no a `ChatbotService` directamente.
- Si se toca un caso de uso, trabajar con comandos/resultados internos, no DTOs HTTP.
- Si se crea un caso de uso HTTP nuevo, definir su `*UseCase` en `domain.ports.in`, implementarlo en un servicio de dominio y cubrir la llamada desde el resource con test.
- Si se toca el flujo conversacional, ubicar la regla en `conversation`, `reply`, `classification` o `policies` antes de ampliar `ChatbotService`.
- Si se toca moderacion, ubicar modelos en `domain.model.safety`, reglas en `domain.services.safety` y registrar solo codigos controlados mediante `ChatbotModerationMetric`.
- Si se toca deteccion PII, cubrir patrones positivos, negativos y falsos positivos relevantes, especialmente IBAN frente a tarjeta.
- Si se toca persistencia, mapear entre entidad y dominio dentro de `adapter.out.mongodb`.
- Si se toca IA de dominio, usar `domain.services.reply.ai`; si se toca proveedor real, mantener Spring AI dentro de `adapter.out.ai`.
- Si se toca la preparacion de `ChatbotAiRequest`, usar `domain.services.reply.ai.prompt.ChatbotAiRequestBuilder`; si se toca el system prompt, usar `domain.services.prompt.ChatbotPromptBuilder`.
- Si se toca el formato de metricas de `LoggingChatbotMetricsRecorder`, actualizar `docs/cloudwatch-logs-insights-metrics.md`.
- Si se toca contexto de plataforma, usar puertos (`EngagementClient`, `UserClient`) y no Feign directo.
- Si se toca seguridad, revisar `ResourceServerConfig` y `Security`.
- Si se introduce una excepcion funcional, mapearla en `ApiExceptionHandler`.
- Si se cambia una ruta, actualizar tests funcionales.
