package com.cybersocial.message;

import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.common.exception.BadRequestException;
import com.cybersocial.common.exception.ForbiddenOperationException;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.friend.Friendship;
import com.cybersocial.friend.FriendshipRepository;
import com.cybersocial.friend.FriendshipStatus;
import com.cybersocial.message.dto.MessageConversationResponse;
import com.cybersocial.message.dto.MessageReactionRequest;
import com.cybersocial.message.dto.MessageReactionResponse;
import com.cybersocial.message.dto.MessageResponse;
import com.cybersocial.message.dto.MessageSendRequest;
import com.cybersocial.user.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public MessageServiceImpl(
            MessageConversationRepository conversationRepository,
            MessageRepository messageRepository,
            MessageReactionRepository reactionRepository,
            FriendshipRepository friendshipRepository,
            UserRepository userRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.reactionRepository = reactionRepository;
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageConversationResponse> findConversations(UUID currentUserId) {
        return conversationRepository.findByParticipantOrderByUpdatedAtDesc(currentUserId).stream()
                .map(conversation -> MessageConversationResponse.from(
                        conversation,
                        currentUserId,
                        messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId()).orElse(null)
                ))
                .toList();
    }

    @Override
    @Transactional
    public MessageConversationResponse getOrCreateConversation(UUID currentUserId, UUID friendId) {
        ensureAcceptedFriendship(currentUserId, friendId);

        OrderedParticipants orderedParticipants = orderParticipants(currentUserId, friendId);
        MessageConversation conversation = conversationRepository
                .findByOrderedParticipants(orderedParticipants.userOneId(), orderedParticipants.userTwoId())
                .orElseGet(() -> conversationRepository.save(MessageConversation.builder()
                        .userOne(getUser(orderedParticipants.userOneId()))
                        .userTwo(getUser(orderedParticipants.userTwoId()))
                        .build()));

        return MessageConversationResponse.from(
                conversation,
                currentUserId,
                messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId()).orElse(null)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<MessageResponse> findMessages(UUID currentUserId, UUID conversationId, Pageable pageable) {
        MessageConversation conversation = getConversation(conversationId);
        ensureParticipant(currentUserId, conversation);

        Page<MessageResponse> page = messageRepository
                .findByConversationIdWithSenderOrderByCreatedAtDesc(conversationId, pageable)
                .map(this::toResponse);
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(UUID currentUserId, UUID conversationId, MessageSendRequest request) {
        MessageConversation conversation = getConversation(conversationId);
        ensureParticipant(currentUserId, conversation);
        User sender = getUser(currentUserId);

        NormalizedMessage normalized = normalizeMessage(request);
        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .sender(sender)
                .messageType(normalized.messageType())
                .content(normalized.content())
                .mediaUrl(normalized.mediaUrl())
                .linkUrl(normalized.linkUrl())
                .build());

        conversation.setUpdatedAt(Instant.now());
        return toResponse(message);
    }

    @Override
    @Transactional
    public MessageReactionResponse react(UUID currentUserId, UUID messageId, MessageReactionRequest request) {
        Message message = getMessage(messageId);
        ensureParticipant(currentUserId, message.getConversation());
        User user = getUser(currentUserId);
        String emoji = normalizeRequired(request.emoji(), "Emoji is required");

        MessageReaction reaction = reactionRepository.findByMessageIdAndUserId(messageId, currentUserId)
                .orElseGet(() -> MessageReaction.builder()
                        .message(message)
                        .user(user)
                        .build());
        reaction.setEmoji(emoji);
        return MessageReactionResponse.from(reactionRepository.save(reaction));
    }

    @Override
    @Transactional
    public void deleteReaction(UUID currentUserId, UUID messageId) {
        Message message = getMessage(messageId);
        ensureParticipant(currentUserId, message.getConversation());
        reactionRepository.findByMessageIdAndUserId(messageId, currentUserId)
                .ifPresent(reactionRepository::delete);
    }

    private MessageResponse toResponse(Message message) {
        List<MessageReactionResponse> reactions = reactionRepository
                .findByMessageIdWithUserOrderByCreatedAtAsc(message.getId())
                .stream()
                .map(MessageReactionResponse::from)
                .toList();
        return MessageResponse.from(message, reactions);
    }

    private NormalizedMessage normalizeMessage(MessageSendRequest request) {
        if (request.messageType() == null) {
            throw new BadRequestException("Message type is required");
        }

        return switch (request.messageType()) {
            case TEXT -> new NormalizedMessage(
                    MessageType.TEXT,
                    normalizeRequired(request.content(), "Text message content is required"),
                    null,
                    null
            );
            case IMAGE -> new NormalizedMessage(
                    MessageType.IMAGE,
                    normalizeOptional(request.content()),
                    normalizeRequired(request.mediaUrl(), "Image URL is required"),
                    null
            );
            case VIDEO -> new NormalizedMessage(
                    MessageType.VIDEO,
                    normalizeOptional(request.content()),
                    normalizeRequired(request.mediaUrl(), "Video URL is required"),
                    null
            );
            case LINK -> new NormalizedMessage(
                    MessageType.LINK,
                    normalizeOptional(request.content()),
                    null,
                    normalizeRequired(request.linkUrl(), "Link URL is required")
            );
        };
    }

    private void ensureAcceptedFriendship(UUID currentUserId, UUID friendId) {
        if (currentUserId.equals(friendId)) {
            throw new BadRequestException("Cannot message yourself");
        }

        Friendship friendship = friendshipRepository.findBetween(currentUserId, friendId)
                .orElseThrow(() -> new ForbiddenOperationException("You can only message friends"));
        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new ForbiddenOperationException("You can only message accepted friends");
        }
    }

    private void ensureParticipant(UUID currentUserId, MessageConversation conversation) {
        if (!conversation.getUserOne().getId().equals(currentUserId)
                && !conversation.getUserTwo().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("You are not a participant in this conversation");
        }
    }

    private OrderedParticipants orderParticipants(UUID firstUserId, UUID secondUserId) {
        return firstUserId.compareTo(secondUserId) <= 0
                ? new OrderedParticipants(firstUserId, secondUserId)
                : new OrderedParticipants(secondUserId, firstUserId);
    }

    private MessageConversation getConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
    }

    private Message getMessage(UUID messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BadRequestException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private record OrderedParticipants(UUID userOneId, UUID userTwoId) {
    }

    private record NormalizedMessage(MessageType messageType, String content, String mediaUrl, String linkUrl) {
    }
}
