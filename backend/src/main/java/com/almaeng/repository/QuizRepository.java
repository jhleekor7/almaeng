package com.almaeng.repository;

import com.almaeng.dto.QuizAttemptResponse;
import com.almaeng.dto.QuizQuestionResponse;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class QuizRepository {

    private final JdbcTemplate jdbcTemplate;

    public QuizRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 노드에 연결된 퀴즈와 선택지를 조회한다.
     * 정답 여부는 응답에 포함하지 않는다.
     */
    public List<QuizQuestionResponse> findByNodeId(long nodeId) {
        Integer nodeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM map_nodes WHERE node_id = ?",
                Integer.class,
                nodeId
        );

        if (nodeCount == null || nodeCount == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "해당 학습 노드를 찾을 수 없습니다."
            );
        }

        Map<Long, QuestionBuilder> questions = new LinkedHashMap<>();

        jdbcTemplate.query(
                """
                SELECT
                    q.question_id,
                    q.node_id,
                    q.question_text,
                    q.difficulty,
                    q.display_order AS question_display_order,
                    o.option_id,
                    o.option_text,
                    o.display_order AS option_display_order
                FROM quiz_questions q
                LEFT JOIN quiz_options o
                    ON q.question_id = o.question_id
                WHERE q.node_id = ?
                ORDER BY
                    q.display_order,
                    q.question_id,
                    o.display_order,
                    o.option_id
                """,
                resultSet -> {
                    long questionId = resultSet.getLong("question_id");

                    QuestionBuilder question = questions.computeIfAbsent(
                            questionId,
                            id -> new QuestionBuilder(
                                    id,
                                    resultSetValue(resultSet, "node_id"),
                                    getString(resultSet, "question_text"),
                                    getString(resultSet, "difficulty"),
                                    getInteger(resultSet, "question_display_order")
                            )
                    );

                    long optionId = resultSet.getLong("option_id");

                    if (!resultSet.wasNull()) {
                        question.options().add(
                                new QuizQuestionResponse.Option(
                                        optionId,
                                        getString(resultSet, "option_text"),
                                        getInteger(resultSet, "option_display_order")
                                )
                        );
                    }
                },
                nodeId
        );

        return questions.values().stream()
                .map(question -> new QuizQuestionResponse(
                        question.questionId(),
                        question.nodeId(),
                        question.questionText(),
                        question.difficulty(),
                        question.displayOrder(),
                        question.options()
                ))
                .toList();
    }

    /**
     * 답안을 검증하고 풀이 기록과 노드의 학습 상태를 갱신한다.
     */
    @Transactional
    public QuizAttemptResponse submit(
            long questionId,
            long selectedOptionId
    ) {
        QuestionData question = findQuestion(questionId);

        // 해당 선택지가 실제로 이 문제에 속하는지 확인한다.
        List<OptionData> selectedOptions = jdbcTemplate.query(
                """
                SELECT option_id, option_text, is_correct
                FROM quiz_options
                WHERE question_id = ? AND option_id = ?
                """,
                (resultSet, rowNumber) -> new OptionData(
                        resultSet.getLong("option_id"),
                        resultSet.getString("option_text"),
                        resultSet.getBoolean("is_correct")
                ),
                questionId,
                selectedOptionId
        );

        if (selectedOptions.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "해당 문제에 속하지 않는 선택지입니다."
            );
        }

        OptionData selectedOption = selectedOptions.get(0);

        // 현재 퀴즈는 단일 정답 방식이므로 정답이 정확히 하나인지 확인한다.
        List<OptionData> correctOptions = jdbcTemplate.query(
                """
                SELECT option_id, option_text, is_correct
                FROM quiz_options
                WHERE question_id = ? AND is_correct = TRUE
                """,
                (resultSet, rowNumber) -> new OptionData(
                        resultSet.getLong("option_id"),
                        resultSet.getString("option_text"),
                        resultSet.getBoolean("is_correct")
                ),
                questionId
        );

        if (correctOptions.size() != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "퀴즈의 정답 구성이 올바르지 않습니다."
            );
        }

        OptionData correctOption = correctOptions.get(0);
        boolean isCorrect = selectedOption.optionId() == correctOption.optionId();

        // 서버가 판정한 정오답을 저장한다.
        long attemptId = insertAttempt(
                questionId,
                selectedOptionId,
                isCorrect
        );

        // 해당 노드의 전체 문제 풀이 상태를 기준으로 학습 상태를 갱신한다.
        String learningStatus = updateLearningStatus(question.nodeId());

        return new QuizAttemptResponse(
                attemptId,
                questionId,
                selectedOptionId,
                isCorrect,
                correctOption.optionId(),
                correctOption.optionText(),
                question.explanation(),
                learningStatus
        );
    }

    private QuestionData findQuestion(long questionId) {
        List<QuestionData> questions = jdbcTemplate.query(
                """
                SELECT question_id, node_id, explanation
                FROM quiz_questions
                WHERE question_id = ?
                """,
                (resultSet, rowNumber) -> new QuestionData(
                        resultSet.getLong("question_id"),
                        resultSet.getLong("node_id"),
                        resultSet.getString("explanation")
                ),
                questionId
        );

        if (questions.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "해당 퀴즈 문제를 찾을 수 없습니다."
            );
        }

        return questions.get(0);
    }

    private long insertAttempt(
            long questionId,
            long selectedOptionId,
            boolean isCorrect
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO quiz_attempts
                        (question_id, selected_option_id, is_correct)
                    VALUES (?, ?, ?)
                    """,
                    new String[]{"attempt_id"}
            );

            statement.setLong(1, questionId);
            statement.setLong(2, selectedOptionId);
            statement.setBoolean(3, isCorrect);

            return statement;
        }, keyHolder);

        Number generatedKey = keyHolder.getKey();

        if (generatedKey == null) {
            throw new IllegalStateException(
                    "퀴즈 풀이 기록 ID를 가져오지 못했습니다."
            );
        }

        return generatedKey.longValue();
    }

    /**
     * 각 문제의 가장 최근 답안을 기준으로 학습 상태를 결정한다.
     *
     * 모든 문제를 풀었고 모두 정답: UNDERSTOOD
     * 최근 답안 중 오답 존재: NEEDS_REVIEW
     * 아직 미응답 문제가 존재: UNCHECKED
     */
    private String updateLearningStatus(long nodeId) {
        List<Boolean> latestResults = jdbcTemplate.query(
                """
                SELECT
                    (
                        SELECT a.is_correct
                        FROM quiz_attempts a
                        WHERE a.question_id = q.question_id
                        ORDER BY a.submitted_at DESC, a.attempt_id DESC
                        LIMIT 1
                    ) AS latest_is_correct
                FROM quiz_questions q
                WHERE q.node_id = ?
                """,
                (resultSet, rowNumber) -> {
                    Object value = resultSet.getObject("latest_is_correct");

                    return value == null
                            ? null
                            : resultSet.getBoolean("latest_is_correct");
                },
                nodeId
        );

        boolean hasWrongAnswer = latestResults.stream()
                .anyMatch(result -> Boolean.FALSE.equals(result));

        boolean allAnsweredCorrectly = !latestResults.isEmpty()
                && latestResults.stream().allMatch(Boolean.TRUE::equals);

        String status;

        if (hasWrongAnswer) {
            status = "NEEDS_REVIEW";
        } else if (allAnsweredCorrectly) {
            status = "UNDERSTOOD";
        } else {
            status = "UNCHECKED";
        }

        jdbcTemplate.update(
                "UPDATE map_nodes SET learning_status = ? WHERE node_id = ?",
                status,
                nodeId
        );

        return status;
    }

    private static Long resultSetValue(
            java.sql.ResultSet resultSet,
            String column
    ) {
        try {
            return resultSet.getLong(column);
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String getString(
            java.sql.ResultSet resultSet,
            String column
    ) {
        try {
            return resultSet.getString(column);
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Integer getInteger(
            java.sql.ResultSet resultSet,
            String column
    ) {
        try {
            return resultSet.getInt(column);
        } catch (java.sql.SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record QuestionData(
            long questionId,
            long nodeId,
            String explanation
    ) {
    }

    private record OptionData(
            long optionId,
            String optionText,
            boolean isCorrect
    ) {
    }

    private record QuestionBuilder(
            long questionId,
            long nodeId,
            String questionText,
            String difficulty,
            int displayOrder,
            List<QuizQuestionResponse.Option> options
    ) {
        private QuestionBuilder(
                long questionId,
                long nodeId,
                String questionText,
                String difficulty,
                int displayOrder
        ) {
            this(
                    questionId,
                    nodeId,
                    questionText,
                    difficulty,
                    displayOrder,
                    new ArrayList<>()
            );
        }
    }
}
