const demoUser = {
  id: 1,
  username: 'demo_user',
  nickname: '旅行者',
  name: '旅行者',
  email: 'demo@travelmind.cn',
  phone: '13800000000',
  status: 'ACTIVE',
  roles: ['user'],
  created_at: '2026-08-01T08:00:00',
};

const demoAdmin = {
  id: 2,
  username: 'admin',
  nickname: '管理员',
  name: '管理员',
  email: 'admin@travelmind.cn',
  phone: '13900000000',
  status: 'ACTIVE',
  roles: ['admin'],
  created_at: '2026-07-01T08:00:00',
};

const demoSession = {
  tokenValue: 'demo-token-for-pages',
  user: demoUser,
};

const demoAdminSession = {
  tokenValue: 'demo-admin-token-for-pages',
  user: demoAdmin,
};

const cities = [
  { id: 1, name: '杭州', province: '浙江', country: '中国', popularity: 98, status: 'ACTIVE', description: '西湖、龙井、灵隐寺，淡妆浓抹总相宜。', tags: '湖景,文化,美食,亲子' },
  { id: 2, name: '成都', province: '四川', country: '中国', popularity: 95, status: 'ACTIVE', description: '宽窄巷子、太古里、熊猫基地，烟火气满满。', tags: '美食,文化,熊猫,市井' },
  { id: 3, name: '厦门', province: '福建', country: '中国', popularity: 88, status: 'ACTIVE', description: '鼓浪屿、环岛路、沙坡尾，海风与文艺。', tags: '海岛,文艺,拍照,美食' },
  { id: 4, name: '西安', province: '陕西', country: '中国', popularity: 90, status: 'ACTIVE', description: '城墙、回民街、兵马俑，古今同框。', tags: '历史,文化,小吃,夜景' },
  { id: 5, name: '大理', province: '云南', country: '中国', popularity: 82, status: 'ACTIVE', description: '苍山洱海，风花雪月，慢生活的理想之地。', tags: '自然,慢游,文艺,徒步' },
  { id: 6, name: '青岛', province: '山东', country: '中国', popularity: 80, status: 'ACTIVE', description: '栈桥、八大关、啤酒节，红瓦绿树碧海。', tags: '海岛,啤酒,拍照,美食' },
];

const attractions = [
  { id: 1, city: '杭州', name: '西湖', category: '自然景观', rating: 5.0, price: 0, tags: '湖景,免费,经典', status: 'ACTIVE' },
  { id: 2, city: '杭州', name: '灵隐寺', category: '宗教文化', rating: 4.7, price: 75, tags: '寺庙,文化,历史', status: 'ACTIVE' },
  { id: 3, city: '杭州', name: '千岛湖', category: '自然景观', rating: 4.6, price: 130, tags: '湖景,岛屿,亲子', status: 'ACTIVE' },
  { id: 4, city: '成都', name: '宽窄巷子', category: '历史街区', rating: 4.5, price: 0, tags: '市井,美食,文化,免费', status: 'ACTIVE' },
  { id: 5, city: '成都', name: '大熊猫繁育研究基地', category: '主题乐园', rating: 4.8, price: 58, tags: '熊猫,亲子,自然', status: 'ACTIVE' },
  { id: 6, city: '厦门', name: '鼓浪屿', category: '海岛', rating: 4.7, price: 35, tags: '海岛,文艺,建筑,拍照', status: 'ACTIVE' },
  { id: 7, city: '西安', name: '秦始皇兵马俑博物馆', category: '博物馆', rating: 4.9, price: 120, tags: '历史,文化,世界遗产', status: 'ACTIVE' },
  { id: 8, city: '西安', name: '大雁塔', category: '宗教文化', rating: 4.6, price: 30, tags: '寺庙,夜景,文化', status: 'ACTIVE' },
];

const hotels = [
  { id: 1, city: '杭州', name: '西湖国宾馆', category: '豪华型', rating: 4.9, price_range: '800-2000', tags: '湖景,经典,高端', status: 'ACTIVE' },
  { id: 2, city: '杭州', name: '西溪花间堂', category: '精品民宿', rating: 4.7, price_range: '500-1000', tags: '民宿,湿地,安静', status: 'ACTIVE' },
  { id: 3, city: '成都', name: '博舍酒店', category: '豪华型', rating: 4.8, price_range: '800-1800', tags: '设计,太古里,高端', status: 'ACTIVE' },
  { id: 4, city: '成都', name: '宽窄民宿', category: '精品民宿', rating: 4.6, price_range: '300-600', tags: '民宿,巷子,市井', status: 'ACTIVE' },
  { id: 5, city: '厦门', name: '鼓浪屿海上花园酒店', category: '精品民宿', rating: 4.7, price_range: '400-900', tags: '海景,民宿,文艺', status: 'ACTIVE' },
  { id: 6, city: '西安', name: '唐华华邑酒店', category: '豪华型', rating: 4.8, price_range: '600-1500', tags: '唐风,高端,大雁塔', status: 'ACTIVE' },
];

const restaurants = [
  { id: 1, city: '杭州', name: '楼外楼', cuisine: '杭帮菜', rating: 4.5, average_cost: 200, tags: '西湖醋鱼,龙井虾仁,老字号', status: 'ACTIVE' },
  { id: 2, city: '杭州', name: '知味观', cuisine: '杭帮菜', rating: 4.3, average_cost: 100, tags: '小笼包,传统,老字号', status: 'ACTIVE' },
  { id: 3, city: '成都', name: '陈麻婆豆腐', cuisine: '川菜', rating: 4.6, average_cost: 80, tags: '麻婆豆腐,川菜,辣', status: 'ACTIVE' },
  { id: 4, city: '成都', name: '蜀大侠火锅', cuisine: '火锅', rating: 4.7, average_cost: 120, tags: '火锅,辣,市井', status: 'ACTIVE' },
  { id: 5, city: '厦门', name: '黄则和花生汤', cuisine: '闽南菜', rating: 4.2, average_cost: 30, tags: '小吃,花生汤,老字号', status: 'ACTIVE' },
  { id: 6, city: '西安', name: '老孙家泡馍', cuisine: '西北菜', rating: 4.5, average_cost: 60, tags: '泡馍,羊肉,老字号', status: 'ACTIVE' },
];

const trips = [
  {
    id: 1,
    title: '杭州周末慢游',
    destination_city: '杭州',
    status: 'COMPLETED',
    start_date: '2026-08-10',
    travel_days: 2,
    transportation: '公共交通',
    accommodation: '舒适型酒店',
    budget: 3000,
    adults: 2,
    children: 0,
    preferences: ['湖景', '美食', '轻松'],
    days: [
      {
        date: '2026-08-10',
        items: [
          { time: '09:00', name: '西湖漫步', type: 'attraction', duration: '2h', cost: 0, note: '白堤—苏堤—断桥，慢慢走' },
          { time: '12:00', name: '楼外楼午餐', type: 'restaurant', duration: '1.5h', cost: 200, note: '西湖醋鱼必点' },
          { time: '14:30', name: '灵隐寺', type: 'attraction', duration: '2h', cost: 75, note: '飞来峰也值得看' },
          { time: '17:00', name: '河坊街闲逛', type: 'shopping', duration: '1.5h', cost: 0, note: '小吃和手伴' },
        ],
      },
      {
        date: '2026-08-11',
        items: [
          { time: '09:00', name: '龙井村采茶', type: 'attraction', duration: '2h', cost: 50, note: '明前龙井的产地' },
          { time: '12:00', name: '知味观午餐', type: 'restaurant', duration: '1h', cost: 100, note: '小笼包和猫耳朵' },
          { time: '14:00', name: '西溪湿地', type: 'attraction', duration: '3h', cost: 80, note: '摇橹船慢游' },
        ],
      },
    ],
    budget_breakdown: { attraction: 205, food: 300, accommodation: 800, transport: 200, shopping: 150, total: 1655 },
    overall_suggestions: '行程节奏轻松，以湖景和美食为主线，适合第一次到杭州的旅行者。建议雨天备选浙江省博物馆。',
  },
  {
    id: 2,
    title: '成都三日烟火行',
    destination_city: '成都',
    status: 'SAVED',
    start_date: '2026-08-20',
    travel_days: 3,
    transportation: '公共交通',
    accommodation: '精品民宿',
    budget: 5000,
    adults: 2,
    children: 0,
    preferences: ['美食', '文化', '市井'],
    days: [],
    budget_breakdown: {},
    overall_suggestions: '以宽窄巷子和太古里为核心，穿插火锅与熊猫基地。',
  },
];

const memories = [
  { id: 1, title: '杭州西湖的清晨', trip_id: 1, city: '杭州', created_at: '2026-08-12T10:00:00', items: [
    { id: 1, type: 'photo', url: '', caption: '白堤日出' },
    { id: 2, type: 'photo', url: '', caption: '断桥残雪碑' },
  ], summary: '清晨的西湖格外安静，白堤上只有晨跑的人。' },
  { id: 2, title: '成都巷子里的猫', trip_id: 2, city: '成都', created_at: '2026-08-22T15:00:00', items: [
    { id: 3, type: 'photo', url: '', caption: '宽窄巷子的橘猫' },
  ], summary: '宽窄巷子口那只橘猫又在晒太阳了。' },
];

const journals = [
  { id: 1, title: '杭州：一座来了就不想走的城市', city: '杭州', status: 'PUBLISHED', created_at: '2026-08-12T20:00:00', content: '从西湖的晨雾到灵隐的钟声，杭州的美不在景点，在路上。', photos: [], locations: [{ name: '西湖', longitude: 120.15, latitude: 30.25 }] },
  { id: 2, title: '成都：在烟火气里找到慢节奏', city: '成都', status: 'DRAFT', created_at: '2026-08-22T21:00:00', content: '宽窄巷子的猫、太古里的灯、玉林路的酒，成都是一座不需要景点的城市。', photos: [], locations: [{ name: '宽窄巷子', longitude: 104.06, latitude: 30.67 }] },
];

const communityPosts = [
  { id: 1, title: '杭州两日精华路线，第一次去就收藏', author: '旅行者', city: '杭州', created_at: '2026-08-13T09:00:00', likes: 128, comments_count: 15, status: 'APPROVED', cover_image: '', excerpt: '白堤日出 → 楼外楼 → 灵隐寺 → 河坊街，第二天龙井 + 西溪。', tags: '杭州,周末,慢游' },
  { id: 2, title: '成都美食地图：跟着本地人吃了三天', author: '美食家小王', city: '成都', created_at: '2026-08-10T14:00:00', likes: 256, comments_count: 32, status: 'APPROVED', cover_image: '', excerpt: '从蜀大侠到陈麻婆，从甜水面到钵钵鸡，成都的辣是分层的。', tags: '成都,美食,火锅' },
  { id: 3, title: '厦门鼓浪屿：最全拍照机位指南', author: '文艺旅人', city: '厦门', created_at: '2026-08-05T10:00:00', likes: 189, comments_count: 21, status: 'APPROVED', cover_image: '', excerpt: '日光岩、菽庄花园、八卦楼……鼓浪屿每个转角都是风景。', tags: '厦门,鼓浪屿,拍照' },
  { id: 4, title: '西安城墙骑行 + 回民街宵夜攻略', author: '历史迷小李', city: '西安', created_at: '2026-07-28T18:00:00', likes: 142, comments_count: 18, status: 'APPROVED', cover_image: '', excerpt: '城墙一圈14公里刚好1小时，下来直奔回民街。', tags: '西安,骑行,美食' },
];

const communityComments = {
  1: [
    { id: 1, author: '小张', content: '白堤日出真的绝美！几点去的？', created_at: '2026-08-13T10:00:00' },
    { id: 2, author: '旅途中', content: '楼外楼价格小贵但风景无价', created_at: '2026-08-13T11:00:00' },
  ],
  2: [
    { id: 3, author: '吃货阿May', content: '甜水面是成都最被低估的小吃！', created_at: '2026-08-10T15:00:00' },
  ],
};

const recommendations = {
  city: [
    { id: 1, type: 'city', city: '杭州', title: '杭州', reason: '基于你的湖景偏好，西湖龙井正当季', score: 0.95 },
    { id: 2, type: 'city', city: '成都', title: '成都', reason: '你偏爱美食与文化，成都宽窄巷子不容错过', score: 0.88 },
    { id: 3, type: 'city', city: '厦门', title: '厦门', reason: '海岛+文艺标签命中你的旅行风格', score: 0.82 },
    { id: 4, type: 'city', city: '西安', title: '西安', reason: '历史爱好者的天堂，城墙骑行体验独特', score: 0.78 },
  ],
  attraction: [
    { id: 5, type: 'attraction', city: '杭州', title: '西湖', reason: '经典必打卡，免费且四季皆美', score: 0.98 },
    { id: 6, type: 'attraction', city: '成都', title: '大熊猫繁育研究基地', reason: '亲子友好，早起去人少', score: 0.90 },
  ],
};

const notifications = [
  { id: 1, title: '行程已生成', content: '你的「杭州周末慢游」行程已生成，快去看看吧！', read_at: null, created_at: '2026-08-09T10:00:00', target_url: '/trip/1' },
  { id: 2, title: '游记审核通过', content: '你的游记「杭州：一座来了就不想走的城市」已通过审核。', read_at: '2026-08-13T09:00:00', created_at: '2026-08-13T08:00:00', target_url: '/journals/1' },
  { id: 3, title: '推荐更新', content: '根据你的旅行偏好，为你推荐了新的目的地。', read_at: null, created_at: '2026-08-14T12:00:00', target_url: '/recommendations' },
];

const profile = {
  user: demoUser,
  preference: {
    budget_level: 'medium',
    travel_style: '轻松慢游',
    preferred_city: '杭州',
    preferred_tags: '湖景,美食,轻松',
    transportation: '公共交通',
    hotel_level: '舒适型',
    diet_preference: '本地菜',
  },
};

const inspirationBag = [
  { id: 1, post_id: 2, intent: 'reference', post: communityPosts[1] },
  { id: 2, post_id: 4, intent: 'reference', post: communityPosts[3] },
];

const assistantConversations = [
  { id: 1, title: '杭州周末怎么玩', created_at: '2026-08-08T10:00:00', updated_at: '2026-08-08T10:30:00' },
  { id: 2, title: '成都美食推荐', created_at: '2026-08-09T14:00:00', updated_at: '2026-08-09T14:15:00' },
];

const adminStats = {
  users: 128,
  'trip-plans': 56,
  cities: 6,
  'ai-records': 312,
};

const adminUsers = [
  { id: 1, username: 'demo_user', nickname: '旅行者', phone: '13800000000', email: 'demo@travelmind.cn', status: 'ACTIVE' },
  { id: 2, username: 'admin', nickname: '管理员', phone: '13900000000', email: 'admin@travelmind.cn', status: 'ACTIVE' },
  { id: 3, username: 'traveler01', nickname: '小张', phone: '13700000001', email: 'zhang@example.com', status: 'ACTIVE' },
  { id: 4, username: 'foodie_may', nickname: '阿May', phone: '13700000002', email: 'may@example.com', status: 'ACTIVE' },
];

const adminTripPlans = [
  { id: 1, user_id: 1, title: '杭州周末慢游', destination_city: '杭州', status: 'COMPLETED' },
  { id: 2, user_id: 1, title: '成都三日烟火行', destination_city: '成都', status: 'SAVED' },
  { id: 3, user_id: 3, title: '厦门海岛两日', destination_city: '厦门', status: 'ACTIVE' },
];

const adminAiRecords = [
  { id: 1, user_id: 1, analysis_type: 'vision_detect', target_type: 'image', status: 'COMPLETED', created_at: '2026-08-12T10:00:00' },
  { id: 2, user_id: 1, analysis_type: 'trip_evaluate', target_type: 'trip', status: 'COMPLETED', created_at: '2026-08-12T11:00:00' },
  { id: 3, user_id: 2, analysis_type: 'content_analyze', target_type: 'post', status: 'COMPLETED', created_at: '2026-08-10T14:00:00' },
];

const adminTravelNotes = [
  { id: 1, user_id: 1, title: '杭州：一座来了就不想走的城市', visibility: 'PUBLIC', status: 'APPROVED', review_reason: '' },
  { id: 2, user_id: 1, title: '成都：在烟火气里找到慢节奏', visibility: 'PRIVATE', status: 'PENDING', review_reason: '' },
];

const adminUserPreferences = [
  { id: 1, user_id: 1, budget_level: 'medium', travel_style: '轻松慢游', preferred_city: '杭州', transportation: '公共交通', hotel_level: '舒适型', diet_preference: '本地菜' },
  { id: 2, user_id: 3, budget_level: 'high', travel_style: '美食优先', preferred_city: '成都', transportation: '打车为主', hotel_level: '高星酒店', diet_preference: '爱吃辣' },
];

const adminMapPois = [
  { id: 1, city: '杭州', name: '西湖', kind: 'attraction', source: 'osm', rating: 5.0, status: 'ACTIVE' },
  { id: 2, city: '杭州', name: '灵隐寺', kind: 'attraction', source: 'osm', rating: 4.7, status: 'ACTIVE' },
  { id: 3, city: '成都', name: '宽窄巷子', kind: 'attraction', source: 'osm', rating: 4.5, status: 'ACTIVE' },
];

const adminTravelTags = [
  { id: 1, name: '湖景', category: 'scenery', status: 'ACTIVE' },
  { id: 2, name: '美食', category: 'food', status: 'ACTIVE' },
  { id: 3, name: '文化', category: 'culture', status: 'ACTIVE' },
  { id: 4, name: '亲子', category: 'family', status: 'ACTIVE' },
  { id: 5, name: '夜景', category: 'scenery', status: 'ACTIVE' },
];

const visionResult = {
  model_mode: 'trained_yolo',
  labels: [{ name: 'crowd', confidence: 0.82 }, { name: 'water', confidence: 0.67 }],
};

const comfortResult = {
  model_mode: 'trained',
  score: 78,
  level: '舒适',
  factors: { pace: '轻松', density: '适中', budget: '合理' },
};

const tripPlanResult = {
  status: 'COMPLETED',
  progress: 100,
  data: trips[0],
};

export const mockData = {
  demoUser,
  demoAdmin,
  demoSession,
  demoAdminSession,
  cities,
  attractions,
  hotels,
  restaurants,
  trips,
  memories,
  journals,
  communityPosts,
  communityComments,
  recommendations,
  notifications,
  profile,
  inspirationBag,
  assistantConversations,
  adminStats,
  adminUsers,
  adminTripPlans,
  adminAiRecords,
  adminTravelNotes,
  adminUserPreferences,
  adminMapPois,
  adminTravelTags,
  visionResult,
  comfortResult,
  tripPlanResult,
};

const resourceMap = {
  cities,
  attractions,
  hotels,
  restaurants,
  'map-pois': adminMapPois,
  'travel-tags': adminTravelTags,
  'travel-notes': adminTravelNotes,
  'trip-plans': adminTripPlans,
  'ai-records': adminAiRecords,
  'user-preferences': adminUserPreferences,
  users: adminUsers,
};

export function getMockResource(key) {
  return resourceMap[key] || [];
}
