package com.cybersocial.message;

import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.message.dto.MessageConversationResponse;
import com.cybersocial.message.dto.MessageReactionRequest;
import com.cybersocial.message.dto.MessageReactionResponse;
import com.cybersocial.message.dto.MessageResponse;
import com.cybersocial.message.dto.MessageSendRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface MessageService {

    List<MessageConversationResponse> findConversations(UUID currentUserId);

    MessageConversationResponse getOrCreateConversation(UUID currentUserId, UUID friendId);

    PagedResponse<MessageResponse> findMessages(UUID currentUserId, UUID conversationId, Pageable pageable);

    MessageResponse sendMessage(UUID currentUserId, UUID conversationId, MessageSendRequest request);

    MessageReactionResponse react(UUID currentUserId, UUID messageId, MessageReactionRequest request);

    void deleteReaction(UUID currentUserId, UUID messageId);
}
