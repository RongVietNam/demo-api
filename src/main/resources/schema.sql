CREATE TABLE products (
    product_id BIGINT PRIMARY KEY,
    gear_name VARCHAR(255),
    brand VARCHAR(255),
    categories VARCHAR(255),
    sub_categories VARCHAR(255),
    color VARCHAR(50),
    price DECIMAL(10, 2),
    num_images INT,
    short_description TEXT,
    description TEXT
);