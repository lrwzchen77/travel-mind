<script setup>
import { computed, onMounted, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { resourceApi } from '../../api/resources.js';
import { adminAiApi } from '../../api/ai.js';
import { modelRunSummary, visionRecordSummary } from '../../data/adminDashboard.js';
import trainingMetrics from '../../data/travelRiskYoloMetrics.json';
import comfortMetrics from '../../data/travelComfortMetrics.json';
import { visionLabelMeta } from '../../data/visionInsights.js';
import { useReveal } from '../../composables/useReveal.js';
import {
  Activity,
  ArrowDown,
  ArrowRight,
  BrainCircuit,
  Camera,
  CheckCircle2,
  Cpu,
  Database,
  Grid3X3,
  Images,
  LineChart,
  MapPinned,
  RefreshCw,
  Route,
  ShieldCheck,
  UsersRound,
} from 'lucide-vue-next';

const root = ref(null);
useReveal(root);

const metrics = ref([]);
const records = ref([]);
const loading = ref(true);
const syncing = ref(false);
const visionRun = ref(null);
const visionLoading = ref(false);
const visionError = ref('');
const previewUrl = ref('');
const trainingArtifact = ref('curve');
const trainingModule = ref('comfort');
const comfortFeedbackStats = ref({ total: 0, labels: { relaxed: 0, balanced: 0, intense: 0 } });

const definitions = [
  ['users', '用户资产', UsersRound, '用户'],
  ['trip-plans', '已生成行程', Route, '行程'],
  ['cities', '目的地内容', MapPinned, '城市'],
  ['ai-records', 'AI 调用记录', Activity, 'AI 记录'],
];
const telemetry = computed(() => modelRunSummary(records.value));
const latestRuns = computed(() => records.value.map(visionRecordSummary).filter((item) => item.modelMode).slice(0, 5));
const visionData = computed(() => visionRun.value?.data || visionRun.value || {});
const visionMeta = computed(() => visionLabelMeta(visionData.value.labels?.[0]?.name));
const visionConfidence = computed(() => Number(visionData.value.labels?.[0]?.confidence || 0));
const modelLabel = computed(() => visionData.value.model_mode === 'trained_yolo' ? '自训 YOLOv8n 分类' : '规则降级');
const visionTrainingStages = [
  { label: '数据构建', value: '946 张 / 6 类', detail: '爬虫 766 + 公开 180', icon: Images },
  { label: '迁移训练', value: '20 epoch', detail: 'YOLOv8n-cls · CPU 5.1 分钟', icon: Cpu },
  { label: '独立评测', value: '93 / 99 正确', detail: '固定 test 集 · seed 42', icon: ShieldCheck },
  { label: '服务部署', value: 'best.pt', detail: 'FastAPI · trained_yolo', icon: BrainCircuit },
];
const testClassMetrics = Object.entries(trainingMetrics.splits.test.per_class).map(([key, value]) => ({
  key,
  label: visionLabelMeta(key).label,
  ...value,
}));
const comfortClassLabels = { relaxed: '偏松', balanced: '正合适', intense: '太赶' };
const comfortClassMetrics = ['relaxed', 'balanced', 'intense'].map((key) => ({
  key,
  label: comfortClassLabels[key],
  precision: comfortMetrics.classification_report[key].precision,
  recall: comfortMetrics.classification_report[key].recall,
  f1: comfortMetrics.classification_report[key]['f1-score'],
  support: comfortMetrics.classification_report[key].support,
}));
const comfortFeatures = Object.entries(comfortMetrics.feature_importance).slice(0, 6).map(([key, value]) => ({ key, value }));
const featureLabels = {
  average_attractions: '日均景点数', transfer_days: '换乘天数', max_attractions: '单日最多景点',
  budget_per_day: '日均预算', city_transfers: '跨城次数', adverse_weather_days: '不利天气天数',
};
const trainingView = computed(() => trainingModule.value === 'comfort' ? {
  eyebrow: 'TRAINING EVIDENCE / TRAVELCOMFORT',
  date: comfortMetrics.generated_at.slice(0, 10),
  description: '可复现模拟场景训练 · 固定随机种子与独立测试集。',
  kpis: [
    ['训练数据', comfortMetrics.samples, `train ${comfortMetrics.train_samples} · test ${comfortMetrics.test_samples}`],
    ['输入特征', comfortMetrics.features.length, '行程密度 · 换乘 · 天气 · 预算'],
    ['测试准确率', confidence(comfortMetrics.accuracy), '1,247 / 1,500'],
    ['Macro-F1', confidence(comfortMetrics.macro_f1), '三类宏平均'],
  ],
  stages: [
    { label: '场景生成', value: '6,000 条', detail: 'seed 42 · 标签含声明噪声', icon: Database },
    { label: '特征提取', value: '12 维', detail: '密度 · 换乘 · 天气 · 预算', icon: Activity },
    { label: '监督训练', value: '180 棵', detail: 'Gradient Boosting', icon: Cpu },
    { label: '独立评测', value: '1,500 条', detail: 'Accuracy 83.13%', icon: ShieldCheck },
    { label: '服务部署', value: 'joblib', detail: 'FastAPI · 规则降级', icon: BrainCircuit },
  ],
  curveTitle: '训练与测试评估摘要',
  matrixTitle: '独立测试集混淆矩阵',
  curveTab: '训练曲线',
  curveCaption: 'GradientBoostingClassifier · 180 estimators；独立测试集 Accuracy 83.13%，Macro-F1 81.64%。',
  matrixCaption: 'test=1,500；对角线正确 1,247 条，太赶类别 F1 89.82%。',
  classTitle: '三类 F1 表现',
  classSample: 'n=1,500',
  classes: comfortClassMetrics,
  limit: '数据边界：首版训练集来自可复现的模拟旅行场景，并非真实用户反馈；管理端如实披露，后续用行后反馈替换或微调。',
} : {
  eyebrow: 'TRAINING EVIDENCE / TRAVELRISK-YOLO',
  date: trainingMetrics.generated_at.slice(0, 10),
  description: '指标来自固定数据划分和独立测试集。',
  kpis: [
    ['数据总量', 946, 'train 659 · val 188 · test 99'],
    ['验证集准确率', confidence(trainingMetrics.val_accuracy), '182 / 188'],
    ['测试集准确率', confidence(trainingMetrics.test_accuracy), '93 / 99'],
    ['测试平均置信度', confidence(trainingMetrics.splits.test.mean_top1_conf), '六类场景分类'],
  ],
  stages: visionTrainingStages,
  curveTitle: '验证与测试指标',
  matrixTitle: '独立测试集混淆矩阵',
  curveTab: '评测对比',
  curveCaption: '20 epoch 原始训练结果；train loss 持续收敛，末轮 Top1 约 95.7%。',
  matrixCaption: '独立 test=99；主要误差集中在暗光场景与景点、交通站点之间。',
  classTitle: '六类 F1 表现',
  classSample: 'n=99',
  classes: testClassMetrics,
  limit: '结论边界：测试集规模为 99，高分包含 ImageNet 预训练贡献；当前结果用于课程项目验收，不宣称生产级泛化能力。',
});
const comfortCharts = computed(() => [
  {
    key: 'loss', title: 'Log loss', hint: '越低越好', min: 0.3, max: 1,
    ticks: [1, 0.8, 0.6, 0.4],
    series: [
      { key: 'train_loss', label: '训练集', color: 'var(--tm-ink)', values: comfortMetrics.learning_curve.map((item) => item.train_loss) },
      { key: 'test_loss', label: '测试集', color: 'var(--tm-accent)', values: comfortMetrics.learning_curve.map((item) => item.test_loss) },
    ],
  },
  {
    key: 'f1', title: '测试集 Macro-F1', hint: '越高越好', min: 0.6, max: 0.85,
    ticks: [0.85, 0.8, 0.7, 0.6],
    series: [{ key: 'test_f1', label: 'Macro-F1', color: 'var(--tm-accent)', values: comfortMetrics.learning_curve.map((item) => item.test_f1) }],
  },
]);
const visionEvaluation = [
  { label: '验证集准确率', value: trainingMetrics.val_accuracy },
  { label: '测试集准确率', value: trainingMetrics.test_accuracy },
  { label: '测试集平均置信度', value: trainingMetrics.splits.test.mean_top1_conf },
];
const activeMatrix = computed(() => trainingModule.value === 'comfort' ? comfortMetrics.confusion_matrix : trainingMetrics.splits.test.confusion_matrix);
const activeMatrixLabels = computed(() => trainingModule.value === 'comfort'
  ? comfortMetrics.classes.map((key) => comfortClassLabels[key])
  : trainingMetrics.splits.test.labels.map((key) => visionLabelMeta(key).label));
const matrixMax = computed(() => Math.max(...activeMatrix.value.flat()));

function chartPoints(values, min, max) {
  return values.map((value, index) => {
    const x = 52 + (index / Math.max(values.length - 1, 1)) * 448;
    const bounded = Math.min(max, Math.max(min, value));
    const y = 18 + (1 - (bounded - min) / (max - min)) * 162;
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  }).join(' ');
}

function chartY(value, min, max) {
  return 18 + (1 - (value - min) / (max - min)) * 162;
}

function matrixCellStyle(value, row, column) {
  const alpha = Math.round((0.08 + (value / Math.max(matrixMax.value, 1)) * 0.76) * 100);
  return { background: row === column ? `color-mix(in srgb, var(--tm-accent) ${alpha}%, transparent)` : `color-mix(in srgb, var(--tm-danger) ${alpha}%, transparent)` };
}

async function load() {
  syncing.value = true;
  try {
    const [results, feedbackStats] = await Promise.all([Promise.all(definitions.map(async ([key, label, icon, routeKey]) => {
      try {
        const data = await resourceApi.list(key, { pageSize: key === 'ai-records' ? 20 : 1 });
        if (key === 'ai-records') records.value = data.records || [];
        return { key, label, icon, routeKey, value: data.total || 0 };
      } catch {
        return { key, label, icon, routeKey, value: '—' };
      }
    })), adminAiApi.comfortFeedbackStats().catch(() => null)]);
    metrics.value = results;
    if (feedbackStats) comfortFeedbackStats.value = feedbackStats;
  } finally {
    loading.value = false;
    syncing.value = false;
  }
}

function readAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(new Error('图片读取失败'));
    reader.readAsDataURL(file);
  });
}

async function runVision(event) {
  const file = event.target.files?.[0];
  event.target.value = '';
  if (!file) return;
  visionError.value = '';
  visionRun.value = null;
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 8 * 1024 * 1024) {
    visionError.value = '请选择不超过 8MB 的 JPG、PNG 或 WebP 图片';
    return;
  }
  visionLoading.value = true;
  try {
    previewUrl.value = await readAsDataUrl(file);
    visionRun.value = await adminAiApi.detectVision({ image_url: previewUrl.value, city: '管理端演示', resource_type: 'travel_scene' });
    await load();
  } catch (error) {
    visionError.value = error?.message || '推理服务暂不可用';
  } finally {
    visionLoading.value = false;
  }
}

function confidence(value) {
  return `${Math.round(value * 10000) / 100}%`;
}

onMounted(load);
</script>

<template>
  <div ref="root" class="admin-command">
    <section class="admin-hero" data-reveal>
      <span class="admin-hero-word type-outline" aria-hidden="true">OPS</span>
      <div class="admin-hero-rail">
        <span class="admin-hero-stamp">A0 · 运营</span>
        <span class="admin-hero-rule" aria-hidden="true" />
        <span class="admin-hero-dot" aria-hidden="true" />
      </div>
      <p class="admin-hero-eyebrow">Operations Console</p>
      <h1 class="admin-hero-title">平台运行<em>指挥台</em></h1>
      <p class="admin-hero-lead">
        资源、用户、行程、模型训练与部署验证在同一观测面内。
      </p>
      <div class="admin-hero-kpis" aria-label="业务核心指标">
        <article v-for="item in metrics" :key="item.key">
          <component :is="item.icon" :size="22" aria-hidden="true" />
          <span>{{ item.label }}</span>
          <strong>{{ loading ? '—' : item.value }}</strong>
          <RouterLink :to="`/admin/resources/${item.key}`">查看{{ item.routeKey }}</RouterLink>
        </article>
      </div>
      <button type="button" class="admin-hero-refresh" :disabled="syncing" data-magnetic @click="load">
        <RefreshCw :size="16" :class="{ 'is-spinning': syncing }" aria-hidden="true" />
        {{ syncing ? '同步中' : '刷新数据' }}
      </button>
    </section>

    <section class="admin-training" aria-labelledby="training-title" data-reveal>
      <div class="admin-model-tabs" role="tablist" aria-label="自训练模型">
        <button type="button" role="tab" :aria-selected="trainingModule === 'comfort'" :class="{ 'is-active': trainingModule === 'comfort' }" @click="trainingModule = 'comfort'"><Route :size="16" aria-hidden="true" />TravelComfort</button>
        <button type="button" role="tab" :aria-selected="trainingModule === 'vision'" :class="{ 'is-active': trainingModule === 'vision' }" @click="trainingModule = 'vision'"><Images :size="16" aria-hidden="true" />TravelRisk-YOLO</button>
      </div>
      <header class="admin-training-head">
        <div>
          <p>{{ trainingView.eyebrow }}</p>
          <h2 id="training-title">模型训练过程与成果</h2>
          <span>证据快照 {{ trainingView.date }} · {{ trainingView.description }}</span>
        </div>
        <span class="admin-training-pass"><CheckCircle2 :size="16" aria-hidden="true" />验收通过</span>
      </header>

      <div class="admin-training-kpis" aria-label="训练核心指标">
        <div v-for="item in trainingView.kpis" :key="item[0]"><span>{{ item[0] }}</span><strong>{{ item[1] }}</strong><small>{{ item[2] }}</small></div>
      </div>

      <ol class="admin-training-pipeline" aria-label="模型训练链路">
        <li v-for="(stage, index) in trainingView.stages" :key="stage.label">
          <span class="admin-training-step"><component :is="stage.icon" :size="17" aria-hidden="true" /></span>
          <div><small>0{{ index + 1 }} · {{ stage.label }}</small><strong>{{ stage.value }}</strong><span>{{ stage.detail }}</span></div>
          <ArrowRight v-if="index < trainingView.stages.length - 1" class="admin-training-arrow" :size="17" aria-hidden="true" />
        </li>
      </ol>

      <div class="admin-training-evidence">
        <section class="admin-training-visual" aria-labelledby="training-artifact-title">
          <div class="admin-training-panel-head">
            <div><span>训练产物</span><h3 id="training-artifact-title">{{ trainingArtifact === 'curve' ? trainingView.curveTitle : trainingView.matrixTitle }}</h3></div>
            <div class="admin-training-tabs" role="tablist" aria-label="训练产物">
              <button type="button" role="tab" :aria-selected="trainingArtifact === 'curve'" :class="{ 'is-active': trainingArtifact === 'curve' }" @click="trainingArtifact = 'curve'"><LineChart :size="15" aria-hidden="true" />{{ trainingView.curveTab }}</button>
              <button type="button" role="tab" :aria-selected="trainingArtifact === 'matrix'" :class="{ 'is-active': trainingArtifact === 'matrix' }" @click="trainingArtifact = 'matrix'"><Grid3X3 :size="15" aria-hidden="true" />混淆矩阵</button>
            </div>
          </div>
          <div class="admin-live-chart">
            <div v-if="trainingArtifact === 'curve' && trainingModule === 'comfort'" class="admin-curve-grid">
              <section v-for="chart in comfortCharts" :key="chart.key">
                <header><strong>{{ chart.title }}</strong><span>{{ chart.hint }}</span></header>
                <svg viewBox="0 0 540 220" role="img" :aria-label="chart.title">
                  <g v-for="tick in chart.ticks" :key="tick">
                    <line x1="52" x2="500" :y1="chartY(tick, chart.min, chart.max)" :y2="chartY(tick, chart.min, chart.max)" />
                    <text x="43" :y="chartY(tick, chart.min, chart.max) + 4" text-anchor="end">{{ tick.toFixed(2) }}</text>
                  </g>
                  <line class="axis" x1="52" x2="500" y1="180" y2="180" />
                  <polyline v-for="series in chart.series" :key="series.key" :points="chartPoints(series.values, chart.min, chart.max)" :stroke="series.color" />
                  <g v-for="(stage, index) in [1, 45, 90, 135, 180]" :key="stage">
                    <text :x="52 + index * 112" y="202" text-anchor="middle">{{ stage }}</text>
                  </g>
                </svg>
                <footer><span v-for="series in chart.series" :key="series.key"><i :style="{ background: series.color }" />{{ series.label }}</span></footer>
              </section>
            </div>
            <div v-else-if="trainingArtifact === 'curve'" class="admin-evaluation-bars">
              <div v-for="item in visionEvaluation" :key="item.label"><span>{{ item.label }}</span><strong>{{ confidence(item.value) }}</strong><i><span :style="{ width: `${item.value * 100}%` }" /></i></div>
              <p><b>{{ trainingMetrics.epochs }}</b> epochs · <b>{{ trainingMetrics.imgsz }} px</b> · batch <b>{{ trainingMetrics.batch }}</b> · seed <b>{{ trainingMetrics.seed }}</b></p>
            </div>
            <div v-else class="admin-matrix-wrap">
              <p>预测类别 →</p>
              <div class="admin-confusion-grid" :class="{ 'is-compact': activeMatrixLabels.length <= 3 }" :style="{ gridTemplateColumns: `88px repeat(${activeMatrixLabels.length}, minmax(54px, 1fr))` }">
                <span class="admin-matrix-corner">实际类别 <ArrowDown :size="11" aria-hidden="true" /></span>
                <strong v-for="label in activeMatrixLabels" :key="`head-${label}`">{{ label }}</strong>
                <template v-for="(row, rowIndex) in activeMatrix" :key="`row-${rowIndex}`">
                  <strong>{{ activeMatrixLabels[rowIndex] }}</strong>
                  <span v-for="(value, columnIndex) in row" :key="`${rowIndex}-${columnIndex}`" :style="matrixCellStyle(value, rowIndex, columnIndex)">{{ value }}</span>
                </template>
              </div>
            </div>
          </div>
          <p class="admin-chart-caption">{{ trainingArtifact === 'curve' ? trainingView.curveCaption : trainingView.matrixCaption }}</p>
        </section>

        <section class="admin-class-results" aria-labelledby="class-results-title">
          <div class="admin-training-panel-head"><div><span>独立测试集</span><h3 id="class-results-title">{{ trainingView.classTitle }}</h3></div><b>{{ trainingView.classSample }}</b></div>
          <div class="admin-class-list">
            <div v-for="item in trainingView.classes" :key="item.key">
              <div><strong>{{ item.label }}</strong><span>F1 {{ confidence(item.f1) }}</span></div>
              <i><span :style="{ width: `${item.f1 * 100}%` }" /></i>
              <small>Precision {{ confidence(item.precision) }} · Recall {{ confidence(item.recall) }} · n={{ item.support }}</small>
            </div>
          </div>
          <div v-if="trainingModule === 'comfort'" class="admin-feature-results">
            <span>主要特征贡献</span>
            <div v-for="item in comfortFeatures" :key="item.key"><strong>{{ featureLabels[item.key] || item.key }}</strong><i><span :style="{ width: `${item.value * 100}%` }" /></i><b>{{ confidence(item.value) }}</b></div>
          </div>
        </section>
      </div>

      <div v-if="trainingModule === 'comfort'" class="admin-feedback-stats" aria-label="真实体验反馈">
        <strong>真实反馈闭环</strong>
        <span :data-value="comfortFeedbackStats.total">累计反馈</span>
        <span :data-value="comfortFeedbackStats.labels.relaxed || 0">偏松</span>
        <span :data-value="comfortFeedbackStats.labels.balanced || 0">正合适</span>
        <span :data-value="comfortFeedbackStats.labels.intense || 0">太赶</span>
      </div>
      <p class="admin-training-limit"><ShieldCheck :size="15" aria-hidden="true" />{{ trainingView.limit }}</p>
    </section>

    <section class="admin-command-grid" data-reveal>
      <section class="admin-model-stage" aria-labelledby="yolo-stage-title">
        <div class="admin-panel-head">
          <div><p>DEPLOYMENT PROOF</p><h2 id="yolo-stage-title">模型部署现场验证</h2></div>
          <span class="admin-model-state" :class="{ 'is-yolo': visionData.model_mode === 'trained_yolo' }">
            <BrainCircuit :size="15" aria-hidden="true" />
            {{ visionRun ? modelLabel : '等待推理' }}
          </span>
        </div>

        <div class="admin-model-workbench">
          <label class="admin-model-upload">
            <input type="file" accept="image/jpeg,image/png,image/webp" @change="runVision" />
            <img v-if="previewUrl" :src="previewUrl" alt="待识别旅行照片" />
            <span v-else><Camera :size="28" aria-hidden="true" /> 选择旅行图片</span>
          </label>
          <div class="admin-model-result" aria-live="polite">
            <template v-if="visionLoading"><strong>正在调用推理服务</strong><span>模型会按当前环境配置返回真实结果。</span></template>
            <template v-else-if="visionRun">
              <span>本次识别</span>
              <strong>{{ visionMeta.label }}</strong>
              <b>{{ confidence(visionConfidence) }}</b>
              <p>{{ visionData.risk_hints?.[0] || '当前场景未触发额外风险提示。' }}</p>
            </template>
            <template v-else><strong>上传一张照片开始验证</strong><span>结果将显示模型路径、分类标签与置信度。</span></template>
            <p v-if="visionError" class="error-line">{{ visionError }}</p>
          </div>
        </div>
        <div v-if="visionRun" class="admin-model-evidence">
          <span>推理路径：{{ visionData.model_mode || 'unknown' }}</span>
          <span>标签：{{ visionData.labels?.map((item) => item.name).join(' / ') || '无' }}</span>
          <span>接口：/api/admin/ai/vision/detect</span>
        </div>
      </section>

      <section class="admin-telemetry" aria-labelledby="telemetry-title">
        <div class="admin-panel-head">
          <div><p>MODEL TELEMETRY</p><h2 id="telemetry-title">近期模型调用</h2></div>
          <RouterLink to="/admin/resources/ai-records">全部记录</RouterLink>
        </div>
        <div class="admin-telemetry-counts">
          <div><span>已记录调用</span><strong>{{ telemetry.total }}</strong></div>
          <div><span>YOLO 推理</span><strong>{{ telemetry.trained }}</strong></div>
          <div><span>规则降级</span><strong>{{ telemetry.fallback }}</strong></div>
        </div>
        <div class="admin-run-list">
          <div v-for="run in latestRuns" :key="run.id" class="admin-run-row">
            <span :class="{ 'is-yolo': run.modelMode === 'trained_yolo' }">{{ run.modelMode === 'trained_yolo' ? 'YOLO' : run.modelMode || '待解析' }}</span>
            <strong>{{ visionLabelMeta(run.label).label }}</strong>
            <em>{{ run.confidence ? confidence(run.confidence) : '—' }}</em>
          </div>
          <p v-if="!latestRuns.length">暂无已保存的模型调用记录。完成上方推理后会自动刷新。</p>
        </div>
      </section>
    </section>

    <section class="admin-command-actions" aria-label="运营操作入口" data-reveal>
      <RouterLink to="/admin/resources/cities" data-magnetic><MapPinned :size="18" aria-hidden="true" /><span>维护目的地内容</span></RouterLink>
      <RouterLink to="/admin/resources/users" data-magnetic><UsersRound :size="18" aria-hidden="true" /><span>处理用户与行程</span></RouterLink>
      <RouterLink to="/admin/settings" data-magnetic><Database :size="18" aria-hidden="true" /><span>检查运行配置</span></RouterLink>
    </section>

    <section class="chapter-bridge" data-reveal>
      <div class="chapter-bridge-copy">
        <p class="chapter-bridge-eyebrow">下一章</p>
        <h2 class="chapter-bridge-title">系统配置与外部服务</h2>
        <p class="chapter-bridge-lead">校验地图 Key、采集 Cookie 与大模型连接，确保所有上游通道稳定在线。</p>
      </div>
      <RouterLink class="chapter-bridge-cta" to="/admin/settings" data-magnetic>
        <span>去运行配置</span>
        <ArrowRight :size="18" :stroke-width="2.2" aria-hidden="true" />
      </RouterLink>
    </section>
  </div>
</template>
