CREATE TABLE quiz_questions (
    quiz_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    explanation TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (node_id)
        REFERENCES map_nodes(node_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;
