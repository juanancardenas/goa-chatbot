# Guia de estilo y arquitectura - GOA Chatbot (v2)

Documento normativo para contribuir en `goa-chatbot`.
Esta version refleja el estado real del codigo tras el refactor de servicios de conversacion, respuestas base e IA, la unificacion del resumen de usuario en `UserSummary`, la tipificacion de modos de respuesta con `ChatbotResponseMode` y el refuerzo de consistencia conversacional de **2026-05-16**.

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
  -> domain.services
      -> domain.ports.out
          <- infrastructure.mongodb
          <- infrastructure.ai
          <- infrastructure.webclients
```

Regla principal:

```text
domain NO DEBE depender de infrastructure.
infrastructure PUEDE depender de domain.
```

## Estructura real del microservicio

```text
es.upm.api/
  configurations/
  domain/
    common/
    enums/
    exceptions/
    model/
      ai/
      configuration/
      platform/
    ports/out/
    services/
      aireply/
      basereply/
      classification/
      conversation/
      policies/
      prompt/
  infrastructure/
    ai/
    dtos/
    mongodb/
      daos/
      entities/
      persistence/
    resources/
      httperrors/
    security/
    webclients/
```

Notas:
- `domain` contiene modelo, reglas, casos de uso y puertos.
- `infrastructure` contiene adaptadores HTTP, MongoDB, IA, Feign y DTOs de entrada/salida.
- Los DTOs HTTP estan actualmente en `infrastructure.dtos`.
- La resolucion del usuario autenticado vive en `infrastructure.security` y entrega `AuthenticatedUserContext` al dominio.
- Los comandos/resultados internos usados para desacoplar DTOs viven actualmente en `domain.model.configuration`.

## Resources HTTP (`infrastructure.resources`)

- DEBE usar `@RestController` y sufijo `Resource`.
- DEBE definir rutas base y subrutas como constantes `public static final String`.
- DEBE delegar la logica de negocio en `domain.services`.
- DEBE aplicar seguridad con `@PreAuthorize` y constantes de `infrastructure.resources.Security`.
- NO DEBE usar expresiones SpEL literales directamente en `@PreAuthorize` si ya existe una constante reutilizable.
- DEBE usar `@Valid` en cuerpos de entrada cuando aplique.
- DEBE convertir DTOs HTTP de entrada a comandos internos antes de llamar al servicio.
- DEBE convertir resultados internos de dominio a DTOs HTTP de salida antes de devolver respuesta.
- DEBE resolver el usuario autenticado con `AuthenticatedUserContextResolver` y pasar `AuthenticatedUserContext` al servicio de dominio.
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
            this.chatbotService.sendMessage(
                    this.authenticatedUserContextResolver.resolve(authentication),
                    requestDto.toCommand()
            )
    );
}
```

## DTOs y contratos HTTP (`infrastructure.dtos`)

- DEBE ubicar los contratos HTTP en `infrastructure.dtos`.
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
- `ChatbotConversationMessageResponseDto`
- `ChatbotConversationResponseDto`
- `ChatbotConversationSummaryDto`
- `ChatbotHistoryMessageDto`
- `ChatbotMessageRequestDto`
- `ChatbotMessageResponseDto`

Regla de direccion de dependencias:

```text
infrastructure.dtos -> domain.model.configuration  OK
domain.services -> infrastructure.dtos             PROHIBIDO
```

## Dominio (`domain`)

- DEBE ubicar el modelo de negocio en `domain.model`.
- DEBE ubicar modelos de IA en `domain.model.ai`.
- DEBE ubicar comandos/resultados internos en `domain.model.configuration` mientras se mantenga la convencion actual del proyecto.
- DEBE ubicar snapshots/contexto de otros microservicios en `domain.model.platform`.
- DEBE ubicar enumerados de negocio en `domain.enums`.
- DEBE ubicar mensajes constantes reutilizables en `domain.common`.
- DEBE ubicar excepciones propias en `domain.exceptions`.
- DEBE ubicar puertos de salida en `domain.ports.out`.
- DEBE ubicar casos de uso y servicios de dominio en `domain.services`.

Modelos principales:
- `Conversation`
- `Message`
- `Escalation`

Modelos IA:
- `ChatbotAiRequest`
- `ChatbotAiResponse`

Modelos internos de comando/resultado actuales:
- `AuthenticatedUserContext`
- `ChatbotMessageCommand`
- `ChatbotMessageResult`
- `ChatbotContextualConversationCommand`
- `ChatbotContextualConversationResult`
- `ChatbotConfigurationStatus`
- `ChatbotConversationHistoryResult`
- `ChatbotConversationSummaryResult`
- `ChatbotHistoryMessageResult`
- `PageResult`

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

- DEBE usar `@Service` y sufijo `Service`.
- DEBE trabajar con modelos de dominio, comandos y resultados internos; no con DTOs HTTP.
- DEBE acceder a infraestructura exclusivamente mediante puertos de `domain.ports.out`.
- NO DEBE importar clases de `infrastructure.*`.
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
- `aireply.ChatbotAiReplyService`
- `basereply.ChatbotBaseReplyBuilder`
- `basereply.ChatbotPlatformContextService`
- `basereply.ChatbotDocumentContextService`
- `classification.ChatbotQuestionClassifier`
- `conversation.ChatbotConversationService`
- `conversation.ChatbotEscalationService`
- `conversation.ChatbotHistoryService`
- `conversation.ChatbotMessageService`
- `conversation.ChatbotResponseSanitizer`
- `policies.ChatbotScopePolicy`
- `prompt.ChatbotPromptBuilder`

Reglas de organizacion:
- `ChatbotService` DEBE actuar como orquestador de caso de uso HTTP/dominio; NO DEBE concentrar reglas que ya pertenezcan a servicios especializados.
- `aireply` DEBE contener la preparacion de `ChatbotAiRequest`, la llamada al puerto `ChatbotAiClient`, el uso de mensajes recientes para prompt y el fallback seguro ante errores de IA.
- `basereply` DEBE contener respuestas base seguras, contexto de plataforma/documental y composicion determinista previa a la IA.
- `classification` DEBE contener clasificadores de intencion o tipo de pregunta.
- `conversation` DEBE contener ciclo de vida de conversaciones, historial, mensajes, escalado y normalizacion de respuestas para frontend.
- `policies` DEBE contener decisiones de permisos, alcance o restricciones funcionales.
- `prompt` DEBE contener construccion de prompts e instrucciones para la IA.
- Los servicios especializados PUEDEN depender entre si dentro de `domain.services` cuando la responsabilidad sea clara; DEBEN seguir dependiendo de infraestructura solo mediante puertos.

## Puertos de salida (`domain.ports.out`)

- DEBE definir contratos que el dominio necesita para salir a infraestructura.
- DEBE permanecer libre de anotaciones de persistencia, Feign, HTTP o Spring MVC.
- DEBERIA evitar tipos especificos de frameworks en firmas de puertos.
- NO DEBE contener logica de implementacion.

Puertos actuales:
- `ChatbotAiClient`
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
ChatbotService -> basereply.ChatbotBaseReplyBuilder
ChatbotService -> basereply.ChatbotPlatformContextService
ChatbotService -> aireply.ChatbotAiReplyService
ChatbotService -> classification.ChatbotQuestionClassifier
ChatbotService -> policies.ChatbotScopePolicy

conversation.ChatbotConversationService -> ConversationGateway
conversation.ChatbotConversationService -> MessageGateway
conversation.ChatbotMessageService -> MessageGateway
conversation.ChatbotMessageService -> ConversationGateway
conversation.ChatbotHistoryService -> ConversationGateway
conversation.ChatbotHistoryService -> MessageGateway
conversation.ChatbotEscalationService -> EscalationGateway
conversation.ChatbotEscalationService -> UserClient

basereply.ChatbotPlatformContextService -> EngagementClient
aireply.ChatbotAiReplyService -> ChatbotAiClient
aireply.ChatbotAiReplyService -> conversation.ChatbotMessageService
```

Deuda tecnica conocida:
- Mantener `ChatbotService` como orquestador fino; si crece la coordinacion de `sendMessage`, DEBERIA extraerse un caso de uso especifico para envio de mensajes.

## Adaptador IA (`infrastructure.ai`)

- DEBE ubicar integraciones reales con Spring AI, OpenAI, Ollama, Gemini u otros proveedores en `infrastructure.ai`.
- DEBE implementar el puerto `ChatbotAiClient`.
- DEBE usar modelos internos `ChatbotAiRequest` y `ChatbotAiResponse`.
- DEBE aislar errores del proveedor y devolver una respuesta segura si la IA no esta disponible.
- DEBE usar `ChatbotAiProperties` para configuracion de proveedor, modelo, limites y activacion.
- DEBE recibir ya preparadas las instrucciones, contexto permitido y mensajes recientes desde `domain.services.aireply.ChatbotAiReplyService`.
- NO DEBE filtrar excepciones tecnicas del proveedor hacia los resources.
- NO DEBE meter reglas de negocio del chatbot en el adaptador, salvo las necesarias para traducir errores tecnicos.

Adaptador actual:
- `SpringAiChatbotClient`

Regla de direccion:

```text
domain.ports.out.ChatbotAiClient
  <- infrastructure.ai.SpringAiChatbotClient
```

## Prompt engineering (`domain.services.prompt`)

- DEBE ubicar la construccion de prompts en `ChatbotPromptBuilder`.
- DEBE construir prompts a partir de `ChatbotAiRequest` y contexto permitido.
- DEBE preservar restricciones funcionales: no inventar datos, no dar asesoramiento legal vinculante, respetar el contexto del encargo y fuentes disponibles.
- DEBE mantener separadas las instrucciones del sistema y el mensaje del usuario.
- NO DEBE realizar llamadas al proveedor de IA.
- NO DEBE acceder a MongoDB, Feign ni recursos HTTP.

## Respuestas base e IA (`domain.services.basereply` y `domain.services.aireply`)

- `ChatbotBaseReplyBuilder` DEBE generar respuestas deterministas y seguras para inicio, FAQ general, contexto de plataforma, contexto no disponible, documentos y cortesia.
- `ChatbotBaseReplyBuilder` DEBE usar `ChatbotQuestionClassifier` y `ChatbotDocumentContextService` cuando la respuesta base dependa de tipo de pregunta o contexto documental.
- `ChatbotAiReplyService` DEBE enriquecer la respuesta base solo cuando `chatbot.ai.enabled=true`.
- `ChatbotAiReplyService` DEBE construir `ChatbotAiRequest` con conversacion, perfil, respuesta base, contexto de plataforma permitido, mensajes recientes y propiedades IA.
- `ChatbotAiReplyService` DEBE devolver la respuesta base si el proveedor IA falla, devuelve error, devuelve contenido vacio o lanza excepcion.
- `ChatbotAiReplyService` NO DEBE persistir mensajes ni modificar conversaciones; esa responsabilidad pertenece a los servicios de conversacion y al orquestador.
- `ChatbotResponseSanitizer` DEBE normalizar la respuesta final para el frontend antes de persistirla cuando aplique.

## Clasificacion y scope conversacional

- `ChatbotQuestionClassifier` DEBE clasificar preguntas de plataforma usando `PlatformQuestionType`.
- `ChatbotScopePolicy` DEBE decidir si una pregunta esta permitida dentro del scope de la conversacion.
- `ChatbotScopeDecision` DEBE representar el resultado de la evaluacion de scope.
- DEBE tratar con especial cuidado referencias a otro encargo, expediente, caso u hoja de encargo.
- DEBE usar respuestas seguras de `ChatbotResponseMessages` cuando la pregunta exceda el alcance permitido.
- DEBERIA evitar hard-code disperso de expresiones: si hay patrones reutilizables, centralizarlos en clasificadores o politicas.

## Persistencia MongoDB (`infrastructure.mongodb`)

- DEBE ubicar repositories Spring Data en `infrastructure.mongodb.daos`.
- DEBE ubicar entidades Mongo en `infrastructure.mongodb.entities`.
- DEBE ubicar implementaciones de puertos en `infrastructure.mongodb.persistence`.
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
- `ConversationPersistenceMongodb`
- `MessagePersistenceMongodb`
- `EscalationPersistenceMongodb`

## Integraciones externas (`infrastructure.webclients`)

- DEBE aislar llamadas a otros microservicios en clientes Feign dentro de `infrastructure.webclients`.
- DEBE usar `FeignConfig` para propagar `Authorization`.
- DEBE depender de modelos de dominio/plataforma cuando sea necesario devolver snapshots internos.
- NO DEBE inyectar clientes Feign concretos en `domain.services`; los servicios deben depender de puertos.
- DEBERIA introducir adaptadores dedicados si la integracion empieza a requerir traduccion compleja, control de errores, reintentos o mapeos no triviales.

Clientes actuales:
- `EngagementWebClient`, implementa `EngagementClient`.
- `UserWebClient`, implementa `UserClient`.

Regla actual permitida:

```text
Feign interface en infrastructure.webclients implementa puerto de domain.ports.out
```

Regla preferida si crece la complejidad:

```text
domain.ports.out.UserClient
  <- infrastructure.webclients.UserClientAdapter
      -> infrastructure.webclients.UserWebClient
```

## Seguridad

- DEBE mantener seguridad stateless.
- DEBE mantener `@EnableMethodSecurity` activo.
- DEBE mantener coherencia entre `SecurityFilterChain` y `@PreAuthorize`.
- DEBE permitir publicamente solo endpoints explicitamente definidos:
  - `/system/**`
  - `/actuator/health`
  - `/v3/api-docs/**`
  - `/swagger-ui/**`
  - `/swagger-ui.html`
  - `OPTIONS /**`
- DEBE usar constantes de `infrastructure.resources.Security` para autorizacion de metodos.
- NO DEBE introducir rutas publicas sin revisar `ResourceServerConfig` y el metodo resource correspondiente.
- DEBE propagar JWT en llamadas Feign cuando exista autenticacion de usuario.
- DEBE usar token tecnico desde `TokenManager` cuando no exista JWT de usuario.
- DEBE resolver el usuario autenticado en `infrastructure.security.AuthenticatedUserContextResolver` y pasar `AuthenticatedUserContext` al dominio.
- `domain.services` NO DEBE leer directamente `SecurityContextHolder`.

Roles actualmente considerados en resources:
- `admin`
- `manager`
- `operator`
- `customer`

Deuda tecnica conocida:
- La propagacion de JWT para Feign sigue leyendo `SecurityContextHolder` en configuracion tecnica; no debe moverse a dominio.

## Configuracion y perfiles

- DEBE ubicar configuracion tecnica en `configurations`.
- DEBE usar `@ConfigurationProperties` para configuracion estructurada.
- DEBE validar propiedades criticas con Bean Validation cuando aplique.
- DEBE mantener configuracion sensible fuera del codigo fuente.
- DEBE usar perfiles separados para `dev`, `test` y `prod`.
- DEBE mantener `LoggingFilter` limitado a `dev`.
- DEBE mantener `DatabaseSeederDev` limitado a perfiles no productivos si se usa para datos iniciales.

Configuraciones actuales:
- `ChatbotAiProperties`
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
- DEBE centralizar el mapeo HTTP en `infrastructure.resources.httperrors.ApiExceptionHandler`.
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
- `ChatbotEscalationService` DEBE centralizar escalado, archivado de conversacion y persistencia de `Escalation`.
- `ChatbotResponseSanitizer` DEBE centralizar transformaciones de salida necesarias para que el frontend renderice respuestas de forma segura.
- DEBE impedir envio de mensajes a conversaciones no activas.
- DEBE impedir reapertura de conversaciones archivadas.
- DEBE borrar mensajes asociados al borrar una conversacion.
- DEBE archivar conversacion cuando se escala.
- DEBE crear la traza de `Escalation` antes de archivar la conversacion; si falla la traza, la conversacion no debe archivarse.
- DEBE tolerar reintentos de escalado tras fallo parcial usando una traza unica por `conversationId`; si la traza existe pero el archivado falla, debe quedar registro del intento y registrarse log de error.
- DEBE persistir escalado en `Escalation` con datos de contacto disponibles.
- DEBE usar `MessageSenderType` para distinguir `USER` y `ASSISTANT`.
- DEBE usar `MessageType` para distinguir `REQUEST` y `RESPONSE`.
- DEBE usar `ChatbotResponseMode` para distinguir respuestas `GENERAL`, `CONTEXTUAL_PLATFORM_DATA` y `CONTEXTUAL_RESTRICTED`.
- DEBE conservar orden conversacional con `sequenceNumber`.
- DEBE impedir duplicados de mensajes mediante indice unico por `conversationId` y `sequenceNumber`.
- DEBE enlazar respuesta con peticion mediante `parentMessageId` cuando aplique.

## Contexto de plataforma

- `basereply.ChatbotPlatformContextService` DEBE cargar contexto asociado al `engagementLetterId` usando `EngagementClient`.
- DEBE tratar el contexto de plataforma como snapshot de lectura.
- DEBE devolver fuentes/resumen mediante `sourcesSummary` cuando se use informacion de plataforma.
- DEBE degradar con seguridad si no hay contexto disponible.
- NO DEBE permitir que una conversacion contextual consulte informacion de otro encargo.

## Documentos y contexto documental

- `basereply.ChatbotDocumentContextService` DEBE encapsular la disponibilidad y preparacion de contexto documental.
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
- `src/test/java/es/upm/api/infrastructure/...`
- `src/test/java/es/upm/api/functionaltests/...`
- `src/test/java/es/upm/api/integrationtests/...`

Reglas:
- DEBE cubrir casos felices y casos de error.
- DEBE cubrir seguridad/autorizacion en endpoints sensibles.
- DEBE probar reglas de scope conversacional.
- DEBE probar persistencia y mapeos `Entity <-> Domain`.
- DEBE probar configuracion critica de IA.
- DEBERIA mockear proveedores externos y clientes Feign en tests de servicio/resource.
- DEBERIA evitar dependencias fragiles de `LocalDateTime.now()` salvo que el comportamiento temporal sea parte del caso probado.
- DEBERIA actualizar paquetes de test cuando se muevan clases en `main`; actualmente puede haber tests con rutas historicas respecto a `domain.services.basereply`.

Tests actuales destacados:
- `ChatbotServiceTest`
- `ChatbotAiReplyServiceTest`
- `ChatbotBaseReplyBuilderTest`
- `ChatbotConversationServiceTest`
- `ChatbotEscalationServiceTest`
- `ChatbotHistoryServiceTest`
- `ChatbotMessageServiceTest`
- `ChatbotResponseSanitizerTest`
- `ChatbotScopePolicyTest`
- `ChatbotQuestionClassifierTest`
- `ChatbotPlatformContextServiceTest`
- `ChatbotDocumentContextServiceTest`
- `SpringAiChatbotClientTest`
- `ChatbotResourceFT`
- `SystemResourceFT`
- `ConversationPersistenceMongodbTest`
- `MessagePersistenceMongodbTest`
- `EscalationPersistenceMongodbTest`

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
- Servicios de dominio importando `infrastructure.*`.
- DTOs HTTP usados como parametros o retornos de `domain.services`.
- Acceso directo a `MongoRepository` desde servicios de dominio.
- Entidades Mongo expuestas fuera de `infrastructure.mongodb`.
- Clientes Feign concretos inyectados en servicios de dominio.
- Uso directo de `ChatClient` de Spring AI fuera de `infrastructure.ai`.
- SpEL literal disperso en `@PreAuthorize`.
- Mezclar contratos HTTP externos con reglas de dominio sin modelo intermedio.
- Inventar datos de plataforma, documentos o encargo cuando no hay fuente disponible.
- Permitir que una conversacion contextual responda sobre otro encargo.
- Registrar en logs tokens, secretos, prompts con datos sensibles o respuestas completas con informacion confidencial en produccion.
- Crear rutas publicas sin revisar seguridad en filtro y en metodo.

## Deuda tecnica priorizada

1. Mantener `ChatbotService` como orquestador fino; si `sendMessage` sigue creciendo, extraer un caso de uso dedicado para flujo de envio.
2. Valorar adaptadores dedicados para `UserClient` y `EngagementClient` si los Feign clients empiezan a requerir mapeo o control de errores no trivial.
3. Revisar DTOs no usados o residuales (`ChatbotConversationMessageResponseDto`, `ChatbotConversationResponseDto`) y eliminarlos si no forman parte del contrato publico.
4. Mover tests historicos `ChatbotPlatformContextServiceTest` y `ChatbotDocumentContextServiceTest` al paquete `domain.services.basereply` si se decide alinear fisicamente ruta de test y paquete main.
5. Revisar si `ChatbotPromptBuilder` sigue siendo necesario como servicio separado ahora que `ChatbotAiReplyService` prepara la request IA.

## Checklist para agentes de IA antes de modificar codigo

- Identificar si el cambio pertenece a `domain`, `infrastructure` o `configurations`.
- Verificar que ninguna clase de `domain` importe `infrastructure.*`.
- Si se toca un endpoint, actualizar DTO, mapper y resource, no el modelo Mongo.
- Si se toca un caso de uso, trabajar con comandos/resultados internos, no DTOs HTTP.
- Si se toca el flujo conversacional, ubicar la regla en `conversation`, `basereply`, `aireply`, `classification` o `policies` antes de ampliar `ChatbotService`.
- Si se toca persistencia, mapear entre entidad y dominio dentro de `infrastructure.mongodb`.
- Si se toca IA de dominio, usar `domain.services.aireply`; si se toca proveedor real, mantener Spring AI dentro de `infrastructure.ai`.
- Si se toca contexto de plataforma, usar puertos (`EngagementClient`, `UserClient`) y no Feign directo.
- Si se toca seguridad, revisar `ResourceServerConfig` y `Security`.
- Si se introduce una excepcion funcional, mapearla en `ApiExceptionHandler`.
- Si se cambia una ruta, actualizar tests funcionales.
