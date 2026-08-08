package com.cybersocial.ai;

import com.cybersocial.ai.RiskLevel;
import com.cybersocial.ai.dto.AIAnalysisRequest;
import com.cybersocial.ai.dto.AIAnalysisResponse;
import com.cybersocial.ai.dto.EventAttributionResponse;
import com.cybersocial.ai.dto.PropagationTimelineEventResponse;
import com.cybersocial.ai.dto.InteractionEventDto;
import com.cybersocial.ai.propagation.PropagationEventBuilder;
import com.cybersocial.common.exception.BadRequestException;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.post.Post;
import com.cybersocial.post.PostRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AIAnalysisServiceImpl implements AIAnalysisService {

    private final RestClient restClient;
    private final PostRepository postRepository;
    private final PropagationEventBuilder propagationEventBuilder;

    public AIAnalysisServiceImpl(
            RestClient.Builder restClientBuilder,
            PostRepository postRepository,
            PropagationEventBuilder propagationEventBuilder,
            AiAnalysisProperties aiAnalysisProperties,
            AiServiceProperties aiServiceProperties
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(aiAnalysisProperties.timeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        RestClient.Builder clientBuilder = restClientBuilder
                .baseUrl(aiServiceProperties.url())
                .requestFactory(requestFactory);
        if (aiServiceProperties.hasApiKey()) {
            clientBuilder = clientBuilder.defaultHeader("X-API-Key", aiServiceProperties.apiKey());
        }

        this.restClient = clientBuilder.build();
        this.postRepository = postRepository;
        this.propagationEventBuilder = propagationEventBuilder;
    }

    @Override
    public AIAnalysisResponse analyze(AIAnalysisRequest request) {
        if (request.postId() == null && (request.text() == null || request.text().isBlank())) {
            throw new BadRequestException("Text is required when postId is not provided");
        }

        String text = request.text() == null ? "" : request.text();
        long articleId = 0L;
        List<InteractionEventDto> events = List.of();

        if (request.postId() != null) {
            Post post = postRepository.findByIdWithAuthor(request.postId())
                    .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
            text = post.getContent();
            articleId = PropagationEventBuilder.stableArticleId(post.getId());
            events = propagationEventBuilder.build(post);
        }

        AIServiceResponse response = restClient.post()
                .uri("/analyze")
                .body(new AIServiceRequest(text, articleId, events, request.includeXai()))
                .retrieve()
                .body(AIServiceResponse.class);

        if (response == null) {
            throw new IllegalStateException("AI service returned an empty response");
        }

        return new AIAnalysisResponse(
                response.fakeProbability(),
                response.explanation(),
                response.headline(),
                response.narrative(),
                response.contextHints() == null ? List.of() : response.contextHints(),
                response.riskLevel(),
                response.label(),
                response.threshold(),
                response.eventCount(),
                response.mode(),
                response.graphContribution(),
                mapEventAttributions(response.eventAttributions()),
                mapPropagationTimeline(response.propagationTimeline())
        );
    }

    private record AIServiceRequest(
            String text,
            long articleId,
            List<InteractionEventDto> events,
            boolean includeXai
    ) {
    }

    private record EventAttributionPayload(
            int eventIndex,
            String eventType,
            String eventTypeLabel,
            String relativeTime,
            String actorLabel,
            Double tigeRemoval,
            Double confidenceDrop,
            Double conditionalTige,
            String summary,
            String impactLevel
    ) {
    }

    private record PropagationTimelinePayload(
            int eventIndex,
            String eventId,
            String parentEventId,
            Integer depth,
            String relativeTime,
            String eventType,
            String eventTypeLabel,
            String actorLabel,
            Double tigeRemoval,
            Double conditionalTige,
            Boolean isInfluential
    ) {
    }

    private record AIServiceResponse(
            double fakeProbability,
            String explanation,
            String headline,
            String narrative,
            List<String> contextHints,
            RiskLevel riskLevel,
            String label,
            Double threshold,
            Integer eventCount,
            String mode,
            Double graphContribution,
            List<EventAttributionPayload> eventAttributions,
            List<PropagationTimelinePayload> propagationTimeline
    ) {
    }

    private List<EventAttributionResponse> mapEventAttributions(List<EventAttributionPayload> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return List.of();
        }
        return payloads.stream()
                .map(payload -> new EventAttributionResponse(
                        payload.eventIndex(),
                        payload.eventType(),
                        payload.eventTypeLabel(),
                        payload.relativeTime(),
                        payload.actorLabel(),
                        payload.tigeRemoval(),
                        payload.confidenceDrop(),
                        payload.conditionalTige(),
                        payload.summary(),
                        payload.impactLevel()
                ))
                .toList();
    }

    private List<PropagationTimelineEventResponse> mapPropagationTimeline(List<PropagationTimelinePayload> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return List.of();
        }
        return payloads.stream()
                .map(payload -> new PropagationTimelineEventResponse(
                        payload.eventIndex(),
                        payload.eventId(),
                        payload.parentEventId(),
                        payload.depth() == null ? 0 : payload.depth(),
                        payload.relativeTime(),
                        payload.eventType(),
                        payload.eventTypeLabel(),
                        payload.actorLabel(),
                        payload.tigeRemoval(),
                        payload.conditionalTige(),
                        Boolean.TRUE.equals(payload.isInfluential())
                ))
                .toList();
    }
}
