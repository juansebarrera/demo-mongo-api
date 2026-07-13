# demo-mongo-api

API REST con Spring Boot + MongoDB + seguridad JWT (login, registro y refresh tokens), documentada con Swagger/OpenAPI y dockerizada con Docker Compose.

## Stack

- Spring Boot **v4.1.0** (Spring Framework 7.x)
- MongoDB
- JWT (JJWT 0.12.6)
- Swagger / OpenAPI
- Lombok
- Tests con Mongo embebido (Flapdoodle)

## Prerrequisitos

- [Docker](https://www.docker.com/) y Docker Compose
- [Git](https://git-scm.com/)
- JDK 21 (solo si vas a correr `mvn test` o compilar fuera de Docker)

## Puesta en marcha en un equipo nuevo

1. **Clonar el repositorio**

   ```bash
   git clone <url-de-tu-repo>
   cd demo-mongo-api
   ```

2. **Cambiar a la rama que necesites**

   ```bash
   git checkout main
   git pull
   ```

3. **Crear el archivo `.env`** (no viaja con el repo, está en `.gitignore`)

   Crea un archivo `.env` en la raíz del proyecto con:

   ```env
   JWT_SECRET=<tu-clave-secreta>
   ```

   > Si generas un `JWT_SECRET` nuevo en vez de reutilizar el anterior, no rompe nada: solo invalida los tokens JWT ya emitidos previamente.

4. **Levantar la aplicación dockerizada**

   ```bash
   docker compose up --build
   ```

   Esto levanta los servicios `mongo` (con healthcheck) y `app`, esta última esperando a que Mongo esté saludable antes de arrancar.

5. **Verificar que todo funciona**

   - Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
     Usa el botón **Authorize** con un token JWT obtenido desde `/api/auth/login` o `/api/auth/registro`.
   - Colección de Postman: `demo-mongo-api.postman_collection.json` (carpetas Auth, Clientes, Productos), apuntando a `http://localhost:8080`.

## Comandos útiles

```bash
# Levantar todo con Docker
docker compose up --build

# Bajar todo (⚠️ -v borra los datos de Mongo)
docker compose down -v

# Ver logs en vivo
docker compose logs -f app

# Correr tests localmente (usa Mongo embebido, no requiere Docker levantado)
mvn test

# Ver qué versión de flapdoodle resuelve Maven
mvn dependency:tree | findstr flapdoodle
```

## ⚠️ Detalle importante de Spring Boot 4.x

Spring Boot 4.x renombró varias propiedades relacionadas con Mongo:

| Propiedad antigua (Boot 3.x)        | Propiedad nueva (Boot 4.x)   |
|-------------------------------------|-------------------------------|
| `spring.data.mongodb.uri`           | `spring.mongodb.uri`          |
| Variable de entorno `MONGODB_URI`   | Variable de entorno `SPRING_MONGODB_URI` |

Además, ya no trae autoconfiguración propia para Mongo embebido: depende del paquete de la comunidad `de.flapdoodle.embed.mongo.spring4x`, cuya propiedad es `de.flapdoodle.mongodb.embedded.version` (no `spring.mongodb.embedded.version`).

**Ante cualquier comportamiento raro de propiedades de Spring en este proyecto, sospecha primero de un renombre de propiedad en Boot 4.x antes que de un error de configuración de Docker/red.**

## Tests

Los tests corren contra un Mongo embebido (Flapdoodle), por lo que **no requieren Docker levantado ni conexión a Mongo real**:

```bash
mvn test
```

Las credenciales dummy usadas en tests (`jwt.secret`, etc.) están en `src/test/resources/application.properties`.

## CI/CD

El repo incluye un workflow de GitHub Actions (`.github/workflows/ci.yml`) que corre automáticamente en cada `push` y `pull request` hacia `main`:

1. Compila el proyecto (`mvn compile`)
2. Ejecuta los tests (`mvn test`)
3. Publica el reporte de Surefire como artefacto
4. Empaqueta el JAR (`mvn package -DskipTests`)

## GUI — Single Page Application

Desde la rama `feature/gui-usuario` se agregó una interfaz web vanilla (HTML + CSS + JS) servida desde `src/main/resources/static/`, sin herramientas de build ni frameworks.

### Archivos

```
src/main/resources/static/
├── index.html          # Página de login/registro
├── app.html            # Dashboard (requiere autenticación)
├── css/
│   └── style.css       # Estilos responsivos
└── js/
    ├── api.js          # Cliente HTTP con manejo de JWT (localStorage)
    ├── auth.js         # Lógica de login y registro
    ├── productos.js    # CRUD de productos con modales
    └── clientes.js     # CRUD de clientes con modales
```

### Flujo

1. El usuario accede a `/index.html` y se loguea o registra.
2. `auth.js` llama a `POST /api/auth/login` y guarda el `accessToken` y `refreshToken` en `localStorage`.
3. Se redirige a `/app.html`, que carga `api.js` → `productos.js` → `clientes.js`.
4. Cada request desde `api.js` envía `Authorization: Bearer <token>` automáticamente.

### Seguridad (backend)

- `SecurityConfig` permite acceso libre a estáticos (`/`, `/index.html`, `/app.html`, `/css/**`, `/js/**`, `/favicon.ico`), a `/api/auth/**` y a Swagger.
- Cualquier otro endpoint requiere autenticación.
- `DELETE` en `/api/productos/{id}` y `/api/clientes/{id}` requiere rol `ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`).
- Errores 401 y 403 se responden en JSON desde un `AuthenticationEntryPoint` y `AccessDeniedHandler` custom.

### Bug resuelto: 401 después de login

**Problema:** después de login exitoso y redirección a `app.html`, las llamadas a `/api/productos` y `/api/clientes` retornaban **401 "Token invalido o ausente"**.

**Causa raíz:** desajuste de nombres de campo entre backend y frontend.

El record `AuthResponse` serializaba el access token con el campo `"token"`:

```java
// AuthResponse.java (ANTES)
public record AuthResponse(String token, String refreshToken) {}
// JSON: {"token":"eyJhbG...", "refreshToken":"..."}
```

Pero el frontend leía `data.accessToken`, que era `undefined`:

```javascript
// auth.js — guardaba undefined
const data = await API.post('/auth/login', { username, password });
API.saveTokens(data.accessToken, data.refreshToken); // data.accessToken === undefined
```

Resultado: cada request enviaba `Bearer undefined` → JWT inválido → 401.

**Fix:** `AuthResponse.java` renombrado de `token` a `accessToken`:

```java
// AuthResponse.java (DESPUÉS)
public record AuthResponse(String accessToken, String refreshToken) {}
// JSON: {"accessToken":"eyJhbG...", "refreshToken":"..."}
```

**Lección:** cuando el backend y frontend están en el mismo repo, verificar que los nombres de campo en DTOs/records coinciden exactamente con lo que el frontend espera leer. Los records de Java serializan por defecto con el nombre del campo, no con `@JsonProperty`.

## Pendientes

_No hay pendientes abiertos._