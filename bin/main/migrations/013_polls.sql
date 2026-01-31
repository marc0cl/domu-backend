-- Polls feature: votaciones con opciones y registro de votos

CREATE TABLE IF NOT EXISTS polls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    title VARCHAR(180) NOT NULL,
    description TEXT,
    closes_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL,
    CONSTRAINT fk_polls_building FOREIGN KEY (building_id) REFERENCES buildings(id),
    CONSTRAINT fk_polls_user FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_polls_status CHECK (status IN ('OPEN','CLOSED')),
    KEY idx_polls_status_building (status, building_id, closes_at)
);

CREATE TABLE IF NOT EXISTS poll_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    poll_id BIGINT NOT NULL,
    label VARCHAR(200) NOT NULL,
    votes INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_poll_options_poll FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE,
    KEY idx_poll_options_poll (poll_id)
);

CREATE TABLE IF NOT EXISTS poll_votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    poll_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    voted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_poll_votes_poll FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_votes_option FOREIGN KEY (option_id) REFERENCES poll_options(id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_votes_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_poll_vote UNIQUE (poll_id, user_id),
    KEY idx_poll_votes_poll (poll_id),
    KEY idx_poll_votes_option (option_id)
);
