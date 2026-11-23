package com.almaeng.dto;

/**
 * 답안 제출 결과.
 * 제출이 끝난 뒤 정답과 해설을 반환한다.
 */
public record QuizAttemptResponse(
        Long attemptId,
        Long questionId,
        Long selectedOptionId,
        boolean correct,
        Long correctOptionId,
        String correctOptionText,
        String explanation,
        String learningStatus
) {
}
