package com.cybersocial.graph.dto;

import com.cybersocial.graph.GraphNodeType;
import java.util.UUID;

public record GraphNodeResponse(
        UUID id,
        String label,
        GraphNodeType type
) {
}
