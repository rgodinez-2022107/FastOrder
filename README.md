# FastOrder

API REST de pedidos para comercios locales. Backend en Java 17 / Spring Boot 3.3.5 con PostgreSQL y autenticación JWT.

**Autor:** R. Godínez — 2022107

## Requisitos

- JDK 17+ (probado con JDK 21)
- PostgreSQL 14+
- Maven (o usar el wrapper incluido: `./mvnw`)

## Base de datos

```sql
CREATE ROLE fastorder WITH LOGIN PASSWORD 'fastorder_dev_password';
CREATE DATABASE fastorder OWNER fastorder;
CREATE DATABASE fastorder_test OWNER fastorder;
```

## Configuración

Variables de entorno (con valores por defecto para desarrollo):

| Variable | Defecto |
|---|---|
| `SERVER_PORT` | `8080` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/fastorder` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | *(vacío)* |
| `JWT_SECRET` | secreto de desarrollo |
| `JWT_EXPIRATION_MS` | `3600000` |

El envío cuesta **Q20.00** (`fastorder.shipping-cost` en `application.properties`).

## Ejecutar

```bash
./mvnw spring-boot:run
# o
./mvnw clean package
java -jar target/fastorder-1.0.0.jar
```

## Pruebas

```bash
./mvnw test          # unit + integración + concurrencia (usa fastorder_test)
./mvnw clean package # verify completo
```

45 pruebas: unitarias (enums, JWT), integración (seguridad/roles, flujo de pedidos) y de concurrencia (bloqueo pesimista de stock).

## Usuarios sembrados (`data.sql`)

| Email | Password | Rol |
|---|---|---|
| `admin@fastorder.com` | `Admin123!` | ADMIN |
| `repartidor@fastorder.com` | `Repartidor123!` | REPARTIDOR |
| `cliente@fastorder.com` | `Cliente123!` | CLIENTE |

`POST /api/v1/auth/register` siempre crea usuarios **CLIENTE** (ignora el rol enviado).

## Endpoints

Base: `/api/v1` — JSON, stateless JWT (`Authorization: Bearer <token>`).

### Auth (público)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/register` | Registro (rol siempre CLIENTE) → 201 |
| POST | `/auth/login` | Login → `{ token, ... }` |

### Comercios

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/comercios` | autenticado | Lista paginada; filtro `?categoria=` (400 si es inválida) |
| POST | `/comercios` | ADMIN | Crear comercio → 201 |
| GET | `/comercios/{id}/productos` | autenticado | Productos paginados del comercio |
| POST | `/comercios/{id}/productos` | ADMIN | Crear producto → 201 |

### Pedidos

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/pedidos` | CLIENTE | Crear pedido (totales calculados en servidor) → 201 |
| GET | `/pedidos/mis-pedidos` | CLIENTE | Pedidos propios, paginados |
| GET | `/pedidos/disponibles` | REPARTIDOR/ADMIN | Para repartir: PENDIENTE sin asignar + EN_CAMINO propios (ADMIN: todos) |
| PATCH | `/pedidos/{id}/estado` | REPARTIDOR/ADMIN | `EN_PREPARACION` → `EN_CAMINO` → `ENTREGADO`; asigna repartidor si está vacío |
| PATCH | `/pedidos/{id}/cancelar` | CLIENTE (propios)/ADMIN | Solo desde PENDIENTE; restaura stock |

### Estados

```
PENDIENTE → EN_PREPARACION → EN_CAMINO → ENTREGADO
PENDIENTE → CANCELADO (vía /cancelar, restaura stock)
```

Transiciones inválidas → 409.

### Errores

Respuesta uniforme: `{ timestamp, status, error, message, path [, fieldErrors] }`
`400` validación · `401` sin token · `403` rol · `404` recurso · `409` conflicto/stock · `405` método · `500` genérico (sin stack trace).

## Concurrencia

- Crear pedido bloquea los productos con `SELECT ... FOR UPDATE` (orden por id, filtrado por comercio) antes de validar stock → sin sobreventa bajo concurrencia.
- Cancelación restaura stock con `UPDATE ... SET stock = stock + :cant` atómico.
- Paginación en dos pasos (ids + `JOIN FETCH`) para evitar N+1.

## Estructura

```
src/main/java/com/fastorder/
├── config/       SecurityConfig
├── controller/   AuthController, ComercioController, PedidoController
├── dto/          Requests, responses, PageResponse, DtoMapper
├── entity/       Usuario, Comercio, Producto, Pedido, DetallePedido
├── enums/        Rol, CategoriaComercio, EstadoPedido
├── exception/    Excepciones + GlobalExceptionHandler
├── repository/   JPA repositories (consultas y locks)
├── security/     JwtService, JwtAuthenticationFilter, CustomUserDetailsService
└── service/      AuthService, ComercioService, ProductoService, PedidoService
```
