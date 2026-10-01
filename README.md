# API Gateway Expo Hub

Maqueta de arquitectura de microservicios con **API Gateway** como punto único de
entrada, autenticación real vía **Amazon Cognito** y un frontend que nunca habla
directamente con los microservicios.

---

## Índice

1. [Arquitectura](#1-arquitectura)
2. [Conceptos clave](#2-conceptos-clave)
3. [Requisitos previos](#3-requisitos-previos)
4. [Infraestructura (Docker)](#4-infraestructura-docker)
5. [Cómo levantar el proyecto](#5-cómo-levantar-el-proyecto)
6. [Autenticación con Amazon Cognito](#6-autenticación-con-amazon-cognito)
7. [Frontend](#7-frontend)
8. [API expuesta](#8-api-expuesta)
9. [Tests y verificación](#9-tests-y-verificación)
10. [Decisiones de diseño](#10-decisiones-de-diseño)

---

## 1. Arquitectura

```
                      ┌──────────────────────────────────────────┐
                      │            client-service                │
                      │      Vite :5173  (login / main)          │
                      │   aws-amplify → login SRP con Cognito    │
                      └───────────────────┬──────────────────────┘
                                          │  fetch con Authorization: Bearer <JWT>
                                          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              api-gateway  :8080                            │
│                        Spring Cloud Gateway 5.0.3 (WebFlux)                │
│                                                                             │
│   -101  RequestLoggingFilter   loguea cada petición (incl. los 401)         │
│   -100  Spring Security        valida JWT de Cognito (iss + client_id)      │
│     ·   route (application.yml)  /appointments → :8081 , /patients → :8000  │
│      0  PatientIdHeaderFilter   inyecta X-Patient-Id desde el sub           │
└──────────────┬──────────────────────────────────────┬───────────────────────┘
               │                                      │
               ▼                                      ▼
  ┌────────────────────────┐            ┌─────────────────────────────┐
  │ patients-service :8000 │            │ appointments-service  :8081 │
  │ FastAPI + SQLAlchemy   │            │ Spring Boot + MongoDB       │
  │ PostgreSQL patients_db │            │ Kafka producer              │
  └────────────────────────┘            └──────────────┬──────────────┘
                                                       │  evento
                                                       ▼  appointment-created-topic
                                        ┌─────────────────────────────┐
                                        │ notifications-service :8082 │
                                        │ Kafka consumer (sin HTTP)   │
                                        │ imprime en consola          │
                                        └─────────────────────────────┘
```

### Servicios

| Directorio | Puerto | Stack | Persistencia |
| --- | ---: | --- | --- |
| `api-gateway/` | 8080 | Spring Cloud Gateway 5.0.3 (WebFlux/reactivo) + Spring Security OAuth2 Resource Server | — |
| `patients-service/` | 8000 | FastAPI + SQLAlchemy 2 | PostgreSQL → `patients_db` |
| `appointments-service/` | 8081 | Spring Boot MVC + Spring Data MongoDB + productor Kafka | MongoDB → `appointments_db` |
| `notifications-service/` | 8082 | Spring Boot MVC + consumidor Kafka. **No expone HTTP** | — |
| `client-service/` | 5173 | Vite 8 + AWS Amplify 6 | — |

**No hay build raíz, ni Makefile, ni compose.** Cada servicio se levanta y se
construye desde dentro de su propio directorio.

---

## 2. Conceptos clave

### ¿Qué es un API Gateway?

Una **fachada** delante de N microservicios: una sola dirección de entrada que
oculta la complejidad interna, valida quién entra y decide a dónde va cada
petición. Centraliza lo transversal para no repetirlo en cada servicio.

### Las cuatro responsabilidades (y cuáles son indispensables)

| Responsabilidad | ¿Indispensable? | En esta maqueta |
| --- | --- | --- |
| **Autenticación** | Sí, en la práctica | ✅ `SecurityConfig` — Resource Server que valida el JWT de Cognito |
| **Enrutamiento** | **Sí, estrictamente** — sin él no hay gateway | ✅ `application.yml` (declarativo) |
| **Transformación y agregación** | No, opcional | ⚠️ `PatientIdHeaderFilter` inyecta `X-Patient-Id`. **Agregación NO implementada** |
| **Monitoreo y gestión de tráfico** | No, recomendable | ⚠️ Solo `RequestLoggingFilter`. **Rate limiting NO implementado** |

### Gateway vs balanceador vs proxy inverso

| | Entrada | Destino | Pregunta de diseño |
| --- | --- | --- | --- |
| **Load balancer** | 1 | M copias del **mismo** servicio | ¿cómo reparto la carga? |
| **Proxy inverso** | 1 | **1** servicio | ¿cómo me interpongo con ese servicio? |
| **API Gateway** | 1 | **N servicios distintos** | ¿qué hago con el tráfico entrante? |

En la práctica los proveedores suman capacidades (Nginx hace de las tres); lo que
importa es qué querés hacer con el tráfico.

### Patrones con los que tiene sinergia

**Alta:** Aggregation (es la responsabilidad que no implementamos), Circuit
Breaker, Bulkhead, Log Aggregation (lo hace `RequestLoggingFilter`).
**Media:** Branch, CQRS (ruteo condicional / lecturas vs escrituras).
**Baja o nula:** SAGA y Event Sourcing (viven dentro de los servicios, hablando
por eventos), Shared Database (anti-patrón de acoplamiento).

---

## 3. Requisitos previos

| Herramienta | Versión usada | Para qué |
| --- | --- | --- |
| **JDK** | 25 instalado (los POM fijan `--release` 21 o 17) | Los 3 servicios Java |
| **Maven** | `./mvnw` (wrapper, resuelve 3.9.16) | **No hay `mvn` de sistema — siempre `./mvnw`** |
| **Node.js** | v22 + npm 10 | `client-service` |
| **Python** | 3.14 (el `venv/` ya está commiteado) | `patients-service` |
| **Docker** | 29 | Postgres, Mongo, Kafka |
| **AWS Cognito** | Pool ya configurado | Login (ver [sección 6](#6-autenticación-con-amazon-cognito)) |

Comprobación rápida:

```bash
java -version          # 25.x
node -v && npm -v      # v22.x / 10.x
docker --version
./patients-service/venv/bin/python --version   # 3.14.x
```

> **No hay lint ni formatter en el repo.** La verificación es
> `./mvnw compile` + `./mvnw test`.

---

## 4. Infraestructura (Docker)

Cuatro contenedores:

| Contenedor | Puerto | Para qué |
| --- | ---: | --- |
| `temp-postgres` | 5432 | `patients_db` (usuario `postgres` / pass `postgres`) |
| `temp-mongo` | 27017 | `appointments_db` |
| `temp-kafka` | 9092-9093 | Broker único, KRaft |
| `quality_pg` | 5433 | **No forma parte del proyecto** — no está referenciado en ningún código |

### Levantar (forma correcta)

```bash
docker start temp-postgres temp-mongo temp-kafka
```

⚠️ **Usá `docker start`, no `docker run`.** Los contenedores ya existen con
volúmenes anónimos: re-ejecutar `docker run` falla por conflicto de nombre, o si
borrás el contenedor anterior **crea volúmenes nuevos y perdés los datos**. Solo
recreá un contenedor si querés descartar sus datos a propósito.

### Si no existieran (solo primera vez en otra máquina)

```bash
docker run -d --name temp-postgres -p 5432:5432 \
  -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=patients_db postgres

docker run -d --name temp-mongo -p 27017:27017 mongo:latest

docker run -d --name temp-kafka -p 9092:9092 -p 9093:9093 \
  -e KAFKA_NODE_ID=1 -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@127.0.0.1:9093 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 apache/kafka:latest

docker update --restart unless-stopped temp-postgres temp-mongo temp-kafka
```

### Notas

- El tema de Kafka **se crea solo** cuando el productor manda el primer evento —
  no hay paso de bootstrap.
- En este host además corre un `mongod` nativo al lado del contenedor. Para
  asegurarte de mirar el contenedor: `docker exec temp-mongo mongosh --quiet appointments_db`.
- Inspeccionar: `docker ps`, `docker logs -f temp-kafka`.

---

## 5. Cómo levantar el proyecto

### 5.1 Primera vez

```bash
# 1. dependencias del frontend
cd client-service && npm install && cd ..

# 2. la infraestructura (ya debe existir; ver sección 4)
docker start temp-postgres temp-mongo temp-kafka

# 3. comprobar que el .env de pacientes está (ya commiteado)
cat patients-service/.env
#   DATABASE_URL=postgresql://postgres:postgres@localhost:5432/patients_db
```

### 5.2 Orden recomendado (5 terminales)

El orden exacto no es crítico: si el gateway arranca con un backend caído,
responde **503** hasta que lo levantes. Lo que sí es obligatorio es que **la
infraestructura Docker esté arriba antes** que los servicios Java.

```bash
# TERMINAL 1 — patients-service
cd patients-service
./venv/bin/uvicorn main:app --reload --port 8000

# TERMINAL 2 — appointments-service
cd appointments-service
./mvnw spring-boot:run

# TERMINAL 3 — notifications-service
cd notifications-service
./mvnw spring-boot:run

# TERMINAL 4 — api-gateway
cd api-gateway
./mvnw spring-boot:run

# TERMINAL 5 — frontend
cd client-service
npm run dev
```

> **Siempre desde el directorio del servicio.** En `patients-service` es
> obligatorio: `load_dotenv()` resuelve `.env` desde el *cwd* y los imports son
> `app.*`.

### 5.3 Verificar

```bash
# los 5 deben aparecer como LISTEN
ss -ltnp | grep -E ':(5173|8000|8080|8081|8082)'

# 401 — el gateway exige token, y en SU terminal debe verse la línea de log:
#   [GATEWAY] GET /appointments -> 401 UNAUTHORIZED [sin ruta (rechazado)] (5 ms)
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/appointments
```

Abrí <http://localhost:5173> y registrá un usuario.

### 5.4 Apagar todo

```bash
# terminales 1-5: Ctrl+C en cada una, o a ciegas:
pkill -f "spring-boot:run"
pkill -f "uvicorn main:app"
pkill -f "node.*vite"

# la infraestructura se puede dejar corriendo
docker stop temp-postgres temp-mongo temp-kafka
```

---

## 6. Autenticación con Amazon Cognito

### 6.1 Qué hay configurado en AWS

| Dato | Valor |
| --- | --- |
| User Pool ID | `us-east-1_WyUxPZ3hF` |
| App client ID | `5mn8kpjmjahao9ru6b6ms99s9i` |
| Región | `us-east-1` |
| Flujo de login | **SRP (Direct Custom)** — `signIn()` de Amplify, sin Hosted UI |
| Atributo custom | `custom:cedula` (writable, mutable) |
| Verificación | Email obligatorio (codigo de confirmación) |

**No hay callback URL ni logout URL** porque no se usa la Hosted UI: el login
ocurre en nuestro propio formulario, así que no hace falta redirect.

### 6.2 Dónde vive la configuración (dos lugares que deben coincidir)

**a) Gateway — valida los tokens** en
`api-gateway/src/main/resources/application.yml`:

```yaml
app:
  cognito:
    issuer-uri: https://cognito-idp.us-east-1.amazonaws.com/us-east-1_WyUxPZ3hF
    jwk-set-uri: https://cognito-idp.us-east-1.amazonaws.com/us-east-1_WyUxPZ3hF/.well-known/jwks.json
    client-id: 5mn8kpjmjahao9ru6b6ms99s9i
```

**b) Frontend — emite los tokens** en `client-service/src/config.js`:

```js
Amplify.configure({
  Auth: {
    Cognito: {
      userPoolId: 'us-east-1_WyUxPZ3hF',
      userPoolClientId: '5mn8kpjmjahao9ru6b6ms99s9i',
      region: 'us-east-1'
    }
  }
});
```

> ⚠️ La clave de config es **`userPoolClientId`**, no `userPoolWebClientId`
> — esa cadena no existe en Amplify 6 y da `Auth UserPool not configured.`

### 6.3 Qué valida el gateway

`SecurityConfig.java` construye un `ReactiveJwtDecoder` con dos validadores:

1. **Issuer** — el token viene de *nuestro* User Pool.
2. **`client_id`** — coincide con el App Client.

**No se valida `aud`**: los *access tokens* de Cognito **no traen ese claim**.
Por eso `spring.security.oauth2.resourceserver.jwt.audiences` debe quedar sin
definir.

`jwk-set-uri` se pasa **explícitamente** para que el arranque no dependa de la
descubrimiento OIDC.

> Nota: el logging de `org.springframework.cloud.gateway` está en `DEBUG` en el
> YAML; eso imprime `RouteDefinition matched: <id>` por ruta al arrancar, que es
> la forma más rápida de comprobar que el enrutamiento se bindeó.

### 6.4 Flujo de login completo (frontend)

1. `signUp({ username: email, password, options: { userAttributes: { 'custom:cedula': ... } } })`
2. `confirmSignUp({ username, confirmationCode })` → ⚠️ la clave es
   **`confirmationCode`**, no `code` (con `code` da `code is required`)
3. `signIn({ username, password })` → SRP
4. `fetchAuthSession().tokens.accessToken` → el JWT
5. Redirige a `main.html`
6. `profile.js` llama `GET /patients/me`; si devuelve 404, hace
   `POST /patients/` con los atributos de Cognito (`fetchUserAttributes()`)

### 6.5 Conseguir un token para probar con `curl`

1. Entrá a <http://localhost:5173> y logueate.
2. DevTools → **Application → Local Storage → `http://localhost:5173`** → la
   clave de Cognito → copiá `accessToken`.
3. Usalo:

```bash
TOKEN="eyJhbGciOi..."
curl localhost:8080/patients/me -H "Authorization: Bearer $TOKEN"
```

### 6.6 Si cambiás el User Pool

Actualizá **los dos archivos** de la sección 6.2 y reiniciá el gateway
(`spring-boot:run` **no** recarga `application.yml`).

---

## 7. Frontend

### Páginas

| Archivo | Qué es |
| --- | --- |
| `login.html` | Tres paneles: login, registro (email + contraseña + cédula) y confirmación con código |
| `main.html` | Dashboard: perfil, widget de citas y form de nueva cita |

### Módulos (`client-service/src/`)

| Archivo | Responsabilidad |
| --- | --- |
| `config.js` | `Amplify.configure` |
| `login.js` | *Entry point* de `login.html` |
| `auth.js` | `signIn` / `signUp` / `confirmSignUp`, guard de sesión, mensajes de error en español |
| `home.js` | *Entry point* de `main.html`: guard (`getCurrentUser()` → si falla, a `login.html`) y botón **Salir** (`signOut()`) |
| `profile.js` | `loadProfile()` → `GET /patients/me`, o `POST /patients/` si no existe |
| `appointments.js` | Carga `GET /appointments` y alta con `POST /appointments` |
| `api.js` | `getToken()`, `apiGet()`, `apiPost()` — todos con `Authorization: Bearer` |

Assets estáticos (Bootstrap, AOS, fuentes) viven en `public/assets/`.
`public/assets/js/header.js` y `main.js` son JS vanilla sin módulos.

### Proxy (evita CORS)

`vite.config.js`:

```js
proxy: {
  '/patients':     'http://localhost:8080',
  '/appointments': 'http://localhost:8080'
}
```

El navegador solo habla con `:5173` y Vite reenvía al gateway. **El gateway no
tiene configurado CORS**, así que probarlo desde otro origen falla.

---

## 8. API expuesta

Todo pasa por `http://localhost:8080`. Superficie de **4 endpoints**:

| Método | Ruta | Backend | Qué hace |
| --- | --- | --- | --- |
| `POST` | `/patients/` | :8000 | Crea el paciente. **Ignora cualquier `id` del body** — la identidad viene del header |
| `GET` | `/patients/me` | :8000 | Devuelve el propio paciente (404 si aún no existe) |
| `POST` | `/appointments` | :8081 | Crea la cita (`status` siempre `PENDING`), guarda en Mongo y publica a Kafka |
| `GET` | `/appointments` | :8081 | Lista las citas **del propio paciente** |

### Contrato de identidad (lo único que comparten entre servicios)

```
Cliente →  Authorization: Bearer <JWT Cognito>
Gateway  →  valida JWT  →  inyecta  X-Patient-Id: <sub>
Backend  →  lee solo X-Patient-Id
```

- Los backends **nunca** leen identidad de la URL ni del body.
- `GET /patients/{id}` **no existe** — era un IDOR y se reemplazó por
  `/patients/me`. No reintroducir lookup de identidad por path.
- `request.mutate().header(...)` **reemplaza** cualquier `X-Patient-Id` que
  mande el cliente, así que el patrón de confianza es correcto.

### Ejemplos

```bash
# sin token → 401 (lo resuelve Spring Security, antes de enrutar)
curl -i localhost:8080/appointments
curl -i localhost:8080/patients/me

# directo al microservicio → le falta X-Patient-Id
curl -i localhost:8081/appointments     # 400
curl -i localhost:8000/patients/me      # 422

# con token → el gateway enruta y loguea el destino
curl -X POST localhost:8080/appointments -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"appointmentDate":"2026-10-01T10:00:00"}'

curl localhost:8080/appointments -H "Authorization: Bearer $TOKEN"
```

Formato de fecha: **`yyyy-MM-dd'T'HH:mm:ss`**. El body solo necesita
`appointmentDate`.

---

## 9. Tests y verificación

```bash
cd api-gateway          ./mvnw test     # o: ./mvnw test -Dtest=ClassName
cd appointments-service ./mvnw test
cd notifications-service ./mvnw test
```

- Solo hay **3 tests**: un `contextLoads()` por servicio Java. No hay
  Testcontainers, ni infra embebida, ni tests de Python.
- `./mvnw -o` (offline) **falla en `test`** porque surefire nunca se bajó a
  `~/.m2`; `compile` offline sí funciona.
- Paciencia en la primera corrida: bajan dependencias.

---

## 10. Decisiones de diseño

### Enrutamiento: declarativo vs programático

Esta maqueta usa **declarativo** (YAML). En `ApiGatewayApplication.java` dejé el
enfoque **programático comentado**, con la comparación:

| | Declarativo (YAML) | Programático (`@Bean RouteLocator`) |
| --- | --- | --- |
| Cambios | editar config, **sin recompilar** | requiere compilar y desplegar |
| Revisión | lo lee ops/infra | lo lee dev |
| Expresividad | lo que soportan los predicados | cualquier lógica en runtime |
| Ideal para | muchos microservicios: añadir servicio = añadir bloque | ruteo condicional (feature flags, % de tráfico) |

Para descomentar hay que quitar la clave `routes:` del YAML: **no dejes los dos
activos** o crearían rutas duplicadas.

> Verificado: el bloque compila y registra `appointments-service → :8081` y
> `patients-service → :8000`.

### Orden de los filtros del gateway

| Orden | Filtro | Qué hace |
| ---: | --- | --- |
| **-101** | `RequestLoggingFilter` | `WebFilter` — loguea método, ruta, status, destino y duración. Va **antes que Security** para registrar también los 401 (los rechazos nunca llegan a la cadena de filtros del gateway) |
| **-100** | Spring Security | Valida el JWT. `anyExchange().authenticated()` |
| *(routing)* | `RoutePredicateHandlerMapping` | Elige la ruta → `GATEWAY_ROUTE_ATTR` |
| **0** | `PatientIdHeaderFilter` | Extrae `sub` del `SecurityContext` y lo inyecta como `X-Patient-Id` |

El orden importa: `PatientIdHeaderFilter` debe correr **antes** que
`RouteToRequestUrlFilter` / `NettyRoutingFilter` (10000 / `LOWEST_PRECEDENCE`),
si no la cabecera llega tarde al cable.

### Cadena de eventos

`POST /appointments` → `AppointmentUseCase` fuerza `status = "PENDING"` → guarda
en Mongo → publica en **`appointment-created-topic`** →
`notifications-service` lo consume (`groupId: notification-group`) y solo hace
`System.out`. Sin persistencia, sin HTTP.

⚠️ El nombre del tema está hardcodeado **en dos lugares**:
`KafkaEventPublisherAdapter.TOPIC` y el `@KafkaListener(topics = ...)`. Cambiás
uno y los eventos desaparecen sin error.

### Qué NO está implementado

- **Agregación** de respuestas de varios servicios.
- **Rate limiting** (SCG 5.0.3 trae `Bucket4jRateLimiter` nativo, sin configurar).
- **Monitoreo de tráfico** más allá del log por request.
- **CORS** en el gateway.

---



