CREATE TABLE map_nodes (
    node_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    map_id BIGINT NOT NULL,
    node_name VARCHAR(100) NOT NULL,
    description TEXT,
    node_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    position_x FLOAT DEFAULT 0,
    position_y FLOAT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (map_id, node_id),

    FOREIGN KEY (map_id)
        REFERENCES knowledge_maps(map_id)
        ON DELETE CASCADE,

    CHECK (node_type IN ('CORE', 'SUB')),
    CHECK (status IN ('UNKNOWN', 'UNDERSTOOD', 'REVIEW'))
);