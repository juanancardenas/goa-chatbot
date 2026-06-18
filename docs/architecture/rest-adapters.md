# Adaptadores REST y contratos HTTP

Reglas detalladas para resources HTTP, DTOs, seguridad de entrada y manejo de errores expuestos por la capa REST.

Este documento conserva reglas extraidas de `AGENTS.md`. Mantiene el mismo caracter normativo: `DEBE`, `DEBERIA` y `PUEDE`.

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
