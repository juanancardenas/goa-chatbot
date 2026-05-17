## [Máster en Ingeniería Web por la Universidad Politécnica de Madrid (miw-upm)](http://miw.etsisi.upm.es)

## Back-end con Tecnologías de Código Abierto (BETCA).

> Este proyecto es un apoyo docente de la asignatura y contiene ejemplos prácticos sobre Spring
### Estado del código
[![CI goa](https://github.com/juanancardenas/goa-chatbot/actions/workflows/ci.yml/badge.svg)](https://github.com/juanancardenas/goa-chatbot/actions/workflows/ci.yml/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=juanancardenas_goa-chatbot&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=juanancardenas_goa-chatbot)
[![AWS broken](https://goa.miwupm.es/api/goa-chatbot/system/version-badge)](https://goa.miwupm.es/api/goa-chatbot/system)


### Tecnologías necesarias

`Java` `Maven` `GitHub` `GitHub Actions` `Spring-Boot` `GitHub Packages` `Docker` `OpenAPI`

### :gear: Instalación del proyecto

1. Clonar el repositorio en tu equipo, **mediante consola**:

```sh
> cd <folder path>
> git clone https://github.com/miw-upm/goa-chatbot
```

2. Importar el proyecto mediante **IntelliJ IDEA**
    * **Open**, y seleccionar la carpeta del proyecto.

### :gear: Ejecución en local con IntelliJ

* Ejecutar la clase **Application** con IntelliJ

### :gear: Ejecución en local con Docker
* Crear la red, solo una vez:

```sh
> docker network create chatbotNet
```

* Ejecutar en el proyecto la siguiente secuencia de comandos de Docker:

```sh
> docker compose up --build -d
```

* Cliente Web: `http://localhost:8086/swagger-ui.html`

### Variables de entorno en produccion

El despliegue en AWS App Runner requiere:

| Variable | Uso |
| --- | --- |
| `CHATBOT_OPENAI_API_KEY` | API key usada por Spring AI/OpenAI en el perfil `prod`. |

Alternativa si el estudio decide cambiar de OpenAI a Gemini:

```yaml
# Alternativa si el estudio decide cambiar de OpenAI a Gemini:
# model:
#   chat: google-genai
# google:
#   genai:
#     api-key: ${CHATBOT_GEMINI_API_KEY}
#     chat:
#       options:
#         model: gemini-2.0-flash
#         temperature: 0.2
#         max-output-tokens: 500
```
