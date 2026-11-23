package com.almaeng.dto;

import java.util.List;

/**
 * Android 앱에 반환하는 개념지도 데이터.
 * DB 저장 기능이 연결되기 전까지 mapId는 null이며 nodeId/edgeId는 응답 내 임시 ID다.
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
