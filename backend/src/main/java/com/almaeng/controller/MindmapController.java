package com.almaeng.controller;

import com.almaeng.dto.MindmapRequest;
import com.almaeng.dto.MindmapResponse;
import com.almaeng.service.MindmapService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mindmaps")
public class MindmapController {

    private final MindmapService mindmapService;

    public MindmapController(MindmapService mindmapService) {
        this.mindmapService = mindmapService;
    }

    /**
     * 새로운 개념지도를 생성하고 DB에 저장한다.
     */
    @PostMapping
    public ResponseEntity<MindmapResponse> createMindmap(
            @Valid @RequestBody MindmapRequest request
    ) {
        return ResponseEntity.ok(mindmapService.createMindmap(request));
    }

    /**
     * 저장된 개념지도와 노드/관계를 조회한다.
     */
    @GetMapping("/{mapId}")
    public ResponseEntity<MindmapResponse> getMindmap(
            @PathVariable long mapId
    ) {
        return ResponseEntity.ok(mindmapService.getMindmap(mapId));
    }
}
