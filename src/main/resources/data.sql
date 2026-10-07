-- FASTORDER - Datos iniciales
-- Idempotente: puede ejecutarse en cada arranque (ON CONFLICT DO NOTHING).

-- Usuarios (passwords BCrypt: Admin123! / Repartidor123! / Cliente123!)
INSERT INTO usuarios (nombre, direccion, telefono, email, password, rol) VALUES
  ('Administrador General', 'Zona 1, Ciudad de Guatemala', '5555-0001', 'admin@fastorder.com',
   '$2a$10$RJJcvXcq/ECwZ8TFmf6D8uTRlPS8Ief9XwVjvJxNGNb7UAlU5sjcm', 'ADMIN'),
  ('Repartidor Demo', 'Zona 5, Ciudad de Guatemala', '5555-0002', 'repartidor@fastorder.com',
   '$2a$10$5AA2wiTeYhypEMmCFmgkD.hLNXK4SgkIkpTkAI75CTWc79CYRfu8q', 'REPARTIDOR'),
  ('Cliente Demo', 'Zona 10, Ciudad de Guatemala', '5555-0003', 'cliente@fastorder.com',
   '$2a$10$Lq.1FkLti8ZgY1P3JU4eT.Y/1fQoQp0qVG0m5TJrkCEx3pYjfwVqK', 'CLIENTE')
ON CONFLICT (email) DO NOTHING;

-- Comercio inicial
INSERT INTO comercios (nombre, categoria, direccion, abierto)
SELECT 'La Receta', 'RESTAURANTE', '0a0101, Ciudad de Guatemala', true
WHERE NOT EXISTS (SELECT 1 FROM comercios WHERE nombre = 'La Receta');

-- Productos del comercio 1 (ids fijos via subconsulta del nombre unico del comercio)
INSERT INTO productos (comercio_id, nombre, precio, stock, disponible)
SELECT c.id, p.nombre, p.precio, p.stock, p.disponible
FROM (VALUES
    ('Pollo Broaster',      45.00, 100, true),
    ('Hamburguesa Clasica', 35.00, 100, true),
    ('Pizza Familiar',      75.00,  50, true),
    ('Ensalada Cesar',      30.00,  40, true),
    ('Arroz con Pollo',     40.00,  60, true),
    ('Refresco 1L',         12.00, 200, true),
    ('Agua Mineral 600ml',   8.00, 200, true),
    ('Postre de Chocolate', 25.00,  30, true)
) AS p(nombre, precio, stock, disponible)
CROSS JOIN comercios c
WHERE c.nombre = 'La Receta'
  AND NOT EXISTS (
    SELECT 1 FROM productos pr WHERE pr.comercio_id = c.id AND pr.nombre = p.nombre
  );
