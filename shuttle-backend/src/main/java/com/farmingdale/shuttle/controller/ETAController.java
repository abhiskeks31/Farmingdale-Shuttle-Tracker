package com.farmingdale.shuttle.controller;

import com.farmingdale.shuttle.dto.ETAResponse;
import com.farmingdale.shuttle.service.ETAService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/eta")
public class ETAController {

    private final ETAService etaService;

    public ETAController(ETAService etaService) {
        this.etaService = etaService;
    }

    // Public endpoint for the shuttle's next-stop ETA.
    @GetMapping("/shuttle/{shuttleId}")
    public ETAResponse getETA(@PathVariable Long shuttleId) {
        return etaService.getETA(shuttleId);
    }
}
