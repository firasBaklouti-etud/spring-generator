package com.firas.generator.controller;

import com.firas.generator.model.AI.AIGeneratedTables;
import com.firas.generator.model.AI.AIGeneratedTablesRequest;
import com.firas.generator.service.AIGeneratedTablesService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            System.out.println("request   :"+request);
            AIGeneratedTables response = aiGeneratedTablesService.generateTables(request);
            System.out.println("response   :"+response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Return error response
            return ResponseEntity.internalServerError().body(
                    new AIGeneratedTables(request.getSessionId(), null, "Error: " + e.getMessage())
            );
        }
    }
}