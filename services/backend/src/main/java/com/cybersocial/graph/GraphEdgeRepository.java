package com.cybersocial.graph;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraphEdgeRepository extends JpaRepository<GraphEdge, UUID> {
}
