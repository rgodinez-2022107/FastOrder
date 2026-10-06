# PROJECT_STATUS

## Fase actual
COMPLETADO — Aplicacion finalizada (build y 45 pruebas en verde).

## Fases completadas
- FASE 0 - Analisis
- FASE 1 - Configuracion (pom, wrapper, application.properties, .gitignore)
- FASE 2 - Modelo (enums, entidades, repositorios, indices)
- FASE 3 - DTOs y validaciones (14 DTOs + excepciones + GlobalExceptionHandler)
- FASE 4 - Seguridad (JwtService, filtro, UserDetailsService, SecurityConfig)
- FASE 5 - Servicios (Auth, Comercio, Producto, Pedido)
- FASE 6 - Controladores (los 11 endpoints del contrato)
- FASE 7 - Datos semilla (data.sql idempotente)
- FASE 8 - Verificacion manual en vivo (smoke tests completos)
- FASE 9 - Pruebas (unit + integracion + concurrencia: 45/45)
- FASE 10 - Optimizacion (bloqueo pesimista, anti-N+1, paginacion en dos pasos)
- FASE 11 - Documentacion (README.md, PROJECT_STATUS.md)

## Archivos creados
- pom.xml (Spring Boot 3.3.5, Java 17, jjwt 0.12.6, surefire profile=test)
- mvnw, mvnw.cmd, .mvn/wrapper/
- src/main: FastOrderApplication, 3 controllers, 4 services, 5 repositories,
  5 entities, 3 enums, 14 DTOs, 5 excepciones + GlobalExceptionHandler,
  3 security + SecurityConfig, application.properties, data.sql
- src/test: EstadoPedidoTest, JwtServiceTest, AuthSecurityIntegrationTest,
  PedidoFlowIntegrationTest, ConcurrencyIntegrationTest, application-test.properties
- README.md, PROJECT_STATUS.md, .gitignore

## Requisitos cumplidos
- 11 endpoints exactos con roles: register/login publicos; comercios GET auth y
  POST ADMIN; productos GET auth y POST ADMIN; pedidos POST CLIENTE,
  mis-pedidos CLIENTE, disponibles REPARTIDOR/ADMIN, estado REPARTIDOR/ADMIN,
  cancelar CLIENTE(propios)/ADMIN
- Register siempre fuerza rol CLIENTE (previene elevacion de privilegios)
- Totales calculados solo en servidor; costo de envio Q20.00
- Transiciones centralizadas en EstadoPedido.puedeTransicionarA; CANCELADO
  solo via /cancelar
- Concurrencia: lock pesimista FOR UPDATE ordenado por id + filtro por comercio;
  validacion de stock bajo lock (sin sobreventa); restauracion atomica con
  incrementarStock; probado con 10 hilos (A), 2x7 vs stock 10 (B),
  rollback parcial (C), lecturas concurrentes (D)
- Anti-N+1: paginacion en dos pasos (ids + JOIN FETCH)
- Errores uniformes JSON: 400/401/403/404/409/405/500 sin stack traces
- data.sql idempotente (ON CONFLICT / NOT EXISTS)
- Indices y CHECKs segun modelo; IDs IDENTITY

## Pruebas realizadas
- mvnw clean package: BUILD SUCCESS, 45/45 tests
  - EstadoPedidoTest (5), JwtServiceTest (5)
  - AuthSecurityIntegrationTest (15): 401/403/roles/elevacion/duplicados
  - PedidoFlowIntegrationTest (16): totales, stock, rollback, transiciones,
    cancelacion con restauracion, aislamiento entre clientes, 404
  - ConcurrencyIntegrationTest (4): pruebas A, B, C, D
- Smoke tests manuales en vivo (puerto 8081): todos los flujos verificados

## Decisiones de diseno
- disponibles: PENDIENTE sin repartidor + EN_CAMINO del repartidor autenticado
  (ADMIN ve PENDIENTE sin asignar + todos los EN_CAMINO)
- GET /comercios/{id}/productos devuelve todos los productos (sin filtro
  disponible), literal al contrato
- Register ignora el campo rol del request (siempre CLIENTE)
- Repartidor se auto-asigna como repartidor_id en la primera actualizacion
  de estado si el pedido no tiene asignado

## Notas de entorno
- Puerto 8081 durante desarrollo (8080 ocupado por PEMHTTPD-x64);
  usar 8080 (default) para la evaluacion si se libera el puerto
- JAVA_TOOL_OPTIONS con truststore propio por firewall Fortinet
- DB: fastorder (dev) / fastorder_test (pruebas, create-drop)
