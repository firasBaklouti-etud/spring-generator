package com.firas.generator.controller;

import com.firas.generator.model.AI.AIGeneratedTables;
import com.firas.generator.model.AI.AIGeneratedTablesRequest;
import com.firas.generator.service.AIGeneratedTablesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AIController {

    @Autowired
    private AIGeneratedTablesService aiGeneratedTablesService;

    @PostMapping("/generateTables")
    public ResponseEntity<AIGeneratedTables> generateTables(@RequestBody AIGeneratedTablesRequest request) {
        try {
            log.debug("AI generate tables request: {}", request);
            AIGeneratedTables response = aiGeneratedTablesService.generateTables(request);
            log.debug("AI generate tables response: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Return error response
            return ResponseEntity.internalServerError().body(
                    new AIGeneratedTables(request.getSessionId(), null, "Error: " + e.getMessage())
            );
        }
    }
}