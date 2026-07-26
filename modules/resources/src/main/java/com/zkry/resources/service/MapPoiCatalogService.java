package com.zkry.resources.service;

import com.zkry.map.dto.PublicTravelMapSnapshot;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/** 保存最近一次成功 POI；外部地图波动时用目录中的旧结果兜底。 */
@Service
public class MapPoiCatalogService {

    private static final Logger log = LoggerFactory.getLogger(MapPoiCatalogService.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MapPoiCatalogService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PublicTravelMapSnapshot.Place> rememberAndList(
        String city, double longitude, double latitude, List<PublicTravelMapSnapshot.Place> live
    ) {
        try {
            remember(city, live);
            List<PublicTravelMapSnapshot.Place> stored = list(city, longitude, latitude);
            return stored.isEmpty() ? rank(live) : stored;
        } catch (DataAccessException ex) {
            log.info("地图 POI 目录暂不可用 city={} reason={}", city, ex.getMessage());
            return rank(live);
        }
    }

    private void remember(String city, List<PublicTravelMapSnapshot.Place> places) {
        if (places == null || places.isEmpty()) return;
        List<Map<String, Object>> rows = places.stream()
            .filter(place -> place != null && !place.name().isBlank() && !place.source().isBlank())
            .map(place -> row(city, place)).toList();
        if (rows.isEmpty()) return;
        @SuppressWarnings("unchecked")
        Map<String, ?>[] batch = rows.toArray(Map[]::new);
        jdbcTemplate.batchUpdate("""
            INSERT INTO tm_map_poi
              (source, source_id, city, city_id, name, kind, longitude, latitude, address, category, opening_hours,
               rating, cost, image_url, tags, source_updated_at)
            VALUES
              (:source, :sourceId, :city,
               (SELECT id FROM tm_city WHERE name = :city AND deleted = 0 ORDER BY status DESC LIMIT 1),
               :name, :kind, :longitude, :latitude, :address, :category, :openingHours,
               :rating, :cost, :imageUrl, :tags, :updatedAt)
            ON DUPLICATE KEY UPDATE
              city = VALUES(city), city_id = VALUES(city_id), name = VALUES(name), kind = VALUES(kind), longitude = VALUES(longitude),
              latitude = VALUES(latitude), address = VALUES(address), category = VALUES(category),
              opening_hours = VALUES(opening_hours), rating = VALUES(rating), cost = VALUES(cost),
              image_url = VALUES(image_url), tags = VALUES(tags), source_updated_at = VALUES(source_updated_at),
              last_seen_at = CURRENT_TIMESTAMP, status = 1, deleted = 0
            """, batch);
    }

    private List<PublicTravelMapSnapshot.Place> list(String city, double longitude, double latitude) {
        List<PublicTravelMapSnapshot.Place> places = jdbcTemplate.query("""
            SELECT p.*,
              (SELECT COUNT(1)
                 FROM tm_travel_note n JOIN tm_city c ON c.id = n.city_id AND c.deleted = 0
                WHERE c.name = p.city AND n.deleted = 0 AND n.visibility = 'public' AND n.status = 1
                  AND (n.title LIKE CONCAT('%', p.name, '%') OR n.content LIKE CONCAT('%', p.name, '%')
                    OR (CHAR_LENGTH(p.community_alias) >= 2 AND p.community_alias <> p.city AND
                      (n.title LIKE CONCAT('%', p.community_alias, '%')
                        OR n.content LIKE CONCAT('%', p.community_alias, '%'))))
              ) AS community_mentions,
              (SELECT n.title
                 FROM tm_travel_note n JOIN tm_city c ON c.id = n.city_id AND c.deleted = 0
                WHERE c.name = p.city AND n.deleted = 0 AND n.visibility = 'public' AND n.status = 1
                  AND (n.title LIKE CONCAT('%', p.name, '%') OR n.content LIKE CONCAT('%', p.name, '%')
                    OR (CHAR_LENGTH(p.community_alias) >= 2 AND p.community_alias <> p.city AND
                      (n.title LIKE CONCAT('%', p.community_alias, '%')
                        OR n.content LIKE CONCAT('%', p.community_alias, '%'))))
                ORDER BY n.update_time DESC, n.id DESC LIMIT 1
              ) AS community_tip
            FROM (
              SELECT p.*,
                TRIM(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                  SUBSTRING_INDEX(SUBSTRING_INDEX(p.name, '(', 1), '（', 1),
                  '风景名胜区', ''), '旅游景区', ''), '博物馆', ''), '博物院', ''), '景区', ''), '公园', ''))
                  AS community_alias
              FROM tm_map_poi p
              WHERE p.city = :city AND p.status = 1 AND p.deleted = 0
              ORDER BY p.last_seen_at DESC
            ) p
            ORDER BY p.last_seen_at DESC
            """, Map.of("city", city), (rs, rowNum) -> place(rs, longitude, latitude));
        return rank(places);
    }

    static List<PublicTravelMapSnapshot.Place> rank(List<PublicTravelMapSnapshot.Place> places) {
        Map<String, PublicTravelMapSnapshot.Place> unique = new LinkedHashMap<>();
        for (PublicTravelMapSnapshot.Place place : places) {
            String key = normalize(place.name());
            PublicTravelMapSnapshot.Place current = unique.get(key);
            if (current == null || better(place, current)) unique.put(key, place);
        }
        Map<String, List<PublicTravelMapSnapshot.Place>> groups = new LinkedHashMap<>();
        groups.put("attraction", new ArrayList<>());
        groups.put("hotel", new ArrayList<>());
        groups.put("restaurant", new ArrayList<>());
        unique.values().forEach(place -> groups.computeIfAbsent(place.kind(), ignored -> new ArrayList<>()).add(place));
        Comparator<PublicTravelMapSnapshot.Place> order = Comparator
            .comparingInt(PublicTravelMapSnapshot.Place::community_mentions).reversed()
            .thenComparing(place -> place.rating() == null ? 0 : place.rating(), Comparator.reverseOrder())
            .thenComparingDouble(PublicTravelMapSnapshot.Place::distance_km);
        return groups.values().stream().flatMap(group -> group.stream().sorted(order)).toList();
    }

    private static boolean better(PublicTravelMapSnapshot.Place candidate, PublicTravelMapSnapshot.Place current) {
        if (candidate.community_mentions() != current.community_mentions()) {
            return candidate.community_mentions() > current.community_mentions();
        }
        double candidateRating = candidate.rating() == null ? 0 : candidate.rating();
        double currentRating = current.rating() == null ? 0 : current.rating();
        return candidateRating > currentRating || candidate.distance_km() < current.distance_km();
    }

    private PublicTravelMapSnapshot.Place place(ResultSet rs, double longitude, double latitude) throws SQLException {
        double placeLongitude = rs.getDouble("longitude");
        double placeLatitude = rs.getDouble("latitude");
        return new PublicTravelMapSnapshot.Place(
            "catalog-" + rs.getLong("id"), rs.getString("name"), rs.getString("kind"),
            placeLongitude, placeLatitude, safe(rs.getString("address")), safe(rs.getString("category")),
            safe(rs.getString("opening_hours")), distanceKm(longitude, latitude, placeLongitude, placeLatitude),
            nullableDouble(rs, "rating"), nullableDouble(rs, "cost"), safe(rs.getString("image_url")),
            safe(rs.getString("tags")), rs.getInt("community_mentions"), safe(rs.getString("community_tip")),
            rs.getString("source"), safe(rs.getString("source_updated_at"))
        );
    }

    private String sourceId(String city, PublicTravelMapSnapshot.Place place) {
        if (place.id() != null && !place.id().isBlank()) return place.id();
        return city + ':' + normalize(place.name()) + ':' + place.longitude() + ':' + place.latitude();
    }

    private Map<String, Object> row(String city, PublicTravelMapSnapshot.Place place) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("source", place.source());
        row.put("sourceId", sourceId(city, place));
        row.put("city", city);
        row.put("name", place.name());
        row.put("kind", place.kind());
        row.put("longitude", place.longitude());
        row.put("latitude", place.latitude());
        row.put("address", safe(place.address()));
        row.put("category", safe(place.category()));
        row.put("openingHours", safe(place.opening_hours()));
        row.put("rating", place.rating());
        row.put("cost", place.cost());
        row.put("imageUrl", safe(place.image_url()));
        row.put("tags", safe(place.tags()));
        row.put("updatedAt", safe(place.updated_at()));
        return row;
    }

    private static String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT).replaceAll("[\\s·•・—_\\-（）()]", "");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private Double nullableDouble(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private double distanceKm(double fromLongitude, double fromLatitude, double toLongitude, double toLatitude) {
        double latitudeDistance = Math.toRadians(toLatitude - fromLatitude);
        double longitudeDistance = Math.toRadians(toLongitude - fromLongitude);
        double a = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
            + Math.cos(Math.toRadians(fromLatitude)) * Math.cos(Math.toRadians(toLatitude))
            * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
        return Math.round(6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 10) / 10.0;
    }
}
