package com.almaeng.service;

import com.almaeng.dto.QuizAttemptRequest;
import com.almaeng.dto.QuizAttemptResponse;
import com.almaeng.dto.QuizQuestionResponse;
import com.almaeng.repository.QuizRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class QuizService {

    private final QuizRepository quizRepository;

    public QuizService(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
    }

    public List<QuizQuestionResponse> getQuizzes(long nodeId) {
        validateId(nodeId, "노드");
        return quizRepository.findByNodeId(nodeId);
    }

    public QuizAttemptResponse submitAnswer(
            long questionId,
            QuizAttemptRequest request
    ) {
        validateId(questionId, "문제");

        if (request == null || request.selectedOptionId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "선택한 답안 ID가 필요합니다."
            );
        }

        validateId(request.selectedOptionId(), "선택지");

        return quizRepository.submit(
                questionId,
                request.selectedOptionId()
        );
    }

    private void validateId(long id, String type) {
        if (id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    type + " ID는 양수여야 합니다."
            );
        }
    }
}
