package com.cybersocial.friend.dto;

import com.cybersocial.friend.Friendship;
import com.cybersocial.friend.FriendshipStatus;
import java.util.UUID;

/**
 * Quan hệ bạn bè giữa người dùng hiện tại và một người dùng khác.
 * state: NONE | PENDING_OUTGOING | PENDING_INCOMING | FRIENDS
 */
public record FriendshipStateResponse(
        String state,
        UUID friendshipId
) {
    public static FriendshipStateResponse none() {
        return new FriendshipStateResponse("NONE", null);
    }

    public static FriendshipStateResponse from(Friendship friendship, UUID currentUserId) {
        if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
            return new FriendshipStateResponse("FRIENDS", friendship.getId());
        }
        boolean sentByCurrentUser = friendship.getRequester().getId().equals(currentUserId);
        return new FriendshipStateResponse(
                sentByCurrentUser ? "PENDING_OUTGOING" : "PENDING_INCOMING",
                friendship.getId()
        );
    }
}
