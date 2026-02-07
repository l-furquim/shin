CREATE TABLE IF NOT EXISTS videos_categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    image_url VARCHAR(255)
);