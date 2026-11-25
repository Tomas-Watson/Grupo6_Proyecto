-- =====================================================================================
-- EcoGestión Verde: datos de prueba de Comprar (CU-05), Consultar estado del pedido (CU-09),
-- Solicitar devolución (CU-13) y Actualizar devolución (CU-22).
-- Se cargan en cada arranque, después de schema.sql. Sirven tanto para MySQL como para H2.
-- La contraseña de todos los usuarios es: verde123  (guardada con BCrypt)
-- =====================================================================================

INSERT INTO usuario (id_usuario, nombre, email, password_hash, tipo_usuario) VALUES
    (1, 'Ana Gómez',      'ana@mail.test',          '$2a$10$c5pf2mzW/S5KYbBsLLRrkOZkiO7NOL51cy7paJQywStHqfDLH13.m', 'CLIENTE'),
    (2, 'Bruno Díaz',     'bruno@mail.test',        '$2a$10$ZO5I/IcAUpewgUZ4znSVi.0a42JyKwxLhhcYX1XZGiNhLFu.n.ETy', 'CLIENTE'),
    (3, 'Carla Ruiz',     'carla@mail.test',        '$2a$10$diu7RAwTjm62oSi1NM7qwuDE7c3Tm2tGZ86v5aVSQh6/BUoa5eABG', 'CLIENTE'),
    (4, 'Diego Paz',      'diego@mail.test',        '$2a$10$JNlrlQadXLutYPV4/ki52OGOl17kf.zVQwjaYtABqjHJf/Cybpx46', 'CLIENTE'),
    (5, 'Elena Sosa',     'elena@mail.test',        '$2a$10$53gmaGUwmUwx2mxV.OlRlew83u/8u5e6czrPOid2.FCNgwggjQV.m', 'CLIENTE'),
    (6, 'Federico Luna',  'fede@mail.test',         '$2a$10$Tqw11IHbAeUbDEsyEHqUEOPRL7nDmFccSRvnNHTIoAR3S5BiEYX4G', 'CLIENTE'),
    (7, 'Valeria Torres', 'ventas@verdegreen.test', '$2a$10$foKnuAl2GMyUGCQkl1SROuVkXSeBxxergGhscu6N/AdGZbYmSt0gm', 'EMPLEADO'),
    (8, 'Tomás Rivas',    'transporte@verdegreen.test', '$2a$10$foKnuAl2GMyUGCQkl1SROuVkXSeBxxergGhscu6N/AdGZbYmSt0gm', 'EMPLEADO');

INSERT INTO cliente (id_cliente, telefono, direccion) VALUES
    (1, '2954-410001', 'Av. Siempre Verde 101'),
    (2, '2954-410002', 'Calle de los Pinos 202'),
    (3, '2954-410003', 'Pasaje Reciclado 303'),
    (4, '2954-410004', 'Bulevar Sustentable 404'),
    (5, '2954-410005', 'Calle del Bosque 505'),
    (6, '2954-410006', 'Av. del Parque 606');

-- Tomás (8) es transportista: sirve para probar que solo Ventas puede actualizar devoluciones (CU-22).
INSERT INTO empleado (id_empleado, rol) VALUES
    (7, 'VENTAS'),
    (8, 'TRANSPORTISTA');


-- Catálogo. Los productos 7 y 8 están dados de baja (publicado = 0) para probar el FE-02.
INSERT INTO producto (id_producto, codigo, nombre, descripcion, precio, estado, categoria,
                      tiempo_estimado_produccion, stock_disponible, publicado) VALUES
    (1, 'MAC-001', 'Maceta mediana de PET reciclado', 'Maceta de 20 cm fabricada con PET reciclado.',
        4500.00, 'DISPONIBLE', 'STOCK_HABITUAL', NULL, 50, 1),
    (2, 'BAN-001', 'Bandeja de PET reciclado', 'Bandeja rectangular apta para alimentos secos.',
        2800.00, 'DISPONIBLE', 'STOCK_HABITUAL', NULL, 120, 1),
    (3, 'BOT-001', 'Botella reutilizable de 1 L', 'Botella con tapa a rosca, libre de BPA.',
        3200.00, 'DISPONIBLE', 'STOCK_HABITUAL', NULL, 80, 1),
    (4, 'PLA-001', 'Set de 6 platos reciclados', 'Juego de seis platos playos de PET reciclado.',
        5600.00, 'DISPONIBLE', 'STOCK_HABITUAL', NULL, 40, 1),
    (5, 'BNC-001', 'Banco de plaza de PET', 'Banco para exterior de 1,5 m que se fabrica a pedido.',
        185000.00, 'DISPONIBLE', 'BAJO_DEMANDA', 10, 2, 1),
    (6, 'MES-001', 'Mesa de exterior de PET', 'Mesa para seis personas que se fabrica a pedido.',
        320000.00, 'AGOTADO', 'BAJO_DEMANDA', 15, 0, 1),
    (7, 'JUE-001', 'Juego infantil con tobogán', 'Juego para plazas con tobogán que se fabrica a pedido.',
        540000.00, 'AGOTADO', 'BAJO_DEMANDA', 15, 0, 0),
    (8, 'MAC-099', 'Maceta colgante (discontinuada)', 'Modelo dado de baja del catálogo.',
        3900.00, 'DISPONIBLE', 'STOCK_HABITUAL', NULL, 25, 0);

-- Un carrito por escenario. El cliente 6 (fede) no tiene carrito.
INSERT INTO carrito (id_carrito, id_cliente) VALUES
    (1, 1),
    (2, 2),
    (3, 3),
    (4, 4),
    (5, 5);

-- ana (carrito 1): camino feliz, hay stock de todo. Total 20600.00, se abona el 100 %.
-- bruno (carrito 2): FA-02, falta stock del banco y de la mesa. Total 879500.00, seña 263850.00.
-- carla (carrito 3): FE-02, la maceta colgante ya no está publicada. Queda un total de 11200.00.
-- diego (carrito 4): FE-02, ningún producto sigue publicado.
-- elena (carrito 5): carrito sin productos.
INSERT INTO item_carrito (id_carrito, id_producto, cantidad) VALUES
    (1, 1, 2),
    (1, 2, 3),
    (1, 3, 1),
    (2, 1, 1),
    (2, 5, 3),
    (2, 6, 1),
    (3, 4, 2),
    (3, 8, 1),
    (4, 7, 1),
    (4, 8, 2);

-- =====================================================================================
-- Consultar estado del pedido (CU-09). Federico (cliente 6, sin carrito) tiene un pedido en cada
-- situación de los flujos alternativos. Bruno (cliente 2) tiene uno para probar la RN-01.
-- Elena (cliente 5) no tiene pedidos (FA-01). Los pedidos que se generan con Comprar se agregan
-- con id 12 en adelante (del 7 al 11 son los de devoluciones, más abajo).
--   1  fede   PENDIENTE_DE_PAGO       FA-02: 100 %, la reserva vence en unas 20 h
--   2  fede   EN_COLA_DE_PRODUCCION   FA-03: seña pagada, se reembolsa si cancela
--   3  fede   EN_PRODUCCION           FA-03: la seña ya no se reembolsa si cancela
--   4  fede   CANCELADO               FA-05: informa la fecha de cancelación
--   5  fede   ENTREGADO               FA-06: el envío se consulta en otro caso de uso
--   6  bruno  CONFIRMADO              pago completo; fede no lo puede consultar (RN-01)
-- =====================================================================================
INSERT INTO pedido (id_pedido, id_cliente, fecha_hora, estado, monto_total, cantidad_productos,
                    importe_a_abonar, vencimiento_reserva, fecha_cancelacion, fecha_entrega) VALUES
    (1, 6, NOW() - INTERVAL 4 HOUR, 'PENDIENTE_DE_PAGO',          9000.00, 2,   9000.00,
        NOW() + INTERVAL 20 HOUR, NULL, NULL),
    (2, 6, '2026-10-20 10:00:00',  'EN_COLA_DE_PRODUCCION',     185000.00, 1,  55500.00,
        '2026-10-21 10:00:00', NULL, NULL),
    (3, 6, '2026-10-15 11:00:00',  'EN_PRODUCCION',             320000.00, 1,  96000.00,
        '2026-10-16 11:00:00', NULL, NULL),
    (4, 6, '2026-10-10 10:00:00',  'CANCELADO',                 185000.00, 1,  55500.00,
        '2026-10-11 10:00:00', '2026-10-13 16:00:00', NULL),
    (5, 6, '2026-10-01 09:30:00',  'ENTREGADO',                   3200.00, 1,   3200.00,
        '2026-10-02 09:30:00', NULL, '2026-10-03 15:00:00'),
    (6, 2, '2026-10-22 18:00:00',  'CONFIRMADO',                  5600.00, 1,   5600.00,
        '2026-10-23 18:00:00', NULL, NULL);

INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal, cantidad_reservada) VALUES
    (1, 1, 2,   4500.00,   9000.00, 2),
    (2, 5, 1, 185000.00, 185000.00, 0),
    (3, 6, 1, 320000.00, 320000.00, 0),
    (4, 5, 1, 185000.00, 185000.00, 0),
    (5, 3, 1,   3200.00,   3200.00, 1),
    (6, 4, 1,   5600.00,   5600.00, 1);

-- =====================================================================================
-- Solicitar devolución (CU-13) y Actualizar devolución (CU-22). Las fechas son relativas a NOW()
-- para que los plazos (10 días desde la entrega, 2 devoluciones en 6 meses) se cumplan siempre.
--   7   ana    ENTREGADO hace 3 días    CU-13 camino feliz (productos 1 y 2)
--   8   carla  ENTREGADO hace 5 días    CU-13 FE-01: carla ya tiene 2 devoluciones en 6 meses
--   9   carla  ENTREGADO hace 2 meses   las 2 devoluciones de carla (solicitudes 1 y 2)
--  10   bruno  ENTREGADO hace 8 días    CU-22: solicitudes 3 y 4, En tránsito a devolución
--  11   diego  ENTREGADO hace 2 días    CU-22: solicitud 5 Pendiente de revisión y 6 En tránsito
-- Además, el pedido 5 de fede se entregó hace más de 10 días y el 1 todavía no se entregó: sirven
-- para probar las precondiciones de CU-13 (RN-01 y RN-02).
-- =====================================================================================
INSERT INTO pedido (id_pedido, id_cliente, fecha_hora, estado, monto_total, cantidad_productos,
                    importe_a_abonar, vencimiento_reserva, fecha_cancelacion, fecha_entrega) VALUES
    (7,  1, NOW() - INTERVAL 6 DAY,   'ENTREGADO',  7300.00, 2,  7300.00,
         NOW() - INTERVAL 5 DAY,   NULL, NOW() - INTERVAL 3 DAY),
    (8,  3, NOW() - INTERVAL 8 DAY,   'ENTREGADO',  8800.00, 2,  8800.00,
         NOW() - INTERVAL 7 DAY,   NULL, NOW() - INTERVAL 5 DAY),
    (9,  3, NOW() - INTERVAL 70 DAY,  'ENTREGADO',  7300.00, 2,  7300.00,
         NOW() - INTERVAL 69 DAY,  NULL, NOW() - INTERVAL 66 DAY),
    (10, 2, NOW() - INTERVAL 11 DAY,  'ENTREGADO',  6000.00, 2,  6000.00,
         NOW() - INTERVAL 10 DAY,  NULL, NOW() - INTERVAL 8 DAY),
    (11, 4, NOW() - INTERVAL 5 DAY,   'ENTREGADO', 10100.00, 2, 10100.00,
         NOW() - INTERVAL 4 DAY,   NULL, NOW() - INTERVAL 2 DAY);

INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal, cantidad_reservada) VALUES
    (7,  1, 1, 4500.00, 4500.00, 1),
    (7,  2, 1, 2800.00, 2800.00, 1),
    (8,  3, 1, 3200.00, 3200.00, 1),
    (8,  4, 1, 5600.00, 5600.00, 1),
    (9,  1, 1, 4500.00, 4500.00, 1),
    (9,  2, 1, 2800.00, 2800.00, 1),
    (10, 2, 1, 2800.00, 2800.00, 1),
    (10, 3, 1, 3200.00, 3200.00, 1),
    (11, 4, 1, 5600.00, 5600.00, 1),
    (11, 1, 1, 4500.00, 4500.00, 1);

-- El reembolso se hace por el mismo medio de pago original (RN-06 de CU-22).
INSERT INTO pago (id_pago, id_pedido, tipo, monto, medio_pago, estado, fecha_hora, nro_transaccion) VALUES
    (1, 7,   'TOTAL',  7300.00, 'TARJETA_CREDITO',   'CONFIRMADO', NOW() - INTERVAL 6 DAY,  'TX-0007'),
    (2, 8,   'TOTAL',  8800.00, 'BILLETERA_VIRTUAL', 'CONFIRMADO', NOW() - INTERVAL 8 DAY,  'TX-0008'),
    (3, 9,   'TOTAL',  7300.00, 'TRANSFERENCIA',     'CONFIRMADO', NOW() - INTERVAL 70 DAY, NULL),
    (4, 10,  'TOTAL',  6000.00, 'TARJETA_CREDITO',   'CONFIRMADO', NOW() - INTERVAL 11 DAY, 'TX-0010'),
    (5, 11,  'TOTAL', 10100.00, 'EFECTIVO',          'CONFIRMADO', NOW() - INTERVAL 5 DAY,  NULL);

--   1, 2  carla  RESUELTAS hace 2 meses: cuentan para su límite de 2 devoluciones (FE-01 de CU-13)
--   3     bruno  En tránsito: CU-22 camino feliz (buen estado)
--   4     bruno  En tránsito: CU-22 FA-01 (llegó dañado)
--   5     diego  Pendiente de revisión: CU-22 FE-01 (no admite actualización)
--   6     diego  En tránsito: CU-22 con un empleado que no es de Ventas
INSERT INTO solicitud_devolucion (id_solicitud, id_pedido, id_producto, fecha_solicitud, motivo, foto_producto,
                                  estado, fecha_recepcion, resultado_verificacion, monto_a_reembolsar,
                                  id_resolutor, fecha_resolucion) VALUES
    (1, 9,  1, NOW() - INTERVAL 64 DAY, 'No era el tamaño que esperaba', 'fotos/sol-1.jpg',
        'RESUELTA', NOW() - INTERVAL 60 DAY, 'BUEN_ESTADO', 4500.00, 7, NOW() - INTERVAL 59 DAY),
    (2, 9,  2, NOW() - INTERVAL 64 DAY, NULL, 'fotos/sol-2.jpg',
        'RESUELTA', NOW() - INTERVAL 60 DAY, 'BUEN_ESTADO', 2800.00, 7, NOW() - INTERVAL 59 DAY),
    (3, 10, 2, NOW() - INTERVAL 4 DAY,  'El color no coincide con la foto', 'fotos/sol-3.jpg',
        'EN_TRANSITO_A_DEVOLUCION', NULL, NULL, NULL, NULL, NULL),
    (4, 10, 3, NOW() - INTERVAL 4 DAY,  'La tapa no cierra', 'fotos/sol-4.jpg',
        'EN_TRANSITO_A_DEVOLUCION', NULL, NULL, NULL, NULL, NULL),
    (5, 11, 4, NOW() - INTERVAL 1 DAY,  'Llegó un plato quebrado', 'fotos/sol-5.jpg',
        'PENDIENTE_DE_REVISION', NULL, NULL, NULL, NULL, NULL),
    (6, 11, 1, NOW() - INTERVAL 1 DAY,  NULL, 'fotos/sol-6.jpg',
        'EN_TRANSITO_A_DEVOLUCION', NULL, NULL, NULL, NULL, NULL);
