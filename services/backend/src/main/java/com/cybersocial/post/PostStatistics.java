package com.cybersocial.post;

import java.io.Serializable;

public record PostStatistics(long likeCount, long commentCount, long shareCount) implements Serializable {
}
