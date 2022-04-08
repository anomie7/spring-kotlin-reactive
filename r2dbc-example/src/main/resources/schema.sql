CREATE TABLE item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    price FLOAT
);

CREATE TABLE cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY
);

CREATE TABLE cart_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quantity INTEGER,
    cart_id BIGINT,
    item_id BIGINT
);

INSERT INTO item(name, price) VALUES ('Alf alarm clock', 19.99);
INSERT INTO item(name, price) VALUES ('Smurf TV tray', 24.99);
INSERT INTO item(name, price) VALUES ('Mac Book air', 120.99);
INSERT INTO item(name, price) VALUES ('Ipad mini', 20.99);
INSERT INTO item(name, price) VALUES ('IPhone 13', 20.99);
INSERT INTO item(name, price) VALUES ('IPhone 15', 20.99);

INSERT INTO cart(id) VALUES (1);
INSERT INTO cart(id) VALUES (2);
INSERT INTO cart_item(quantity, cart_id, item_id) VALUES (4, 1, 1);
INSERT INTO cart_item(quantity, cart_id, item_id) VALUES (4, 1, 2);
INSERT INTO cart_item(quantity, cart_id, item_id) VALUES (4, 1, 5);

INSERT INTO cart_item(quantity, cart_id, item_id) VALUES (1, 2, 1);
INSERT INTO cart_item(quantity, cart_id, item_id) VALUES (2, 2, 3);
INSERT INTO cart_item(quantity, cart_id, item_id) VALUES (4, 2, 4);
INSERT INTO cart_item(quantity, cart_id, item_id) VALUES (5, 2, 6);