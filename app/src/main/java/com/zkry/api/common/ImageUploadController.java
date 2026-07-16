package com.zkry.api.common;

import com.zkry.common.core.domain.R;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/uploads")
public class ImageUploadController {

    private static final long MAX_SIZE = 8L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
        MediaType.IMAGE_JPEG_VALUE, ".jpg",
        MediaType.IMAGE_PNG_VALUE, ".png",
        "image/webp", ".webp"
    );

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, String>> image(@RequestParam("image") MultipartFile image, HttpServletRequest request) throws IOException {
        String extension = EXTENSIONS.get(image.getContentType());
        if (image.isEmpty() || extension == null || image.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("仅支持不超过 8MB 的 JPG、PNG 或 WebP 图片。");
        }
        Path directory = Path.of(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String name = UUID.randomUUID() + extension;
        image.transferTo(directory.resolve(name));
        String origin = request.getScheme() + "://" + request.getServerName()
            + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
        return R.ok(Map.of("url", origin + "/uploads/" + name));
    }
}
