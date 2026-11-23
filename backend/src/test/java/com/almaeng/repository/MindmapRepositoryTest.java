package com.almaeng.repository;

import com.almaeng.client.AiMindmapResult;
import com.almaeng.dto.MindmapResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MindmapRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private MindmapRepository repository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:almaeng_" + System.nanoTime()
                        + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "sa",
                ""
        );

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/schema-h2.sql")
        );
        populator.execute(dataSource);

        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new MindmapRepository(jdbcTemplate);
    }

    @Test
    void savesAndLoadsMapWithGeneratedDatabaseIds() {
        AiMindmapResult generated = new AiMindmapResult(
                List.of(
                        new AiMindmapResult.Node("광합성의 원리", "식물이 빛 에너지를 화학 에너지로 바꾸는 과정"),
                        new AiMindmapResult.Node("빛 에너지", "광합성에 필요한 에너지원"),
                        new AiMindmapResult.Node("엽록체", "광합성이 일어나는 세포 소기관")
                ),
                List.of(
                        new AiMindmapResult.Edge("광합성의 원리", "빛 에너지", "에너지원"),
                        new AiMindmapResult.Edge("광합성의 원리", "엽록체", "발생 장소")
                )
        );

        MindmapResponse saved = repository.save("광합성의 원리", 3, generated);

        assertNotNull(saved.mapId());
        assertEquals("COMPLETED", saved.generationStatus());
        assertEquals(3, saved.nodes().size());
        assertEquals(2, saved.edges().size());
        assertNotNull(saved.nodes().get(0).nodeId());
        assertNotNull(saved.edges().get(0).edgeId());

        MindmapResponse loaded = repository.findById(saved.mapId());

        assertEquals(saved.mapId(), loaded.mapId());
        assertEquals("광합성의 원리", loaded.sourceText());
        assertEquals(3, loaded.depth());
        assertEquals("COMPLETED", loaded.generationStatus());
        assertEquals("광합성의 원리", loaded.nodes().get(0).conceptName());
        assertEquals("엽록체", loaded.nodes().get(2).conceptName());
        assertEquals(
                saved.edges().get(0).sourceNodeId(),
                loaded.edges().get(0).sourceNodeId()
        );
    }

    @Test
    void rejectsInvalidEdgeBeforeWritingMap() {
        AiMindmapResult generated = new AiMindmapResult(
                List.of(
                        new AiMindmapResult.Node("주제", "설명"),
                        new AiMindmapResult.Node("하위 개념", "설명"),
                        new AiMindmapResult.Node("또 다른 개념", "설명")
                ),
                List.of(
                        new AiMindmapResult.Edge("주제", "존재하지 않는 개념", "잘못된 연결")
                )
        );

        assertThrows(
                ResponseStatusException.class,
                () -> repository.save("주제", 3, generated)
        );

        Integer mapCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_maps",
                Integer.class
        );
        assertEquals(0, mapCount);
    }

    @Test
    void returnsNotFoundForUnknownMapId() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> repository.findById(9999L)
        );

        assertEquals(404, exception.getStatusCode().value());
    }
}
