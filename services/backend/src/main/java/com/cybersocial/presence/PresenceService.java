package com.cybersocial.presence;

import com.cybersocial.friend.FriendshipRepository;
import com.cybersocial.message.websocket.MessageSocketEvent;
import com.cybersocial.message.websocket.MessageSocketSessionRegistry;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PresenceService {

    private final FriendshipRepository friendshipRepository;
    private final MessageSocketSessionRegistry sessionRegistry;

    public PresenceService(
            FriendshipRepository friendshipRepository,
            MessageSocketSessionRegistry sessionRegistry
    ) {
        this.friendshipRepository = friendshipRepository;
        this.sessionRegistry = sessionRegistry;
    }

    public void notifyFriendsOnline(UUID userId) {
        broadcastPresence(userId, true);
    }

    public void notifyFriendsOffline(UUID userId) {
        broadcastPresence(userId, false);
    }

    @Transactional(readOnly = true)
    public List<UUID> listOnlineFriendIds(UUID userId) {
        return friendshipRepository.findAcceptedFriendIds(userId).stream()
                .filter(sessionRegistry::isOnline)
                .toList();
    }

    private void broadcastPresence(UUID userId, boolean online) {
        List<UUID> friendIds = friendshipRepository.findAcceptedFriendIds(userId);
        if (friendIds.isEmpty()) {
            return;
        }
        sessionRegistry.sendToUsers(friendIds, MessageSocketEvent.presenceUpdated(userId, online));
    }
}
