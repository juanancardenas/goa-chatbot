# Contexto de plataforma e integraciones externas

Reglas detalladas para integraciones con microservicios externos, contexto de plataforma, contexto documental y snapshots de usuario o encargo.

Este documento conserva reglas extraidas de `AGENTS.md`. Mantiene el mismo caracter normativo: `DEBE`, `DEBERIA` y `PUEDE`.

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
