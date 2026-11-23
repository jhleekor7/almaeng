package com.almaeng.dto;

import java.util.List;

/**
 * 퀴즈 문제 조회 응답.
 * 정답 여부(is_correct)는 클라이언트에 전달하지 않는다.
 */
public record QuizQuestionResponse(
        Long questionId,
        Long nodeId,
        String questionText,
        String difficulty,
        Integer displayOrder,
        List<Option> options
) {
    public record Option(
            Long optionId,
            String optionText,
            Integer displayOrder
    ) {
    }
}
