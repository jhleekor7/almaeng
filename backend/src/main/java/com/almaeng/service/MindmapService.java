package com.almaeng.service;

import com.almaeng.client.AiClient;
import com.almaeng.client.AiMindmapResult;
import com.almaeng.dto.MindmapRequest;
import com.almaeng.dto.MindmapResponse;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MindmapService {

    private static final List<Integer> ALLOWED_DEPTHS = List.of(3, 5, 8);

    private final ObjectProvider<AiClient> aiClientProvider;
    private final boolean aiEnabled;

    public MindmapService(
            ObjectProvider<AiClient> aiClientProvider,
            @Value("${almaeng.ai.enabled:false}") boolean aiEnabled
    ) {
        this.aiClientProvider = aiClientProvider;
        this.aiEnabled = aiEnabled;
    }

    public MindmapResponse createMindmap(MindmapRequest request) {
        validateDepth(request.depth());

        String sourceText = request.sourceText().strip();

        AiMindmapResult result;
        String generationStatus;

        if (aiEnabled) {
            AiClient aiClient = aiClientProvider.getIfAvailable();

            if (aiClient == null) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "AI 클라이언트를 사용할 수 없습니다."
                );
            }

            result = aiClient.generate(sourceText, request.depth());
            generationStatus = "AI_GENERATED_NOT_SAVED";
        } else {
            result = createMockResult(sourceText, request.depth());
            generationStatus = "MOCK_GENERATED_NOT_SAVED";
        }

        Map<String, Long> nodeIds = new LinkedHashMap<>();
        for (int i = 0; i < result.nodes().size(); i++) {
            nodeIds.put(result.nodes().get(i).conceptName(), (long) i + 1);
        }

        List<MindmapResponse.Node> nodes = new ArrayList<>();
        for (AiMindmapResult.Node node : result.nodes()) {
            nodes.add(new MindmapResponse.Node(
                    nodeIds.get(node.conceptName()),
                    node.conceptName(),
                    node.description(),
                    "UNCHECKED"
            ));
        }

        List<MindmapResponse.Edge> edges = new ArrayList<>();
        for (int i = 0; i < result.edges().size(); i++) {
            AiMindmapResult.Edge edge = result.edges().get(i);
            Long sourceNodeId = nodeIds.get(edge.sourceConcept());
            Long targetNodeId = nodeIds.get(edge.targetConcept());

            if (sourceNodeId == null || targetNodeId == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "개념 연결 정보가 올바르지 않습니다."
                );
            }

            edges.add(new MindmapResponse.Edge(
                    (long) i + 1,
                    sourceNodeId,
                    targetNodeId,
                    edge.relationshipLabel()
            ));
        }

        return new MindmapResponse(
                null,
                sourceText,
                request.depth(),
                generationStatus,
                nodes,
                edges
        );
    }

    private void validateDepth(Integer depth) {
        if (depth == null || !ALLOWED_DEPTHS.contains(depth)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "깊이는 3, 5, 8 중에서 선택해야 합니다."
            );
        }
    }

    /**
     * AI 키 없이도 Android와 API의 데이터 연결을 확인할 수 있는 임시 응답.
     * 실제 개념 생성을 대체하지 않으며 generationStatus로 구분한다.
     */
    private AiMindmapResult createMockResult(String sourceText, int nodeCount) {
        List<AiMindmapResult.Node> nodes = new ArrayList<>();
        List<AiMindmapResult.Edge> edges = new ArrayList<>();

        nodes.add(new AiMindmapResult.Node(
                sourceText,
                "사용자가 입력한 학습 주제입니다."
        ));

        for (int i = 1; i < nodeCount; i++) {
            String conceptName = "테스트 개념 " + i;
            nodes.add(new AiMindmapResult.Node(
                    conceptName,
                    "백엔드와 Android 간 데이터 전달을 확인하기 위한 임시 설명입니다."
            ));
            edges.add(new AiMindmapResult.Edge(
                    sourceText,
                    conceptName,
                    "세부 개념"
            ));
        }

        return new AiMindmapResult(nodes, edges);
    }
}
