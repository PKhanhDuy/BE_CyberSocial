package com.cybersocial.post;

import java.util.UUID;

public interface PostCountProjection {

    UUID getPostId();

    long getCount();
}
