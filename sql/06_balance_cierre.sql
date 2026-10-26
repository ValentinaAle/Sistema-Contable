-- Módulo Balance y cierre. Compatible con MySQL 8 y MariaDB.
USE economia_db;

-- Agrega asientos.origen solo si todavía no existe
-- (MySQL no admite ADD COLUMN IF NOT EXISTS).
SET @existe := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'economia_db'
                  AND TABLE_NAME = 'asientos'
                  AND COLUMN_NAME = 'origen');
SET @sql := IF(@existe = 0,
               'ALTER TABLE asientos ADD COLUMN origen VARCHAR(20) NOT NULL DEFAULT ''MANUAL''',
               'SELECT ''La columna asientos.origen ya existe'' AS aviso');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS procesos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL,
    fecha_desde DATE NOT NULL,
    fecha_hasta DATE NOT NULL,
    asiento_id INT NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_procesos_asiento FOREIGN KEY (asiento_id) REFERENCES asientos(id) ON DELETE CASCADE,
    CONSTRAINT ck_procesos_tipo CHECK (tipo IN ('POSICION_IVA','REFUNDICION','AJUSTE')),
    INDEX ix_procesos_periodo (tipo, fecha_desde, fecha_hasta)
);

DESCRIBE asientos;
DESCRIBE procesos;
