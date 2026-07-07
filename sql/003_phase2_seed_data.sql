USE travelmind;

INSERT INTO tm_user (id, username, nickname, phone, email, status)
VALUES
  (1001, 'demo_user', 'Demo Traveler', '13800000000', 'demo@example.local', 1)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), update_time = CURRENT_TIMESTAMP;

INSERT INTO tm_user_preference (id, user_id, budget_level, travel_style, preferred_city, preferred_tags, transportation, hotel_level, diet_preference)
VALUES
  (1101, 1001, 'medium', 'culture,food,slow-travel', 'Hangzhou', 'culture,food,lake', 'train', 'comfortable', 'local food')
ON DUPLICATE KEY UPDATE travel_style = VALUES(travel_style), update_time = CURRENT_TIMESTAMP;

INSERT INTO tm_city (id, name, province, country, description, popularity, status)
VALUES
  (2001, 'Hangzhou', 'Zhejiang', 'China', 'Lake, tea culture, and relaxed city walks.', 95, 1),
  (2002, 'Beijing', 'Beijing', 'China', 'History, museums, hutongs, and classic landmarks.', 98, 1),
  (2003, 'Chengdu', 'Sichuan', 'China', 'Food, pandas, parks, and laid-back city life.', 92, 1)
ON DUPLICATE KEY UPDATE description = VALUES(description), popularity = VALUES(popularity), update_time = CURRENT_TIMESTAMP;

INSERT INTO tm_travel_tag (id, name, category, color, status)
VALUES
  (3001, 'culture', 'theme', '#136f63', 1),
  (3002, 'food', 'theme', '#9a3412', 1),
  (3003, 'family', 'traveler', '#1d4ed8', 1),
  (3004, 'lake', 'scene', '#047857', 1),
  (3005, 'museum', 'scene', '#7c3aed', 1)
ON DUPLICATE KEY UPDATE category = VALUES(category), color = VALUES(color), update_time = CURRENT_TIMESTAMP;

INSERT INTO tm_attraction (id, city_id, name, category, address, description, rating, price, tags, opening_hours, status)
VALUES
  (4001, 2001, 'West Lake', 'nature', 'Xihu District, Hangzhou', 'Classic lakeside walking and cycling area.', 4.9, 0.00, 'lake,culture,family', 'All day', 1),
  (4002, 2001, 'Lingyin Temple', 'culture', 'Fayun Lane, Hangzhou', 'Historic temple near forested hills.', 4.7, 45.00, 'culture,temple', '07:00-18:00', 1),
  (4003, 2002, 'Forbidden City', 'culture', 'Dongcheng District, Beijing', 'Imperial palace museum and landmark.', 4.8, 60.00, 'culture,museum', '08:30-17:00', 1),
  (4004, 2003, 'Chengdu Research Base of Giant Panda Breeding', 'family', 'Chenghua District, Chengdu', 'Panda conservation and visitor park.', 4.8, 55.00, 'family,nature', '07:30-18:00', 1)
ON DUPLICATE KEY UPDATE description = VALUES(description), rating = VALUES(rating), update_time = CURRENT_TIMESTAMP;

INSERT INTO tm_hotel (id, city_id, name, category, address, description, rating, price_range, tags, status)
VALUES
  (5001, 2001, 'West Lake Comfort Hotel', 'comfort', 'Near West Lake, Hangzhou', 'Convenient hotel for lake walks.', 4.5, '500-800', 'lake,comfort', 1),
  (5002, 2002, 'Hutong Boutique Stay', 'boutique', 'Dongcheng District, Beijing', 'Small hotel close to historic streets.', 4.4, '600-900', 'culture,boutique', 1),
  (5003, 2003, 'Chengdu Food Street Hotel', 'comfort', 'Jinjiang District, Chengdu', 'Good base for food exploration.', 4.3, '400-700', 'food,comfort', 1)
ON DUPLICATE KEY UPDATE rating = VALUES(rating), price_range = VALUES(price_range), update_time = CURRENT_TIMESTAMP;

INSERT INTO tm_restaurant (id, city_id, name, category, cuisine, address, description, rating, average_cost, tags, status)
VALUES
  (6001, 2001, 'Longjing Tea House', 'local', 'Hangzhou cuisine', 'Meijiawu, Hangzhou', 'Tea village dishes and green tea.', 4.6, 120.00, 'food,tea', 1),
  (6002, 2002, 'Hutong Noodle Kitchen', 'local', 'Beijing cuisine', 'Dongcheng District, Beijing', 'Noodles and Beijing snacks.', 4.4, 80.00, 'food,culture', 1),
  (6003, 2003, 'Chengdu Hotpot Demo', 'hotpot', 'Sichuan cuisine', 'Jinjiang District, Chengdu', 'Classic spicy hotpot.', 4.7, 130.00, 'food,spicy', 1)
ON DUPLICATE KEY UPDATE rating = VALUES(rating), average_cost = VALUES(average_cost), update_time = CURRENT_TIMESTAMP;

INSERT INTO tm_favorite (id, user_id, target_type, target_id, note)
VALUES
  (7001, 1001, 'attraction', 4001, 'Good for first day walk')
ON DUPLICATE KEY UPDATE note = VALUES(note), update_time = CURRENT_TIMESTAMP;

INSERT INTO tm_travel_note (id, user_id, city_id, attraction_id, title, content, visibility, status)
VALUES
  (8001, 1001, 2001, 4001, 'West Lake morning route', 'Start early from Broken Bridge and walk toward Su Causeway.', 'public', 1)
ON DUPLICATE KEY UPDATE content = VALUES(content), update_time = CURRENT_TIMESTAMP;

INSERT INTO tm_trip_plan (id, user_id, title, destination_city, start_date, end_date, travel_days, budget, total_cost, status, summary)
VALUES
  (9001, 1001, 'Hangzhou demo weekend', 'Hangzhou', '2026-08-01', '2026-08-02', 2, 2000.00, 1280.00, 'saved', 'A relaxed West Lake and tea culture weekend.')
ON DUPLICATE KEY UPDATE summary = VALUES(summary), update_time = CURRENT_TIMESTAMP;

INSERT INTO tm_trip_day (id, trip_plan_id, day_no, date, title, summary)
VALUES
  (9101, 9001, 1, '2026-08-01', 'West Lake walk', 'Lake walk, lunch, and evening rest.'),
  (9102, 9001, 2, '2026-08-02', 'Temple and tea village', 'Lingyin Temple and Longjing tea area.')
ON DUPLICATE KEY UPDATE summary = VALUES(summary), update_time = CURRENT_TIMESTAMP;

INSERT INTO tm_trip_item (id, trip_day_id, item_order, item_type, title, location, start_time, end_time, cost, note)
VALUES
  (9201, 9101, 1, 'attraction', 'West Lake', 'Hangzhou', '09:00', '12:00', 0.00, 'Walk slowly and avoid noon heat.'),
  (9202, 9102, 1, 'attraction', 'Lingyin Temple', 'Hangzhou', '09:00', '11:30', 45.00, 'Buy tickets before arrival.')
ON DUPLICATE KEY UPDATE note = VALUES(note), update_time = CURRENT_TIMESTAMP;

INSERT INTO tm_ai_analysis_record (id, user_id, analysis_type, target_type, target_id, request_summary, result_summary, status)
VALUES
  (10001, 1001, 'content_analysis', 'travel_note', 8001, 'Analyze West Lake route note', 'Positive slow-travel note with lake and walking keywords.', 'success')
ON DUPLICATE KEY UPDATE result_summary = VALUES(result_summary), update_time = CURRENT_TIMESTAMP;
