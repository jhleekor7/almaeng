-- 알맹이 MVP 데이터베이스 스키마
-- 대상: MySQL 8.0.16+
-- 실행 순서: schema.sql 실행 후 sample_data.sql 실행
-- 로그인/회원 기능은 아직 확정되지 않아 users 테이블은 포함하지 않았습니다.

CREATE DATABASE IF NOT EXISTS almaeng
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE almaeng;

CREATE TABLE IF NOT EXISTS knowledge_maps (
    map_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_text TEXT NOT NULL COMMENT '사용자가 입력한 학습 문장 또는 개념',
    requested_node_count TINYINT UNSIGNED NOT NULL DEFAULT 5,
    generation_status ENUM('PENDING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (map_id),
    CONSTRAINT chk_map_node_count CHECK (requested_node_count BETWEEN 1 AND 30)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS map_nodes (
    node_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    map_id BIGINT UNSIGNED NOT NULL,
    concept_name VARCHAR(200) NOT NULL,
    description TEXT NULL,
    learning_status ENUM('UNCHECKED', 'UNDERSTOOD', 'NEEDS_REVIEW') NOT NULL DEFAULT 'UNCHECKED',
    display_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    position_x DECIMAL(8,2) NULL COMMENT '레이아웃을 DB에 보존할 경우 사용; 아니면 NULL',
    position_y DECIMAL(8,2) NULL COMMENT '레이아웃을 DB에 보존할 경우 사용; 아니면 NULL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (node_id),
    UNIQUE KEY uq_map_node_name (map_id, concept_name),
    KEY idx_nodes_map_order (map_id, display_order),
    CONSTRAINT fk_nodes_map
        FOREIGN KEY (map_id) REFERENCES knowledge_maps(map_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_node_positions CHECK (
        (position_x IS NULL AND position_y IS NULL)
        OR (position_x IS NOT NULL AND position_y IS NOT NULL)
    )
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS map_edges (
    edge_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    map_id BIGINT UNSIGNED NOT NULL,
    source_node_id BIGINT UNSIGNED NOT NULL,
    target_node_id BIGINT UNSIGNED NOT NULL,
    relationship_label VARCHAR(120) NULL COMMENT '예: 선행 개념, 구성 요소, 원인-결과',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (edge_id),
    UNIQUE KEY uq_edge_direction (source_node_id, target_node_id),
    KEY idx_edges_map (map_id),
    CONSTRAINT fk_edges_map
        FOREIGN KEY (map_id) REFERENCES knowledge_maps(map_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_edges_source_node
        FOREIGN KEY (source_node_id) REFERENCES map_nodes(node_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_edges_target_node
        FOREIGN KEY (target_node_id) REFERENCES map_nodes(node_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_edge_not_self CHECK (source_node_id <> target_node_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_questions (
    question_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    node_id BIGINT UNSIGNED NOT NULL,
    question_text TEXT NOT NULL,
    explanation TEXT NULL,
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL DEFAULT 'MEDIUM',
    display_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (question_id),
    KEY idx_questions_node_order (node_id, display_order),
    CONSTRAINT fk_questions_node
        FOREIGN KEY (node_id) REFERENCES map_nodes(node_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_options (
    option_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    question_id BIGINT UNSIGNED NOT NULL,
    option_text VARCHAR(1000) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    display_order TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (option_id),
    UNIQUE KEY uq_question_option_order (question_id, display_order),
    KEY idx_options_question (question_id),
    CONSTRAINT fk_options_question
        FOREIGN KEY (question_id) REFERENCES quiz_questions(question_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS quiz_attempts (
    attempt_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    question_id BIGINT UNSIGNED NOT NULL,
    selected_option_id BIGINT UNSIGNED NOT NULL,
    is_correct BOOLEAN NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attempt_id),
    KEY idx_attempts_question_time (question_id, submitted_at),
    CONSTRAINT fk_attempts_question
        FOREIGN KEY (question_id) REFERENCES quiz_questions(question_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_attempts_selected_option
        FOREIGN KEY (selected_option_id) REFERENCES quiz_options(option_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 참고:
-- 1) map_edges의 두 노드가 동일한 map_id에 속하는지는 애플리케이션/API 계층에서도 검증해야 합니다.
-- 2) quiz_attempts의 selected_option_id가 해당 question_id에 속하는지와
--    is_correct 값의 일치 여부도 API 계층에서 검증해야 합니다.
-- 3) 노드의 학습 상태는 퀴즈 제출 결과를 반영해 API에서 갱신합니다.
