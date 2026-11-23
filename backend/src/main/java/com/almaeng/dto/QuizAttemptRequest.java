package com.almaeng.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 사용자가 선택한 답안을 서버로 전달하는 요청 DTO.
 */
public record QuizAttemptRequest(

        @NotNull(message = "선택한 답안 ID가 필요합니다.")
        @Positive(message = "선택지 ID는 양수여야 합니다.")
        Long selectedOptionId

) {
}
