package com.cybersocial.graph;

import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.graph.dto.GraphEdgeResponse;
import com.cybersocial.graph.dto.GraphNodeResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/nodes")
    public ResponseEntity<ApiResponse<List<GraphNodeResponse>>> nodes() {
        return ResponseEntity.ok(ApiResponse.success(graphService.getNodes()));
    }

    @GetMapping("/edges")
    public ResponseEntity<ApiResponse<List<GraphEdgeResponse>>> edges() {
        return ResponseEntity.ok(ApiResponse.success(graphService.getEdges()));
    }
}
