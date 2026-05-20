package com.cybersocial.upload;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.cybersocial.common.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageUploadServiceImpl implements ImageUploadService {

    private static final Set<String> IMAGE_IO_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif"
    );

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

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        Map<?, ?> uploadResult;
        try {
            uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", properties.folder(),
                    "resource_type", "image",
                    "use_filename", true,
                    "unique_filename", true,
                    "overwrite", false
            ));
        } catch (IOException exception) {
            throw new BadRequestException("Could not upload image to Cloudinary");
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

        try (InputStream inputStream = file.getInputStream()) {
            if (IMAGE_IO_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT)) && ImageIO.read(inputStream) == null) {
                throw new BadRequestException("Uploaded file is not a valid image");
            }
        } catch (IOException exception) {
            throw new BadRequestException("Could not read image");
        }
    }

    private void validateCloudinaryConfig() {
        if (properties.cloudName().isBlank() || properties.apiKey().isBlank() || properties.apiSecret().isBlank()) {
            throw new BadRequestException("Cloudinary configuration is missing");
        }
    }
}
