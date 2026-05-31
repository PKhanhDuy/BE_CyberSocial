package com.cybersocial.upload;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.cybersocial.common.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageUploadServiceImpl implements ImageUploadService {

    private static final int IMAGE_SIGNATURE_LENGTH = 12;

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    public ImageUploadServiceImpl(Cloudinary cloudinary, CloudinaryProperties properties) {
        this.cloudinary = cloudinary;
        this.properties = properties;
    }

    @Override
    public ImageUploadResponse uploadImage(MultipartFile file) {
        validateCloudinaryConfig();
        validateImage(file);

        return uploadToCloudinary(file, "image");
    }

    @Override
    public ImageUploadResponse uploadVideo(MultipartFile file) {
        validateCloudinaryConfig();
        validateVideo(file);

        return uploadToCloudinary(file, "video");
    }

    private ImageUploadResponse uploadToCloudinary(MultipartFile file, String resourceType) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? resourceType : file.getOriginalFilename());
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        Map<?, ?> uploadResult;
        try {
            uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", properties.folder(),
                    "resource_type", resourceType,
                    "use_filename", true,
                    "unique_filename", true,
                    "overwrite", false
            ));
        } catch (IOException exception) {
            throw new BadRequestException("Could not upload " + resourceType + " to Cloudinary");
        }

        String publicId = String.valueOf(uploadResult.get("public_id"));
        String secureUrl = String.valueOf(uploadResult.get("secure_url"));

        return new ImageUploadResponse(publicId, originalFileName, contentType, file.getSize(), secureUrl);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }

        if (file.getSize() > properties.maxImageSize().toBytes()) {
            throw new BadRequestException("Image must be " + properties.maxImageSize().toMegabytes() + "MB or smaller");
        }

        String contentType = file.getContentType();
        if (contentType == null || !properties.allowedImageContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Only JPG, PNG, WEBP, and GIF images are allowed");
        }

        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        try (InputStream inputStream = file.getInputStream()) {
            byte[] signature = inputStream.readNBytes(IMAGE_SIGNATURE_LENGTH);
            if (!hasValidImageSignature(normalizedContentType, signature)) {
                throw new BadRequestException("Uploaded file is not a valid image");
            }
        } catch (IOException exception) {
            throw new BadRequestException("Could not read image");
        }
    }

    private boolean hasValidImageSignature(String contentType, byte[] signature) {
        return switch (contentType) {
            case "image/jpeg" -> hasPrefix(signature, new int[]{0xFF, 0xD8, 0xFF});
            case "image/png" -> hasPrefix(signature, new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "image/gif" -> hasPrefix(signature, "GIF87a".getBytes()) || hasPrefix(signature, "GIF89a".getBytes());
            case "image/webp" -> signature.length >= 12
                    && hasPrefix(signature, "RIFF".getBytes())
                    && signature[8] == 'W'
                    && signature[9] == 'E'
                    && signature[10] == 'B'
                    && signature[11] == 'P';
            default -> true;
        };
    }

    private boolean hasPrefix(byte[] value, int[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if ((value[index] & 0xFF) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPrefix(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private void validateVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Video file is required");
        }

        if (file.getSize() > properties.maxVideoSize().toBytes()) {
            throw new BadRequestException("Video must be " + properties.maxVideoSize().toMegabytes() + "MB or smaller");
        }

        String contentType = file.getContentType();
        if (contentType == null || !properties.allowedVideoContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Only MP4, WEBM, and MOV videos are allowed");
        }
    }

    private void validateCloudinaryConfig() {
        if (properties.cloudName().isBlank() || properties.apiKey().isBlank() || properties.apiSecret().isBlank()) {
            throw new BadRequestException("Cloudinary configuration is missing");
        }
    }
}
