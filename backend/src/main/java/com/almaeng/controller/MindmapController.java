
package com.almaeng.controller;

import com.almaeng.dto.MindmapRequest;
import com.almaeng.dto.MindmapResponse;
import com.almaeng.service.MindmapService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mindmaps")
public class MindmapController {

    private final MindmapService mindmapService;

    public MindmapController(MindmapService mindmapService) {
        this.mindmapService = mindmapService;
    }

    @PostMapping
    public ResponseEntity<MindmapResponse> createMindmap(
            @Valid @RequestBody MindmapRequest request) {

        MindmapResponse response =
                mindmapService.createMindmap(request);

        return ResponseEntity.ok(response);
    }
}
