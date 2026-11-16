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

        // 1. 깊이 검증
        List<Integer> allowedDepths = List.of(3, 5, 8);

        if (!allowedDepths.contains(request.depth())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "깊이는 3, 5, 8 중에서 선택해야 합니다."
            );
        }

        String sourceText = request.sourceText().strip();

        // 2. AI 활성화 여부에 따라 실행
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

        // 3. 개념 이름과 임시 ID 연결
        Map<String, Long> nodeIds = new LinkedHashMap<>();

        for (int i = 0; i < result.nodes().size(); i++) {
            nodeIds.put(
                    result.nodes().get(i).conceptName(),
                    (long) i + 1
            );
        }

        // 4. 노드 응답 생성
        List<MindmapResponse.Node> nodes = new ArrayList<>();

        for (AiMindmapResult.Node node : result.nodes()) {
            nodes.add(new MindmapResponse.Node(
                    nodeIds.get(node.conceptName()),
                    node.conceptName(),
                    node.description(),
                    "UNCHECKED"
            ));
        }

        // 5. 관계 응답 생성
        List<MindmapResponse.Edge> edges = new ArrayList<>();

        for (int i = 0; i < result.edges().size(); i++) {
            AiMindmapResult.Edge edge = result.edges().get(i);

            edges.add(new MindmapResponse.Edge(
                    (long) i + 1,
                    nodeIds.get(edge.sourceConcept()),
                    nodeIds.get(edge.targetConcept()),
                    edge.relationshipLabel()
            ));
        }

        // 6. DB 저장 없이 응답 반환
        return new MindmapResponse(
                null,
                sourceText,
                request.depth(),
                generationStatus,
                nodes,
                edges
        );
    }

    // Android 연동 테스트를 위한 임시 데이터
    private AiMindmapResult createMockResult(
            String sourceText,
            int nodeCount
    ) {
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
                    "Android와 Backend 연결을 확인하기 위한 임시 설명입니다."
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
