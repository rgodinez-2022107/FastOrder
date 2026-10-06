# PROJECT_STATUS

## Fase actual
FASE 2 - MODELO (archivos creados, compilacion con Maven NO verificada)

## Fases completadas
- FASE 0 - Analisis
- FASE 1 - Configuracion (pendiente de verificar mvn compile local)

## Fases pendientes
FASE 3 a FASE 11

## Archivos creados
FASE 1:
- pom.xml, FastOrderApplication.java, application.properties, .gitignore, PROJECT_STATUS.md
FASE 2:
- enums/Rol.java, enums/CategoriaComercio.java, enums/EstadoPedido.java
- entity/Usuario.java, Comercio.java, Producto.java, Pedido.java, DetallePedido.java
- repository/UsuarioRepository.java, ComercioRepository.java, ProductoRepository.java, PedidoRepository.java, DetallePedidoRepository.java

## Archivos modificados
Ninguno

## Requisitos cumplidos
- Entidades y campos segun el modelo de datos; BigDecimal en dinero; enums STRING
- Relaciones: Comercio-Producto, Usuario-Pedido (cliente y repartidor nullable), Pedido-DetallePedido (cascade ALL, orphanRemoval), DetallePedido-Producto; todo LAZY
- Transiciones de estado centralizadas en EstadoPedido.puedeTransicionarA
- Indices: uk_usuario_email, idx_comercio_categoria, idx_producto_comercio_disponible (comercio_id, disponible), idx_pedido_cliente_fecha (cliente_id, fecha_pedido), idx_pedido_repartidor, idx_pedido_estado, idx_detalle_pedido
- CHECK en BD: stock >= 0 y precio >= 0 (solo se aplica a tablas creadas por Hibernate)
- Locking: ProductoRepository.lockAllByIdInAndComercioId (PESSIMISTIC_WRITE, ORDER BY id, filtra por comercio); PedidoRepository.findByIdForUpdate
- Restauracion de stock atomica: ProductoRepository.incrementarStock
- Paginacion en dos pasos para evitar N+1 y paginacion en memoria: ids paginados + findAllWithDetallesByIdIn (JOIN FETCH)
- IDs con IDENTITY (data.sql podra insertar sin id)

## Pruebas realizadas
- Revision sintactica con javac (sin errores de sintaxis; solo simbolos de dependencias no disponibles)
- mvn compile: NO ejecutado (sin Maven ni acceso a Maven Central en el entorno de Claude)

## Problemas encontrados
- Compilacion Maven no verificable en el entorno de Claude. Ejecutar localmente: mvn -q compile

## Decisiones a revisar
- disponibles: PENDIENTE sin repartidor + EN_CAMINO del repartidor (ADMIN ve todos los EN_CAMINO)
- Indices compuestos cubren cliente_id y comercio_id por prefijo; no hay indice aislado en producto.disponible por baja selectividad

## Proximo paso exacto
FASE 3: DTOs y validaciones (register, login, comercio, producto, crear pedido, detalle, pedido, estado, error).
