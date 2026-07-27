package com.zkry.api.common;

import com.zkry.common.core.domain.R;
import com.zkry.common.satoken.core.LoginHelper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/uploads")
public class ImageUploadController {

    private static final long MAX_SIZE = 8L * 1024 * 1024;
    private static final Pattern SAFE_NAME = Pattern.compile("[0-9a-f-]{36}\\.(?:jpg|png|webp)");
    private static final Map<String, String> EXTENSIONS = Map.of(
        MediaType.IMAGE_JPEG_VALUE, ".jpg",
        MediaType.IMAGE_PNG_VALUE, ".png",
        "image/webp", ".webp"
    );
    private final Path uploadRoot;

    public ImageUploadController() {
        this(Path.of(System.getProperty("user.dir"), "uploads"));
    }

    ImageUploadController(Path uploadRoot) {
        this.uploadRoot = uploadRoot.toAbsolutePath().normalize();
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Map<String, String>> image(@RequestParam("image") MultipartFile image) throws IOException {
        String extension = EXTENSIONS.get(image.getContentType());
        if (image.isEmpty() || extension == null || image.getSize() > MAX_SIZE || !hasExpectedSignature(image, extension)) {
            throw new IllegalArgumentException("仅支持不超过 8MB 的 JPG、PNG 或 WebP 图片。");
        }
        long userId = LoginHelper.getUserId();
        Path directory = privateDirectory(userId);
        Files.createDirectories(directory);
        String name = UUID.randomUUID() + extension;
        image.transferTo(directory.resolve(name));
        return R.ok(Map.of("url", "/private-uploads/" + userId + "/" + name));
    }

    @GetMapping("/images/{name}")
    public ResponseEntity<Resource> image(@PathVariable String name) throws IOException {
        if (!SAFE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("图片地址无效。");
        }
        Path file = privateDirectory(LoginHelper.getUserId()).resolve(name).normalize();
        if (!file.startsWith(privateDirectory(LoginHelper.getUserId())) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        String type = name.endsWith(".png") ? MediaType.IMAGE_PNG_VALUE
            : name.endsWith(".webp") ? "image/webp" : MediaType.IMAGE_JPEG_VALUE;
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .contentType(MediaType.parseMediaType(type))
            .body(new FileSystemResource(file));
    }

    private Path privateDirectory(long userId) {
        return uploadRoot.resolve("private").resolve(String.valueOf(userId));
    }

    private boolean hasExpectedSignature(MultipartFile image, String extension) throws IOException {
        byte[] header = new byte[12];
        int read;
        try (InputStream input = image.getInputStream()) {
            read = input.read(header);
        }
        if (read < 12) return false;
        return switch (extension) {
            case ".jpg" -> (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
            case ".png" -> (header[0] & 0xff) == 0x89 && header[1] == 0x50 && header[2] == 0x4e
                && header[3] == 0x47 && header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a;
            case ".webp" -> header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }
}
