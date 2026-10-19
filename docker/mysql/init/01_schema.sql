-- Esquema inicial para el contenedor MySQL. Docker crea la base indicada por
-- MYSQL_DATABASE antes de ejecutar este archivo.

CREATE TABLE grupos (
    id      INT PRIMARY KEY,
    nombre  VARCHAR(50) NOT NULL
);

CREATE TABLE rubros (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    grupo_id  INT NOT NULL,
    tipo      INT NOT NULL,
    codigo    VARCHAR(2) NOT NULL,
    nombre    VARCHAR(100) NOT NULL,
    orden_exposicion INT NOT NULL DEFAULT 99,
    FOREIGN KEY (grupo_id) REFERENCES grupos(id),
    UNIQUE KEY uq_rubro_codigo (codigo)
);

CREATE TABLE cuentas (
    codigo         VARCHAR(12) PRIMARY KEY,
    grupo_id       INT NOT NULL,
    tipo           INT NOT NULL,
    rubro_id       INT NOT NULL,
    numero_cuenta  INT NOT NULL,
    nombre         VARCHAR(40) NOT NULL,
    saldo          DECIMAL(11,2) NOT NULL DEFAULT 0,
    tipo_saldo     CHAR(1) NOT NULL,
    FOREIGN KEY (grupo_id) REFERENCES grupos(id),
    FOREIGN KEY (rubro_id) REFERENCES rubros(id)
);

CREATE TABLE asientos (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    numero    INT NOT NULL,
    fecha     DATE NOT NULL,
    concepto  VARCHAR(200) NOT NULL,
    total_debe DECIMAL(11,2) NOT NULL DEFAULT 0,
    total_haber DECIMAL(11,2) NOT NULL DEFAULT 0,
    UNIQUE KEY uq_asiento_numero (numero)
);

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

CREATE TABLE iva_compras (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    asiento_id         INT NOT NULL,
    fecha              DATE NOT NULL,
    nro_comprobante    VARCHAR(20) NOT NULL,
    tipo_comprobante   VARCHAR(40) NOT NULL,
    cuit               VARCHAR(13) NOT NULL,
    razon_social       VARCHAR(100) NOT NULL,
    condicion_iva      VARCHAR(30) NOT NULL DEFAULT '',
    neto_gravado       DECIMAL(11,2) NOT NULL,
    neto_no_gravado    DECIMAL(14,2) NOT NULL DEFAULT 0,
    exento             DECIMAL(14,2) NOT NULL DEFAULT 0,
    alicuota           DECIMAL(5,2) NOT NULL DEFAULT 0,
    iva                DECIMAL(11,2) NOT NULL,
    total              DECIMAL(11,2) NOT NULL,
    punto_venta        INT NOT NULL DEFAULT 0,
    dni                VARCHAR(8) NOT NULL DEFAULT '',
    otros_percepciones DECIMAL(11,2) NOT NULL DEFAULT 0,
    horas              DECIMAL(11,2) NOT NULL DEFAULT 0,
    FOREIGN KEY (asiento_id) REFERENCES asientos(id)
);

CREATE TABLE iva_ventas (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    asiento_id         INT NOT NULL,
    fecha              DATE NOT NULL,
    nro_comprobante    VARCHAR(20) NOT NULL,
    tipo_comprobante   VARCHAR(40) NOT NULL,
    cuit               VARCHAR(13) NOT NULL,
    razon_social       VARCHAR(100) NOT NULL,
    condicion_iva      VARCHAR(30) NOT NULL DEFAULT '',
    neto_gravado       DECIMAL(11,2) NOT NULL,
    neto_no_gravado    DECIMAL(14,2) NOT NULL DEFAULT 0,
    exento             DECIMAL(14,2) NOT NULL DEFAULT 0,
    alicuota           DECIMAL(5,2) NOT NULL DEFAULT 0,
    iva                DECIMAL(11,2) NOT NULL,
    total              DECIMAL(11,2) NOT NULL,
    punto_venta        INT NOT NULL DEFAULT 0,
    dni                VARCHAR(8) NOT NULL DEFAULT '',
    otros_percepciones DECIMAL(11,2) NOT NULL DEFAULT 0,
    horas              DECIMAL(11,2) NOT NULL DEFAULT 0,
    FOREIGN KEY (asiento_id) REFERENCES asientos(id)
);

INSERT INTO grupos (id, nombre) VALUES
    (1, 'Activo'), (2, 'Pasivo'), (3, 'Patrimonio Neto'),
    (4, 'Ingresos'), (5, 'Egresos');

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
    (1, 1, '11', 'Inversiones'),
    (1, 1, '12', 'Otros Créditos'),
    (1, 1, '13', 'Otros Activos'),
    (1, 2, '14', 'Créditos por Ventas'),
    (1, 2, '15', 'Otros Créditos'),
    (1, 2, '16', 'Bienes de Cambio'),
    (1, 2, '17', 'Participaciones Permanentes en Sociedades'),
    (1, 2, '18', 'Otras Inversiones'),
    (1, 2, '19', 'Activos Intangibles'),
    (1, 2, '20', 'Otros Activos'),
    (2, 1, '21', 'Préstamos'),
    (2, 1, '22', 'Remuneraciones y Cargas Sociales'),
    (2, 1, '23', 'Cargas Fiscales'),
    (2, 1, '24', 'Anticipos de Clientes'),
    (2, 1, '25', 'Dividendos a Pagar'),
    (2, 1, '26', 'Otras Deudas'),
    (2, 2, '27', 'Deudas Comerciales'),
    (2, 2, '28', 'Préstamos'),
    (2, 2, '29', 'Previsiones'),
    (2, 2, '30', 'Otras Deudas'),
    (3, 0, '31', 'Capital'),
    (3, 0, '32', 'Aportes Irrevocables'),
    (3, 0, '33', 'Reservas'),
    (4, 0, '34', 'Resultados por Tenencia'),
    (4, 0, '35', 'Resultados de Inversiones en Entes Relacionados'),
    (4, 0, '36', 'Resultados de Otras Inversiones'),
    (4, 0, '37', 'Resultados Financieros y por Tenencia'),
    (4, 0, '38', 'Otros Ingresos y Egresos'),
    (5, 0, '39', 'Costo de Ventas'),
    (5, 0, '40', 'Gastos de Comercialización'),
    (5, 0, '41', 'Gastos de Administración'),
    (5, 0, '42', 'Otros Gastos'),
    (5, 0, '43', 'Impuesto a las Ganancias'),
    (5, 0, '44', 'Resultados de Operaciones Discontinuadas');

-- Plan mínimo para poder registrar asientos, compras y ventas con IVA desde
-- la primera ejecución. El usuario puede ampliarlo desde Plan de Cuentas.
INSERT INTO cuentas (codigo, grupo_id, tipo, rubro_id, numero_cuenta, nombre, saldo, tipo_saldo) VALUES
    ('1.1.01.01', 1, 1, 1, 1, 'Caja', 0, 'D'),
    ('1.1.01.02', 1, 1, 1, 2, 'Bancos', 0, 'D'),
    ('1.1.02.01', 1, 1, 2, 1, 'Créditos por Ventas', 0, 'D'),
    ('1.1.02.02', 1, 1, 2, 2, 'IVA Crédito Fiscal', 0, 'D'),
    ('2.1.05.01', 2, 1, 5, 1, 'Proveedores', 0, 'A'),
    ('2.1.05.02', 2, 1, 5, 2, 'IVA Débito Fiscal', 0, 'A'),
    ('4.0.09.01', 4, 0, 9, 1, 'Ventas', 0, 'A'),
    ('5.0.10.01', 5, 0, 10, 1, 'Compras', 0, 'D');

-- Orden de exposición RT 8 para los informes, separado del código técnico.
INSERT IGNORE INTO rubros (grupo_id, tipo, codigo, nombre) VALUES
(1, 2, '45', 'Propiedades de Inversión'), (2, 1, '46', 'IVA Débito Fiscal'), (2, 1, '47', 'IVA a Pagar'),
(2, 1, '48', 'Acreedores Varios'), (2, 1, '49', 'Deudas sin Documentar'), (2, 1, '50', 'Documentos a Pagar'),
(2, 1, '51', 'Sueldos a Pagar'), (2, 1, '52', 'Cargas Sociales a Pagar'),
(2, 2, '53', 'Remuneraciones y Cargas Sociales'), (2, 2, '54', 'Cargas Fiscales'),
(2, 2, '55', 'Anticipos de Clientes'), (2, 2, '56', 'Dividendos a Pagar'), (2, 2, '57', 'Otras Deudas'), (3, 0, '58', 'Reserva Legal');
UPDATE rubros SET nombre = CASE codigo WHEN '11' THEN 'Inversiones Temporarias' WHEN '05' THEN 'Proveedores'
WHEN '06' THEN 'Deudas Comerciales' WHEN '07' THEN 'Capital' WHEN '08' THEN 'Resultados No Asignados' ELSE nombre END;
UPDATE cuentas SET rubro_id = (SELECT id FROM rubros WHERE codigo = '46') WHERE codigo = '2.1.05.02';
UPDATE rubros SET orden_exposicion = CASE codigo
WHEN '01' THEN 1 WHEN '11' THEN 2 WHEN '02' THEN 3 WHEN '12' THEN 4 WHEN '03' THEN 5 WHEN '13' THEN 6
WHEN '14' THEN 1 WHEN '15' THEN 2 WHEN '16' THEN 3 WHEN '04' THEN 4 WHEN '45' THEN 5 WHEN '17' THEN 6 WHEN '18' THEN 7 WHEN '19' THEN 8 WHEN '20' THEN 9
WHEN '05' THEN 1 WHEN '48' THEN 2 WHEN '49' THEN 3 WHEN '50' THEN 4 WHEN '46' THEN 5 WHEN '47' THEN 6 WHEN '51' THEN 7 WHEN '52' THEN 8
WHEN '06' THEN 1 WHEN '28' THEN 2 WHEN '53' THEN 3 WHEN '54' THEN 4 WHEN '55' THEN 5 WHEN '56' THEN 6 WHEN '57' THEN 7
WHEN '07' THEN 1 WHEN '58' THEN 2 WHEN '08' THEN 3
WHEN '09' THEN 1 WHEN '34' THEN 2 WHEN '35' THEN 3 WHEN '36' THEN 4 WHEN '37' THEN 5 WHEN '38' THEN 6
WHEN '10' THEN 1 WHEN '39' THEN 2 WHEN '40' THEN 3 WHEN '41' THEN 4 WHEN '42' THEN 5 WHEN '43' THEN 6 WHEN '44' THEN 7 ELSE 99 END;
