package com.almaeng.service;

import com.almaeng.client.AiClient;
import com.almaeng.dto.MindmapRequest;
import com.almaeng.dto.MindmapResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class MindmapServiceTest {

    private MindmapService createMockModeService() {
        ObjectProvider<AiClient> provider = mock(ObjectProvider.class);
        return new MindmapService(provider, false);
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 5, 8})
    void createsRequestedNumberOfNodes(int depth) {
        MindmapService service = createMockModeService();

        MindmapResponse response = service.createMindmap(
                new MindmapRequest("  광합성의 원리  ", depth)
        );

        assertEquals("광합성의 원리", response.sourceText());
        assertEquals(depth, response.depth());
        assertEquals("MOCK_GENERATED_NOT_SAVED", response.generationStatus());
        assertEquals(depth, response.nodes().size());
        assertEquals(depth - 1, response.edges().size());
        assertEquals("광합성의 원리", response.nodes().get(0).conceptName());
        assertEquals("UNCHECKED", response.nodes().get(0).learningStatus());
        assertNull(response.mapId());
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 4, 6, 7, 9})
    void rejectsUnsupportedDepth(int depth) {
        MindmapService service = createMockModeService();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createMindmap(new MindmapRequest("광합성", depth))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
