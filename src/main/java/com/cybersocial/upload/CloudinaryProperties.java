package com.cybersocial.upload;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        String folder,
        DataSize maxImageSize,
        List<String> allowedImageContentTypes,
        DataSize maxVideoSize,
        List<String> allowedVideoContentTypes
) {
    public CloudinaryProperties {
        cloudName = cloudName == null ? "" : cloudName.trim();
        apiKey = apiKey == null ? "" : apiKey.trim();
        apiSecret = apiSecret == null ? "" : apiSecret.trim();
        folder = (folder == null || folder.isBlank()) ? "cybersocial/posts" : folder.trim();
        maxImageSize = maxImageSize == null ? DataSize.ofMegabytes(5) : maxImageSize;
        allowedImageContentTypes = allowedImageContentTypes == null || allowedImageContentTypes.isEmpty()
                ? List.of("image/jpeg", "image/png", "image/webp", "image/gif")
                : allowedImageContentTypes.stream().map(String::toLowerCase).toList();
        maxVideoSize = maxVideoSize == null ? DataSize.ofMegabytes(50) : maxVideoSize;
        allowedVideoContentTypes = allowedVideoContentTypes == null || allowedVideoContentTypes.isEmpty()
                ? List.of("video/mp4", "video/webm", "video/quicktime")
                : allowedVideoContentTypes.stream().map(String::toLowerCase).toList();
    }
}
