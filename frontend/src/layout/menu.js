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
