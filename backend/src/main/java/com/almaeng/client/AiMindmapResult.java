package com.almaeng.client;

import java.util.List;

/**
 * AI 응답을 서비스 계층에서 사용하기 위한 내부 데이터 모델.
 */
public record AiMindmapResult(
        List<Node> nodes,
        List<Edge> edges
) {
    public record Node(
            String conceptName,
            String description
    ) {
    }

    public record Edge(
            String sourceConcept,
            String targetConcept,
            String relationshipLabel
    ) {
    }
}
