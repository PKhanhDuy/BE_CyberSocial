package com.cybersocial.graph;

import com.cybersocial.graph.dto.GraphEdgeResponse;
import com.cybersocial.graph.dto.GraphNodeResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GraphServiceImpl implements GraphService {

    private static final UUID USER_NODE = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID TOPIC_NODE = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID POST_NODE = UUID.fromString("00000000-0000-0000-0000-000000000103");
    private static final UUID EDGE_AUTHORS = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID EDGE_RELATED = UUID.fromString("00000000-0000-0000-0000-000000000202");

    @Override
    public List<GraphNodeResponse> getNodes() {
        return List.of(
                new GraphNodeResponse(USER_NODE, "CyberSocial User", GraphNodeType.USER),
                new GraphNodeResponse(TOPIC_NODE, "Digital Trust", GraphNodeType.TOPIC),
                new GraphNodeResponse(POST_NODE, "Example Post", GraphNodeType.POST)
        );
    }

    @Override
    public List<GraphEdgeResponse> getEdges() {
        return List.of(
                new GraphEdgeResponse(EDGE_AUTHORS, USER_NODE, POST_NODE, GraphEdgeType.AUTHORS, 1.0),
                new GraphEdgeResponse(EDGE_RELATED, POST_NODE, TOPIC_NODE, GraphEdgeType.RELATED_TO, 0.84)
        );
    }
}
