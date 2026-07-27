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

createApp(App).use(router).mount('#app');

// 首屏空闲后再预拉地图样式 / MapLibre，进入地图页更快
scheduleMapWarmup();
