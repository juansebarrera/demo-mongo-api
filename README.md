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

   Usa el template como base:

   ```bash
   cp .env.example .env
   ```

   Edita `.env` con tus valores. Al mínimo necesitas:

   ```env
   JWT_SECRET=<tu-clave-secreta>
   ADMIN_SEED_PASSWORD=<tu-password-admin>
   ```

   > Si generas un `JWT_SECRET` nuevo en vez de reutilizar el anterior, no rompe nada: solo invalida los tokens JWT ya emitidos previamente.

4. **Levantar la aplicación dockerizada**

   La imagen pre-compilada se descarga automáticamente desde GHCR (no necesita compilar):

   ```bash
   docker compose pull
   docker compose up
   ```

   Si preferís compilar localmente (por ejemplo, durante desarrollo):

   ```bash
   docker compose up --build
   ```

   En ambos casos se levantan los servicios `mongo` (con healthcheck) y `app`, esta última esperando a que Mongo esté saludable antes de arrancar.

5. **Verificar que todo funciona**

   - Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
     Usa el botón **Authorize** con un token JWT obtenido desde `/api/auth/login` o `/api/auth/registro`.
   - Colección de Postman: `demo-mongo-api.postman_collection.json` (carpetas Auth, Clientes, Productos), apuntando a `http://localhost:8080`.

## Comandos útiles

```bash
# Levantar todo con Docker (imagen pre-compilada desde GHCR)
docker compose pull
docker compose up

# Levantar compilando localmente
docker compose up --build

# Bajar todo (⚠️ -v borra los datos de Mongo)
docker compose down -v

# Ver logs en vivo
docker compose logs -f app

# Correr tests localmente (usa Mongo embebido, no requiere Docker levantado)
mvn test

# Ver qué versión de flapdoodle resuelve Maven
mvn dependency:tree | findstr flapdoodle

# Exportar dump de MongoDB
.\scripts\dump-mongo.ps1

# Importar dump de MongoDB
.\scripts\restore-mongo.ps1
```

## Paginación y búsqueda

`GET /api/productos` y `GET /api/clientes` soportan paginación, ordenamiento y búsqueda.

### Parámetros query

| Parámetro | Default | Descripción |
|-----------|---------|-------------|
| `page` | `0` | Número de página (0-indexed) |
| `size` | `10` | Elementos por página |
| `sort` | `nombre` | Campo por el cual ordenar |
| `direction` | `asc` | Dirección del orden (`asc` o `desc`) |
| `search` | _(vacío)_ | Término de búsqueda (nombre para productos, nombre o email para clientes) |

### Ejemplos

```bash
# Primera página, 10 elementos, ordenados por nombre ascendente
GET /api/productos

# Página 2, 5 elementos, ordenados por precio descendente
GET /api/productos?page=1&size=5&sort=precio&direction=desc

# Buscar productos que contengan "teclado" en el nombre
GET /api/productos?search=teclado

# Buscar clientes por nombre o email
GET /api/clientes?search=juan
```

### Respuesta

```json
{
  "content": [{ "id": "...", "nombre": "...", ... }],
  "totalElements": 50,
  "totalPages": 5,
  "number": 0,
  "size": 10,
  "first": true,
  "last": false,
  "empty": false
}
```

## Carga masiva de datos

Los endpoints de carga masiva permiten insertar múltiples registros en una sola petición JSON. Los registros válidos se insertan y los inválidos se reportan con sus errores.

### Endpoints

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `POST /api/productos/cargar` | POST | Carga masiva de productos |
| `POST /api/clientes/cargar` | POST | Carga masiva de clientes |

### Request

Envía un array JSON con los registros a insertar:

```json
POST /api/productos/cargar
Content-Type: application/json

[
  { "nombre": "Teclado", "precio": 89.90, "stock": 50 },
  { "nombre": "Mouse", "precio": 25.50, "stock": 100 },
  { "nombre": "", "precio": -10, "stock": -5 }
]
```

### Respuesta — todos válidos (201)

```json
{
  "totalRecibidos": 2,
  "insertados": 2,
  "fallidos": 0,
  "ids": ["665a...", "665b..."],
  "errores": []
}
```

### Respuesta — mix válidos e inválidos (207 Multi-Status)

```json
{
  "totalRecibidos": 3,
  "insertados": 2,
  "fallidos": 1,
  "ids": ["665a...", "665b..."],
  "errores": [
    { "index": 2, "campo": "nombre", "mensaje": "El nombre es obligatorio" }
  ]
}
```

### Campos de la respuesta

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `totalRecibidos` | int | Total de registros recibidos en el request |
| `insertados` | int | Registros insertados exitosamente |
| `fallidos` | int | Registros con errores de validación |
| `ids` | List\<String\> | IDs de MongoDB generados para los registros insertados |
| `errores` | List\<BulkError\> | Detalle de errores por registro |

### Validaciones

Los mismos constraints del CRUD individual aplican:

**Productos:** `nombre` (obligatorio), `precio` (> 0), `stock` (>= 0)

**Clientes:** `nombre` (obligatorio), `email` (formato válido)

La validación se realiza registro por registro — un error no bloquea la inserción de los demás.

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

## Validación de datos

Los mensajes de validación están externalizados en `src/main/resources/messages.properties`, siguiendo la convención `{entidad}.{campo}.{validación}`.

### Modelo de Mensajes

```properties
# Producto
producto.nombre.notblank=El nombre del producto es obligatorio
producto.precio.positive=El precio del producto debe ser mayor a 0
producto.stock.min=El stock del producto no puede ser negativo

# Cliente
cliente.nombre.notblank=El nombre del cliente es obligatorio
cliente.email.notblank=El correo electrónico es obligatorio
cliente.email.email=El correo electrónico no tiene un formato válido
```

### Uso en anotaciones

```java
@NotBlank(message = "{producto.nombre.notblank}")
private String nombre;
```

### Respuesta de error estandarizada

Cuando falla la validación, todos los errores se devuelven con el mismo formato:

```json
{
  "timestamp": "2026-07-13T00:00:00",
  "status": 400,
  "error": "Bad Request",
  "mensaje": "Error de validación",
  "errores": {
    "nombre": "El nombre del producto es obligatorio",
    "precio": "El precio del producto debe ser mayor a 0"
  },
  "path": "/api/productos"
}
```

### Agregar nuevas validaciones

1. Agregar el mensaje en `messages.properties`
2. Usar la clave en la anotación: `@NotBlank(message = "{nueva.clave}")`
3. No es necesario tocar el `GlobalExceptionHandler`

## CI/CD

El repo incluye dos workflows de GitHub Actions:

### `ci.yml` — Build, test y package

Se ejecuta en cada `push` y `pull request` hacia `main`:

1. Compila el proyecto (`mvn compile`)
2. Ejecuta los tests (`mvn test`)
3. Publica el reporte de Surefire como artefacto
4. Empaqueta el JAR (`mvn package -DskipTests`)

### `docker-publish.yml` — Build y push de imagen Docker

Se ejecuta en cada `push` a `main` y manualmente (`workflow_dispatch`):

1. Build de la imagen Docker (multi-etapa: Maven → JRE Alpine)
2. Push a GitHub Container Registry (`ghcr.io/juansebarrera/demo-mongo-api`)
3. Tags: `latest` y SHA del commit

La imagen queda disponible para que otros desarrolladores la descarguen sin necesidad de compilar.

## Health Checks (Actuator)

El proyecto incluye `spring-boot-starter-actuator` para monitoreo y health checks.

### Endpoints disponibles

| Endpoint | Descripción |
|----------|-------------|
| `GET /actuator/health` | Estado general de la aplicación (`UP` / `DOWN`) |
| `GET /actuator/health/mongo` | Verificación de conexión a MongoDB |
| `GET /actuator/info` | Información de la aplicación (versión, etc.) |

### Ejemplo de respuesta

```json
{
  "status": "UP",
  "components": {
    "mongo": {
      "status": "UP",
      "details": {
        "database": "demo_mongo_api"
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

### Docker

El `docker-compose.yml` ya incluye un healthcheck para el servicio `app` que usa `/actuator/health`. Docker no enruta tráfico al contenedor hasta que el healthcheck responda `UP`.

### Seguridad

Los endpoints de actuator (`/actuator/**`) están exentos de autenticación JWT para que Docker y balanceadores de carga puedan verificar el estado sin credenciales.

## Desarrollo desde otro equipo

Si necesitás desarrollar en otra máquina, seguí estos pasos:

### 1. Autenticarse en GHCR

Necesitás un Personal Access Token (PAT) de GitHub con scope `write:packages`:

```bash
echo <tu-pat> | docker login ghcr.io -u <tu-usuario> --password-stdin
```

> Si solo vas a **usar** la imagen (no modificarla), alcanza con un PAT con scope `read:packages`.

### 2. Clonar y configurar

```bash
git clone https://github.com/juansebarrera/demo-mongo-api.git
cd demo-mongo-api
cp .env.example .env
# Edita .env con tus valores
```

### 3. Descargar imagen y datos

```bash
# Descargar la imagen pre-compilada desde GHCR
docker compose pull

# Levantar la app
docker compose up -d

# Importar datos de MongoDB (si existe el dump en el repo)
.\scripts\restore-mongo.ps1
```

### 4. Verificar

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- GUI: http://localhost:8080/index.html

### Scripts de MongoDB

En `scripts/` hay utilidades para exportar e importar datos:

| Script | Descripción |
|--------|-------------|
| `dump-mongo.ps1` / `.sh` | Exporta un dump de la DB al directorio `mongo-dump/` |
| `restore-mongo.ps1` / `.sh` | Importa un dump desde `mongo-dump/` al contenedor |

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

### Prioridad alta (producción)

- [x] Paginación y filtrado en `GET /api/productos` y `/api/clientes` (parámetros `page`, `size`, `sort`)
- [x] Búsqueda por nombre (productos) y email/nombre (clientes)
- [x] Validación robusta con mensajes personalizados desde `messages.properties`
- [x] Health checks con `spring-boot-starter-actuator`
- [x] Carga masiva de productos y clientes (`POST /api/productos/cargar`, `/api/clientes/cargar`)

### Prioridad mediana (UX)

- [ ] Dashboard con estadísticas (conteo de productos, clientes, usuarios)
- [ ] Gestión de usuarios para admin (CRUD, asignación de roles, desactivación)
- [ ] Cambio de contraseña desde la GUI
- [ ] Exportar listas a CSV

### Prioridad baja (calidad de vida)

- [ ] Dark mode con toggle y `localStorage`
- [ ] Notificaciones toast (reemplazar `alert()`)
- [ ] Tests de integración para flujos completos (login → CRUD → refresh)
- [ ] Logging estructurado con correlation IDs