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

## Variables De Produccion

El perfil `prod` exige estas variables. Si falta alguna, la app no debe arrancar correctamente:

- `DB_ROOT_PASS`
- `DB_NAME`
- `DB_USER`
- `DB_PASS`
- `ADMIN_USERNAME`
- `ADMIN_PASSWORD`

`ADMIN_PASSWORD` debe tener al menos 16 caracteres en produccion.

## Seguridad

- Crea el `.env` con `scripts/generate-prod-env.ps1`.
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
