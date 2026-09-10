import { createApp } from 'vue';
import App from './App.vue';
import router from './router/index.js';
import './styles.css';
import './workspace.css';
import './community.css';
import './memory.css';
import './theme.css';
import './awwwards.css';
import './admin.css';
import './experience.css';
import { scheduleMapWarmup } from './map/prefetch.js';
import { refreshAuthSession } from './api/auth.js';
import { authSession } from './auth/session.js';
import { mockData } from './data/mockData.js';

const isPages = import.meta.env.BASE_URL !== '/';

function initPagesMock() {
  authSession.save(mockData.demoSession);
}

async function start() {
  if (isPages) initPagesMock();
  if (!isPages) await refreshAuthSession();
  createApp(App).use(router).mount('#app');
  scheduleMapWarmup();
}

void start();
