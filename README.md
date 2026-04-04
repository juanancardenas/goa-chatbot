## [Máster en Ingeniería Web por la Universidad Politécnica de Madrid (miw-upm)](http://miw.etsisi.upm.es)

## Back-end con Tecnologías de Código Abierto (BETCA).

> Este proyecto es un apoyo docente de la asignatura y contiene ejemplos prácticos sobre Spring
### Estado del código
[![CI goa](https://github.com/juanancardenas/goa-chatbot/actions/workflows/ci.yml/badge.svg)](https://github.com/juanancardenas/goa-chatbot/actions/workflows/ci.yml/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=juanancardenas_goa-chatbot&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=juanancardenas_goa-chatbot)
[![AWS broken](https://tqnubzb5ee.eu-west-1.awsapprunner.com/system/version-badge)](https://tqnubzb5ee.eu-west-1.awsapprunner.com/system)


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
