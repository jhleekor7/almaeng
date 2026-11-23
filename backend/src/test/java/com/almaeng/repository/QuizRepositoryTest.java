package com.almaeng.repository;

import com.almaeng.dto.QuizAttemptResponse;
import com.almaeng.dto.QuizQuestionResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuizRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private QuizRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:quiz_" + System.nanoTime()
                        + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );

        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new QuizRepository(jdbcTemplate);

        jdbcTemplate.execute("""
                CREATE TABLE map_nodes (
                    node_id BIGINT PRIMARY KEY,
                    learning_status VARCHAR(30) NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE quiz_questions (
                    question_id BIGINT PRIMARY KEY,
                    node_id BIGINT NOT NULL,
                    question_text VARCHAR(500) NOT NULL,
                    explanation VARCHAR(1000),
                    difficulty VARCHAR(20) NOT NULL,
                    display_order INT NOT NULL,
                    FOREIGN KEY (node_id) REFERENCES map_nodes(node_id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE quiz_options (
                    option_id BIGINT PRIMARY KEY,
                    question_id BIGINT NOT NULL,
                    option_text VARCHAR(255) NOT NULL,
                    is_correct BOOLEAN NOT NULL,
                    display_order INT NOT NULL,
                    FOREIGN KEY (question_id)
                        REFERENCES quiz_questions(question_id)
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE quiz_attempts (
                    attempt_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    question_id BIGINT NOT NULL,
                    selected_option_id BIGINT NOT NULL,
                    is_correct BOOLEAN NOT NULL,
                    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (question_id)
                        REFERENCES quiz_questions(question_id),
                    FOREIGN KEY (selected_option_id)
                        REFERENCES quiz_options(option_id)
                )
                """);

        // 노드 21에 두 개의 퀴즈 문제를 연결한다.
        jdbcTemplate.update(
                "INSERT INTO map_nodes VALUES (?, ?)",
                21L, "UNCHECKED"
        );

        jdbcTemplate.update(
                """
                INSERT INTO quiz_questions
                    VALUES (?, ?, ?, ?, ?, ?)
                """,
                11L, 21L, "첫 번째 문제", "첫 문제 해설", "EASY", 1
        );

        jdbcTemplate.update(
                """
                INSERT INTO quiz_questions
                    VALUES (?, ?, ?, ?, ?, ?)
                """,
                12L, 21L, "두 번째 문제", "둘째 문제 해설", "MEDIUM", 2
        );

        // 111, 121은 정답이고 112, 122는 오답이다.
        insertOption(111L, 11L, "첫 문제 정답", true, 1);
        insertOption(112L, 11L, "첫 문제 오답", false, 2);
        insertOption(121L, 12L, "둘째 문제 정답", true, 1);
        insertOption(122L, 12L, "둘째 문제 오답", false, 2);
    }

    private void insertOption(
            long optionId,
            long questionId,
            String text,
            boolean correct,
            int displayOrder
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO quiz_options
                    (option_id, question_id, option_text, is_correct, display_order)
                VALUES (?, ?, ?, ?, ?)
                """,
                optionId, questionId, text, correct, displayOrder
        );
    }

    @Test
    void returnsQuestionsAndOptionsWithoutCorrectAnswerFlag() {
        List<QuizQuestionResponse> questions =
                repository.findByNodeId(21L);

        assertEquals(2, questions.size());
        assertEquals(11L, questions.get(0).questionId());
        assertEquals(2, questions.get(0).options().size());
        assertEquals("첫 문제 정답",
                questions.get(0).options().get(0).optionText());

        List<String> responseFields =
                java.util.Arrays.stream(
                        QuizQuestionResponse.Option.class.getRecordComponents()
                ).map(java.lang.reflect.RecordComponent::getName).toList();

        assertFalse(responseFields.contains("isCorrect"));
    }

    @Test
    void updatesLearningStatusUsingLatestAttemptOfEachQuestion() {
        // 첫 번째 문제만 정답: 아직 미응답 문제가 있으므로 UNCHECKED
        QuizAttemptResponse first = repository.submit(11L, 111L);

        assertTrue(first.correct());
        assertEquals("UNCHECKED", first.learningStatus());

        // 두 번째 문제 오답: NEEDS_REVIEW
        QuizAttemptResponse secondWrong = repository.submit(12L, 122L);

        assertFalse(secondWrong.correct());
        assertEquals("NEEDS_REVIEW", secondWrong.learningStatus());

        // 두 번째 문제를 재시도해 정답: 모든 최신 답안이 정답
        QuizAttemptResponse secondCorrect = repository.submit(12L, 121L);

        assertTrue(secondCorrect.correct());
        assertEquals("UNDERSTOOD", secondCorrect.learningStatus());

        // 첫 번째 문제를 다시 틀리면 NEEDS_REVIEW로 변경
        QuizAttemptResponse firstWrong = repository.submit(11L, 112L);

        assertFalse(firstWrong.correct());
        assertEquals("NEEDS_REVIEW", firstWrong.learningStatus());

        Integer attemptCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quiz_attempts",
                Integer.class
        );

        assertEquals(4, attemptCount);
    }

    @Test
    void rejectsOptionBelongingToAnotherQuestion() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> repository.submit(11L, 121L)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        Integer attemptCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM quiz_attempts",
                Integer.class
        );

        assertEquals(0, attemptCount);
    }

    @Test
    void returnsNotFoundForUnknownNode() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> repository.findByNodeId(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void returnsNotFoundForUnknownQuestion() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> repository.submit(999L, 111L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
