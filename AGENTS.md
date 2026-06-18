# Guia de estilo y arquitectura - GOA Chatbot (v2)

Documento normativo principal para contribuir en `goa-chatbot`.

Este fichero es el punto de entrada para agentes y contribuidores. Las reglas detalladas se han movido a documentos tematicos bajo `docs/architecture/`; esos documentos tambien son normativos y deben consultarse antes de modificar el area correspondiente.

La particion documental no cambia la arquitectura, paquetes, responsabilidades ni contratos del microservicio.

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

## Referencias detalladas

- [Adaptadores REST y contratos HTTP](docs/architecture/rest-adapters.md): resources, DTOs, seguridad REST y manejo de errores.
- [Dominio, servicios y puertos](docs/architecture/domain-services.md): modelo de dominio, servicios, puertos, clasificacion y conversaciones.
- [Respuestas, contexto e IA](docs/architecture/reply-ai.md): adaptador IA, prompt engineering, respuestas base, contexto e IA.
- [Metricas conversacionales](docs/architecture/metrics.md): `ChatbotMetricsRecorder`, metricas de mensajes, IA, fallback, escalado y moderacion.
- [Seguridad conversacional y moderacion](docs/architecture/safety.md): PII, moderacion previa a persistencia y politica conversacional.
- [Contexto de plataforma e integraciones externas](docs/architecture/platform-context.md): Feign, engagement, usuario, contexto de plataforma y documentos.
- [Persistencia y configuracion](docs/architecture/persistence-configuration.md): MongoDB, entidades, gateways, propiedades y perfiles.
- [Testing, tecnologia y build](docs/architecture/testing-build.md): pruebas, build Maven y convenciones tecnicas transversales.

## Reglas criticas siempre aplicables

- `domain` NO DEBE depender de `adapter`; `adapter` PUEDE depender de `domain`.
- Los resources HTTP DEBEN delegar en puertos de entrada de `domain.ports.in`, resolver `AuthenticatedUserContext` y convertir DTOs a comandos/resultados.
- Los DTOs HTTP DEBEN permanecer en `adapter.in.rest.dto` y no deben contener logica de negocio.
- Los servicios de dominio DEBEN trabajar con modelos internos, implementar casos de uso cuando aplique y acceder a externos solo mediante puertos de `domain.ports.out`.
- `ChatbotService` DEBE actuar como orquestador fino; la decision de respuesta pertenece a `reply.ChatbotReplyOrchestrator`.
- Las conversaciones DEBEN validar ownership y estado antes de leer, modificar, borrar, cerrar, reabrir, escalar o aceptar mensajes.
- Los mensajes DEBEN persistir usuario y asistente, mantener `sequenceNumber` y usar el contador atomico `Conversation.lastSequenceNumber`.
- Los modos internos de respuesta DEBEN usar `ChatbotResponseMode`, no literales `String`.
- Las integraciones externas, MongoDB, Feign, Spring AI y metricas DEBEN atravesar puertos de salida o adaptadores tecnicos, nunca services de dominio acoplados a implementaciones.
- `ChatbotMetricsRecorder` DEBE permanecer libre de detalles tecnicos y los fallos de registro de metricas NO DEBEN romper el flujo funcional.
- La IA DEBE degradar a respuestas base seguras si esta desactivada, falla, devuelve contenido vacio o responde de forma invalida.
- `usedAi` DEBE indicar uso real de contenido aceptado del proveedor IA, no mera activacion o intento.
- La moderacion conversacional DEBE ejecutarse antes de persistir mensajes cuando aplique y NO DEBE registrar contenido sensible en metricas.
- El chatbot NO DEBE inventar informacion de plataforma ni mezclar datos de encargos fuera del contexto permitido.

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
