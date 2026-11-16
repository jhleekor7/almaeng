package com.almaeng.client;

import java.util.List;

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
