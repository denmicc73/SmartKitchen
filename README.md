# Smart Kitchen

Gestion inteligente de alimentos de casa: inventario, caducidades, lista de la compra y sugerencias de recetas.

## Stack

- Backend + frontend: Spring Boot 3 + Thymeleaf
- Seguridad: Spring Security con login por sesion
- Base de datos: H2 en desarrollo y MySQL en produccion
- IA opcional: `ANTHROPIC_API_KEY`

## Desarrollo Local

Requisitos: Java 17 y Maven.

```bash
mvn spring-boot:run
```

Por defecto usa el perfil `dev` con H2 en `./data/smartkitchen.mv.db`.
La app queda en `http://localhost:8080`.

Credenciales por defecto en desarrollo:

- Usuario: `admin`
- Contrasena: `changeme123`

## Produccion Con Docker Y MySQL

1. Genera un `.env` con contrasenas seguras:

   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts/generate-prod-env.ps1
   ```

   El script imprime la contrasena inicial del admin una sola vez. Guardala.

2. Levanta la app y MySQL:

   ```bash
   docker compose up -d --build
   ```

3. Accede a:

   ```text
   http://localhost:8080
   ```

   O desde otro dispositivo de tu red:

   ```text
   http://IP_DEL_SERVIDOR:8080
   ```

MySQL queda dentro de la red interna de Docker. No se publica el puerto `3306`.

## Despliegue Con Coolify (VPS + MySQL gestionado)

[Coolify](https://coolify.io) despliega la app desde el `Dockerfile` del repo,
inyecta las variables de entorno y pone HTTPS con su proxy (Traefik). La base
de datos es un recurso aparte de Coolify, **no** el contenedor `mysql` del
`docker-compose.yml` (ese solo se usa en el despliegue manual con Compose).

1. **Crear la base de datos.** En el proyecto/entorno de Coolify:
   *New Resource → Database → MySQL* (8.x). Al crearla, en su pagina apunta:
   `Host` interno, `Port` (3306), `Database`, `Username`, `Password`.

2. **Crear la aplicacion.** *New Resource → Application →* este repositorio,
   *Build Pack:* **Dockerfile**. En *Ports Exposes* pon `8080`.

3. **Variables de entorno** de la aplicacion (pestana *Environment Variables*):

   | Variable | Valor |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DB_HOST` | host interno de la base de datos de Coolify |
   | `DB_PORT` | `3306` |
   | `DB_NAME` | nombre de la base de datos |
   | `DB_USER` | usuario de la base de datos |
   | `DB_PASS` | contrasena de la base de datos |
   | `ADMIN_USERNAME` | `admin` |
   | `ADMIN_PASSWORD` | contrasena de 16+ caracteres (con esta entras) |
   | `ADMIN_EMAIL` | tu correo (para "He olvidado mi contrasena") |
   | `BASE_URL` | la URL publica que Coolify asigna a la app (con `https://`) |

   Correo (opcional): `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS` y
   `MAIL_FROM`; o bien `BREVO_API_KEY` + `MAIL_FROM` si el VPS bloquea el SMTP.
   IA (opcional): `ANTHROPIC_API_KEY`.

4. **Desplegar.** Coolify construye el `Dockerfile` y arranca. La app crea el
   esquema sola (`ddl-auto=update`) y el usuario `admin` en el primer arranque
   (`Usuario admin inicial creado: admin` en los logs). Entra en la URL con
   `admin` y `ADMIN_PASSWORD`.

5. **Persistencia de imagenes.** Anade un *Persistent Storage* (volumen) a la
   aplicacion montado en `/app/data/uploads` para que las fotos subidas
   sobrevivan a los redepliegues.

Actualizar: haz push a la rama; con *Auto Deploy* activado Coolify redespliega
solo. Los datos viven en el recurso MySQL y en el volumen de `/app/data/uploads`.

## Despliegue En Un VPS (por IP, sin dominio)

Requisitos: un VPS con Ubuntu/Debian y acceso SSH. Con 1 GB de RAM conviene
anadir memoria de intercambio para que la compilacion no se quede sin memoria:

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

1. Instala Docker:

   ```bash
   curl -fsSL https://get.docker.com | sudo sh
   sudo usermod -aG docker "$USER"    # cierra y reabre la sesion SSH despues
   ```

2. Clona el repositorio y entra en la carpeta:

   ```bash
   git clone <URL_DEL_REPO> smart-kitchen && cd smart-kitchen
   ```

3. Genera el `.env` pasando la IP publica del VPS (para los enlaces de correo):

   ```bash
   ./scripts/generate-prod-env.sh 203.0.113.10
   ```

   Apunta la contrasena del admin que imprime: solo se muestra una vez.
   Si vas a usar correo, edita `.env` y rellena `SMTP_*` o `BREVO_API_KEY`
   mas `MAIL_FROM` y `ADMIN_EMAIL`.

4. Arranca:

   ```bash
   docker compose up -d --build
   ```

5. Abre en el firewall del proveedor **solo** los puertos `22` (SSH) y `8080`.
   El `3306` de MySQL no se publica: vive en la red interna de Docker.

6. Accede desde el navegador a `http://IP_DEL_VPS:8080` e inicia sesion con
   `admin` y la contrasena generada.

Los datos (MySQL e imagenes subidas) viven en volumenes Docker (`mysql_data`,
`uploads_data`) y sobreviven a `docker compose up --build` y a los reinicios
del VPS (`restart: unless-stopped`). Para actualizar la app: `git pull &&
docker compose up -d --build`.

Copia de seguridad manual de la base de datos:

```bash
docker compose exec -T mysql \
  mysqldump -u root -p"$DB_ROOT_PASS" smartkitchen > backup-$(date +%F).sql
```

El escaner de productos funciona por HTTP porque usa la camara del movil a
traves de un campo de archivo, no `getUserMedia`. Si mas adelante quieres
HTTPS (y un dominio en vez de la IP), se puede anadir un proxy inverso Caddy
al `docker-compose.yml` con certificado automatico.

## Variables De Produccion

La **app** (perfil `prod`) necesita, o no arranca correctamente:

- `DB_USER`, `DB_PASS` — credenciales de MySQL
- `ADMIN_USERNAME`, `ADMIN_PASSWORD` — usuario admin inicial
- `DB_HOST`, `DB_PORT`, `DB_NAME` — opcionales (por defecto `mysql`, `3306`, `smartkitchen`)

`ADMIN_PASSWORD` debe tener al menos 16 caracteres en produccion.

El `docker-compose.yml` (despliegue manual con su propio contenedor MySQL)
usa **ademas** `DB_ROOT_PASS` y `DB_NAME` para crear la base de datos. Con una
base de datos gestionada (p. ej. la de Coolify) esas dos no hacen falta.

## Seguridad

- Crea el `.env` con `scripts/generate-prod-env.sh` (Linux) o `.ps1` (Windows).
- No subas `.env` a git.
- Guarda `ADMIN_PASSWORD` en un gestor de contrasenas.
- No abras el puerto `3306` de MySQL.
- Para acceder desde fuera de casa, usa VPN o Tailscale en vez de abrir `8080` al router.

## Estructura

```text
src/main/java/com/smartkitchen/
  config/       Seguridad
  controller/   Controladores MVC
  model/        Entidades JPA
  repository/   Repositorios Spring Data
  service/      Logica de negocio

src/main/resources/
  templates/        Vistas Thymeleaf
  static/css/       Estilos
  application.yml   Configuracion dev/prod
```
