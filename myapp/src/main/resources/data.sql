--Aqui podemos ir añadiendo datos de prueba hasta implemnetar la persistencia
--Esto se hará para no arrastrar datos de prueba basura a la base de datos real

MERGE INTO usuarios (username, email, password) KEY(email) VALUES ('Prueba', 'test@mail.com', '000');
MERGE INTO usuarios (username, email, password) KEY(email) VALUES ('Adrien', 'adrien@mail.com', '123');
MERGE INTO usuarios (username, email, password) KEY(email) VALUES ('Adrien', 'adrien@gmail.com', '123');
MERGE INTO usuarios (username, email, password) KEY(email) VALUES ('Aimar', 'aimar@gmail.com', '123');

ALTER TABLE IF EXISTS gastos ADD COLUMN IF NOT EXISTS categoria VARCHAR(20);
ALTER TABLE IF EXISTS gastos ADD COLUMN IF NOT EXISTS emote VARCHAR(8);
UPDATE gastos SET categoria = 'OTROS' WHERE categoria IS NULL;
UPDATE gastos SET emote = '🧾' WHERE emote IS NULL OR TRIM(emote) = '';
