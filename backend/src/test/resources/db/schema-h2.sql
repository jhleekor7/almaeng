DROP TABLE IF EXISTS map_edges;
DROP TABLE IF EXISTS map_nodes;
DROP TABLE IF EXISTS knowledge_maps;

CREATE TABLE knowledge_maps (
    map_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_text CLOB NOT NULL,
    requested_node_count SMALLINT NOT NULL DEFAULT 5,
    generation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE map_nodes (
    node_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    map_id BIGINT NOT NULL,
    concept_name VARCHAR(200) NOT NULL,
    description CLOB,
    learning_status VARCHAR(20) NOT NULL DEFAULT 'UNCHECKED',
    display_order SMALLINT NOT NULL DEFAULT 0,
    position_x DECIMAL(8,2),
    position_y DECIMAL(8,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_map_node_name UNIQUE (map_id, concept_name),
    CONSTRAINT fk_nodes_map FOREIGN KEY (map_id)
        REFERENCES knowledge_maps(map_id) ON DELETE CASCADE
);

CREATE TABLE map_edges (
    edge_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    map_id BIGINT NOT NULL,
    source_node_id BIGINT NOT NULL,
    target_node_id BIGINT NOT NULL,
    relationship_label VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_edge_direction UNIQUE (source_node_id, target_node_id),
    CONSTRAINT fk_edges_map FOREIGN KEY (map_id)
        REFERENCES knowledge_maps(map_id) ON DELETE CASCADE,
    CONSTRAINT fk_edges_source_node FOREIGN KEY (source_node_id)
        REFERENCES map_nodes(node_id) ON DELETE CASCADE,
    CONSTRAINT fk_edges_target_node FOREIGN KEY (target_node_id)
        REFERENCES map_nodes(node_id) ON DELETE CASCADE,
    CONSTRAINT chk_edge_not_self CHECK (source_node_id <> target_node_id)
);
