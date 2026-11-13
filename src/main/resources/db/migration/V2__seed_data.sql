INSERT INTO categories (name) VALUES
    ('Electronics'),
    ('Books'),
    ('Clothing');

INSERT INTO products (name, description, price, stock_quantity, image_url, category_id) VALUES
    ('Wireless Headphones', 'Over-ear Bluetooth headphones with 30h battery life.', 199.99, 50, 'https://example.com/img/headphones.jpg', 1),
    ('Mechanical Keyboard', 'Hot-swappable mechanical keyboard with RGB lighting.', 149.50, 30, 'https://example.com/img/keyboard.jpg', 1),
    ('Clean Code', 'Robert C. Martin''s classic book on writing maintainable software.', 32.99, 100, 'https://example.com/img/clean-code.jpg', 2),
    ('The Pragmatic Programmer', 'Your journey to mastery, 20th anniversary edition.', 39.99, 80, 'https://example.com/img/pragmatic.jpg', 2),
    ('Cotton T-Shirt', '100% organic cotton t-shirt, unisex fit.', 19.99, 200, 'https://example.com/img/tshirt.jpg', 3),
    ('Denim Jacket', 'Classic blue denim jacket with button closure.', 89.99, 40, 'https://example.com/img/denim.jpg', 3);
