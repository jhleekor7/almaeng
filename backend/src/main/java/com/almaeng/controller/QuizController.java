package com.almaeng.controller;

import com.almaeng.dto.QuizAttemptRequest;
import com.almaeng.dto.QuizAttemptResponse;
import com.almaeng.dto.QuizQuestionResponse;
import com.almaeng.service.QuizService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    /**
     * 특정 학습 노드에 연결된 퀴즈 목록을 조회한다.
     */
    @GetMapping("/nodes/{nodeId}/quizzes")
    public ResponseEntity<List<QuizQuestionResponse>> getQuizzes(
            @PathVariable long nodeId
    ) {
        return ResponseEntity.ok(
                quizService.getQuizzes(nodeId)
        );
    }

    /**
     * 퀴즈 답안을 제출하고 서버의 채점 결과를 반환한다.
     */
    @PostMapping("/quizzes/{questionId}/attempts")
    public ResponseEntity<QuizAttemptResponse> submitAnswer(
            @PathVariable long questionId,
            @Valid @RequestBody QuizAttemptRequest request
    ) {
        return ResponseEntity.ok(
                quizService.submitAnswer(questionId, request)
        );
    }
}
