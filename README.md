# MesaTech Cloud · EP2 (DSY1107)

Evolución del proyecto de la EP1 (ejemplo del profesor) a la arquitectura de la EP2: dos microservicios
en EC2 distintas, cada uno con su propio RDS, integrados por RabbitMQ.

```
React ─► API Gateway ─► BFF (EC2 A :8080) ─► ms-catalogo (EC2 A :8082) ─► rds-catalogo
                                   │                 │ publica CATEGORIA_CREADA
                                   │                 ▼
                                   │      RabbitMQ (EC2 C :5672) vhost mesatech
                                   │      catalogo.exchange ─[categoria.creada]─► solicitudes.catalogo.queue
                                   │                                                   │ consume
                                   └──────────────► ms-solicitudes (EC2 B :8081) ◄─────┘ ─► rds-solicitudes
```

## Versiones

| Componente | Versión |
| --- | --- |
| Java | 17 (imágenes `maven:3.9.9-eclipse-temurin-17` y `eclipse-temurin:17-jre-alpine`) |
| Spring Boot | 4.1.1 (Spring AMQP 4, Jackson 3) |
| RabbitMQ | `rabbitmq:4.1-management` (EC2 C) |
| Docker / Compose | 29.9.3 / v5.7.0 (documentar con `docker --version` en cada EC2) |
| MySQL | Amazon RDS for MySQL 8.x, puerto 3306 |
| React | 19 + MSAL (`@azure/msal-react`) |

## Estructura

```
ec2-a/            → se copia a EC2 A
  compose.yaml    bff + ms-catalogo
  bff/            BFF (Spring Security, valida JWT y roles, no accede a BD)
  ms-catalogo/    publicador
  bff.env.example, catalogo.env.example
ec2-b/            → se copia a EC2 B
  compose.yaml    ms-solicitudes
  ms-solicitudes/ consumidor
  solicitudes.env.example
frontend/         React (se ejecuta en tu PC + túnel Cloudflare, como en la EP1)
sql/              scripts de usuario y tablas para cada RDS
```

## Cambios respecto de la EP1

| Antes (EP1) | Ahora (EP2) |
| --- | --- |
| Un `docker-compose.yml` con MySQL, los 3 servicios y React en una sola máquina | `compose.yaml` en EC2 A (BFF + ms-catalogo) y en EC2 B (ms-solicitudes) |
| MySQL en contenedor, una base compartida, usuario `root` | Dos RDS exclusivos, usuarios de aplicación propios |
| Credenciales en `application.properties` | Variables de entorno desde archivos `.env` fuera del repositorio |
| Solicitudes leía las categorías del catálogo | ms-solicitudes guarda una copia (`categoria_local`) recibida por RabbitMQ |
| BFF solo verificaba autenticación | BFF exige scope `access_as_user` y roles (`ROLE_ADMINISTRADOR`, `ROLE_OPERADOR`) |

## Contrato del evento

Exchange `catalogo.exchange` (direct, durable) · routing key `categoria.creada` · cola
`solicitudes.catalogo.queue` · mensaje persistente (`delivery_mode=2`) · `message_id = eventId`.

```json
{
  "eventId": "6f1c2b0e-3d0a-4c35-9a52-0b7b1f8f6a11",
  "tipoEvento": "CATEGORIA_CREADA",
  "fechaEvento": "2026-11-10T14:32:05.123Z",
  "categoriaId": 12,
  "nombre": "Redes"
}
```

| Campo | Tipo | Detalle |
| --- | --- | --- |
| eventId | String (UUID v4) | Único por publicación; también es `message_id` y `CorrelationData` |
| tipoEvento | String | Constante `CATEGORIA_CREADA` |
| fechaEvento | String ISO-8601 UTC | `Instant.now().toString()` |
| categoriaId | Long | Id original en rds-catalogo; se guarda en `categoria_local.categoria_id` (UNIQUE) |
| nombre | String | Nombre de la categoría |

Solo los ítems de tipo `CATEGORIA` publican evento; las prioridades se guardan sin publicar.

## Endpoints

| Servicio | Método y ruta | Uso | Rol requerido (BFF) |
| --- | --- | --- | --- |
| BFF | `GET /api/catalogo` | Catálogo completo | autenticado |
| BFF | `POST /api/catalogo` | Crear ítem → ms-catalogo guarda y publica | ROLE_ADMINISTRADOR |
| BFF | `GET /api/categorias-disponibles` | Categorías ya copiadas en Solicitudes | autenticado |
| BFF | `POST /api/solicitudes` | Crear solicitud (valida categoría local) | autenticado |
| BFF | `GET /api/solicitudes/mias` | Solicitudes del usuario | autenticado |
| BFF | `GET /api/solicitudes` | Todas | ROLE_OPERADOR / ROLE_ADMINISTRADOR |
| BFF | `PUT /api/solicitudes/{id}/estado` | Cambiar estado (regla EN_PROCESO → RESUELTA) | ROLE_OPERADOR / ROLE_ADMINISTRADOR |
| ms-catalogo | `POST /v1/catalogo` | 201 con `id` y `eventId`; 503 controlado si RabbitMQ no confirma | interno |
| ms-catalogo | `GET /v1/catalogo/v2/catalogo` | Versión v2 (catálogo con metadatos) | interno |
| ms-solicitudes | `GET /v1/categorias-locales` | Copia local de categorías | interno |

## Despliegue

### 1. RDS (una vez)

Desde EC2 A: `mysql -h <endpoint-rds-catalogo> -u admin -p < sql/01_rds_catalogo.sql`
Desde EC2 B: `mysql -h <endpoint-rds-solicitudes> -u admin -p < sql/02_rds_solicitudes.sql`
(cambia las contraseñas de los scripts antes; no las subas).

### 2. Docker en EC2 A y B

Mismos comandos del repositorio oficial usados en EC2 C (ver guía, Fase 4), más
`sudo usermod -aG docker ubuntu` y reconectar.

### 3. Copiar carpetas (desde tu PC)

```powershell
scp -i .\mesatech-ep2.pem -r .\ec2-a ubuntu@<IP-publica-A>:~
scp -i .\mesatech-ep2.pem -r .\ec2-b ubuntu@<IP-publica-B>:~
```

### 4. Variables y arranque

EC2 A:
```bash
cd ~/ec2-a
cp catalogo.env.example catalogo.env && nano catalogo.env
cp bff.env.example bff.env && nano bff.env
chmod 600 *.env
docker compose config --quiet && echo "compose valido"
docker compose up -d --build
docker compose ps
docker compose logs -f
```

EC2 B: lo mismo en `~/ec2-b` con `solicitudes.env`.

La primera compilación descarga dependencias de Maven y tarda unos minutos.

### 5. Security groups

| Destino | Puerto | Origen |
| --- | --- | --- |
| EC2 A (BFF) | 8080 | 0.0.0.0/0 (API Gateway no tiene IP fija; el BFF revalida el JWT) |
| EC2 B (ms-solicitudes) | 8081 | `sg_ec2_a` |
| EC2 C (RabbitMQ) | 5672 | `sg_ec2_a`, `sg_ec2_b` |
| EC2 C (RabbitMQ) | 15672 | IP administrativa /32 |
| rds-catalogo | 3306 | `sg_ec2_a` |
| rds-solicitudes | 3306 | `sg_ec2_b` |

ms-catalogo (8082) solo se publica en `127.0.0.1` de EC2 A; el BFF lo llama por la red de Docker.

### 6. Frontend

```bash
cd frontend
cp .env.example .env    # REACT_APP_BFF_URL = invoke URL de API Gateway + /api
npm install
npm start
```

Exponer con `cloudflared tunnel --url http://localhost:3000` y agregar esa URL como Redirect URI (SPA)
en Entra ID y en `CORS_ALLOWED_ORIGINS` del BFF.

## Orden de arranque y recuperación

1. RDS (Available) → 2. RabbitMQ en EC2 C → 3. ms-solicitudes (EC2 B) → 4. ms-catalogo y BFF (EC2 A)
→ 5. API Gateway → 6. React.
Detención en orden inverso. En EC2 C, `docker compose down` **sin `-v`** para conservar el volumen.

## Comandos para las pruebas

| Prueba | Comando |
| --- | --- |
| P3 | EC2 B: `docker compose stop ms-solicitudes` |
| P4 | EC2 B: `docker compose start ms-solicitudes` y `docker compose logs -f ms-solicitudes` |
| P5 | EC2 C: `docker compose down && docker compose up -d` |
| P6 | EC2 A y B: `docker compose restart` |
| P7 | EC2 C: `docker compose stop rabbitmq` → crear categoría → 503 `EVENTO_NO_PUBLICADO` → `docker compose start rabbitmq` |

Logs que permiten seguir un evento:
`[RDS CATALOGO] Item guardado id=…` → `[PUBLICADOR] Evento confirmado por RabbitMQ eventId=…`
→ `[CONSUMIDOR] Mensaje recibido eventId=…` → `[RDS SOLICITUDES] Copia guardada … eventId=…`.

## Publicación, confirmaciones y fallos

- La confirmación del publicador (publisher confirm) indica que RabbitMQ aceptó el mensaje; no que
  ms-solicitudes lo guardó. El ack del consumidor (modo AUTO) se envía recién cuando el listener
  termina de guardar en rds-solicitudes; si falla, el mensaje vuelve a la cola.
- `mandatory=true` + returns: si el exchange no puede enrutar a ninguna cola, se trata como fallo.
- Guardar en rds-catalogo y publicar son operaciones separadas: si RabbitMQ no está, la categoría queda
  guardada y la API responde 503. Sin patrón outbox no hay reenvío automático; reintentar la petición
  crearía otra categoría. La restricción UNIQUE en `categoria_local.categoria_id` evita copias duplicadas
  ante una reentrega. No se garantiza procesamiento exactamente una vez.
