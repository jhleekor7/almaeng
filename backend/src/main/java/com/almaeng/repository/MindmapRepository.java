package com.almaeng.repository;

import com.almaeng.client.AiMindmapResult;
import com.almaeng.dto.MindmapResponse;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * knowledge_maps, map_nodes, map_edges 테이블에 대한 JDBC 접근 계층.
 */
@Repository
public class MindmapRepository {

    private static final String COMPLETED = "COMPLETED";
    private static final String UNCHECKED = "UNCHECKED";

    private final JdbcTemplate jdbcTemplate;

    public MindmapRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 지도, 노드, 관계를 하나의 트랜잭션으로 저장한다.
     * 중간 단계에서 실패하면 전체 저장 작업이 롤백된다.
     */
    @Transactional
    public MindmapResponse save(
            String sourceText,
            int requestedNodeCount,
            AiMindmapResult result
    ) {
        validateResult(result, requestedNodeCount);

        long mapId = insertAndReturnId(
                """
                INSERT INTO knowledge_maps
                    (source_text, requested_node_count, generation_status)
                VALUES (?, ?, 'PENDING')
                """,
                statement -> {
                    statement.setString(1, sourceText);
                    statement.setInt(2, requestedNodeCount);
                }
        );

        Map<String, Long> nodeIds = new HashMap<>();
        List<AiMindmapResult.Node> generatedNodes = result.nodes();

        for (int i = 0; i < generatedNodes.size(); i++) {
            AiMindmapResult.Node node = generatedNodes.get(i);
            int displayOrder = i + 1;

            long nodeId = insertAndReturnId(
                    """
                    INSERT INTO map_nodes
                        (map_id, concept_name, description, learning_status, display_order)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    statement -> {
                        statement.setLong(1, mapId);
                        statement.setString(2, node.conceptName());
                        statement.setString(3, node.description());
                        statement.setString(4, UNCHECKED);
                        statement.setInt(5, displayOrder);
                    }
            );

            nodeIds.put(node.conceptName(), nodeId);
        }

        List<AiMindmapResult.Edge> generatedEdges = result.edges();
        for (AiMindmapResult.Edge edge : generatedEdges) {
            Long sourceNodeId = nodeIds.get(edge.sourceConcept());
            Long targetNodeId = nodeIds.get(edge.targetConcept());

            jdbcTemplate.update(
                    """
                    INSERT INTO map_edges
                        (map_id, source_node_id, target_node_id, relationship_label)
                    VALUES (?, ?, ?, ?)
                    """,
                    mapId,
                    sourceNodeId,
                    targetNodeId,
                    edge.relationshipLabel()
            );
        }

        jdbcTemplate.update(
                "UPDATE knowledge_maps SET generation_status = ? WHERE map_id = ?",
                COMPLETED,
                mapId
        );

        return findById(mapId);
    }

    /**
     * 지도 ID를 기준으로 지도, 노드, 관계를 다시 조회한다.
     */
    public MindmapResponse findById(long mapId) {
        MapRow map = findMapRow(mapId);

        List<MindmapResponse.Node> nodes = jdbcTemplate.query(
                """
                SELECT node_id, concept_name, description, learning_status
                FROM map_nodes
                WHERE map_id = ?
                ORDER BY display_order, node_id
                """,
                nodeRowMapper(),
                mapId
        );

        List<MindmapResponse.Edge> edges = jdbcTemplate.query(
                """
                SELECT edge_id, source_node_id, target_node_id, relationship_label
                FROM map_edges
                WHERE map_id = ?
                ORDER BY edge_id
                """,
                edgeRowMapper(),
                mapId
        );

        return new MindmapResponse(
                map.mapId(),
                map.sourceText(),
                map.requestedNodeCount(),
                map.generationStatus(),
                nodes,
                edges
        );
    }

    private MapRow findMapRow(long mapId) {
        List<MapRow> rows = jdbcTemplate.query(
                """
                SELECT map_id, source_text, requested_node_count, generation_status
                FROM knowledge_maps
                WHERE map_id = ?
                """,
                (resultSet, rowNumber) -> new MapRow(
                        resultSet.getLong("map_id"),
                        resultSet.getString("source_text"),
                        resultSet.getInt("requested_node_count"),
                        resultSet.getString("generation_status")
                ),
                mapId
        );

        if (rows.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "해당 개념지도를 찾을 수 없습니다."
            );
        }

        return rows.get(0);
    }

    private RowMapper<MindmapResponse.Node> nodeRowMapper() {
        return (resultSet, rowNumber) -> new MindmapResponse.Node(
                resultSet.getLong("node_id"),
                resultSet.getString("concept_name"),
                resultSet.getString("description"),
                resultSet.getString("learning_status")
        );
    }

    private RowMapper<MindmapResponse.Edge> edgeRowMapper() {
        return (resultSet, rowNumber) -> new MindmapResponse.Edge(
                resultSet.getLong("edge_id"),
                resultSet.getLong("source_node_id"),
                resultSet.getLong("target_node_id"),
                resultSet.getString("relationship_label")
        );
    }

    private long insertAndReturnId(String sql, StatementBinder binder) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        PreparedStatementCreator creator = connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );
            binder.bind(statement);
            return statement;
        };

        jdbcTemplate.update(creator, keyHolder);

        Number generatedKey = keyHolder.getKey();
        if (generatedKey == null) {
            throw new IllegalStateException("DB에서 생성된 ID를 가져오지 못했습니다.");
        }

        return generatedKey.longValue();
    }

    /**
     * DB에 저장하기 전에 AI 결과의 노드 수와 관계 참조를 검사한다.
     */
    private void validateResult(AiMindmapResult result, int requestedNodeCount) {
        if (result == null || result.nodes() == null || result.edges() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "개념지도 생성 결과가 올바르지 않습니다."
            );
        }

        if (result.nodes().size() != requestedNodeCount) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "생성된 개념 노드 수가 요청한 깊이와 다릅니다."
            );
        }

        Set<String> conceptNames = new HashSet<>();
        for (AiMindmapResult.Node node : result.nodes()) {
            if (node == null
                    || node.conceptName() == null
                    || node.conceptName().isBlank()
                    || !conceptNames.add(node.conceptName())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "개념 이름이 비어 있거나 중복됩니다."
                );
            }
        }

        Set<EdgeKey> edgeKeys = new HashSet<>();
        for (AiMindmapResult.Edge edge : result.edges()) {
            if (edge == null
                    || edge.sourceConcept() == null
                    || edge.targetConcept() == null
                    || edge.relationshipLabel() == null
                    || edge.relationshipLabel().isBlank()
                    || edge.sourceConcept().equals(edge.targetConcept())
                    || !conceptNames.contains(edge.sourceConcept())
                    || !conceptNames.contains(edge.targetConcept())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "노드 연결 정보가 올바르지 않습니다."
                );
            }

            EdgeKey key = new EdgeKey(edge.sourceConcept(), edge.targetConcept());
            if (!edgeKeys.add(key)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "중복된 노드 연결 정보가 있습니다."
                );
            }
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    private record EdgeKey(String sourceConcept, String targetConcept) {
    }

    private record MapRow(
            long mapId,
            String sourceText,
            int requestedNodeCount,
            String generationStatus
    ) {
    }
}
