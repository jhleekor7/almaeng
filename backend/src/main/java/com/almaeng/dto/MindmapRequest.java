package com.almaeng.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Android 앱에서 전달하는 개념지도 생성 요청.
 */
public record MindmapRequest(

        @NotBlank(message = "학습 주제를 입력해야 합니다.")
        @Size(max = 500, message = "학습 주제는 500자 이하여야 합니다.")
        String sourceText,

        @NotNull(message = "개념 깊이를 선택해야 합니다.")
        @Min(value = 3, message = "깊이는 최소 3이어야 합니다.")
        @Max(value = 8, message = "깊이는 최대 8이어야 합니다.")
        Integer depth

) {
}
