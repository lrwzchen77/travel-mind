import { createRouter, createWebHistory } from 'vue-router';
import DashboardView from '../views/DashboardView.vue';
import AiLabView from '../views/AiLabView.vue';
import PlanningView from '../views/PlanningView.vue';
import ResourceCrudView from '../views/ResourceCrudView.vue';
import TripDetailView from '../views/TripDetailView.vue';
import TripHistoryView from '../views/TripHistoryView.vue';
import UserProfileView from '../views/UserProfileView.vue';

export const routes = [
  { path: '/', name: 'dashboard', component: DashboardView },
  { path: '/planning', name: 'planning', component: PlanningView },
  { path: '/profile', name: 'profile', component: UserProfileView },
  {
    path: '/cities',
    name: 'cities',
    component: ResourceCrudView,
    meta: { resourceKey: 'cities', title: 'Cities', fields: ['id', 'name', 'province', 'country', 'popularity', 'status'] },
  },
  {
    path: '/attractions',
    name: 'attractions',
    component: ResourceCrudView,
    meta: { resourceKey: 'attractions', title: 'Attractions', fields: ['id', 'city_id', 'name', 'category', 'rating', 'price', 'tags', 'status'] },
  },
  {
    path: '/hotels',
    name: 'hotels',
    component: ResourceCrudView,
    meta: { resourceKey: 'hotels', title: 'Hotels', fields: ['id', 'city_id', 'name', 'category', 'rating', 'price_range', 'tags', 'status'] },
  },
  {
    path: '/restaurants',
    name: 'restaurants',
    component: ResourceCrudView,
    meta: { resourceKey: 'restaurants', title: 'Restaurants', fields: ['id', 'city_id', 'name', 'cuisine', 'rating', 'average_cost', 'tags', 'status'] },
  },
  { path: '/trip-history', name: 'trip-history', component: TripHistoryView },
  { path: '/ai-lab', name: 'ai-lab', component: AiLabView },
  {
    path: '/favorites',
    name: 'favorites',
    component: ResourceCrudView,
    meta: { resourceKey: 'favorites', title: 'Favorites', fields: ['id', 'user_id', 'target_type', 'target_id', 'note'] },
  },
  {
    path: '/travel-notes',
    name: 'travel-notes',
    component: ResourceCrudView,
    meta: { resourceKey: 'travel-notes', title: 'Travel Notes', fields: ['id', 'user_id', 'city_id', 'attraction_id', 'title', 'visibility', 'status'] },
  },
  {
    path: '/ai-records',
    name: 'ai-records',
    component: ResourceCrudView,
    meta: {
      resourceKey: 'ai-records',
      title: 'AI Records',
      fields: ['id', 'user_id', 'analysis_type', 'target_type', 'target_id', 'status'],
      canToggleStatus: false,
    },
  },
  { path: '/trip/:id', name: 'trip-detail', component: TripDetailView },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
