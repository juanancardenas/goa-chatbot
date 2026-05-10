# Guia de estilo y arquitectura - GOA Chatbot (v1)

Documento normativo para contribuir en `goa-chatbot`.
Esta version refleja el estado real del codigo entregado en la auditoria de **2026-05-10**.

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
      classification/
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
    webclients/
```

Notas:
- `domain` contiene modelo, reglas, casos de uso y puertos.
- `infrastructure` contiene adaptadores HTTP, MongoDB, IA, Feign y DTOs de entrada/salida.
- Los DTOs HTTP estan actualmente en `infrastructure.dtos`.
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
- NO DEBE acceder a repositorios, clientes Feign, Spring AI ni MongoDB directamente.
- NO DEBE contener reglas de negocio conversacional, reglas de scope, prompt engineering ni logica de persistencia.

Resources actuales:
- `ChatbotResource`
- `SystemResource`

Ejemplo correcto:

```java
@PostMapping(MESSAGES)
public ChatbotMessageResponseDto sendMessage(@Valid @RequestBody ChatbotMessageRequestDto requestDto) {
    return ChatbotMessageResponseDto.fromDomain(
            this.chatbotService.sendMessage(requestDto.toCommand())
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
- `UserDto`

Modelos IA:
- `ChatbotAiRequest`
- `ChatbotAiResponse`

Modelos internos de comando/resultado actuales:
- `ChatbotMessageCommand`
- `ChatbotMessageResult`
- `ChatbotContextualConversationCommand`
- `ChatbotContextualConversationResult`
- `ChatbotConfigurationStatus`
- `ChatbotConversationHistoryResult`
- `ChatbotConversationSummaryResult`
- `ChatbotHistoryMessageResult`

Modelos de plataforma/contexto:
- `ChatbotPlatformContext`
- `ChatbotDocumentContext`
- `EngagementLetterSummary`
- `EngagementEventPage`
- `EngagementEventSummary`
- `LegalProcedureSummary`
- `UserSummary`

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
- DEBE usar respuestas seguras y restringidas cuando el usuario pregunte fuera del scope del encargo.
- DEBE evitar inventar informacion de plataforma cuando no hay contexto disponible.

Servicios actuales:
- `ChatbotService`
- `ChatbotPlatformContextService`
- `ChatbotDocumentContextService`
- `classification.ChatbotQuestionClassifier`
- `policies.ChatbotScopePolicy`
- `prompt.ChatbotPromptBuilder`

Reglas de organizacion:
- `classification` DEBE contener clasificadores de intencion o tipo de pregunta.
- `policies` DEBE contener decisiones de permisos, alcance o restricciones funcionales.
- `prompt` DEBE contener construccion de prompts e instrucciones para la IA.

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
ChatbotService -> ChatbotAiClient
ChatbotService -> ConversationGateway
ChatbotService -> MessageGateway
ChatbotService -> EscalationGateway
ChatbotService -> UserClient
ChatbotPlatformContextService -> EngagementClient
```

Deuda tecnica conocida:
- `MessageGateway` todavia expone `org.springframework.data.domain.Page`. DEBERIA sustituirse gradualmente por un modelo propio de paginacion del dominio, por ejemplo `PageResult<T>`.

## Adaptador IA (`infrastructure.ai`)

- DEBE ubicar integraciones reales con Spring AI, OpenAI, Ollama, Gemini u otros proveedores en `infrastructure.ai`.
- DEBE implementar el puerto `ChatbotAiClient`.
- DEBE usar modelos internos `ChatbotAiRequest` y `ChatbotAiResponse`.
- DEBE aislar errores del proveedor y devolver una respuesta segura si la IA no esta disponible.
- DEBE usar `ChatbotAiProperties` para configuracion de proveedor, modelo, limites y activacion.
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

Roles actualmente considerados en resources:
- `admin`
- `manager`
- `operator`
- `customer`

Deuda tecnica conocida:
- `ChatbotService` todavia obtiene usuario autenticado mediante `SecurityContextHolder`. En una hexagonal mas estricta, el usuario autenticado DEBERIA entrar desde el adaptador HTTP o mediante un puerto especifico.

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
- DEBE impedir envio de mensajes a conversaciones no activas.
- DEBE impedir reapertura de conversaciones archivadas.
- DEBE borrar mensajes asociados al borrar una conversacion.
- DEBE archivar conversacion cuando se escala.
- DEBE persistir escalado en `Escalation` con datos de contacto disponibles.
- DEBE usar `MessageSenderType` para distinguir `USER` y `ASSISTANT`.
- DEBE usar `MessageType` para distinguir `REQUEST` y `RESPONSE`.
- DEBE conservar orden conversacional con `sequenceNumber`.
- DEBE enlazar respuesta con peticion mediante `parentMessageId` cuando aplique.

## Contexto de plataforma

- `ChatbotPlatformContextService` DEBE cargar contexto asociado al `engagementLetterId` usando `EngagementClient`.
- DEBE tratar el contexto de plataforma como snapshot de lectura.
- DEBE devolver fuentes/resumen mediante `sourcesSummary` cuando se use informacion de plataforma.
- DEBE degradar con seguridad si no hay contexto disponible.
- NO DEBE permitir que una conversacion contextual consulte informacion de otro encargo.

## Documentos y contexto documental

- `ChatbotDocumentContextService` DEBE encapsular la disponibilidad y preparacion de contexto documental.
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
- DEBERIA actualizar paquetes de test cuando se muevan clases en `main`; actualmente puede haber tests con rutas historicas de `domain.services.ai`.

Tests actuales destacados:
- `ChatbotServiceTest`
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

1. Sustituir `Page<Message>` en `MessageGateway` por un modelo propio de paginacion del dominio.
2. Sacar `SecurityContextHolder` de `ChatbotService`, pasando `userId` desde `ChatbotResource` o mediante un puerto de usuario autenticado.
3. Valorar adaptadores dedicados para `UserClient` y `EngagementClient` si los Feign clients empiezan a requerir mapeo o control de errores no trivial.
4. Revisar DTOs no usados o residuales (`ChatbotConversationMessageResponseDto`, `ChatbotConversationResponseDto`) y eliminarlos si no forman parte del contrato publico.
5. Actualizar paquetes de tests historicos para que reflejen la estructura actual (`prompt`, `classification`, `infrastructure.ai`).

## Checklist para agentes de IA antes de modificar codigo

- Identificar si el cambio pertenece a `domain`, `infrastructure` o `configurations`.
- Verificar que ninguna clase de `domain` importe `infrastructure.*`.
- Si se toca un endpoint, actualizar DTO, mapper y resource, no el modelo Mongo.
- Si se toca un caso de uso, trabajar con comandos/resultados internos, no DTOs HTTP.
- Si se toca persistencia, mapear entre entidad y dominio dentro de `infrastructure.mongodb`.
- Si se toca IA, mantener Spring AI dentro de `infrastructure.ai`.
- Si se toca contexto de plataforma, usar puertos (`EngagementClient`, `UserClient`) y no Feign directo.
- Si se toca seguridad, revisar `ResourceServerConfig` y `Security`.
- Si se introduce una excepcion funcional, mapearla en `ApiExceptionHandler`.
- Si se cambia una ruta, actualizar tests funcionales.
