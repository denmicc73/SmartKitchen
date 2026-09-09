# Despliegue en producción (Docker + Coolify)

Smart Kitchen es una sola imagen Docker (Spring Boot + Thymeleaf). La base de
datos MySQL es un servicio aparte (contenedor de Coolify o el `mysql` del
`docker-compose.yml`). Este documento lista **todas** las variables de entorno
de producción.

## Imagen

- `Dockerfile` multi-stage: compila con `maven:3.9-eclipse-temurin-21`
  (`mvn clean package -DskipTests`) y la imagen final `eclipse-temurin:21-jre-alpine`
  solo lleva el `.jar`.
- **Puerto real: `8080`** (servidor embebido, `server.port` por defecto). No hay
  nada que lo cambie, así que en Coolify → *Ports Exposes* = `8080`.
- El build necesita RAM: en un VPS de 1 GB añade swap (ver `README.md`).
- No copia secretos: solo entran `pom.xml` y `src/`. `.dockerignore` excluye
  `.env*`, `smtp.properties`, `ia.properties`, `target/`, `.git/`.

## Perfil de Spring — OBLIGATORIO

| Variable | Valor | Por qué |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Sin esto la app arranca en perfil `dev` con **H2 dentro del contenedor** y pierde todos los datos en cada redeploy. `prod` usa MySQL. |

## Base de datos (MySQL) — el perfil `prod` no arranca sin `DB_USER` y `DB_PASS`

| Variable | Default | Notas |
|---|---|---|
| `DB_HOST` | `mysql` | Host del MySQL. En Coolify, el hostname interno del recurso de base de datos. La app y la base de datos deben estar en el mismo *Project/Environment* o el hostname no resuelve. |
| `DB_PORT` | `3306` | |
| `DB_NAME` | `smartkitchen` | Nombre de la base de datos (en Coolify suele ser `default`). |
| `DB_USER` | — (obligatoria) | |
| `DB_PASS` | — (obligatoria) | |
| `DB_USE_SSL` | `false` | Ponlo a `true` solo si conectas a un MySQL remoto con TLS. |
| `DB_ALLOW_PUBLIC_KEY_RETRIEVAL` | `true` | Necesario con MySQL 8 y `caching_sha2_password` sobre conexión sin TLS. |

URL resultante:
`jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?useSSL=$DB_USE_SSL&serverTimezone=UTC&allowPublicKeyRetrieval=$DB_ALLOW_PUBLIC_KEY_RETRIEVAL`

El esquema se crea solo (`spring.jpa.hibernate.ddl-auto=update`).

## Usuario administrador — sin default en `prod`, obligatorias

| Variable | Notas |
|---|---|
| `ADMIN_USERNAME` | p. ej. `admin`. |
| `ADMIN_PASSWORD` | **Mínimo 16 caracteres** o el perfil `prod` aborta el arranque a propósito. Se usa solo la primera vez, para crear la cuenta admin en una base de datos vacía. |
| `ADMIN_EMAIL` | Correo del admin. Necesario para poder usar "He olvidado mi contraseña" con la cuenta admin. |

## URL pública

| Variable | Default | Notas |
|---|---|---|
| `BASE_URL` | `http://localhost:8080` | La URL pública real (p. ej. `https://smartkitchen.midominio.com` o la que asigna Coolify). Se usa para construir los enlaces de los correos de confirmación y de reseteo. Si está mal, los enlaces de los correos no funcionan. |

## Correo (confirmación de cuenta y reseteo de contraseña)

Sin correo configurado, las altas por registro público quedan bloqueadas
(`activo=false`) hasta que el admin las activa desde *Ajustes → Usuarios*.

Orden de preferencia del envío: **API Brevo → SMTP → log**.

### Opción A — SMTP (Gmail por defecto)

| Variable | Default | Notas |
|---|---|---|
| `SMTP_USER` | vacío | Tu dirección de Gmail. |
| `SMTP_PASS` | vacío | **Contraseña de aplicación** de Google (16 caracteres). Quita los espacios al pegarla. |
| `SMTP_HOST` | `smtp.gmail.com` | |
| `SMTP_PORT` | `587` | |
| `SMTP_AUTH` | `true` | Avanzada; no tocar salvo servidor raro. |
| `SMTP_STARTTLS` | `true` | Avanzada; no tocar salvo servidor raro. |
| `MAIL_FROM` | usa `SMTP_USER` | Remitente. Con Gmail debe ser tu propia dirección o un alias verificado. |

### Opción B — API HTTP de Brevo (si el proveedor bloquea el SMTP saliente)

| Variable | Notas |
|---|---|
| `BREVO_API_KEY` | Clave de brevo.com (300 correos/día gratis). Si está puesta, tiene prioridad sobre SMTP. |
| `MAIL_FROM` | Debe ser un remitente **verificado** en la cuenta de Brevo. |

## Imágenes subidas (escáner, fotos de recetas)

| Variable | Default | Notas |
|---|---|---|
| `UPLOADS_DIR` | `~/.smart-kitchen/uploads` | En Docker se pone a `/app/data/uploads` (ya lo hace `docker-compose.yml`). En Coolify, monta ahí un *Persistent Storage* o las fotos se borran en cada redeploy. |

## IA (Anthropic) — opcional

| Variable | Default | Notas |
|---|---|---|
| `ANTHROPIC_API_KEY` | vacío | De console.anthropic.com. Sin ella, el escáner con IA y las sugerencias de recetas se desactivan; el resto de la app funciona igual. |
| `ANTHROPIC_MODEL` | `claude-sonnet-4-6` | Verifica el identificador contra los modelos vigentes de Anthropic antes de confiar en esta función. |

## Puerto del servidor

| Variable | Default | Notas |
|---|---|---|
| `SERVER_PORT` | `8080` | **No hace falta tocarla.** Deja 8080 y pon `8080` en *Ports Exposes* de Coolify. |

## Comprobación tras el deploy (logs)

1. `The following 1 profile is active: "prod"`
2. `HikariPool-1 - Start completed` (conectó a MySQL). Si sale `UnknownHostException`
   o `Communications link failure`, la app no ve la base de datos: revisa `DB_HOST`
   y que ambos recursos estén en el mismo *Project/Environment*.
3. `Envio de correo ACTIVADO — ...` (o `DESACTIVADO` si no configuraste correo).
4. `Usuario admin inicial creado: admin` (solo la primera vez, con la base vacía).

Luego entra en `BASE_URL` con `ADMIN_USERNAME` / `ADMIN_PASSWORD`.

## Dónde NO poner los secretos

- **Nunca** en `smtp.properties.example` ni en `ia.properties.example`: esos
  ficheros se versionan (son plantillas). Los reales van en variables de entorno
  (producción) o en `smtp.properties` / `ia.properties` sin `.example`, que están
  en `.gitignore` (desarrollo local).
- `.env` está en `.gitignore`; no lo subas.
