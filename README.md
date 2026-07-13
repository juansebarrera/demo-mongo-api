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
   - Colección de Postman: `demo-mongo-api.postman_collection.json` (carpetas Auth, Clientes, Productos, Dashboard, Usuarios, Perfiles de Riesgo), apuntando a `http://localhost:8080`.

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

## Dashboard — Estadísticas generales

El endpoint de dashboard retorna el conteo total de productos, clientes y usuarios registrados.

### Endpoint

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `GET /api/dashboard` | GET | Estadísticas generales (requiere autenticación) |

### Respuesta

```json
{
  "totalProductos": 15,
  "totalClientes": 30,
  "totalUsuarios": 3
}
```

### Campos

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `totalProductos` | long | Total de productos en la base de datos |
| `totalClientes` | long | Total de clientes en la base de datos |
| `totalUsuarios` | long | Total de usuarios registrados |

## Gestión de usuarios (solo ADMIN)

El panel de administración permite gestionar usuarios del sistema. Solo los usuarios con rol `ROLE_ADMIN` pueden acceder.

### Endpoints

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `GET /api/usuarios` | GET | Listar todos los usuarios |
| `GET /api/usuarios/{id}` | GET | Buscar usuario por ID |
| `POST /api/usuarios` | POST | Crear un nuevo usuario |
| `PUT /api/usuarios/{id}` | PUT | Actualizar usuario (username, roles, enabled) |
| `DELETE /api/usuarios/{id}` | DELETE | Eliminar usuario |
| `PUT /api/usuarios/{id}/password` | PUT | Cambiar contraseña (solo ADMIN) |
| `PUT /api/usuarios/{id}/toggle-active` | PUT | Activar/desactivar usuario |

### Frontend

La sección **"Usuarios"** aparece en el sidebar solo para ADMIN. Permite:
- Crear usuarios con rol Usuario o Administrador
- Editar username y roles
- Cambiar contraseña de cualquier usuario
- Activar/desactivar usuarios
- Eliminar usuarios

**Nota:** después de cambiar los roles de un usuario, debe hacer logout y login para que el JWT se regenere con los nuevos permisos.

## Perfiles de Riesgo (parámetricos)

Los perfiles de riesgo son una colección paramétrica en MongoDB que permite definir y administrar categorías de riesgo asignables a los clientes. Los perfiles se crean automáticamente al iniciar la aplicación (seeder) y pueden gestionarse desde el panel de administración.

### Perfiles por defecto

Al iniciar la aplicación, el `PerfilRiesgoSeeder` crea automáticamente los siguientes registros en la colección `perfil_riesgo`:

| Nombre | Descripción |
|--------|-------------|
| `SIN_PERFIL` | Sin perfil de riesgo asignado |
| `CONSERVADOR` | Perfil conservador, prioriza la preservación del capital |
| `MODERADO` | Perfil moderado, equilibrio entre riesgo y rentabilidad |
| `ARRIESGADO` | Perfil agresivo, busca máxima rentabilidad asumiendo mayor riesgo |

### Modelo

```json
{
  "id": "665a...",
  "nombre": "CONSERVADOR",
  "descripcion": "Perfil conservador, prioriza la preservación del capital",
  "activo": true
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | String | ID de MongoDB (generado automáticamente) |
| `nombre` | String | Nombre del perfil (obligatorio, único) |
| `descripcion` | String | Descripción del perfil (opcional) |
| `activo` | boolean | Si está activo es visible para asignar a clientes |

### Endpoints

| Endpoint | Método | Acceso | Descripción |
|----------|--------|--------|-------------|
| `GET /api/perfil-riesgo` | GET | ADMIN | Listar todos los perfiles |
| `GET /api/perfil-riesgo/activos` | GET | Cualquier usuario | Listar perfiles activos |
| `GET /api/perfil-riesgo/{id}` | GET | ADMIN | Buscar perfil por ID |
| `POST /api/perfil-riesgo` | POST | ADMIN | Crear nuevo perfil |
| `PUT /api/perfil-riesgo/{id}` | PUT | ADMIN | Actualizar perfil |
| `DELETE /api/perfil-riesgo/{id}` | DELETE | ADMIN | Eliminar perfil |
| `PUT /api/perfil-riesgo/{id}/toggle-active` | PUT | ADMIN | Activar/desactivar perfil |

### Relación con Clientes

Cada cliente contiene un subdocumento embebido `perfilRiesgo` con un snapshot del perfil asignado al momento de la asignación. El API acepta `perfilRiesgoId` como campo de input y el backend resuelve el catálogo para construir el subdocumento.

**Request (input):**

```json
{
  "nombre": "Juan Pérez",
  "email": "juan@example.com",
  "perfilRiesgoId": "665a..."
}
```

**Documento en MongoDB (respuesta):**

```json
{
  "_id": ObjectId("6a55053d..."),
  "nombre": "Juan Pérez",
  "email": "juan.perez@test.com",
  "telefono": "+57 300 123 4567",
  "direccion": "Calle 10 #5-20, Bogotá",
  "perfilRiesgo": {
    "PerfilRiesgoID": "6a552c29...",
    "PerfilDescripcion": "CONSERVADOR",
    "fechaAsignacion": ISODate("2026-07-13T18:47:48.978Z")
  },
  "_class": "com.example.demo_mongo_api.model.Cliente"
}
```

| Campo del subdoc | Tipo | Descripción |
|------------------|------|-------------|
| `PerfilRiesgoID` | String | ID del perfil en el catálogo |
| `PerfilDescripcion` | String | Nombre del perfil al momento de asignación |
| `fechaAsignacion` | DateTime | Fecha y hora de la asignación (auto-generada) |

Al asignar o cambiar el perfil, `fechaAsignacion` se actualiza automáticamente. Si se desasigna el perfil (`perfilRiesgoId: null`), el subdocument `perfilRiesgo` se limpia a `null`. El campo `perfilRiesgoId` en el request es de solo escritura — no aparece en la respuesta.

### Frontend

La sección **"Perfiles Riesgo"** aparece en el sidebar solo para ADMIN. Permite:
- Crear nuevos perfiles de riesgo
- Editar nombre y descripción
- Activar/desactivar perfiles (sin eliminar)
- Eliminar perfiles

Al crear o editar un cliente, el select de "Perfil de Riesgo" se carga dinámicamente desde `GET /api/perfil-riesgo/activos`, mostrando solo los perfiles activos.

### Validación

```properties
perfilRiesgo.nombre.notblank=El nombre del perfil de riesgo es obligatorio
```

## Exportar a CSV

Los endpoints de exportación permiten descargar la totalidad de registros en formato CSV.

### Endpoints

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `GET /api/productos/export` | GET | Exporta todos los productos a `productos.csv` |
| `GET /api/clientes/export` | GET | Exporta todos los clientes a `clientes.csv` |

### Respuesta

Archivos CSV con `Content-Disposition: attachment`. Ejemplo para productos:

```csv
id,nombre,descripcion,precio,stock
665a...,Teclado mecánico,Switches azules,89.90,50
```

Ejemplo para clientes (incluye nombre del perfil y fecha de asignación):

```csv
id,nombre,email,telefono,direccion,perfilRiesgo,fechaAsignacion
665a...,Juan Pérez,juan@example.com,+34 600 123 456,Calle Falsa 123,CONSERVADOR,2026-07-13T18:47:48.978
```

Los campos que contengan comas, comillas o saltos de línea se encierran entre comillas dobles automáticamente.

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

# Perfil de Riesgo
perfilRiesgo.nombre.notblank=El nombre del perfil de riesgo es obligatorio
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

## Kubernetes

El proyecto incluye manifiestos Kubernetes para desplegar app + MongoDB en un cluster minikube o similar.

### Estructura de manifiestos

```
k8s/
├── namespace.yaml          # Namespace demo-mongo-api
├── secrets.yaml            # JWT_SECRET, credenciales Mongo (base64)
├── configmap.yaml          # Variables no sensibles (SPRING_PROFILES_ACTIVE, etc.)
├── mongo-service.yaml      # Service headless para MongoDB
├── mongo-statefulset.yaml  # MongoDB con volumen persistente (1Gi)
├── mongo-pv.yaml           # PersistentVolume local (solo minikube/desarrollo)
├── app-deployment.yaml     # Deployment de la API (2 réplicas)
├── app-service.yaml        # Service LoadBalancer
└── ingress.yaml            # Ingress con nombre de dominio (opcional)
```

### Paso a paso: desplegar desde cero en otro equipo

#### 1. Instalar prerrequisitos

| Herramienta | Propósito | Enlace |
|---|---|---|
| Docker Desktop | Runtime de contenedores | https://www.docker.com/products/docker-desktop/ |
| kubectl | CLI de Kubernetes | https://kubernetes.io/docs/tasks/tools/ |
| minikube | Cluster K8s local | https://minikube.sigs.k8s.io/docs/start/ |

#### 2. Clonar el repositorio

```bash
git clone https://github.com/juansebarrera/demo-mongo-api.git
cd demo-mongo-api
git checkout main
git pull
```

#### 3. Generar un JWT_SECRET seguro

JJWT requiere una clave HMAC-SHA de **mínimo 32 bytes (256 bits)**. Si usas una clave más corta obtendrás `WeakKeyException`.

```bash
# Genera un secreto de 32 bytes en base64
openssl rand -base64 32
```

Copia el resultado (algo como `/NS2U3L7jiZEFgI1a/RixHxeehP5SuYu92DN86cnEuU=`).

#### 4. Configurar los secretos

Edita `k8s/secrets.yaml` y reemplaza los valores base64:

```yaml
data:
  # JWT_SECRET: tu secreto generado en el paso anterior (base64)
  JWT_SECRET: <tu-base64-de-32-bytes>
  # ADMIN_SEED_PASSWORD: contraseña del admin que se crea automáticamente (base64)
  ADMIN_SEED_PASSWORD: YWRtaW4xMjM=
  # SPRING_MONGODB_URI: URI de conexión (base64)
  SPRING_MONGODB_URI: bW9uZ29kYjovL2FkbWluOmFkbWlucGFzc3dvcmRAbW9uZ286MjcwMTcvZGVtb19tb25nb19hcGk/YXV0aFNvdXJjZT1hZG1pbg==
  # Mongo root credentials (base64) - deben coincidir con SPRING_MONGODB_URI
  MONGO_INITDB_ROOT_USERNAME: YWRtaW4=
  MONGO_INITDB_ROOT_PASSWORD: YWRtaW5wYXNzd29yZA==
```

Para generar base64 de tus propios valores:

```bash
echo -n 'tu-clave' | base64
# Ejemplo: echo -n 'admin' | base64 → YWRtaW4=
```

> **Importante:** Si cambias `MONGO_INITDB_ROOT_PASSWORD`, también debes cambiar la contraseña en la URI de `SPRING_MONGODB_URI`. Ambos valores deben coincidir. Si la base de datos ya tiene datos con credenciales anteriores, borra el PersistentVolumeClaim antes de re-desplegar.

#### 5. Iniciar minikube (si usas cluster local)

```bash
minikube start --cpus=2 --memory=4096
```

#### 6. Desplegar la aplicación

**Opción A — Script automático:**

```bash
# Windows (PowerShell)
.\scripts\deploy-k8s.ps1

# Linux/macOS
chmod +x ./scripts/deploy-k8s.sh
./scripts/deploy-k8s.sh
```

**Opción B — Manual (paso a paso):**

```bash
# Namespace
kubectl apply -f k8s/namespace.yaml

# Secretos y configuración
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/configmap.yaml

# MongoDB (StatefulSet con volumen persistente)
kubectl apply -f k8s/mongo-service.yaml
kubectl apply -f k8s/mongo-statefulset.yaml
kubectl wait --for=condition=ready pod -l app=mongo -n demo-mongo-api --timeout=120s

# App (Deployment + Service)
kubectl apply -f k8s/app-service.yaml
kubectl apply -f k8s/app-deployment.yaml
kubectl wait --for=condition=ready pod -l app=app -n demo-mongo-api --timeout=180s
```

#### 7. Verificar el despliegue

```bash
# Ver pods (ambos deben estar Running con 1/1 Ready)
kubectl get pods -n demo-mongo-api

# Ver logs — busca "Started DemoMongoApiApplication" y los seeders
kubectl logs -l app=app -n demo-mongo-api --tail=50

# Health check
kubectl exec -n demo-mongo-api deploy/app -- wget -qO- http://localhost:8080/actuator/health
```

#### 8. Acceder a la app

```bash
# Port-forward (funciona en cualquier cluster)
kubectl port-forward svc/app 8080:80 -n demo-mongo-api
```

Abrir en el navegador:

| URL | Descripción |
|---|---|
| http://localhost:8080 | GUI (login/registro) |
| http://localhost:8080/swagger-ui/index.html | Swagger UI |
| http://localhost:8080/actuator/health | Health check |

Credenciales por defecto:

| Campo | Valor |
|---|---|
| Usuario | `admin` |
| Contraseña | `admin123` |

### Credenciales por defecto

| Credencial | Valor | Nota |
|---|---|---|
| Admin user | `admin` | Creado automáticamente por `AdminSeeder` (solo perfil dev/test) |
| Admin password | `admin123` | Definido en `ADMIN_SEED_PASSWORD` del Secret |
| Mongo root | `admin` | `MONGO_INITDB_ROOT_USERNAME` |
| Mongo password | `adminpassword` | `MONGO_INITDB_ROOT_PASSWORD` |
| DB name | `demo_mongo_api` | Definido en `SPRING_MONGODB_URI` |

### Troubleshooting

#### `WeakKeyException: The specified key byte array is X bits`

El `JWT_SECRET` es demasiado corto. JJWT HMAC-SHA256 requiere mínimo 32 bytes (256 bits).

```bash
# Generar un secreto válido
openssl rand -base64 32
```

Reemplaza el valor en `k8s/secrets.yaml` y re-despliega.

#### Pods en `CrashLoopBackOff` — MongoDB `AuthenticationFailed`

El `SPRING_MONGODB_URI` no coincide con las credenciales de `MONGO_INITDB_ROOT_PASSWORD`. Si la base de datos ya tiene datos viejos, borra el PVC:

```bash
kubectl delete statefulset mongo -n demo-mongo-api
kubectl delete pvc mongo-data-mongo-0 -n demo-mongo-api
kubectl apply -f k8s/mongo-statefulset.yaml
```

#### La app tarda mucho en levantar y el pod se reinicia

La app tarda ~60 segundos en iniciar (Spring Boot + MongoDB driver). Los `readinessProbe` y `livenessProbe` tienen `initialDelaySeconds: 70` para compensar. Si tu cluster es más lento, ajusta en `k8s/app-deployment.yaml`:

```yaml
readinessProbe:
  initialDelaySeconds: 90  # aumentar si la app tarda más
livenessProbe:
  initialDelaySeconds: 90
```

#### `AdminSeeder` no crea el usuario admin

Verifica que `SPRING_PROFILES_ACTIVE=dev` esté en el ConfigMap. El seeder solo corre en perfiles `dev` o `test`.

### Secretos

Los valores en `k8s/secrets.yaml` son de ejemplo para desarrollo. **En producción:**

1. Genera valores reales con `openssl rand -base64 32`
2. Usa un `Secret` management (Vault, sealed-secrets, etc.)
3. Nunca commitees secretos reales al repo

### Diferencias vs Docker Compose

| Aspecto | Docker Compose | Kubernetes |
|---|---|---|
| Mongo hostname | `mongo` (service name) | `mongo` (headless service + StatefulSet) |
| Datos persistentes | Named volume | PersistentVolumeClaim (1Gi) |
| Secretos | `.env` | `Secret` + `secretKeyRef` |
| Health checks | `docker-compose.yml` | `readinessProbe` + `livenessProbe` |
| Réplicas | 1 | 2 (configurable en Deployment) |
| Exposición | `ports: 8080:8080` | Service LoadBalancer / Ingress |
| JWT_SECRET mínimo | 32 bytes (JJWT requirement) | 32 bytes (JJWT requirement) |

### Comandos útiles

```bash
# Ver pods
kubectl get pods -n demo-mongo-api

# Ver logs de la app
kubectl logs -l app=app -n demo-mongo-api -f

# Reiniciar la app (rolling update)
kubectl rollout restart deployment/app -n demo-mongo-api

# Ver logs del seed admin (buscar "Usuario admin creado")
kubectl logs -l app=app -n demo-mongo-api | Select-String "admin"

# Verificar estado
kubectl get all -n demo-mongo-api

# Eliminar todo (incluye datos)
kubectl delete namespace demo-mongo-api
```

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
    ├── clientes.js     # CRUD de clientes con modales (incluye select de perfil de riesgo)
    ├── users.js        # Gestión de usuarios (admin)
    └── perfiles.js     # Gestión de perfiles de riesgo (admin)
```

### Flujo

1. El usuario accede a `/index.html` y se loguea o registra.
2. `auth.js` llama a `POST /api/auth/login` y guarda el `accessToken` y `refreshToken` en `localStorage`.
3. Se redirige a `/app.html`, que carga `api.js` → `productos.js` → `clientes.js` → `users.js` → `perfiles.js`.
4. Cada request desde `api.js` envía `Authorization: Bearer <token>` automáticamente.
5. Si el usuario es ADMIN, aparecen las secciones "Usuarios" y "Perfiles Riesgo" en el sidebar.

### Seguridad (backend)

- `SecurityConfig` permite acceso libre a estáticos (`/`, `/index.html`, `/app.html`, `/css/**`, `/js/**`, `/favicon.ico`), a `/api/auth/**` y a Swagger.
- Cualquier otro endpoint requiere autenticación.
- `DELETE` en `/api/productos/{id}` y `/api/clientes/{id}` requiere rol `ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`).
- El CRUD de `/api/perfil-riesgo/**` requiere rol `ADMIN`, excepto `/api/perfil-riesgo/activos` que es accesible para cualquier usuario autenticado (para poblar el select al crear/editar clientes).
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

- [x] Dashboard con estadísticas (conteo de productos, clientes, usuarios)
- [x] Gestión de usuarios para admin (CRUD, asignación de roles, desactivación)
- [ ] Cambio de contraseña desde la GUI
- [x] Exportar listas a CSV

### Prioridad baja (calidad de vida)

- [x] Dark mode con toggle y `localStorage`
- [x] Notificaciones toast (reemplazar `alert()`)
- [x] Tests de integración para flujos completos (login → CRUD → refresh)
- [ ] Logging estructurado con correlation IDs