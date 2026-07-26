package com.zkry.resources.service;

import com.zkry.common.core.domain.PageResult;
import com.zkry.common.core.exception.BizException;
import com.zkry.common.json.utils.JsonUtils;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 私有旅行记忆册；所有入口都以 JWT userId 重新校验数据归属。 */
@Service
public class TripMemoryService {

    private static final Pattern UPLOAD_PATH = Pattern.compile("^/uploads/[0-9a-fA-F-]{36}\\.(?:jpg|png|webp)$");
    private static final Set<String> GENERATION_TYPES = Set.of("timeline", "daily_summary", "travelogue", "trip_summary");
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TripMemoryService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Map<String, Object> createFromTrip(long userId, long tripId) {
        Map<String, Object> trip = ownedTrip(userId, tripId);
        jdbcTemplate.update("""
                INSERT INTO tm_trip_memory
                  (id, user_id, trip_plan_id, title, destination_city, summary, visibility)
                VALUES (:id, :userId, :tripId, :title, :city, :summary, 'private')
                ON DUPLICATE KEY UPDATE id = id
                """, new MapSqlParameterSource()
            .addValue("id", nextId()).addValue("userId", userId).addValue("tripId", tripId)
            .addValue("title", trip.get("title")).addValue("city", trip.get("destination_city"))
            .addValue("summary", trip.get("summary")));
        long memoryId = ((Number) jdbcTemplate.queryForMap("""
                SELECT id FROM tm_trip_memory WHERE trip_plan_id = :tripId AND user_id = :userId
                """, Map.of("tripId", tripId, "userId", userId)).get("id")).longValue();
        seedItems(memoryId, trip);
        return detail(userId, memoryId);
    }

    public PageResult<Map<String, Object>> list(long userId, int pageNum, int pageSize) {
        int page = Math.max(pageNum, 1);
        int size = Math.min(Math.max(pageSize, 1), 50);
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId)
            .addValue("limit", size).addValue("offset", (page - 1) * size);
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM tm_trip_memory WHERE user_id = :userId", params, Long.class);
        List<Map<String, Object>> records = jdbcTemplate.queryForList("""
                SELECT m.id, m.trip_plan_id, m.title, m.destination_city, m.summary, m.cover_image,
                       m.status, m.visibility, m.generation_status, m.index_status, m.indexed_at,
                       m.create_time, m.update_time,
                       (SELECT COUNT(1) FROM tm_trip_memory_item i WHERE i.memory_id = m.id) AS item_count
                FROM tm_trip_memory m WHERE m.user_id = :userId
                ORDER BY m.update_time DESC, m.id DESC LIMIT :limit OFFSET :offset
                """, params);
        return PageResult.of(records, total == null ? 0 : total, page, size);
    }

    public Map<String, Object> detail(long userId, long memoryId) {
        Map<String, Object> memory = new LinkedHashMap<>(ownedMemory(userId, memoryId));
        memory.put("items", jdbcTemplate.queryForList("""
                SELECT id, item_type, source_type, source_url, taken_at, latitude, longitude, city, place_name,
                       content, ai_caption, ai_tags, confidence, day_index, sort_order, status, create_time, update_time
                FROM tm_trip_memory_item WHERE memory_id = :memoryId
                ORDER BY COALESCE(day_index, 2147483647), COALESCE(taken_at, '9999-12-31 23:59:59'), sort_order, id
                """, Map.of("memoryId", memoryId)));
        memory.put("generations", jdbcTemplate.queryForList("""
                SELECT id, generation_type, content, evidence_json, version, accepted, create_time
                FROM tm_trip_memory_generation WHERE memory_id = :memoryId
                ORDER BY create_time DESC, id DESC
                """, Map.of("memoryId", memoryId)));
        return memory;
    }

    @Transactional
    public Map<String, Object> addPhoto(long userId, long memoryId, Map<String, Object> payload) {
        ownedMemory(userId, memoryId);
        String sourceUrl = controlledUpload(text(payload, "url", 512));
        LocalDateTime takenAt = dateTime(text(payload, "taken_at", 32));
        BigDecimal latitude = coordinate(payload == null ? null : payload.get("latitude"), -90, 90, "纬度");
        BigDecimal longitude = coordinate(payload == null ? null : payload.get("longitude"), -180, 180, "经度");
        Integer dayIndex = integer(payload == null ? null : payload.get("day_index"), 1, 366, "day_index");
        Integer sortOrder = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(sort_order), 0) + 1 FROM tm_trip_memory_item WHERE memory_id = :memoryId",
            Map.of("memoryId", memoryId), Integer.class);
        long itemId = nextId();
        jdbcTemplate.update("""
                INSERT INTO tm_trip_memory_item
                  (id, memory_id, item_type, source_url, taken_at, latitude, longitude, day_index, sort_order)
                VALUES (:id, :memoryId, 'photo', :sourceUrl, :takenAt, :latitude, :longitude, :dayIndex, :sortOrder)
                """, new MapSqlParameterSource().addValue("id", itemId).addValue("memoryId", memoryId)
            .addValue("sourceUrl", sourceUrl).addValue("takenAt", takenAt).addValue("latitude", latitude)
            .addValue("longitude", longitude).addValue("dayIndex", dayIndex)
            .addValue("sortOrder", sortOrder == null ? 1 : sortOrder));
        jdbcTemplate.update("UPDATE tm_trip_memory SET generation_status = 'pending', index_status = 'pending', indexed_at = NULL WHERE id = :memoryId",
            Map.of("memoryId", memoryId));
        return jdbcTemplate.queryForMap("""
                SELECT id, item_type, source_url, taken_at, latitude, longitude, day_index, sort_order, status
                FROM tm_trip_memory_item WHERE id = :itemId
                """, Map.of("itemId", itemId));
    }

    @Transactional
    public void deleteItem(long userId, long memoryId, long itemId) {
        ownedMemory(userId, memoryId);
        int changed = jdbcTemplate.update(
            "DELETE FROM tm_trip_memory_item WHERE id = :itemId AND memory_id = :memoryId",
            Map.of("itemId", itemId, "memoryId", memoryId));
        if (changed == 0) throw new BizException("记忆项目不存在或无权删除。");
        jdbcTemplate.update("UPDATE tm_trip_memory SET generation_status = 'pending', index_status = 'pending', indexed_at = NULL WHERE id = :memoryId",
            Map.of("memoryId", memoryId));
    }

    @Transactional
    public void delete(long userId, long memoryId) {
        int changed = jdbcTemplate.update(
            "DELETE FROM tm_trip_memory WHERE id = :memoryId AND user_id = :userId",
            Map.of("memoryId", memoryId, "userId", userId));
        if (changed == 0) throw new BizException("旅行记忆不存在或无权删除。");
    }

    public TripMemoryKnowledgeContract.Identity knowledgeIdentity(long userId, long memoryId) {
        Map<String, Object> memory = ownedMemory(userId, memoryId);
        return new TripMemoryKnowledgeContract.Identity(memoryId, ((Number) memory.get("trip_plan_id")).longValue());
    }

    public TripMemoryKnowledgeContract.Source knowledgeSource(long userId, long memoryId) {
        Map<String, Object> memory = ownedMemory(userId, memoryId);
        List<Long> timelineEvidence = jdbcTemplate.queryForList("""
                SELECT evidence_json FROM tm_trip_memory_generation
                WHERE memory_id = :memoryId AND generation_type = 'timeline'
                ORDER BY version DESC LIMIT 1
                """, Map.of("memoryId", memoryId)).stream().findFirst()
            .map(row -> JsonUtils.parseArray(String.valueOf(row.get("evidence_json")), Long.class)).orElse(List.of());
        List<TripMemoryKnowledgeContract.Item> items = jdbcTemplate.query("""
                SELECT id, item_type, source_type, source_id, city, place_name, content, ai_caption, ai_tags, day_index
                FROM tm_trip_memory_item WHERE memory_id = :memoryId AND status = 'ready'
                ORDER BY COALESCE(day_index, 2147483647), sort_order, id
                """, Map.of("memoryId", memoryId), (rs, rowNum) -> new TripMemoryKnowledgeContract.Item(
                    rs.getLong("id"), rs.getString("item_type"), rs.getString("source_type"),
                    rs.getObject("source_id") == null ? null : rs.getLong("source_id"), rs.getString("city"),
                    rs.getString("place_name"), rs.getString("content"), rs.getString("ai_caption"),
                    JsonUtils.parseArray(rs.getString("ai_tags"), String.class), (Integer) rs.getObject("day_index"),
                    timelineEvidence.contains(rs.getLong("id"))));
        return new TripMemoryKnowledgeContract.Source(memoryId, ((Number) memory.get("trip_plan_id")).longValue(),
            String.valueOf(memory.get("title")), String.valueOf(memory.get("destination_city")), items);
    }

    public void knowledgeStatus(long userId, long memoryId, String status) {
        ownedMemory(userId, memoryId);
        if (!Set.of("pending", "indexing", "ready", "failed", "unavailable").contains(status)) {
            throw new BizException("旅行记忆索引状态不支持。");
        }
        jdbcTemplate.update("""
                UPDATE tm_trip_memory SET index_status = :status,
                  indexed_at = CASE WHEN :status = 'ready' THEN CURRENT_TIMESTAMP ELSE indexed_at END
                WHERE id = :memoryId
                """, Map.of("status", status, "memoryId", memoryId));
    }

    public TripMemoryKnowledgeContract.Answer validateAnswer(
        long userId,
        long memoryId,
        TripMemoryKnowledgeContract.Answer answer
    ) {
        ownedMemory(userId, memoryId);
        if (answer == null || answer.answer() == null || answer.answer().isBlank()) {
            throw new BizException("旅行记忆问答结果为空。");
        }
        List<TripMemoryKnowledgeContract.Citation> citations = answer.citations() == null ? List.of() : answer.citations();
        if (citations.isEmpty()) {
            if (!TripMemoryKnowledgeContract.NO_EVIDENCE.equals(answer.answer())) {
                throw new BizException("旅行记忆回答缺少可验证引用。");
            }
            return new TripMemoryKnowledgeContract.Answer(answer.answer(), List.of(), answer.fallback());
        }
        List<Long> ids = citations.stream().map(TripMemoryKnowledgeContract.Citation::memoryItemId)
            .filter(id -> id > 0).distinct().toList();
        if (ids.size() != citations.size()) throw new BizException("旅行记忆回答包含非法引用。");
        Map<Long, Map<String, Object>> owned = new HashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList("""
                SELECT id, item_type, source_type, source_id FROM tm_trip_memory_item
                WHERE memory_id = :memoryId AND id IN (:ids)
                """, Map.of("memoryId", memoryId, "ids", ids))) {
            owned.put(((Number) row.get("id")).longValue(), row);
        }
        for (TripMemoryKnowledgeContract.Citation citation : citations) {
            Map<String, Object> row = owned.get(citation.memoryItemId());
            String expectedType = row == null ? null : String.valueOf(row.get("source_type") == null
                ? row.get("item_type") : row.get("source_type"));
            long expectedId = row == null ? -1 : (row.get("source_id") instanceof Number number
                ? number.longValue() : citation.memoryItemId());
            if (row == null || !java.util.Objects.equals(expectedType, citation.sourceType())
                || citation.sourceId() == null || citation.sourceId() != expectedId) {
                throw new BizException("旅行记忆回答包含不属于当前记忆册的引用。");
            }
        }
        return new TripMemoryKnowledgeContract.Answer(answer.answer(), List.copyOf(citations), answer.fallback());
    }

    public TripMemoryAnalysisContract.Input analysisInput(long userId, long memoryId) {
        Map<String, Object> memory = ownedMemory(userId, memoryId);
        List<TripMemoryAnalysisContract.ItemInput> items = jdbcTemplate.query("""
                SELECT id, item_type, source_url, taken_at, latitude, longitude, city, place_name, content, day_index
                FROM tm_trip_memory_item WHERE memory_id = :memoryId AND status = 'ready'
                ORDER BY COALESCE(day_index, 2147483647), COALESCE(taken_at, '9999-12-31 23:59:59'), sort_order, id
                """, Map.of("memoryId", memoryId), (rs, rowNum) -> new TripMemoryAnalysisContract.ItemInput(
                    rs.getLong("id"), rs.getString("item_type"), rs.getString("source_url"),
                    rs.getTimestamp("taken_at") == null ? null : rs.getTimestamp("taken_at").toLocalDateTime(),
                    rs.getBigDecimal("latitude"), rs.getBigDecimal("longitude"), rs.getString("city"),
                    rs.getString("place_name"), rs.getString("content"), (Integer) rs.getObject("day_index")));
        return new TripMemoryAnalysisContract.Input(memoryId, ((Number) memory.get("trip_plan_id")).longValue(),
            String.valueOf(memory.get("title")), String.valueOf(memory.get("destination_city")), items);
    }

    public void analysisStatus(long userId, long memoryId, String status) {
        ownedMemory(userId, memoryId);
        if (!Set.of("processing", "failed").contains(status)) throw new BizException("旅行记忆分析状态不支持。");
        jdbcTemplate.update("UPDATE tm_trip_memory SET generation_status = :status WHERE id = :memoryId",
            Map.of("status", status, "memoryId", memoryId));
    }

    @Transactional
    public TripMemoryAnalysisContract.Saved saveAnalysis(
        long userId,
        long memoryId,
        TripMemoryAnalysisContract.Result result
    ) {
        ownedMemory(userId, memoryId);
        if (result == null) throw new BizException("旅行记忆分析结果为空。");
        for (TripMemoryAnalysisContract.ItemResult item : result.items() == null ? List.<TripMemoryAnalysisContract.ItemResult>of() : result.items()) {
            BigDecimal confidence = item.confidence();
            if (confidence != null && (confidence.signum() < 0 || confidence.compareTo(BigDecimal.ONE) > 0)) {
                throw new BizException("AI 置信度必须在 0 到 1 之间。");
            }
            List<String> tags = safeTags(item.tags());
            BigDecimal latitude = checkedCoordinate(item.latitude(), -90, 90, "AI 纬度");
            BigDecimal longitude = checkedCoordinate(item.longitude(), -180, 180, "AI 经度");
            if (item.dayIndex() != null && (item.dayIndex() < 1 || item.dayIndex() > 366)) {
                throw new BizException("AI dayIndex 超出有效范围。");
            }
            if (item.matchedItemId() != null) {
                Long matchCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1) FROM tm_trip_memory_item WHERE memory_id = :memoryId AND id = :itemId
                        """, Map.of("memoryId", memoryId, "itemId", item.matchedItemId()), Long.class);
                if (matchCount == null || matchCount != 1) throw new BizException("AI 匹配结果引用了不属于该记忆册的项目。");
            }
            int changed = jdbcTemplate.update("""
                    UPDATE tm_trip_memory_item SET ai_caption = :caption, ai_tags = :tags,
                      place_name = COALESCE(NULLIF(:placeName, ''), place_name), confidence = :confidence,
                      taken_at = COALESCE(taken_at, :takenAt), latitude = COALESCE(latitude, :latitude),
                      longitude = COALESCE(longitude, :longitude), day_index = COALESCE(day_index, :dayIndex)
                    WHERE id = :itemId AND memory_id = :memoryId AND item_type = 'photo'
                    """, new MapSqlParameterSource().addValue("caption", limited(item.caption(), 2000, "图片说明"))
                .addValue("tags", JsonUtils.toJsonString(tags)).addValue("placeName", limited(item.placeName(), 255, "地点"))
                .addValue("confidence", confidence).addValue("takenAt", item.takenAt()).addValue("latitude", latitude)
                .addValue("longitude", longitude).addValue("dayIndex", item.dayIndex())
                .addValue("itemId", item.itemId()).addValue("memoryId", memoryId));
            if (changed == 0) throw new BizException("AI 结果包含不属于该记忆册的项目。");
        }
        TripMemoryAnalysisContract.Generation generation = result.generation();
        if (generation == null) {
            jdbcTemplate.update("UPDATE tm_trip_memory SET generation_status = 'ready', index_status = 'pending', indexed_at = NULL WHERE id = :memoryId",
                Map.of("memoryId", memoryId));
            return new TripMemoryAnalysisContract.Saved(0, 0);
        }
        if (!GENERATION_TYPES.contains(generation.type())) throw new BizException("旅行记忆生成类型不支持。");
        String content = limited(generation.content(), 50000, "生成内容");
        if (content.isBlank()) throw new BizException("旅行记忆生成内容为空。");
        List<Long> evidence = generation.evidenceItemIds() == null ? List.of() : generation.evidenceItemIds().stream()
            .filter(id -> id != null && id > 0).distinct().toList();
        if (!evidence.isEmpty()) {
            Long count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM tm_trip_memory_item WHERE memory_id = :memoryId AND id IN (:ids)
                    """, Map.of("memoryId", memoryId, "ids", evidence), Long.class);
            if (count == null || count != evidence.size()) throw new BizException("生成内容引用了不属于该记忆册的证据。");
        }
        Integer version = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(version), 0) + 1 FROM tm_trip_memory_generation
                WHERE memory_id = :memoryId AND generation_type = :type
                """, Map.of("memoryId", memoryId, "type", generation.type()), Integer.class);
        long generationId = nextId();
        jdbcTemplate.update("""
                INSERT INTO tm_trip_memory_generation
                  (id, memory_id, generation_type, content, evidence_json, version)
                VALUES (:id, :memoryId, :type, :content, :evidence, :version)
                """, Map.of("id", generationId, "memoryId", memoryId, "type", generation.type(), "content", content,
                    "evidence", JsonUtils.toJsonString(evidence), "version", version == null ? 1 : version));
        jdbcTemplate.update("UPDATE tm_trip_memory SET generation_status = 'ready', index_status = 'pending', indexed_at = NULL WHERE id = :memoryId",
            Map.of("memoryId", memoryId));
        return new TripMemoryAnalysisContract.Saved(generationId, version == null ? 1 : version);
    }

    private void seedItems(long memoryId, Map<String, Object> trip) {
        insertSeed(memoryId, "trip_summary", "trip_plan", ((Number) trip.get("id")).longValue(), null, null, 0,
            trip.get("destination_city"), trip.get("title"), trip.get("summary"));
        for (Map<String, Object> item : jdbcTemplate.queryForList("""
                SELECT i.id, i.item_order, i.title, i.location, i.start_time, i.note, i.cost, d.day_no, d.date
                FROM tm_trip_item i JOIN tm_trip_day d ON d.id = i.trip_day_id
                WHERE d.trip_plan_id = :tripId AND d.deleted = 0 AND i.deleted = 0
                ORDER BY d.day_no, i.item_order
                """, Map.of("tripId", trip.get("id")))) {
            insertSeed(memoryId, "place", "trip_item", number(item, "id").longValue(), dateTime(item.get("date"), item.get("start_time")), number(item, "day_no").intValue(),
                number(item, "item_order").intValue(), trip.get("destination_city"), item.get("location"),
                join(item.get("title"), item.get("note"), item.get("cost") == null ? null : "预计花费 ¥" + item.get("cost")));
        }
        for (Map<String, Object> expense : jdbcTemplate.queryForList("""
                SELECT e.id, e.title, e.category, e.amount, e.spent_on, e.note,
                       CASE WHEN p.start_date IS NULL THEN NULL ELSE DATEDIFF(e.spent_on, p.start_date) + 1 END AS day_no
                FROM tm_trip_expense e JOIN tm_trip_plan p ON p.id = e.trip_plan_id
                WHERE e.trip_plan_id = :tripId AND e.deleted = 0 ORDER BY e.spent_on, e.id
                """, Map.of("tripId", trip.get("id")))) {
            Number day = number(expense, "day_no");
            insertSeed(memoryId, "expense", "trip_expense", number(expense, "id").longValue(), dateAtStart(expense.get("spent_on")),
                day == null ? null : day.intValue(), 0,
                trip.get("destination_city"), expense.get("title"),
                join(expense.get("category"), "¥" + expense.get("amount"), expense.get("note")));
        }
        // ponytail: tm_travel_note 没有 trip_plan_id；有显式行程关联后再自动归档，避免仅凭同城污染记忆。
    }

    private void insertSeed(
        long memoryId, String itemType, String sourceType, long sourceId, LocalDateTime takenAt, Integer dayIndex, int sortOrder,
        Object city, Object placeName, Object content
    ) {
        jdbcTemplate.update("""
                INSERT IGNORE INTO tm_trip_memory_item
                  (id, memory_id, item_type, source_type, source_id, taken_at, city, place_name, content, day_index, sort_order)
                VALUES (:id, :memoryId, :itemType, :sourceType, :sourceId, :takenAt, :city, :placeName, :content, :dayIndex, :sortOrder)
                """, new MapSqlParameterSource().addValue("id", nextId()).addValue("memoryId", memoryId)
            .addValue("itemType", itemType).addValue("sourceType", sourceType).addValue("sourceId", sourceId)
            .addValue("takenAt", takenAt)
            .addValue("city", city).addValue("placeName", placeName).addValue("content", content)
            .addValue("dayIndex", dayIndex).addValue("sortOrder", sortOrder));
    }

    private Map<String, Object> ownedTrip(long userId, long tripId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, title, destination_city, summary, end_date,
                       end_date IS NOT NULL AND end_date <= CURRENT_DATE AS has_ended
                FROM tm_trip_plan
                WHERE id = :tripId AND user_id = :userId AND deleted = 0 LIMIT 1
                """, Map.of("tripId", tripId, "userId", userId));
        if (rows.isEmpty()) throw new BizException("行程不存在或无权操作。");
        Map<String, Object> trip = rows.get(0);
        Object ended = trip.get("has_ended");
        boolean hasEnded = Boolean.TRUE.equals(ended) || ended instanceof Number number && number.intValue() == 1;
        if (!hasEnded) {
            throw new BizException("行程结束后才能创建旅行记忆。");
        }
        return trip;
    }

    private Map<String, Object> ownedMemory(long userId, long memoryId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, trip_plan_id, title, destination_city, summary, cover_image, status, visibility,
                       generation_status, index_status, indexed_at, create_time, update_time
                FROM tm_trip_memory WHERE id = :memoryId AND user_id = :userId LIMIT 1
                """, Map.of("memoryId", memoryId, "userId", userId));
        if (rows.isEmpty()) throw new BizException("旅行记忆不存在或无权访问。");
        return rows.get(0);
    }

    private String controlledUpload(String value) {
        if (value.isBlank()) throw new BizException("请先上传照片。");
        try {
            URI uri = URI.create(value);
            if (uri.getQuery() != null || uri.getFragment() != null || uri.getUserInfo() != null
                || (uri.getScheme() != null && !Set.of("http", "https").contains(uri.getScheme().toLowerCase()))) {
                throw new BizException("照片地址不是受控上传路径。");
            }
            String path = uri.getPath();
            if (path == null || !UPLOAD_PATH.matcher(path).matches()) throw new BizException("照片地址不是受控上传路径。");
            Path directory = Path.of(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
            Path file = directory.resolve(Path.of(path).getFileName()).normalize();
            if (!file.startsWith(directory) || !Files.isRegularFile(file)) throw new BizException("上传的照片不存在。");
            return path;
        } catch (IllegalArgumentException ex) {
            throw new BizException("照片地址不是受控上传路径。");
        }
    }

    private LocalDateTime dateTime(String value) {
        if (value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new BizException("拍摄时间格式不正确，请使用 ISO 日期时间。");
        }
    }

    private BigDecimal coordinate(Object value, int min, int max, String label) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try {
            BigDecimal coordinate = new BigDecimal(String.valueOf(value));
            if (coordinate.compareTo(BigDecimal.valueOf(min)) < 0 || coordinate.compareTo(BigDecimal.valueOf(max)) > 0) {
                throw new BizException(label + "超出有效范围。");
            }
            return coordinate;
        } catch (NumberFormatException ex) {
            throw new BizException(label + "格式不正确。");
        }
    }

    private BigDecimal checkedCoordinate(BigDecimal value, int min, int max, String label) {
        if (value != null && (value.compareTo(BigDecimal.valueOf(min)) < 0 || value.compareTo(BigDecimal.valueOf(max)) > 0)) {
            throw new BizException(label + "超出有效范围。");
        }
        return value;
    }

    private Integer integer(Object value, int min, int max, String label) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try {
            int result = Integer.parseInt(String.valueOf(value));
            if (result < min || result > max) throw new BizException(label + "超出有效范围。");
            return result;
        } catch (NumberFormatException ex) {
            throw new BizException(label + "格式不正确。");
        }
    }

    private List<String> safeTags(List<String> values) {
        if (values == null) return List.of();
        if (values.size() > 20) throw new BizException("AI 标签最多 20 个。");
        List<String> tags = new ArrayList<>();
        for (String value : values) {
            String tag = limited(value, 64, "AI 标签");
            if (!tag.isBlank() && !tags.contains(tag)) tags.add(tag);
        }
        return List.copyOf(tags);
    }

    private String text(Map<String, Object> payload, String key, int max) {
        Object value = payload == null ? null : payload.get(key);
        return limited(value == null ? "" : String.valueOf(value), max, key);
    }

    private String limited(String value, int max, String label) {
        String result = value == null ? "" : value.trim();
        if (result.length() > max) throw new BizException(label + "内容过长。");
        return result;
    }

    private Number number(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number : null;
    }

    private LocalDateTime dateAtStart(Object value) {
        if (value instanceof java.sql.Date date) return date.toLocalDate().atStartOfDay();
        if (value instanceof java.time.LocalDate date) return date.atStartOfDay();
        return null;
    }

    private LocalDateTime dateTime(Object dateValue, Object timeValue) {
        java.time.LocalDate date = dateValue instanceof java.sql.Date sqlDate ? sqlDate.toLocalDate()
            : dateValue instanceof java.time.LocalDate localDate ? localDate : null;
        if (date == null || timeValue == null || String.valueOf(timeValue).isBlank()) return null;
        try {
            return date.atTime(java.time.LocalTime.parse(String.valueOf(timeValue)));
        } catch (java.time.format.DateTimeParseException ex) {
            return null;
        }
    }

    private String join(Object... values) {
        return java.util.Arrays.stream(values).filter(value -> value != null && !String.valueOf(value).isBlank())
            .map(String::valueOf).collect(java.util.stream.Collectors.joining("；"));
    }

    private long nextId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }
}
