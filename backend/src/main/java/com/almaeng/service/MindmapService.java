package com.almaeng.service;

import com.almaeng.client.AiClient;
import com.almaeng.client.AiMindmapResult;
import com.almaeng.dto.MindmapRequest;
import com.almaeng.dto.MindmapResponse;
import com.almaeng.repository.MindmapRepository;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class MindmapService {

    private static final List<Integer> ALLOWED_DEPTHS = List.of(3, 5, 8);

    private final ObjectProvider<AiClient> aiClientProvider;
    private final boolean aiEnabled;
    private final MindmapRepository mindmapRepository;

    public MindmapService(
            ObjectProvider<AiClient> aiClientProvider,
            @Value("${almaeng.ai.enabled:false}") boolean aiEnabled,
            MindmapRepository mindmapRepository
    ) {
        this.aiClientProvider = aiClientProvider;
        this.aiEnabled = aiEnabled;
        this.mindmapRepository = mindmapRepository;
    }

    public MindmapResponse createMindmap(MindmapRequest request) {
        validateDepth(request.depth());

        String sourceText = request.sourceText().strip();

        AiMindmapResult result;
        if (aiEnabled) {
            AiClient aiClient = aiClientProvider.getIfAvailable();

            if (aiClient == null) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "AI 클라이언트를 사용할 수 없습니다."
                );
            }

            result = aiClient.generate(sourceText, request.depth());
        } else {
            result = createMockResult(sourceText, request.depth());
        }

        // 생성된 개념, 노드 관계를 DB에 저장한 뒤 실제 PK가 포함된 응답을 반환한다.
        return mindmapRepository.save(sourceText, request.depth(), result);
    }

    public MindmapResponse getMindmap(long mapId) {
        if (mapId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "개념지도 ID는 양수여야 합니다."
            );
        }

        return mindmapRepository.findById(mapId);
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
     * AI 키 없이 DB 저장 및 API 연동을 확인하기 위한 임시 데이터.
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
