package com.almaeng.client;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

@ConditionalOnProperty(prefix = "almaeng.ai", name = "enabled", havingValue = "true")
@Component
public class AiClient {

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String model;

    public AiClient(
            RestClient.Builder restClientBuilder,
            @Value("${almaeng.ai.base-url}") String baseUrl,
            @Value("${almaeng.ai.api-key:}") String apiKey,
            @Value("${almaeng.ai.model:}") String model,
            JsonMapper jsonMapper
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();

        this.apiKey = apiKey;
        this.model = model;
        this.jsonMapper = jsonMapper;
    }

    public AiMindmapResult generate(String sourceText, int nodeCount) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI API 키가 설정되지 않았습니다."
            );
        }

        if (model == null || model.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 모델이 설정되지 않았습니다."
            );
        }

        String systemPrompt = """
                당신은 학습 개념 마인드맵을 만드는 교육용 AI입니다.
                사용자가 입력한 주제를 바탕으로 개념 간 관계를 구성하세요.

                반드시 JSON 객체만 반환하세요.
                마크다운 코드 블록이나 JSON 외의 설명은 출력하지 마세요.

                다음 형식을 지키세요.
                {
                  "nodes": [
                    {
                      "conceptName": "개념 이름",
                      "description": "개념에 대한 간결한 설명"
                    }
                  ],
                  "edges": [
                    {
                      "sourceConcept": "출발 개념 이름",
                      "targetConcept": "도착 개념 이름",
                      "relationshipLabel": "두 개념의 관계"
                    }
                  ]
                }

                노드는 정확히 요청된 개수만 생성하세요.
                모든 관계의 sourceConcept과 targetConcept은
                nodes에 실제 존재하는 conceptName이어야 합니다.
                중복된 개념 이름은 만들지 마세요.
                """;

        String userPrompt = """
                학습 주제: %s
                생성할 개념 노드 수: %d개

                위 주제에 적합한 개념 마인드맵을 생성하세요.
                """.formatted(sourceText, nodeCount);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new IllegalStateException("AI 응답이 비어 있습니다.");
            }

            String content = response
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("");

            if (content.isBlank()) {
                throw new IllegalStateException("AI 응답에 생성 결과가 없습니다.");
            }

            return parseResult(content, nodeCount);

        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI API 호출에 실패했습니다."
            );
        } catch (JacksonException | IllegalStateException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI 응답을 마인드맵 데이터로 변환하지 못했습니다."
            );
        }
    }

    private AiMindmapResult parseResult(
            String content,
            int expectedNodeCount
    ) {

        JsonNode root = jsonMapper.readTree(content);
        JsonNode nodesJson = root.path("nodes");
        JsonNode edgesJson = root.path("edges");

        if (!nodesJson.isArray()) {
            throw new IllegalStateException("nodes 배열이 없습니다.");
        }

        List<AiMindmapResult.Node> nodes = new ArrayList<>();
        Set<String> conceptNames = new HashSet<>();

        for (JsonNode node : nodesJson) {
            String name = node.path("conceptName").asText("").strip();
            String description = node.path("description").asText("").strip();

            if (name.isBlank() || !conceptNames.add(name)) {
                throw new IllegalStateException("개념 이름이 비어 있거나 중복됩니다.");
            }

            nodes.add(new AiMindmapResult.Node(name, description));
        }

        if (nodes.size() != expectedNodeCount) {
            throw new IllegalStateException("생성된 개념 노드 수가 요청과 다릅니다.");
        }

        List<AiMindmapResult.Edge> edges = new ArrayList<>();

        if (edgesJson.isArray()) {
            for (JsonNode edge : edgesJson) {
                String source = edge.path("sourceConcept").asText("").strip();
                String target = edge.path("targetConcept").asText("").strip();
                String label = edge.path("relationshipLabel").asText("").strip();

                if (source.isBlank()
                        || target.isBlank()
                        || source.equals(target)
                        || !conceptNames.contains(source)
                        || !conceptNames.contains(target)) {
                    throw new IllegalStateException("노드 연결 정보가 올바르지 않습니다.");
                }

                edges.add(new AiMindmapResult.Edge(source, target, label));
            }
        }

        return new AiMindmapResult(nodes, edges);
    }
}
