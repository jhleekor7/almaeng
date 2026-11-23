package com.almaeng.dto;

import java.util.List;

/**
 * DB에 저장된 개념지도와 구성 노드/관계를 반환하는 응답 DTO.
 */
public record MindmapResponse(
        Long mapId,
        String sourceText,
        Integer depth,
        String generationStatus,
        List<Node> nodes,
        List<Edge> edges
) {
    public record Node(
            Long nodeId,
            String conceptName,
            String description,
            String learningStatus
    ) {
    }

    public record Edge(
            Long edgeId,
            Long sourceNodeId,
            Long targetNodeId,
            String relationshipLabel
    ) {
    }
}
