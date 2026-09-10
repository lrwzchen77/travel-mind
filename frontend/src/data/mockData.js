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
  { id: 1, title: '杭州两日精华路线，第一次去就收藏', author: '旅行者', city: '杭州', topic: 'route', created_at: '2026-08-13T09:00:00', like_count: 128, comment_count: 15, liked_by_me: false, status: 'APPROVED', cover_image: '', excerpt: '白堤日出 → 楼外楼 → 灵隐寺 → 河坊街，第二天龙井 + 西溪。', tags: '杭州,周末,慢游', content: '第一天早上6点白堤看日出，7点前到断桥几乎没人，拍完照去楼外楼吃西湖醋鱼。上午灵隐寺飞来峰，下午河坊街买手伴。第二天龙井村采茶 + 知味观午餐，下午西溪湿地摇橹船。总花费约1600元，住西湖边民宿步行可达白堤。' },
  { id: 2, title: '成都美食地图：跟着本地人吃了三天', author: '美食家小王', city: '成都', topic: 'food', created_at: '2026-08-10T14:00:00', like_count: 256, comment_count: 32, liked_by_me: false, status: 'APPROVED', cover_image: '', excerpt: '从蜀大侠到陈麻婆，从甜水面到钵钵鸡，成都的辣是分层的。', tags: '成都,美食,火锅', content: '第一天蜀大侠火锅（牛油锅底必点），第二天陈麻婆豆腐（真正的麻婆豆腐是麻在前辣在后），第三天去玉林路吃甜水面和钵钵鸡。人均80-120元，建议避开景区，宽窄巷子只逛不吃。' },
  { id: 3, title: '厦门鼓浪屿：最全拍照机位指南', author: '文艺旅人', city: '厦门', topic: 'play', created_at: '2026-08-05T10:00:00', like_count: 189, comment_count: 21, liked_by_me: false, status: 'APPROVED', cover_image: '', excerpt: '日光岩、菽庄花园、八卦楼……鼓浪屿每个转角都是风景。', tags: '厦门,鼓浪屿,拍照', content: '日光岩最佳拍摄时间是早上7点，人少光好。菽庄花园的海边钢琴博物馆角度绝了。八卦楼的红砖墙配蓝天是必拍机位。岛上没有机动车，全程步行，穿舒服的鞋。轮渡35元往返，建议住一晚体验夜晚的鼓浪屿。' },
  { id: 4, title: '西安城墙骑行 + 回民街宵夜攻略', author: '历史迷小李', city: '西安', topic: 'route', created_at: '2026-07-28T18:00:00', like_count: 142, comment_count: 18, liked_by_me: false, status: 'APPROVED', cover_image: '', excerpt: '城墙一圈14公里刚好1小时，下来直奔回民街。', tags: '西安,骑行,美食', content: '城墙骑行租自行车45元/2小时，一圈14公里慢慢骑约1小时。南门（永宁门）上下最方便。骑完刚好傍晚，从南门步行10分钟到回民街。老孙家泡馍要掰馍掰得细，配糖蒜和辣酱。宵夜推荐红柳烤肉，10元一串。' },
  { id: 5, title: '大理环洱海骑行：风花雪月两日版', author: '环海骑手', city: '大理', topic: 'route', created_at: '2026-07-20T08:00:00', like_count: 98, comment_count: 12, liked_by_me: false, status: 'APPROVED', cover_image: '', excerpt: '才村码头 → 喜洲古镇 → 双廊，130公里最美的环湖路。', tags: '大理,洱海,骑行', content: '第一天从大理古城出发，沿西岸到喜洲古镇（约40公里），吃喜洲粑粑，住海景客栈。第二天从喜洲到双廊再到挖庄（约90公里），双廊看洱海日落绝美。电瓶车续航焦虑的话，沿途村子都有充电桩。建议秋季去，风不大光线暖。' },
  { id: 6, title: '青岛啤酒节避坑指南：本地人教你省钱', author: '青岛老张', city: '青岛', topic: 'tip', created_at: '2026-08-01T16:00:00', like_count: 175, comment_count: 24, liked_by_me: false, status: 'APPROVED', cover_image: '', excerpt: '门票、酒券、吃饭全套攻略，别在会场里买全价票。', tags: '青岛,啤酒节,避坑', content: '啤酒节门票提前在官方小程序买能省30%，现场买全价。进去了别直接点大杯，先买试饮券尝4种再决定。吃东西别在会场里吃，出来走5分钟到台东步行街，海鲜烧烤便宜一半。住宿住市南区地铁沿线，别住会场旁边贵3倍。' },
  { id: 7, title: '杭州西溪湿地摇橹船：比西湖安静十倍', author: '慢游日记', city: '杭州', topic: 'play', created_at: '2026-08-08T11:00:00', like_count: 67, comment_count: 8, liked_by_me: false, status: 'APPROVED', cover_image: '', excerpt: '摇橹船100元/人，60分钟，全程只有船桨声和鸟叫。', tags: '杭州,西溪,慢游', content: '西湖人太多的时候，来西溪就对了。摇橹船在深潭口坐，100元每人60分钟。船工会讲西溪的故事，比电动船安静太多。建议下午3点坐，光线最好。下了船走绿堤到高庄出口，全程约3小时。门票80元，杭州公园卡免费。' },
  { id: 8, title: '成都太古里博舍酒店住后感：设计感拉满', author: '酒店控', city: '成都', topic: 'stay', created_at: '2026-07-25T20:00:00', like_count: 112, comment_count: 16, liked_by_me: false, status: 'APPROVED', cover_image: '', excerpt: '博舍把川西民居和现代设计融合，中庭的竹林太绝了。', tags: '成都,博舍,酒店', content: '博舍在太古里中心，出门就是逛吃。大堂在二楼，中庭竹林设计非常出片。房间面积不大但用色和材质高级，浴缸靠窗。服务细节到位，欢迎点心是熊猫造型马卡龙。价格800-1800元，性价比看个人。隔壁大慈寺值得顺路逛。' },
  { id: 9, title: '厦门沙坡尾：文艺青年的秘密基地', author: '文艺旅人', city: '厦门', topic: 'play', created_at: '2026-07-30T13:00:00', like_count: 84, comment_count: 10, liked_by_me: false, status: 'APPROVED', cover_image: '', excerpt: '老渔港改造的文艺街区，比鼓浪屿人少，比曾厝垵干净。', tags: '厦门,沙坡尾,文艺', content: '沙坡尾在大学路附近，原来是老渔港，改造成文艺街区。有独立书店、手冲咖啡、 vintage古着店。推荐的店：不辍旧物空间（复古杂货）、SAICAT（咖啡），傍晚看渔船回港很有味道。离厦门大学和南普陀寺步行10分钟，可以串在一起逛。' },
  { id: 10, title: '西安回民街踩雷+推荐：本地人带你避坑', author: '西安吃货', city: '西安', topic: 'tip', created_at: '2026-08-02T17:00:00', like_count: 203, comment_count: 28, liked_by_me: false, status: 'APPROVED', cover_image: '', excerpt: '老孙家泡馍别在主街吃，红柳烤肉认准老马家。', tags: '西安,回民街,避坑', content: '回民街主街的店有一半是游客店。泡馍推荐西羊市的老孙家（不在主街上），掰馍掰得越细越好。红柳烤肉认准老马家，10元一串肉大。镜糕和甑糕别在第一家买，往里走第三家便宜。酸梅汤自己冲的比鲜榨好喝。避开节假日中午人最多。' },
  { id: 11, title: '青岛建筑漫步：八大关的红瓦绿树', author: '建筑爱好者', city: '青岛', topic: 'route', created_at: '2026-07-18T09:00:00', like_count: 76, comment_count: 7, liked_by_me: false, status: 'APPROVED', cover_image: '', excerpt: '花石楼 → 公主楼 → 蝴蝶楼，八大关一条路看遍万国建筑。', tags: '青岛,八大关,建筑', content: '八大关是青岛最美的一条散步路线。从花石楼开始（门票8.5元），顺时针走公主楼（丹麦风格）、蝴蝶楼（日本风格），全程约2公里。最佳季节是10月，银杏和枫叶都黄了。花石楼三楼阳台看海角度最好。走完去第二海水浴场坐坐，免费。' },
  { id: 12, title: '大理古城夜市：洋人街的饼和酒', author: '环海骑手', city: '大理', topic: 'food', created_at: '2026-07-22T21:00:00', like_count: 54, comment_count: 9, liked_by_me: false, status: 'APPROVED', cover_image: '', excerpt: '烤乳扇、鲜花饼、木瓜水，洋人街的夜市便宜又好吃。', tags: '大理,夜市,美食', content: '大理古城洋人街夜市从晚上7点开始。必吃：烤乳扇（5元/个，玫瑰酱夹心）、鲜花饼（现烤的3元/个）、木瓜水（3元/碗，解辣解腻）。杨记烤饵块也推荐，5元/份。如果想喝酒，坏猴子酒吧的精酿不错，38元/杯。夜市到晚上11点左右收摊，建议9点去最热闹。' },
];

const communityComments = {
  1: [
    { id: 1, author: '小张', content: '白堤日出真的绝美！几点去的？', created_at: '2026-08-13T10:00:00' },
    { id: 2, author: '旅途中', content: '楼外楼价格小贵但风景无价', created_at: '2026-08-13T11:00:00' },
    { id: 3, author: '慢游日记', content: '第二天龙井村采茶需要提前预约吗？', created_at: '2026-08-13T14:00:00' },
  ],
  2: [
    { id: 4, author: '吃货阿May', content: '甜水面是成都最被低估的小吃！', created_at: '2026-08-10T15:00:00' },
    { id: 5, author: '川味控', content: '蜀大侠牛油锅确实顶，但微辣就够', created_at: '2026-08-10T16:00:00' },
  ],
  3: [
    { id: 6, author: '摄影小白', content: '日光岩早上几点去最好？', created_at: '2026-08-05T11:00:00' },
  ],
  4: [
    { id: 7, author: '骑行爱好者', content: '城墙自行车可以自带吗？', created_at: '2026-07-28T19:00:00' },
    { id: 8, author: '西安吃货', content: '老孙家泡馍确实正宗，掰馍是灵魂', created_at: '2026-07-28T20:00:00' },
  ],
  5: [
    { id: 9, author: '洱海常客', content: '双廊看日落真的绝，推荐住一晚', created_at: '2026-07-20T09:00:00' },
  ],
  6: [
    { id: 10, author: '啤酒达人', content: '试饮券这个信息太有用了！', created_at: '2026-08-01T17:00:00' },
  ],
  7: [
    { id: 11, author: '杭州土著', content: '西溪确实比西湖安静多了，推荐秋天去', created_at: '2026-08-08T12:00:00' },
  ],
  8: [
    { id: 12, author: '设计控', content: '博舍中庭竹林确实出片', created_at: '2026-07-25T21:00:00' },
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
