package com.cybersocial.post;

import com.cybersocial.auth.repository.UserRepository;
import com.cybersocial.common.exception.BadRequestException;
import com.cybersocial.common.exception.ForbiddenOperationException;
import com.cybersocial.common.exception.ResourceNotFoundException;
import com.cybersocial.common.response.PagedResponse;
import com.cybersocial.post.dto.PostRequest;
import com.cybersocial.post.dto.PostResponse;
import com.cybersocial.user.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostServiceImpl(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> findPosts(Pageable pageable) {
        Page<PostResponse> page = postRepository.findAllByOrderByCreatedAtDesc(pageable).map(PostResponse::from);
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
    @Transactional(readOnly = true)
    public PostResponse findPost(UUID id) {
        return PostResponse.from(getPost(id));
    }

    @Override
    @Transactional
    public PostResponse createPost(UUID currentUserId, PostRequest request) {
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String content = request.content() == null ? "" : request.content().trim();
        List<String> mediaUrls = normalizeMediaUrls(request.mediaUrls());

        if (content.isBlank() && mediaUrls.isEmpty()) {
            throw new BadRequestException("Post content or media is required");
        }

        Post post = Post.builder()
                .author(author)
                .content(content)
                .visibility(request.visibility() == null ? PostVisibility.PUBLIC : request.visibility())
                .mediaUrls(mediaUrls)
                .build();
        // Post post = new Post();
        // post.setAuthor(author);

        return PostResponse.from(postRepository.save(post));
    }

    @Override
    @Transactional
    public void deletePost(UUID currentUserId, UUID postId) {
        Post post = getPost(postId);
        if (!post.getAuthor().getId().equals(currentUserId)) {
            throw new ForbiddenOperationException("You can only delete your own posts");
        }
        postRepository.delete(post);
    }

    private Post getPost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    private List<String> normalizeMediaUrls(List<String> mediaUrls) {
        if (mediaUrls == null) {
            return List.of();
        }
        return mediaUrls.stream()
                .map(url -> url == null ? "" : url.trim())
                .filter(url -> !url.isBlank())
                .toList();
    }
}
