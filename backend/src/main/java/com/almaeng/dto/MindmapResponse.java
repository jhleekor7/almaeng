
package com.almaeng.dto;

import java.util.List;

/**
 * Android에 반환하는 마인드맵 데이터
 */
public record MindmapResponse(

        Long mapId,

        String sourceText,

        Integer depth,

        String generationStatus,

        List<Node> nodes,

        List<Edge> edges

) {

    /**
     * 마인드맵 개념 노드
     */
    public record Node(
            Long nodeId,
            String conceptName,
            String description,
            String learningStatus
    ) {
    }

    /**
     * 노드 사이의 연결 관계
     */
    public record Edge(
            Long edgeId,
            Long sourceNodeId,
            Long targetNodeId,
            String relationshipLabel
    ) {
    }
}
