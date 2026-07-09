package com.cybersocial.demo;

import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.demo.dto.DemoUserGenerationRequest;
import com.cybersocial.demo.dto.DemoUserGenerationResponse;
import com.cybersocial.demo.dto.PropagationSimulationRequest;
import com.cybersocial.demo.dto.PropagationSimulationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DemoPropagationController {

    private final DemoPropagationService demoPropagationService;

    public DemoPropagationController(DemoPropagationService demoPropagationService) {
        this.demoPropagationService = demoPropagationService;
    }

    @PostMapping("/users/generate")
    public ResponseEntity<ApiResponse<DemoUserGenerationResponse>> generateUsers(
            @Valid @RequestBody DemoUserGenerationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Demo users generated", demoPropagationService.generateDemoUsers(request.count())));
    }

    @PostMapping("/propagation/simulate")
    public ResponseEntity<ApiResponse<PropagationSimulationResponse>> simulatePropagation(
            @Valid @RequestBody PropagationSimulationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Demo propagation simulated", demoPropagationService.simulatePropagation(request)));
    }
}
