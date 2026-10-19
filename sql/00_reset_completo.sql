-- ============================================================
-- RESET COMPLETO - Sistema Contable (economia_db)
-- Borra todo lo existente y lo vuelve a crear desde cero,
-- ya con los tipos de datos correctos según la consigna.
-- ============================================================

DROP DATABASE IF EXISTS economia_db;
CREATE DATABASE economia_db;
USE economia_db;

-- ------------------------------------------------------------
-- GRUPOS (1er dígito del código)
-- ------------------------------------------------------------
CREATE TABLE grupos (
    id      INT PRIMARY KEY,
    nombre  VARCHAR(50) NOT NULL
);

-- ------------------------------------------------------------
-- RUBROS (3er y 4to dígito del código - 2 dígitos, ampliable)
-- tipo: 0 = no aplica corriente/no corriente
--       1 = corriente
--       2 = no corriente
-- ------------------------------------------------------------
CREATE TABLE rubros (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    grupo_id  INT NOT NULL,
    tipo      INT NOT NULL,
    codigo    VARCHAR(2) NOT NULL,
    nombre    VARCHAR(100) NOT NULL,
    FOREIGN KEY (grupo_id) REFERENCES grupos(id),
    UNIQUE KEY uq_rubro_codigo (codigo)
);

-- ------------------------------------------------------------
-- CUENTAS
-- codigo se genera automáticamente: grupo.tipo.rubro.numero
-- nombre: 40 caracteres | saldo: 9 enteros + 2 decimales | tipo_saldo: 1 carácter ('D'/'A')
-- ------------------------------------------------------------
CREATE TABLE cuentas (
    codigo         VARCHAR(12) PRIMARY KEY,
    grupo_id       INT NOT NULL,
    tipo           INT NOT NULL,
    rubro_id       INT NOT NULL,
    numero_cuenta  INT NOT NULL,
    nombre         VARCHAR(40)   NOT NULL,
    saldo          DECIMAL(11,2) NOT NULL DEFAULT 0,
    tipo_saldo     CHAR(1)       NOT NULL,
    FOREIGN KEY (grupo_id) REFERENCES grupos(id),
    FOREIGN KEY (rubro_id) REFERENCES rubros(id)
);

-- ------------------------------------------------------------
-- DATOS INICIALES: GRUPOS
-- ------------------------------------------------------------
INSERT INTO grupos (id, nombre) VALUES
(1, 'Activo'),
(2, 'Pasivo'),
(3, 'Patrimonio Neto'),
(4, 'Ingresos'),
(5, 'Egresos');

-- ------------------------------------------------------------
-- DATOS INICIALES: RUBROS (lista para el trabajo de desarrollo)
-- Podés agregar más filas cuando quieras ampliar el catálogo,
-- solo respetá grupo_id y tipo correctos.
-- ------------------------------------------------------------
INSERT INTO rubros (grupo_id, tipo, codigo, nombre) VALUES
(1, 1, '01', 'Caja y Bancos'),
(1, 1, '02', 'Créditos por Ventas'),
(1, 1, '03', 'Bienes de Cambio'),
(1, 2, '04', 'Bienes de Uso'),
(2, 1, '05', 'Deudas Comerciales'),
(2, 2, '06', 'Deudas a Largo Plazo'),
(3, 0, '07', 'Patrimonio Neto'),
(3, 0, '08', 'Resultados Acumulados'),
(4, 0, '09', 'Ventas'),
(5, 0, '10', 'Gastos'),
(1, 1, '11', 'Inversiones'), (1, 1, '12', 'Otros Créditos'), (1, 1, '13', 'Otros Activos'),
(1, 2, '14', 'Créditos por Ventas'), (1, 2, '15', 'Otros Créditos'), (1, 2, '16', 'Bienes de Cambio'),
(1, 2, '17', 'Participaciones Permanentes en Sociedades'), (1, 2, '18', 'Otras Inversiones'),
(1, 2, '19', 'Activos Intangibles'), (1, 2, '20', 'Otros Activos'),
(2, 1, '21', 'Préstamos'), (2, 1, '22', 'Remuneraciones y Cargas Sociales'), (2, 1, '23', 'Cargas Fiscales'),
(2, 1, '24', 'Anticipos de Clientes'), (2, 1, '25', 'Dividendos a Pagar'), (2, 1, '26', 'Otras Deudas'),
(2, 2, '27', 'Deudas Comerciales'), (2, 2, '28', 'Préstamos'), (2, 2, '29', 'Previsiones'), (2, 2, '30', 'Otras Deudas'),
(3, 0, '31', 'Capital'), (3, 0, '32', 'Aportes Irrevocables'), (3, 0, '33', 'Reservas'),
(4, 0, '34', 'Resultados por Tenencia'), (4, 0, '35', 'Resultados de Inversiones en Entes Relacionados'),
(4, 0, '36', 'Resultados de Otras Inversiones'), (4, 0, '37', 'Resultados Financieros y por Tenencia'),
(4, 0, '38', 'Otros Ingresos y Egresos'),
(5, 0, '39', 'Costo de Ventas'), (5, 0, '40', 'Gastos de Comercialización'), (5, 0, '41', 'Gastos de Administración'),
(5, 0, '42', 'Otros Gastos'), (5, 0, '43', 'Impuesto a las Ganancias'), (5, 0, '44', 'Resultados de Operaciones Discontinuadas');

-- Plan mínimo para registrar asientos, compras y ventas con IVA.
INSERT INTO cuentas (codigo, grupo_id, tipo, rubro_id, numero_cuenta, nombre, saldo, tipo_saldo) VALUES
('1.1.01.01', 1, 1, 1, 1, 'Caja', 0, 'D'),
('1.1.01.02', 1, 1, 1, 2, 'Bancos', 0, 'D'),
('1.1.02.01', 1, 1, 2, 1, 'Créditos por Ventas', 0, 'D'),
('1.1.02.02', 1, 1, 2, 2, 'IVA Crédito Fiscal', 0, 'D'),
('2.1.05.01', 2, 1, 5, 1, 'Proveedores', 0, 'A'),
('2.1.05.02', 2, 1, 5, 2, 'IVA Débito Fiscal', 0, 'A'),
('4.0.09.01', 4, 0, 9, 1, 'Ventas', 0, 'A'),
('5.0.10.01', 5, 0, 10, 1, 'Compras', 0, 'D');

-- ------------------------------------------------------------
-- ASIENTOS (cabecera del Libro Diario)
-- numero: número correlativo visible para el usuario (1, 2, 3...)
-- ------------------------------------------------------------
CREATE TABLE asientos (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    numero    INT NOT NULL,
    fecha     DATE NOT NULL,
    concepto  VARCHAR(200) NOT NULL,
    total_debe DECIMAL(11,2) NOT NULL DEFAULT 0,
    total_haber DECIMAL(11,2) NOT NULL DEFAULT 0,
    UNIQUE KEY uq_asiento_numero (numero)
);

-- ------------------------------------------------------------
-- ASIENTO_DETALLE (líneas del Debe/Haber de cada asiento)
-- Cada línea afecta una cuenta del Plan de Cuentas con un importe
-- en el Debe o en el Haber (partida doble: suma Debe = suma Haber).
-- ------------------------------------------------------------
CREATE TABLE asiento_detalle (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    asiento_id     INT NOT NULL,
    cuenta_codigo  VARCHAR(12) NOT NULL,
    debe           DECIMAL(11,2) NOT NULL DEFAULT 0,
    haber          DECIMAL(11,2) NOT NULL DEFAULT 0,
    orden          INT NOT NULL,
    FOREIGN KEY (asiento_id) REFERENCES asientos(id) ON DELETE CASCADE,
    FOREIGN KEY (cuenta_codigo) REFERENCES cuentas(codigo)
);

-- ------------------------------------------------------------
-- VERIFICACIÓN
-- ------------------------------------------------------------
SHOW TABLES;
SELECT * FROM grupos;
SELECT * FROM rubros;
