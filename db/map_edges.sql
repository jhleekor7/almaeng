CREATE TABLE map_edges (
    edge_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    map_id BIGINT NOT NULL,
    source_node_id BIGINT NOT NULL,
    target_node_id BIGINT NOT NULL,
    relationship VARCHAR(50),

    UNIQUE (map_id, source_node_id, target_node_id),

    FOREIGN KEY (map_id)
        REFERENCES knowledge_maps(map_id)
        ON DELETE CASCADE,

    FOREIGN KEY (map_id, source_node_id)
        REFERENCES map_nodes(map_id, node_id)
        ON DELETE CASCADE,

    FOREIGN KEY (map_id, target_node_id)
        REFERENCES map_nodes(map_id, node_id)
        ON DELETE CASCADE,

    CHECK (source_node_id <> target_node_id)
) ENGINE=InnoDB;
