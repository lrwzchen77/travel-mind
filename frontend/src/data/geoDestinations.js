/** 目的地地理数据（WGS84）— 供 3D 地图飞航与标注 */

export const geoDestinations = [
  {
    city: '杭州',
    lng: 120.1551,
    lat: 30.2741,
    zoom: 12.2,
    pitch: 58,
    bearing: -28,
    tag: '湖光山色',
    blurb: '西湖、灵隐与河坊街，周末慢游首选。',
    mood: 'haze',
    pois: [
      { name: '西湖', lng: 120.1485, lat: 30.242 },
      { name: '灵隐寺', lng: 120.1012, lat: 30.2411 },
      { name: '河坊街', lng: 120.1689, lat: 30.2428 },
    ],
  },
  {
    city: '成都',
    lng: 104.0668,
    lat: 30.5728,
    zoom: 12,
    pitch: 55,
    bearing: 18,
    tag: '烟火人间',
    blurb: '宽窄巷子与火锅，胃和眼睛一起放假。',
    mood: 'spice',
    pois: [
      { name: '宽窄巷子', lng: 104.0545, lat: 30.6693 },
      { name: '春熙路', lng: 104.0815, lat: 30.6565 },
      { name: '大熊猫基地', lng: 104.148, lat: 30.738 },
    ],
  },
  {
    city: '厦门',
    lng: 118.0894,
    lat: 24.4798,
    zoom: 12.4,
    pitch: 52,
    bearing: -12,
    tag: '海风小岛',
    blurb: '鼓浪屿的风，沙茶面的鲜。',
    mood: 'sea',
    pois: [
      { name: '鼓浪屿', lng: 118.0628, lat: 24.4475 },
      { name: '中山路', lng: 118.082, lat: 24.457 },
      { name: '曾厝垵', lng: 118.1305, lat: 24.4365 },
    ],
  },
  {
    city: '西安',
    lng: 108.9398,
    lat: 34.3416,
    zoom: 12.1,
    pitch: 56,
    bearing: 24,
    tag: '古今同框',
    blurb: '城墙骑行、回民街宵夜。',
    mood: 'terra',
    pois: [
      { name: '钟楼', lng: 108.9425, lat: 34.2606 },
      { name: '回民街', lng: 108.941, lat: 34.2635 },
      { name: '大雁塔', lng: 108.9642, lat: 34.2194 },
    ],
  },
  {
    city: '大理',
    lng: 100.2676,
    lat: 25.6065,
    zoom: 11.6,
    pitch: 50,
    bearing: -8,
    tag: '风花雪月',
    blurb: '洱海骑行与古城慢时光。',
    mood: 'haze',
    pois: [
      { name: '大理古城', lng: 100.161, lat: 25.695 },
      { name: '洱海', lng: 100.22, lat: 25.72 },
    ],
  },
  {
    city: '青岛',
    lng: 120.3826,
    lat: 36.0671,
    zoom: 12,
    pitch: 54,
    bearing: 32,
    tag: '红瓦绿树',
    blurb: '海边栈桥与啤酒飘香。',
    mood: 'sea',
    pois: [
      { name: '栈桥', lng: 120.321, lat: 36.061 },
      { name: '八大关', lng: 120.356, lat: 36.053 },
    ],
  },
  {
    city: '丽江',
    lng: 100.227,
    lat: 26.855,
    zoom: 12.3,
    pitch: 52,
    bearing: -22,
    tag: '高原古城',
    blurb: '玉龙雪山下的石板路与灯火。',
    mood: 'terra',
    pois: [
      { name: '丽江古城', lng: 100.233, lat: 26.877 },
      { name: '束河古镇', lng: 100.213, lat: 26.916 },
    ],
  },
  {
    city: '桂林',
    lng: 110.29,
    lat: 25.2736,
    zoom: 11.8,
    pitch: 48,
    bearing: 10,
    tag: '山水甲天下',
    blurb: '漓江山水与阳朔西街。',
    mood: 'haze',
    pois: [
      { name: '象鼻山', lng: 110.295, lat: 25.268 },
      { name: '两江四湖', lng: 110.3, lat: 25.275 },
    ],
  },
];

export function findDestination(city) {
  if (!city) return geoDestinations[0];
  const key = String(city).trim();
  return (
    geoDestinations.find((item) => item.city === key)
    || geoDestinations.find((item) => key.includes(item.city) || item.city.includes(key))
    || {
      city: key,
      lng: 116.4074,
      lat: 39.9042,
      zoom: 10.5,
      pitch: 45,
      bearing: 0,
      tag: '目的地',
      blurb: '在地图上探索这座城。',
      mood: 'haze',
      pois: [],
    }
  );
}
