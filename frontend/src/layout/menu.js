/** 顶栏主导航：C 端旅行产品路径（动作导向，非后台模块名） */
export const primaryNav = [
  { path: '/', label: '首页' },
  { path: '/inspirations', label: '旅行灵感' },
  { path: '/assistant', label: '先问 AI' },
  { path: '/cities', label: '目的地' },
  { path: '/trip-history', label: '我的行程' },
];

/** 账户菜单只承载当前用户拥有的内容与设置。 */
export const accountNav = [
  { path: '/notifications', label: '消息中心' },
  { path: '/memories', label: '旅行记录' },
  { path: '/inspiration-bag', label: '我的灵感包' },
  { path: '/my-posts', label: '我的分享' },
  { path: '/favorites', label: '我的收藏' },
  { path: '/travel-notes', label: '我的笔记' },
  { path: '/ai-lab', label: 'AI 内容解读' },
  { path: '/ai-records', label: '灵感足迹' },
  { path: '/profile', label: '旅行偏好' },
];

/** 页面内或行动按钮入口，仍属于用户可达的产品路径。 */
export const auxiliaryNav = [
  { path: '/map', label: '生成行程' },
  { path: '/map', label: '地图情报' },
  { path: '/ai-lab', label: 'AI 内容解读' },
];

/** 扁平列表，供路由/测试校验。 */
export const navigationItems = [...new Map([...primaryNav, ...auxiliaryNav, ...accountNav].map((item) => [item.path, item])).values()];

/** 跨页面章节码：用 00–09 编号每个主路由，帘幕换页与 HUD 仪表共用。 */
export const chapterMap = {
  dashboard: ['00', '序'],
  planning: ['01', '规划'],
  'explore-map': ['02', '地图'],
  inspirations: ['03', '社区'],
  'inspiration-detail': ['03', '社区'],
  cities: ['04', '城市'],
  'city-detail': ['04', '城市'],
  'discovery-detail': ['05', '发现'],
  attractions: ['05', '去哪玩'],
  hotels: ['05', '住哪里'],
  restaurants: ['05', '吃什么'],
  'trip-history': ['06', '行程'],
  'trip-detail': ['06', '行程'],
  memories: ['07', '记录'],
  'memory-detail': ['07', '记录'],
  'my-posts': ['08', '我的'],
  'inspiration-bag': ['08', '灵感包'],
  assistant: ['09', '助手'],
  'ai-lab': ['09', '助手'],
  favorites: ['08', '收藏'],
  'travel-notes': ['08', '笔记'],
  'ai-records': ['08', '足迹'],
  profile: ['08', '偏好'],
  notifications: ['08', '消息'],
  // 管理端章节：A 系编号，与用户端 00–09 手记语法同源但自成一册
  'admin-dashboard': ['A0', '运营'],
  'admin-cities': ['A1', '资源'],
  'admin-attractions': ['A1', '资源'],
  'admin-hotels': ['A1', '资源'],
  'admin-restaurants': ['A1', '资源'],
  'admin-map-pois': ['A1', '资源'],
  'admin-travel-tags': ['A1', '资源'],
  'admin-users': ['A2', '用户'],
  'admin-user-preferences': ['A2', '用户'],
  'admin-trip-plans': ['A2', '用户'],
  'admin-travel-notes': ['A2', '用户'],
  'admin-ai-records': ['A2', '用户'],
  'admin-settings': ['A3', '配置'],
  'admin-ai-tools': ['A3', '工具'],
};

/** 取某路由的章节码，未登记的路由返回占位破折号。 */
export function chapterFor(routeName) {
  return chapterMap[routeName] || ['—', '—'];
}

/** 首页轮播/高亮城市名 */
export const rotatingCities = ['杭州', '成都', '北京'];

/** 首页推荐目的地 */
export const featuredDestinations = [
  {
    city: '杭州',
    tag: '湖光山色',
    blurb: '西湖慢游，龙井与夜市一口气收进周末。',
    days: '2–3 天',
    mood: 'haze',
    hint: '轻松 · 美食 · 亲子',
  },
  {
    city: '成都',
    tag: '烟火人间',
    blurb: '火锅与宽窄巷子，给胃和眼睛都放个假。',
    days: '3 天',
    mood: 'spice',
    hint: '美食 · 市井 · 熊猫',
  },
  {
    city: '厦门',
    tag: '海风小岛',
    blurb: '鼓浪屿的风，沙茶面的鲜，适合慢慢走。',
    days: '2–4 天',
    mood: 'sea',
    hint: '海岛 · 拍照 · 轻徒步',
  },
  {
    city: '西安',
    tag: '古今同框',
    blurb: '城墙骑行、回民街宵夜，历史感拉满。',
    days: '3 天',
    mood: 'terra',
    hint: '文化 · 夜景 · 小吃',
  },
];

export const marqueeTags = [
  '周末短途', '亲子友好', '美食地图', '湖景慢游', '海岛微风',
  '古城骑行', '夜游路线', '预算可控', '少走路', '第一次去',
];
