import { createRouter, createWebHistory } from 'vue-router';
import DashboardView from '../views/DashboardView.vue';
import AiLabView from '../views/AiLabView.vue';
import PlanningView from '../views/PlanningView.vue';
import ExploreMapView from '../views/ExploreMapView.vue';
import ResourceCrudView from '../views/ResourceCrudView.vue';
import TripDetailView from '../views/TripDetailView.vue';
import TripHistoryView from '../views/TripHistoryView.vue';
import UserProfileView from '../views/UserProfileView.vue';

const fieldLabels = {
  id: '编号',
  name: '名称',
  province: '省份',
  country: '国家',
  popularity: '热度',
  status: '状态',
  city_id: '城市',
  category: '分类',
  rating: '评分',
  price: '参考价',
  tags: '标签',
  price_range: '价位',
  cuisine: '菜系',
  average_cost: '人均',
  user_id: '用户',
  target_type: '类型',
  target_id: '目标',
  note: '备注',
  attraction_id: '景点',
  title: '标题',
  visibility: '可见',
  analysis_type: '分析类型',
};

export const routes = [
  { path: '/', name: 'dashboard', component: DashboardView },
  { path: '/planning', name: 'planning', component: PlanningView },
  { path: '/map', name: 'explore-map', component: ExploreMapView },
  { path: '/profile', name: 'profile', component: UserProfileView },
  {
    path: '/cities',
    name: 'cities',
    component: ResourceCrudView,
    meta: {
      resourceKey: 'cities',
      title: '发现城市',
      description: '逛逛目的地灵感墙，看中了就一键带进行程规划。',
      fields: ['id', 'name', 'province', 'country', 'popularity', 'status'],
      fieldLabels,
    },
  },
  {
    path: '/attractions',
    name: 'attractions',
    component: ResourceCrudView,
    meta: {
      resourceKey: 'attractions',
      title: '景点清单',
      description: '收藏想打卡的景点，规划时更容易排进日程。',
      fields: ['id', 'city_id', 'name', 'category', 'rating', 'price', 'tags', 'status'],
      fieldLabels,
    },
  },
  {
    path: '/hotels',
    name: 'hotels',
    component: ResourceCrudView,
    meta: {
      resourceKey: 'hotels',
      title: '住哪里',
      description: '浏览住宿选项与价位，找到睡得安心的那一晚。',
      fields: ['id', 'city_id', 'name', 'category', 'rating', 'price_range', 'tags', 'status'],
      fieldLabels,
    },
  },
  {
    path: '/restaurants',
    name: 'restaurants',
    component: ResourceCrudView,
    meta: {
      resourceKey: 'restaurants',
      title: '吃什么',
      description: '本地味道与人气馆子，让行程不只有走路。',
      fields: ['id', 'city_id', 'name', 'cuisine', 'rating', 'average_cost', 'tags', 'status'],
      fieldLabels,
    },
  },
  { path: '/trip-history', name: 'trip-history', component: TripHistoryView },
  { path: '/ai-lab', name: 'ai-lab', component: AiLabView },
  {
    path: '/favorites',
    name: 'favorites',
    component: ResourceCrudView,
    meta: {
      resourceKey: 'favorites',
      title: '我的收藏',
      description: '你盯上的城市、景点都在这里，方便下次规划直接复用。',
      fields: ['id', 'user_id', 'target_type', 'target_id', 'note'],
      fieldLabels,
    },
  },
  {
    path: '/travel-notes',
    name: 'travel-notes',
    component: ResourceCrudView,
    meta: {
      resourceKey: 'travel-notes',
      title: '旅行笔记',
      description: '记下路上的片段，也能一键让 AI 帮你提炼亮点。',
      fields: ['id', 'user_id', 'city_id', 'attraction_id', 'title', 'visibility', 'status'],
      fieldLabels,
    },
  },
  {
    path: '/ai-records',
    name: 'ai-records',
    component: ResourceCrudView,
    meta: {
      resourceKey: 'ai-records',
      title: '分析足迹',
      description: '回顾你用过的视觉识别、舒适度评估和文本分析。',
      fields: ['id', 'user_id', 'analysis_type', 'target_type', 'target_id', 'status'],
      fieldLabels,
      canToggleStatus: false,
    },
  },
  { path: '/trip/:id', name: 'trip-detail', component: TripDetailView },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});

export default router;
