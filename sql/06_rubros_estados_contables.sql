-- Amplía el catálogo de rubros de exposición sin eliminar ni alterar datos.
-- Ejecutar una vez en instalaciones existentes antes de usar Estados Contables.
USE economia_db;

INSERT IGNORE INTO rubros (grupo_id, tipo, codigo, nombre) VALUES
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
