-- Catálogo y orden de exposición solicitado para Estados Contables (RT 8).
-- Es seguro en instalaciones existentes: no elimina cuentas ni asientos.
USE economia_db;

SET @ddl_orden = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE rubros ADD COLUMN orden_exposicion INT NOT NULL DEFAULT 99', 'SELECT 1')
    FROM information_schema.columns WHERE table_schema = DATABASE()
      AND table_name = 'rubros' AND column_name = 'orden_exposicion');
PREPARE migracion_orden FROM @ddl_orden;
EXECUTE migracion_orden;
DEALLOCATE PREPARE migracion_orden;

-- Rubros nuevos. INSERT IGNORE mantiene los datos definidos por el usuario.
INSERT IGNORE INTO rubros (grupo_id, tipo, codigo, nombre) VALUES
(1, 2, '45', 'Propiedades de Inversión'),
(2, 1, '46', 'IVA Débito Fiscal'), (2, 1, '47', 'IVA a Pagar'),
(2, 1, '48', 'Acreedores Varios'), (2, 1, '49', 'Deudas sin Documentar'),
(2, 1, '50', 'Documentos a Pagar'), (2, 1, '51', 'Sueldos a Pagar'),
(2, 1, '52', 'Cargas Sociales a Pagar'),
(2, 2, '53', 'Remuneraciones y Cargas Sociales'), (2, 2, '54', 'Cargas Fiscales'),
(2, 2, '55', 'Anticipos de Clientes'), (2, 2, '56', 'Dividendos a Pagar'), (2, 2, '57', 'Otras Deudas'),
(3, 0, '58', 'Reserva Legal');

-- Se renombran solo rubros base para ajustarlos a la denominación pedida.
UPDATE rubros SET nombre = CASE codigo
    WHEN '11' THEN 'Inversiones Temporarias'
    WHEN '05' THEN 'Proveedores'
    WHEN '06' THEN 'Deudas Comerciales'
    WHEN '07' THEN 'Capital'
    WHEN '08' THEN 'Resultados No Asignados'
    ELSE nombre END;

-- El IVA débito existente se expone separado de Proveedores.
UPDATE cuentas SET rubro_id = (SELECT id FROM rubros WHERE codigo = '46')
WHERE codigo = '2.1.05.02';

-- Orden exacto de exposición para los estados. Los rubros históricos que no
-- pertenecen a esta presentación quedan disponibles en el Plan de Cuentas,
-- pero no duplican filas en el informe.
UPDATE rubros SET orden_exposicion = CASE codigo
    WHEN '01' THEN 1 WHEN '11' THEN 2 WHEN '02' THEN 3 WHEN '12' THEN 4 WHEN '03' THEN 5 WHEN '13' THEN 6
    WHEN '14' THEN 1 WHEN '15' THEN 2 WHEN '16' THEN 3 WHEN '04' THEN 4 WHEN '45' THEN 5 WHEN '17' THEN 6 WHEN '18' THEN 7 WHEN '19' THEN 8 WHEN '20' THEN 9
    WHEN '05' THEN 1 WHEN '48' THEN 2 WHEN '49' THEN 3 WHEN '50' THEN 4 WHEN '46' THEN 5 WHEN '47' THEN 6 WHEN '51' THEN 7 WHEN '52' THEN 8
    WHEN '06' THEN 1 WHEN '28' THEN 2 WHEN '53' THEN 3 WHEN '54' THEN 4 WHEN '55' THEN 5 WHEN '56' THEN 6 WHEN '57' THEN 7
    WHEN '07' THEN 1 WHEN '58' THEN 2 WHEN '08' THEN 3
    WHEN '09' THEN 1 WHEN '34' THEN 2 WHEN '35' THEN 3 WHEN '36' THEN 4 WHEN '37' THEN 5 WHEN '38' THEN 6
    WHEN '10' THEN 1 WHEN '39' THEN 2 WHEN '40' THEN 3 WHEN '41' THEN 4 WHEN '42' THEN 5 WHEN '43' THEN 6 WHEN '44' THEN 7
    ELSE 99 END;
