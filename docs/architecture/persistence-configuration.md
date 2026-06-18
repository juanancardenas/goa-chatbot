# Persistencia y configuracion

Reglas detalladas para persistencia MongoDB, entidades, gateways, configuracion tecnica, perfiles y propiedades.

Este documento conserva reglas extraidas de `AGENTS.md`. Mantiene el mismo caracter normativo: `DEBE`, `DEBERIA` y `PUEDE`.

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
