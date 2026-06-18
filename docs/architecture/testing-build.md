# Testing, tecnologia y build

Reglas detalladas para pruebas, tecnologia soportada, build Maven y convenciones tecnicas transversales.

Este documento conserva reglas extraidas de `AGENTS.md`. Mantiene el mismo caracter normativo: `DEBE`, `DEBERIA` y `PUEDE`.

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
- `ChatbotResourceIT`
- `SystemResourceIT`
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
