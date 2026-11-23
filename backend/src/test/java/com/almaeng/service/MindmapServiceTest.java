package com.almaeng.service;

import com.almaeng.client.AiClient;
import com.almaeng.dto.MindmapRequest;
import com.almaeng.dto.MindmapResponse;
import com.almaeng.repository.MindmapRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MindmapServiceTest {

    private MindmapService createMockModeService(MindmapRepository repository) {
        ObjectProvider<AiClient> provider = mock(ObjectProvider.class);
        return new MindmapService(provider, false, repository);
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 5, 8})
    void createsMapWithRequestedNodeCount(int depth) {
        MindmapRepository repository = mock(MindmapRepository.class);
        MindmapResponse expected = new MindmapResponse(
                101L, "광합성의 원리", depth, "COMPLETED", java.util.List.of(), java.util.List.of()
        );
        when(repository.save(anyString(), anyInt(), any())).thenReturn(expected);

        MindmapService service = createMockModeService(repository);

        MindmapResponse response = service.createMindmap(
                new MindmapRequest("  광합성의 원리  ", depth)
        );

        assertEquals(101L, response.mapId());
        assertEquals("광합성의 원리", response.sourceText());
        assertEquals(depth, response.depth());
        assertEquals("COMPLETED", response.generationStatus());
        verify(repository).save(anyString(), anyInt(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 4, 6, 7, 9})
    void rejectsUnsupportedDepthWithoutSaving(int depth) {
        MindmapRepository repository = mock(MindmapRepository.class);
        MindmapService service = createMockModeService(repository);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createMindmap(new MindmapRequest("광합성", depth))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(repository, never()).save(anyString(), anyInt(), any());
    }

    @Test
    void retrievesStoredMapById() {
        MindmapRepository repository = mock(MindmapRepository.class);
        MindmapResponse expected = new MindmapResponse(
                7L, "정렬 알고리즘", 3, "COMPLETED", java.util.List.of(), java.util.List.of()
        );
        when(repository.findById(7L)).thenReturn(expected);

        MindmapService service = createMockModeService(repository);
        MindmapResponse actual = service.getMindmap(7L);

        assertNotNull(actual);
        assertEquals(7L, actual.mapId());
        verify(repository).findById(7L);
    }
}
