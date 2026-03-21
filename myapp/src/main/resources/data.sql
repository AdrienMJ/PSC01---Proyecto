--Aqui podemos ir añadiendo datos de prueba hasta implemnetar la persistencia
--Esto se hará para no arrastrar datos de prueba basura a la base de datos real

MERGE INTO usuarios (username, email, password) KEY(email) VALUES ('Prueba', 'test@mail.com', '000');
MERGE INTO usuarios (username, email, password) KEY(email) VALUES ('Adrien', 'adrien@mail.com', '123');
MERGE INTO usuarios (username, email, password) KEY(email) VALUES ('Adrien', 'adrien@gmail.com', '123');

