package com.cybersocial.friend;

import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.friend.dto.FriendUserResponse;
import com.cybersocial.friend.dto.FriendshipResponse;
import com.cybersocial.friend.dto.FriendshipStateResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface FriendshipService {

    List<FriendshipResponse> findFriends(UUID currentUserId);

    List<FriendshipResponse> findIncomingRequests(UUID currentUserId);

    List<FriendshipResponse> findOutgoingRequests(UUID currentUserId);

    PagedResponse<FriendUserResponse> searchUsers(UUID currentUserId, String query, Pageable pageable);

    FriendshipStateResponse findState(UUID currentUserId, UUID targetUserId);

    FriendshipResponse sendRequest(UUID currentUserId, UUID targetUserId);

    FriendshipResponse acceptRequest(UUID currentUserId, UUID requestId);

    void deleteRequest(UUID currentUserId, UUID requestId);

    void removeFriend(UUID currentUserId, UUID friendshipId);
}
