package com.cybersocial.graph.dto;

import com.cybersocial.graph.GraphEdgeType;
import java.util.UUID;

public record GraphEdgeResponse(
        UUID id,
        UUID sourceNodeId,
        UUID targetNodeId,
        GraphEdgeType type,
        double weight
) {
}
