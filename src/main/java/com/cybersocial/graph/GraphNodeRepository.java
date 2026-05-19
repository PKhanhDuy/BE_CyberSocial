package com.cybersocial.graph;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraphNodeRepository extends JpaRepository<GraphNode, UUID> {
}
