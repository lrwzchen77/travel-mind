package com.zkry.resources.service;

import com.zkry.common.core.domain.PageResult;
import com.zkry.common.core.exception.BizException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户原创旅行社区内容；公开笔记与个人灵感包共用这一处服务。 */
@Service
public class CommunityService {

    private static final List<String> TOPICS = List.of("food", "stay", "play", "route", "tip");
    private static final List<String> INTENTS = List.of("must", "priority", "reference");
    private static final Pattern PRIVATE_DETAIL = Pattern.compile(
        "(?:经度|纬度|GPS|坐标)|(?:[零一二三四五六七八九十百千万两]+|\\d+(?:\\.\\d+)?)\\s*(?:元|块|人民币)|[¥￥]\\s*\\d|[-+]?\\d{1,3}\\.\\d{4,}\\s*[,，]\\s*[-+]?\\d{1,3}\\.\\d{4,}",
        Pattern.CASE_INSENSITIVE);
    private static final int MAX_PUBLIC_COVER_EDGE = 16_384;
    private static final long MAX_PUBLIC_COVER_PIXELS = 40_000_000L;
    private static final Pattern PRIVATE_UPLOAD = Pattern.compile(
        "^/private-uploads/(?<user>[0-9]+)/(?<name>[0-9a-fA-F-]{36}\\.(?:jpg|png|webp))$");
    private static final Pattern PUBLIC_UPLOAD = Pattern.compile(
        "^/public-uploads/[0-9a-fA-F-]{36}\\.png$");
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CommunityService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<Map<String, Object>> posts(String keyword, String city, String topic, int pageNum, int pageSize) {
        List<String> where = new ArrayList<>(List.of("n.deleted = 0", "n.visibility = 'public'", "n.status = 1"));
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (hasText(keyword)) {
            where.add("(n.title LIKE :keyword OR n.content LIKE :keyword OR n.tags LIKE :keyword)");
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }
        if (hasText(city)) {
            where.add("c.name = :city");
            params.addValue("city", city.trim());
        }
        if (hasText(topic)) {
            where.add("n.topic = :topic");
            params.addValue("topic", topic.trim());
        }
        String condition = String.join(" AND ", where);
        int safePage = Math.max(pageNum, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 30);
        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM tm_travel_note n
                LEFT JOIN tm_city c ON c.id = n.city_id AND c.deleted = 0
                WHERE %s
                """.formatted(condition), params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (safePage - 1) * safeSize);
        List<Map<String, Object>> records = jdbcTemplate.queryForList("""
                SELECT n.id, n.title, n.content, n.city_id, n.attraction_id, n.topic, n.cover_image, n.tags,
                       n.create_time, c.name AS city, u.nickname AS author,
                       (SELECT COUNT(1) FROM tm_travel_note_like l WHERE l.travel_note_id = n.id) AS like_count,
                       (SELECT COUNT(1) FROM tm_travel_note_comment cm WHERE cm.travel_note_id = n.id AND cm.deleted = 0) AS comment_count
                FROM tm_travel_note n
                LEFT JOIN tm_city c ON c.id = n.city_id AND c.deleted = 0
                LEFT JOIN tm_user u ON u.id = n.user_id AND u.deleted = 0
                WHERE %s
                ORDER BY n.update_time DESC, n.id DESC
                LIMIT :limit OFFSET :offset
                """.formatted(condition), params);
        return PageResult.of(records, total == null ? 0 : total, safePage, safeSize);
    }

    public Map<String, Object> post(long postId, Long viewerId) {
        List<Map<String, Object>> records = jdbcTemplate.queryForList("""
                SELECT n.id, n.title, n.content, n.city_id, n.attraction_id, n.topic, n.cover_image, n.tags,
                       n.visibility, n.status, n.create_time, c.name AS city, u.nickname AS author,
                       (SELECT COUNT(1) FROM tm_travel_note_like l WHERE l.travel_note_id = n.id) AS like_count,
                       (SELECT COUNT(1) FROM tm_travel_note_comment cm WHERE cm.travel_note_id = n.id AND cm.deleted = 0) AS comment_count,
                       EXISTS(SELECT 1 FROM tm_travel_note_like ml WHERE ml.travel_note_id = n.id AND ml.user_id = :viewerId) AS liked_by_me
                FROM tm_travel_note n
                LEFT JOIN tm_city c ON c.id = n.city_id AND c.deleted = 0
                LEFT JOIN tm_user u ON u.id = n.user_id AND u.deleted = 0
                WHERE n.id = :id AND n.deleted = 0 AND n.visibility = 'public' AND n.status = 1
                LIMIT 1
                """, Map.of("id", postId, "viewerId", viewerId == null ? 0L : viewerId));
        if (records.isEmpty()) throw new BizException("社区分享不存在或暂不可见。");
        return records.get(0);
    }

    @Transactional
    public Map<String, Object> createPost(long userId, Map<String, Object> payload) {
        String title = required(payload, "title", 128, "请填写标题。");
        String content = required(payload, "content", 8000, "请写下旅行体验。");
        String topic = text(payload, "topic", 32);
        if (!TOPICS.contains(topic)) throw new BizException("请选择吃、住、玩、路线或避坑分类。");
        String visibility = "public".equals(text(payload, "visibility", 16)) ? "public" : "private";
        String city = text(payload, "city", 64);
        Long cityId = findCityId(city);
        long id = nextId();
        int status = "public".equals(visibility) ? 0 : 1;
        String cover = text(payload, "cover_image", 500);
        Path generatedCover = null;
        if ("public".equals(visibility) && !cover.isBlank() && !PUBLIC_UPLOAD.matcher(cover).matches()) {
            generatedCover = sanitizedUserCover(userId, cover);
            cover = "/public-uploads/" + generatedCover.getFileName();
        }
        try {
            jdbcTemplate.update("""
                INSERT INTO tm_travel_note
                  (id, user_id, city_id, title, content, visibility, status, topic, cover_image, tags)
                VALUES (:id, :userId, :cityId, :title, :content, :visibility, :status, :topic, :coverImage, :tags)
                """, new MapSqlParameterSource()
            .addValue("id", id).addValue("userId", userId).addValue("cityId", cityId)
            .addValue("title", title).addValue("content", content).addValue("visibility", visibility)
                .addValue("status", status).addValue("topic", topic)
                .addValue("coverImage", cover).addValue("tags", text(payload, "tags", 255)));
            return Map.of("id", id, "title", title, "visibility", visibility, "status", status, "cover_image", cover);
        } catch (RuntimeException ex) {
            if (generatedCover != null) try { Files.deleteIfExists(generatedCover); } catch (IOException ignored) { }
            throw ex;
        }
    }

    /** 从本人私有记忆生成脱敏公开副本；公开帖子不保留 memory/item/evidence 关联。 */
    @Transactional
    public Map<String, Object> publishMemory(long userId, long memoryId, Map<String, Object> payload) {
        Map<String, Object> memory = jdbcTemplate.queryForList("""
                SELECT title, destination_city FROM tm_trip_memory
                WHERE id = :memoryId AND user_id = :userId AND visibility = 'private' LIMIT 1
                """, Map.of("memoryId", memoryId, "userId", userId)).stream().findFirst()
            .orElseThrow(() -> new BizException("旅行记忆不存在或无权发布。"));
        String title = optionalPublicText(payload, "title", 128, String.valueOf(memory.get("title")));
        String note = optionalPublicText(payload, "note", 600, "");
        String tags = optionalPublicText(payload, "tags", 200, "");
        List<Map<String, Object>> facts = jdbcTemplate.queryForList("""
                SELECT item_type, day_index, place_name, ai_caption
                FROM tm_trip_memory_item
                WHERE memory_id = :memoryId AND status = 'ready' AND item_type IN ('place', 'photo')
                ORDER BY COALESCE(day_index, 2147483647), sort_order, id LIMIT 30
                """, Map.of("memoryId", memoryId));
        String content = publicContent(memory, note, facts);
        Path sanitizedCover = null;
        try {
            Object photoId = payload == null ? null : payload.get("photo_item_id");
            if (photoId != null) {
                sanitizedCover = sanitizedCover(userId, memoryId, positiveId(photoId, "公开封面选择无效。"));
            }
            Map<String, Object> post = new LinkedHashMap<>(createPost(userId, Map.of(
                "title", title,
                "content", content,
                "topic", "route",
                "city", String.valueOf(memory.get("destination_city")),
                "visibility", "public",
                "cover_image", sanitizedCover == null ? "" : "/public-uploads/" + sanitizedCover.getFileName(),
                "tags", joinTags(tags, "真实行程")
            )));
            post.put("cover_image", sanitizedCover == null ? "" : "/public-uploads/" + sanitizedCover.getFileName());
            return post;
        } catch (RuntimeException ex) {
            if (sanitizedCover != null) try { Files.deleteIfExists(sanitizedCover); } catch (IOException ignored) { }
            throw ex;
        }
    }

    public PageResult<Map<String, Object>> myPosts(long userId, int pageNum, int pageSize) {
        int safePage = Math.max(pageNum, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 30);
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM tm_travel_note WHERE user_id = :userId AND deleted = 0", Map.of("userId", userId), Long.class);
        List<Map<String, Object>> records = jdbcTemplate.queryForList("""
                SELECT n.id, n.title, n.content, n.topic, n.cover_image, n.tags, n.visibility, n.status, n.create_time,
                       c.name AS city,
                       (SELECT COUNT(1) FROM tm_travel_note_like l WHERE l.travel_note_id = n.id) AS like_count,
                       (SELECT COUNT(1) FROM tm_travel_note_comment cm WHERE cm.travel_note_id = n.id AND cm.deleted = 0) AS comment_count
                FROM tm_travel_note n LEFT JOIN tm_city c ON c.id = n.city_id AND c.deleted = 0
                WHERE n.user_id = :userId AND n.deleted = 0
                ORDER BY n.update_time DESC, n.id DESC LIMIT :limit OFFSET :offset
                """, new MapSqlParameterSource().addValue("userId", userId).addValue("limit", safeSize)
            .addValue("offset", (safePage - 1) * safeSize));
        return PageResult.of(records, total == null ? 0 : total, safePage, safeSize);
    }

    @Transactional
    public Map<String, Object> addToBag(long userId, long postId, String intent) {
        post(postId, null);
        String safeIntent = INTENTS.contains(intent) ? intent : "reference";
        jdbcTemplate.update("""
                INSERT INTO tm_inspiration_item (id, user_id, travel_note_id, intent)
                VALUES (:id, :userId, :postId, :intent)
                ON DUPLICATE KEY UPDATE intent = VALUES(intent), deleted = 0
                """, Map.of("id", nextId(), "userId", userId, "postId", postId, "intent", safeIntent));
        return bagItem(userId, postId);
    }

    public PageResult<Map<String, Object>> bag(long userId) {
        List<Map<String, Object>> records = jdbcTemplate.queryForList("""
                SELECT i.travel_note_id AS post_id, i.intent, i.create_time AS added_time,
                       n.title, n.content, n.topic, n.cover_image, n.tags, c.name AS city, u.nickname AS author
                FROM tm_inspiration_item i
                JOIN tm_travel_note n ON n.id = i.travel_note_id AND n.deleted = 0
                  AND n.visibility = 'public' AND n.status = 1
                LEFT JOIN tm_city c ON c.id = n.city_id AND c.deleted = 0
                LEFT JOIN tm_user u ON u.id = n.user_id AND u.deleted = 0
                WHERE i.user_id = :userId AND i.deleted = 0
                ORDER BY i.update_time DESC, i.id DESC
                """, Map.of("userId", userId));
        return PageResult.of(records, records.size(), 1, Math.max(records.size(), 1));
    }

    @Transactional
    public void removeFromBag(long userId, long postId) {
        jdbcTemplate.update("""
                UPDATE tm_inspiration_item SET deleted = 1
                WHERE user_id = :userId AND travel_note_id = :postId AND deleted = 0
                """, Map.of("userId", userId, "postId", postId));
    }

    @Transactional
    public Map<String, Object> like(long userId, long postId) {
        post(postId, null);
        jdbcTemplate.update("""
                INSERT IGNORE INTO tm_travel_note_like (travel_note_id, user_id)
                VALUES (:postId, :userId)
                """, Map.of("postId", postId, "userId", userId));
        return reaction(userId, postId);
    }

    @Transactional
    public Map<String, Object> unlike(long userId, long postId) {
        post(postId, null);
        jdbcTemplate.update("DELETE FROM tm_travel_note_like WHERE travel_note_id = :postId AND user_id = :userId",
            Map.of("postId", postId, "userId", userId));
        return reaction(userId, postId);
    }

    public PageResult<Map<String, Object>> comments(long postId, Long viewerId, int pageNum, int pageSize) {
        post(postId, null);
        int safePage = Math.max(pageNum, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 50);
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("postId", postId).addValue("viewerId", viewerId == null ? 0L : viewerId)
            .addValue("limit", safeSize).addValue("offset", (safePage - 1) * safeSize);
        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM tm_travel_note_comment
                WHERE travel_note_id = :postId AND deleted = 0
                """, params, Long.class);
        List<Map<String, Object>> records = jdbcTemplate.queryForList("""
                SELECT cm.id, COALESCE(u.nickname, '旅行者') AS author, cm.content, cm.create_time,
                       CASE WHEN cm.user_id = :viewerId THEN TRUE ELSE FALSE END AS is_mine
                FROM tm_travel_note_comment cm
                LEFT JOIN tm_user u ON u.id = cm.user_id AND u.deleted = 0
                WHERE cm.travel_note_id = :postId AND cm.deleted = 0
                ORDER BY cm.create_time DESC, cm.id DESC
                LIMIT :limit OFFSET :offset
                """, params);
        return PageResult.of(records, total == null ? 0 : total, safePage, safeSize);
    }

    @Transactional
    public Map<String, Object> createComment(long userId, long postId, Map<String, Object> payload) {
        post(postId, null);
        String content = required(payload, "content", 1000, "请填写评论内容。");
        long id = nextId();
        jdbcTemplate.update("""
                INSERT INTO tm_travel_note_comment (id, travel_note_id, user_id, content)
                VALUES (:id, :postId, :userId, :content)
                """, Map.of("id", id, "postId", postId, "userId", userId, "content", content));
        return Map.of("id", id, "content", content);
    }

    @Transactional
    public void deleteComment(long userId, long commentId) {
        int changed = jdbcTemplate.update("""
                UPDATE tm_travel_note_comment cm
                JOIN tm_travel_note n ON n.id = cm.travel_note_id
                  AND n.deleted = 0 AND n.visibility = 'public' AND n.status = 1
                SET cm.deleted = 1
                WHERE cm.id = :commentId AND cm.user_id = :userId AND cm.deleted = 0
                """, Map.of("commentId", commentId, "userId", userId));
        if (changed == 0) throw new BizException("评论不存在或无权删除。");
    }

    /** 仅返回审核通过的公开帖子，供 AI/规划在服务端构造有限上下文。 */
    public List<Map<String, Object>> sourcePosts(List<Long> postIds) {
        List<Long> ids = postIds == null ? List.of() : postIds.stream().filter(id -> id != null && id > 0)
            .collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toCollection(LinkedHashSet::new), List::copyOf));
        if (ids.isEmpty()) return List.of();
        if (ids.size() > 5) throw new BizException("一次最多引用 5 篇社区分享。");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT n.id AS post_id, n.title, n.content, n.topic, c.name AS city
                FROM tm_travel_note n LEFT JOIN tm_city c ON c.id = n.city_id AND c.deleted = 0
                WHERE n.id IN (:ids) AND n.deleted = 0 AND n.visibility = 'public' AND n.status = 1
                """, Map.of("ids", ids));
        if (rows.size() != ids.size()) throw new BizException("有社区分享已下架或无权引用，请重新选择。");
        Map<Long, Map<String, Object>> indexed = new LinkedHashMap<>();
        rows.forEach(row -> indexed.put(((Number) row.get("post_id")).longValue(), row));
        return ids.stream().map(indexed::get).toList();
    }

    public List<Map<String, Object>> sourcePosts(long userId, List<Long> postIds) {
        List<Map<String, Object>> posts = new ArrayList<>(sourcePosts(postIds));
        if (posts.isEmpty()) return posts;
        Map<Long, String> intents = new LinkedHashMap<>();
        jdbcTemplate.queryForList("""
                SELECT travel_note_id, intent FROM tm_inspiration_item
                WHERE user_id = :userId AND travel_note_id IN (:ids) AND deleted = 0
                """, Map.of("userId", userId, "ids", posts.stream().map(row -> ((Number) row.get("post_id")).longValue()).toList()))
            .forEach(row -> intents.put(((Number) row.get("travel_note_id")).longValue(), String.valueOf(row.get("intent"))));
        posts.forEach(row -> row.put("intent", intents.getOrDefault(((Number) row.get("post_id")).longValue(), "reference")));
        return posts;
    }

    private Map<String, Object> bagItem(long userId, long postId) {
        return jdbcTemplate.queryForList("""
                SELECT i.travel_note_id AS post_id, i.intent, n.title, n.topic, c.name AS city
                FROM tm_inspiration_item i JOIN tm_travel_note n ON n.id = i.travel_note_id
                LEFT JOIN tm_city c ON c.id = n.city_id AND c.deleted = 0
                WHERE i.user_id = :userId AND i.travel_note_id = :postId AND i.deleted = 0
                """, Map.of("userId", userId, "postId", postId)).stream().findFirst()
            .orElseThrow(() -> new BizException("加入灵感包失败。"));
    }

    private Map<String, Object> reaction(long userId, long postId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM tm_travel_note_like WHERE travel_note_id = :postId",
            Map.of("postId", postId), Long.class);
        Long mine = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM tm_travel_note_like
                WHERE travel_note_id = :postId AND user_id = :userId
                """, Map.of("postId", postId, "userId", userId), Long.class);
        return Map.of("like_count", count == null ? 0 : count, "liked_by_me", mine != null && mine > 0);
    }

    private Long findCityId(String city) {
        if (!hasText(city)) return null;
        return jdbcTemplate.queryForList("SELECT id FROM tm_city WHERE name = :name AND deleted = 0 LIMIT 1", Map.of("name", city))
            .stream().findFirst().map(row -> ((Number) row.get("id")).longValue()).orElse(null);
    }

    private String optionalPublicText(Map<String, Object> payload, String key, int max, String fallback) {
        String value = text(payload, key, max);
        if (value.isBlank()) value = fallback;
        if (PRIVATE_DETAIL.matcher(value).find()) {
            throw new BizException("公开内容不能包含消费金额、精确坐标或 GPS 信息。");
        }
        return value;
    }

    private String publicContent(Map<String, Object> memory, String note, List<Map<String, Object>> facts) {
        List<String> lines = new ArrayList<>();
        lines.add("来自真实行程 · 已由旅行者确认公开");
        if (!note.isBlank()) lines.add(note);
        for (Map<String, Object> fact : facts) {
            String place = String.valueOf(fact.get("place_name") == null ? "" : fact.get("place_name")).trim();
            String caption = "photo".equals(fact.get("item_type"))
                ? String.valueOf(fact.get("ai_caption") == null ? "" : fact.get("ai_caption")).trim() : "";
            String safe = !place.isBlank() ? place : caption;
            if (safe.isBlank() || PRIVATE_DETAIL.matcher(safe).find()) continue;
            Object day = fact.get("day_index");
            lines.add("- " + (day == null ? "旅行片段" : "Day " + day) + " · " + safe);
        }
        if (lines.size() == (note.isBlank() ? 1 : 2)) {
            lines.add("- " + memory.get("destination_city") + "旅行片段");
        }
        return String.join("\n\n", lines);
    }

    private Path sanitizedCover(long userId, long memoryId, long photoId) {
        Map<String, Object> photo = jdbcTemplate.queryForList("""
                SELECT i.source_url FROM tm_trip_memory_item i
                JOIN tm_trip_memory m ON m.id = i.memory_id AND m.user_id = :userId
                WHERE i.id = :photoId AND i.memory_id = :memoryId AND i.item_type = 'photo' AND i.status = 'ready'
                LIMIT 1
                """, Map.of("userId", userId, "memoryId", memoryId, "photoId", photoId)).stream().findFirst()
            .orElseThrow(() -> new BizException("公开封面不属于当前记忆册。"));
        Path source = privateFile(userId, String.valueOf(photo.get("source_url")));
        return writeSanitizedCover(source);
    }

    private Path sanitizedUserCover(long userId, String sourceUrl) {
        return writeSanitizedCover(privateFile(userId, sourceUrl));
    }

    private Path privateFile(long userId, String sourceUrl) {
        var matcher = PRIVATE_UPLOAD.matcher(sourceUrl == null ? "" : sourceUrl);
        if (!matcher.matches() || !String.valueOf(userId).equals(matcher.group("user"))) {
            throw new BizException("公开封面必须来自当前用户的受控上传。");
        }
        Path directory = Path.of(System.getProperty("user.dir"), "uploads", "private", String.valueOf(userId))
            .toAbsolutePath().normalize();
        Path source = directory.resolve(matcher.group("name")).normalize();
        if (!source.startsWith(directory) || !Files.isRegularFile(source)) throw new BizException("公开封面文件不存在。");
        return source;
    }

    private Path writeSanitizedCover(Path source) {
        Path directory = Path.of(System.getProperty("user.dir"), "uploads", "public").toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw new BizException("公开封面目录不可用。");
        }
        Path target = directory.resolve(UUID.randomUUID() + ".png");
        BufferedImage image = readPublicCover(source);
        try {
            if (!ImageIO.write(image, "png", target.toFile())) throw new IOException("PNG writer unavailable");
            return target;
        } catch (IOException ex) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            throw new BizException("公开封面处理失败，请换一张照片。");
        }
    }

    private BufferedImage readPublicCover(Path source) {
        try (ImageInputStream input = ImageIO.createImageInputStream(source.toFile())) {
            if (input == null) throw new BizException("这张照片无法安全去除位置信息，请选择 JPG 或 PNG。");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new BizException("这张照片无法安全去除位置信息，请选择 JPG 或 PNG。");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                validatePublicCoverDimensions(reader.getWidth(0), reader.getHeight(0));
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new BizException("这张照片无法安全去除位置信息，请选择 JPG 或 PNG。");
        }
    }

    static void validatePublicCoverDimensions(int width, int height) {
        if (width <= 0 || height <= 0 || width > MAX_PUBLIC_COVER_EDGE || height > MAX_PUBLIC_COVER_EDGE
            || (long) width * height > MAX_PUBLIC_COVER_PIXELS) {
            throw new BizException("公开封面尺寸过大，请选择不超过 4000 万像素的图片。");
        }
    }

    private long positiveId(Object value, String message) {
        String text = String.valueOf(value).trim();
        if (!text.matches("[0-9]+")) throw new BizException(message);
        try {
            long id = Long.parseLong(text);
            if (id <= 0) throw new BizException(message);
            return id;
        } catch (NumberFormatException ex) {
            throw new BizException(message);
        }
    }

    private String joinTags(String tags, String required) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String tag : tags.split("[,，、\\s]+")) if (!tag.isBlank()) values.add(tag);
        values.add(required);
        return String.join("，", values);
    }

    private String required(Map<String, Object> payload, String key, int max, String message) {
        String value = text(payload, key, max);
        if (!hasText(value)) throw new BizException(message);
        return value;
    }

    private String text(Map<String, Object> payload, String key, int max) {
        Object value = payload == null ? null : payload.get(key);
        String result = value == null ? "" : String.valueOf(value).trim();
        if (result.length() > max) throw new BizException(key + " 内容过长。");
        return result;
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private long nextId() { return System.currentTimeMillis() * 1000 + ThreadLocalRandom.current().nextInt(1000); }
}
