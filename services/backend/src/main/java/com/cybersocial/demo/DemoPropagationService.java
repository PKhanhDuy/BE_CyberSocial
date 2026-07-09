package com.cybersocial.demo;

import com.cybersocial.demo.dto.DemoUserGenerationResponse;
import com.cybersocial.demo.dto.PropagationSimulationRequest;
import com.cybersocial.demo.dto.PropagationSimulationResponse;

public interface DemoPropagationService {

    DemoUserGenerationResponse generateDemoUsers(Integer count);

    PropagationSimulationResponse simulatePropagation(PropagationSimulationRequest request);
}
