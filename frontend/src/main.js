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
import { scheduleMapWarmup } from './map/prefetch.js';
import { refreshAuthSession } from './api/auth.js';

async function start() {
  await refreshAuthSession();
  createApp(App).use(router).mount('#app');
  scheduleMapWarmup();
}

void start();
