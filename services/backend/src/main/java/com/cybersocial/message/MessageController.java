package com.cybersocial.message;

import com.cybersocial.common.response.ApiResponse;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.common.util.SecurityUtils;
import com.cybersocial.message.dto.MessageConversationResponse;
import com.cybersocial.message.dto.MessageReactionRequest;
import com.cybersocial.message.dto.MessageReactionResponse;
import com.cybersocial.message.dto.MessageResponse;
import com.cybersocial.message.dto.MessageSendRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<MessageConversationResponse>>> conversations() {
        return ResponseEntity.ok(ApiResponse.success(messageService.findConversations(SecurityUtils.requireCurrentUserId())));
    }

    @PostMapping("/conversations/friends/{friendId}")
    public ResponseEntity<ApiResponse<MessageConversationResponse>> getOrCreateConversation(@PathVariable UUID friendId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conversation ready", messageService.getOrCreateConversation(SecurityUtils.requireCurrentUserId(), friendId)));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<PagedResponse<MessageResponse>>> messages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(messageService.findMessages(SecurityUtils.requireCurrentUserId(), conversationId, pageable)));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody MessageSendRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent", messageService.sendMessage(SecurityUtils.requireCurrentUserId(), conversationId, request)));
    }

    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<ApiResponse<MessageReactionResponse>> react(
            @PathVariable UUID messageId,
            @Valid @RequestBody MessageReactionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Message reaction saved", messageService.react(SecurityUtils.requireCurrentUserId(), messageId, request)));
    }

    @DeleteMapping("/{messageId}/reactions")
    public ResponseEntity<ApiResponse<Void>> deleteReaction(@PathVariable UUID messageId) {
        messageService.deleteReaction(SecurityUtils.requireCurrentUserId(), messageId);
        return ResponseEntity.ok(ApiResponse.success("Message reaction deleted", null));
    }
}
