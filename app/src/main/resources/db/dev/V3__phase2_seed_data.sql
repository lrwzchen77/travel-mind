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
  (2001, '杭州', '浙江省', '中国', '西湖、龙井茶与江南街巷交织的慢生活目的地。', 95, 1),
  (2002, '北京', '北京市', '中国', '故宫、胡同、博物馆与经典地标汇聚的古都。', 98, 1),
  (2003, '成都', '四川省', '中国', '美食、熊猫、公园与闲适生活共同构成天府之城。', 96, 1),
  (2004, '天津', '天津市', '中国', '海河风光、万国建筑与津味小吃相映成趣。', 86, 1),
  (2005, '石家庄', '河北省', '中国', '连接正定古城与太行山风光的燕赵门户。', 78, 1),
  (2006, '太原', '山西省', '中国', '晋祠古建、汾河夜景与山西面食值得细品。', 82, 1),
  (2007, '呼和浩特', '内蒙古自治区', '中国', '青城人文与辽阔草原自然衔接。', 84, 1),
  (2008, '沈阳', '辽宁省', '中国', '盛京古迹、东北老街与工业文化并存。', 87, 1),
  (2009, '长春', '吉林省', '中国', '电影记忆、城市公园与冬日冰雪各有风情。', 82, 1),
  (2010, '哈尔滨', '黑龙江省', '中国', '欧式建筑、中央大街与冰雪世界组成北国童话。', 94, 1),
  (2011, '上海', '上海市', '中国', '外滩天际线、梧桐街区与海派生活交融。', 98, 1),
  (2012, '南京', '江苏省', '中国', '城墙、博物院与梧桐大道写满六朝故事。', 94, 1),
  (2013, '合肥', '安徽省', '中国', '逍遥津、包公园与巢湖岸边适合慢游。', 80, 1),
  (2014, '福州', '福建省', '中国', '三坊七巷、温泉与闽味构成温润榕城。', 86, 1),
  (2015, '南昌', '江西省', '中国', '滕王阁、赣江夜色与豫章文脉相互映照。', 83, 1),
  (2016, '济南', '山东省', '中国', '泉水穿城，大明湖与老街共同描绘泉城日常。', 88, 1),
  (2017, '郑州', '河南省', '中国', '从黄河文明到当代城市，一站读懂中原脉络。', 85, 1),
  (2018, '武汉', '湖北省', '中国', '长江、东湖、大学与市井美食汇成江城气质。', 95, 1),
  (2019, '长沙', '湖南省', '中国', '湘江夜色、岳麓文脉与热辣夜市充满活力。', 96, 1),
  (2020, '广州', '广东省', '中国', '骑楼老街、珠江夜游与广府早茶最具花城风味。', 97, 1),
  (2021, '南宁', '广西壮族自治区', '中国', '青秀山常绿，壮乡文化与酸辣鲜香相遇。', 84, 1),
  (2022, '海口', '海南省', '中国', '骑楼老街、火山地貌与海边落日适合轻松度假。', 89, 1),
  (2023, '重庆', '重庆市', '中国', '穿楼轻轨、江岸灯火与热辣火锅组成立体山城。', 97, 1),
  (2024, '贵阳', '贵州省', '中国', '清凉林城、黔地人文与酸汤风味带来舒适旅程。', 87, 1),
  (2025, '昆明', '云南省', '中国', '四季花开，滇池与云南风味让春城适合慢游。', 93, 1),
  (2026, '拉萨', '西藏自治区', '中国', '雪域古城、高原阳光与深厚人文令人放慢脚步。', 92, 1),
  (2027, '西安', '陕西省', '中国', '城墙、博物馆、唐风街区与西北美食汇聚。', 97, 1),
  (2028, '兰州', '甘肃省', '中国', '黄河穿城，牛肉面与西北文化都不可错过。', 87, 1),
  (2029, '西宁', '青海省', '中国', '清凉夏都、河湟文化与青海湖公路在此相遇。', 84, 1),
  (2030, '银川', '宁夏回族自治区', '中国', '贺兰山、湖泊湿地与西夏遗迹铺开塞上风光。', 83, 1),
  (2031, '乌鲁木齐', '新疆维吾尔自治区', '中国', '大巴扎烟火与天山脚下的辽阔风景相互映照。', 91, 1),
  (2032, '台北', '台湾省', '中国', '老街、博物馆、夜市与近郊山景适合慢慢探索。', 90, 1),
  (2033, '香港', '香港特别行政区', '中国', '维港两岸、山海步道与街巷美食浓缩都市节奏。', 96, 1),
  (2034, '澳门', '澳门特别行政区', '中国', '世界遗产街区、葡式风味与海边小城尺度刚刚好。', 90, 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name), province = VALUES(province), country = VALUES(country),
  description = VALUES(description), popularity = VALUES(popularity), status = VALUES(status),
  update_time = CURRENT_TIMESTAMP;

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
