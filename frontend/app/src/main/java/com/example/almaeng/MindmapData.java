package com.example.almaeng;

import java.util.List;

/**
 * Backend API에서 반환한 개념지도 데이터.
 */
public class MindmapData {

    private final String sourceText;
    private final int depth;
    private final String generationStatus;
    private final List<Node> nodes;
    private final List<Edge> edges;

    public MindmapData(
            String sourceText,
            int depth,
            String generationStatus,
            List<Node> nodes,
            List<Edge> edges
    ) {
        this.sourceText = sourceText;
        this.depth = depth;
        this.generationStatus = generationStatus;
        this.nodes = nodes;
        this.edges = edges;
    }

    public String getSourceText() {
        return sourceText;
    }

    public int getDepth() {
        return depth;
    }

    public String getGenerationStatus() {
        return generationStatus;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public static class Node {
        private final long nodeId;
        private final String conceptName;
        private final String description;
        private final String learningStatus;

        public Node(long nodeId, String conceptName, String description, String learningStatus) {
            this.nodeId = nodeId;
            this.conceptName = conceptName;
            this.description = description;
            this.learningStatus = learningStatus;
        }

        public long getNodeId() {
            return nodeId;
        }

        public String getConceptName() {
            return conceptName;
        }

        public String getDescription() {
            return description;
        }

        public String getLearningStatus() {
            return learningStatus;
        }
    }

    public static class Edge {
        private final long edgeId;
        private final long sourceNodeId;
        private final long targetNodeId;
        private final String relationshipLabel;

        public Edge(
                long edgeId,
                long sourceNodeId,
                long targetNodeId,
                String relationshipLabel
        ) {
            this.edgeId = edgeId;
            this.sourceNodeId = sourceNodeId;
            this.targetNodeId = targetNodeId;
            this.relationshipLabel = relationshipLabel;
        }

        public long getEdgeId() {
            return edgeId;
        }

        public long getSourceNodeId() {
            return sourceNodeId;
        }

        public long getTargetNodeId() {
            return targetNodeId;
        }

        public String getRelationshipLabel() {
            return relationshipLabel;
        }
    }
}
