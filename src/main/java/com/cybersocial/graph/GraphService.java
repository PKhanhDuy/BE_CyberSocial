package com.cybersocial.graph;

import com.cybersocial.graph.dto.GraphEdgeResponse;
import com.cybersocial.graph.dto.GraphNodeResponse;
import java.util.List;

public interface GraphService {

    List<GraphNodeResponse> getNodes();

    List<GraphEdgeResponse> getEdges();
}
