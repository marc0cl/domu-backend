CREATE TABLE IF NOT EXISTS forum_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    label VARCHAR(100) NOT NULL,
    icon VARCHAR(50)
);

INSERT INTO forum_categories (name, label, icon) VALUES 
('announcement', 'Anuncio', '📢'),
('alert', 'Alerta', '⚠️'),
('news', 'Noticia', '📰'),
('event', 'Evento', '📅')
ON DUPLICATE KEY UPDATE label=VALUES(label);

CREATE TABLE IF NOT EXISTS forum_threads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    pinned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    CONSTRAINT fk_threads_building FOREIGN KEY (building_id) REFERENCES buildings(id),
    CONSTRAINT fk_threads_category FOREIGN KEY (category_id) REFERENCES forum_categories(id),
    CONSTRAINT fk_threads_author FOREIGN KEY (author_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS forum_posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    thread_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_posts_thread FOREIGN KEY (thread_id) REFERENCES forum_threads(id),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(id)
);
