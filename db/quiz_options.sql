CREATE TABLE quiz_options (
    option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    option_text VARCHAR(255) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,

    UNIQUE (quiz_id, option_id),

    FOREIGN KEY (quiz_id)
        REFERENCES quiz_questions(quiz_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;
