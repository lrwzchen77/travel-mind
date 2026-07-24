import { createRouter, createWebHistory } from 'vue-router';
import { authSession } from '../auth/session.js';
import AppLayout from '../layout/AppLayout.vue';
import AdminLayout from '../layout/AdminLayout.vue';
import DashboardView from '../views/DashboardView.vue';
import AiLabView from '../views/AiLabView.vue';
import AiInspirationView from '../views/AiInspirationView.vue';
import PlanningView from '../views/PlanningView.vue';
import ExploreMapView from '../views/ExploreMapView.vue';
import DiscoveryListView from '../views/DiscoveryListView.vue';
import CityDetailView from '../views/CityDetailView.vue';
import ResourceCrudView from '../views/ResourceCrudView.vue';
import TripDetailView from '../views/TripDetailView.vue';
import TripHistoryView from '../views/TripHistoryView.vue';
import UserProfileView from '../views/UserProfileView.vue';
import UserLibraryView from '../views/UserLibraryView.vue';
import LoginView from '../views/LoginView.vue';
import CommunityView from '../views/CommunityView.vue';
import MyPostsView from '../views/MyPostsView.vue';
import MemoryListView from '../views/MemoryListView.vue';
import MemoryDetailView from '../views/MemoryDetailView.vue';
import InspirationDetailView from '../views/InspirationDetailView.vue';
import InspirationBagView from '../views/InspirationBagView.vue';
import AssistantView from '../views/AssistantView.vue';
import AdminDashboardView from '../views/admin/AdminDashboardView.vue';
import AdminSettingsView from '../views/admin/AdminSettingsView.vue';

const fieldLabels = {
  id: '编号', name: '名称', username: '账号', nickname: '昵称', phone: '手机号', email: '邮箱',
  province: '省份', country: '国家', popularity: '热度', status: '状态', city_id: '城市',
  category: '分类', rating: '评分', price: '参考价', tags: '标签', price_range: '价位',
  cuisine: '菜系', average_cost: '人均', user_id: '用户', target_type: '类型', target_id: '目标',
  note: '备注', attraction_id: '景点', title: '标题', visibility: '可见', analysis_type: '分析类型',
  city: '城市', kind: '大类', source: '来源', longitude: '经度', latitude: '纬度', cost: '消费',
};

const discoveryRoutes = [
  ['cities', '发现城市', ['name', 'province', 'description', 'popularity']],
  ['attractions', '去哪玩', ['name', 'category', 'rating', 'price', 'tags']],
  ['hotels', '住哪里', ['name', 'category', 'rating', 'price_range', 'tags']],
  ['restaurants', '吃什么', ['name', 'cuisine', 'rating', 'average_cost', 'tags']],
].map(([resourceKey, title, fields]) => ({
  path: resourceKey,
  name: resourceKey,
  component: DiscoveryListView,
  meta: { resourceKey, title, fields, public: true },
}));

const adminResources = [
  ['users', '用户管理', ['id', 'username', 'nickname', 'phone', 'email', 'status']],
  ['cities', '城市内容', ['id', 'name', 'province', 'country', 'popularity', 'status']],
  ['attractions', '景点内容', ['id', 'city_id', 'name', 'category', 'rating', 'price', 'status']],
  ['hotels', '住宿内容', ['id', 'city_id', 'name', 'category', 'rating', 'price_range', 'status']],
  ['restaurants', '餐饮内容', ['id', 'city_id', 'name', 'cuisine', 'rating', 'average_cost', 'status']],
  ['map-pois', '地图地点库', ['id', 'city', 'name', 'kind', 'source', 'rating', 'status']],
  ['travel-tags', '标签体系', ['id', 'name', 'category', 'status']],
  ['travel-notes', '用户笔记', ['id', 'user_id', 'title', 'visibility', 'status']],
  ['trip-plans', '行程记录', ['id', 'user_id', 'title', 'destination_city', 'status']],
  ['ai-records', 'AI 调用记录', ['id', 'user_id', 'analysis_type', 'target_type', 'status']],
].map(([resourceKey, title, fields]) => ({
  path: `resources/${resourceKey}`,
  name: `admin-${resourceKey}`,
  component: ResourceCrudView,
  meta: {
    resourceKey,
    title,
    fields,
    fieldLabels,
    admin: true,
    requiresAuth: true,
    canToggleStatus: resourceKey !== 'ai-records',
  },
}));

export const routes = [
  {
    path: '/',
    component: AppLayout,
    children: [
      { path: '', name: 'dashboard', component: DashboardView, meta: { public: true } },
      { path: 'planning', name: 'planning', component: PlanningView, meta: { public: true } },
      { path: 'map', name: 'explore-map', component: ExploreMapView, meta: { public: true } },
      { path: 'inspirations', name: 'inspirations', component: CommunityView, meta: { public: true } },
      { path: 'inspirations/:id', name: 'inspiration-detail', component: InspirationDetailView, meta: { public: true } },
      { path: 'my-posts', name: 'my-posts', component: MyPostsView, meta: { requiresAuth: true } },
      { path: 'inspiration-bag', name: 'inspiration-bag', component: InspirationBagView, meta: { requiresAuth: true } },
      { path: 'assistant', name: 'assistant', component: AssistantView, meta: { requiresAuth: true } },
      ...discoveryRoutes,
      { path: 'city/:city', name: 'city-detail', component: CityDetailView, meta: { public: true } },
      { path: 'trip-history', name: 'trip-history', component: TripHistoryView, meta: { requiresAuth: true } },
      { path: 'trip/:id', name: 'trip-detail', component: TripDetailView, meta: { requiresAuth: true } },
      { path: 'memories', name: 'memories', component: MemoryListView, meta: { requiresAuth: true } },
      { path: 'memories/:id', name: 'memory-detail', component: MemoryDetailView, meta: { requiresAuth: true } },
      { path: 'ai-lab', name: 'ai-lab', component: AiInspirationView, meta: { requiresAuth: true } },
      { path: 'favorites', name: 'favorites', component: UserLibraryView, meta: { resourceKey: 'favorites', title: '我的收藏', requiresAuth: true } },
      { path: 'travel-notes', name: 'travel-notes', component: UserLibraryView, meta: { resourceKey: 'travel-notes', title: '旅行笔记', requiresAuth: true } },
      { path: 'ai-records', name: 'ai-records', component: UserLibraryView, meta: { resourceKey: 'ai-records', title: '灵感足迹', requiresAuth: true } },
      { path: 'profile', name: 'profile', component: UserProfileView, meta: { requiresAuth: true } },
    ],
  },
  { path: '/login', name: 'user-login', component: LoginView, meta: { portal: 'user', public: true } },
  { path: '/admin/login', name: 'admin-login', component: LoginView, meta: { portal: 'admin', public: true } },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { admin: true, requiresAuth: true },
    children: [
      { path: '', name: 'admin-dashboard', component: AdminDashboardView },
      { path: 'settings', name: 'admin-settings', component: AdminSettingsView },
      { path: 'ai-tools', name: 'admin-ai-tools', component: AiLabView },
      ...adminResources,
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});

const pageTitles = {
  dashboard: '旅行灵感', planning: '规划行程', 'explore-map': '地图情报', inspirations: '旅行社区',
  'inspiration-detail': '旅行分享', 'my-posts': '我的分享', 'inspiration-bag': '我的灵感包', assistant: '旅行助手',
  attractions: '去哪玩', hotels: '住哪里', restaurants: '吃什么', cities: '发现城市',
  'trip-history': '我的行程', 'trip-detail': '行程详情', memories: '旅行记录', 'memory-detail': '旅行回忆',
  'ai-lab': 'AI 内容解读', favorites: '我的收藏', 'travel-notes': '旅行笔记', 'ai-records': '灵感足迹', profile: '旅行偏好',
  'user-login': '旅行账号登录', 'admin-login': '运营登录',
};

router.afterEach((to) => {
  const title = to.name === 'city-detail' ? `${String(to.params.city || '')}旅行`
    : (to.meta.title || pageTitles[to.name] || 'Travel Mind');
  document.title = title === 'Travel Mind' ? '旅行灵感 · Travel Mind' : `${title} · Travel Mind`;
});

router.beforeEach((to) => {
  const isAdmin = to.matched.some((route) => route.meta.admin);
  const requiresAuth = to.matched.some((route) => route.meta.requiresAuth);
  if (!authSession.isLoggedIn()) {
    if (!requiresAuth) return true;
    return { path: isAdmin ? '/admin/login' : '/login', query: { redirect: to.fullPath } };
  }
  if (authSession.hasRole('admin') && !isAdmin) return '/admin';
  if (isAdmin && !authSession.hasRole('admin')) {
    authSession.clear();
    return { path: '/admin/login', query: { reason: 'role' } };
  }
  return true;
});

export default router;
