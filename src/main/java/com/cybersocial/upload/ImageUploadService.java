package com.cybersocial.upload;

import org.springframework.web.multipart.MultipartFile;

public interface ImageUploadService {
    ImageUploadResponse uploadImage(MultipartFile file);

    ImageUploadResponse uploadVideo(MultipartFile file);
}
