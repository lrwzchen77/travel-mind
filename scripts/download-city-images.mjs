import { access, mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

const outputDir = path.resolve('frontend/public/city-images');
const cities = [
  ['北京', 'beijing', 'Beijing skyline China'],
  ['天津', 'tianjin', 'Tianjin skyline China'],
  ['石家庄', 'shijiazhuang', 'Shijiazhuang city China'],
  ['太原', 'taiyuan', 'Taiyuan city China'],
  ['呼和浩特', 'hohhot', 'Hohhot Dazhao Temple'],
  ['沈阳', 'shenyang', 'Shenyang skyline China'],
  ['长春', 'changchun', 'Changchun city China'],
  ['哈尔滨', 'harbin', 'Harbin city skyline China'],
  ['上海', 'shanghai', 'Shanghai skyline China'],
  ['南京', 'nanjing', 'Nanjing skyline China'],
  ['杭州', 'hangzhou', 'Hangzhou West Lake China'],
  ['合肥', 'hefei', 'Hefei skyline China'],
  ['福州', 'fuzhou', 'Fuzhou city China'],
  ['南昌', 'nanchang', 'Nanchang skyline China'],
  ['济南', 'jinan', 'Jinan city China'],
  ['郑州', 'zhengzhou', 'Zhengzhou skyline China'],
  ['武汉', 'wuhan', 'Wuhan skyline China'],
  ['长沙', 'changsha', 'Changsha skyline China'],
  ['广州', 'guangzhou', 'Guangzhou skyline China'],
  ['南宁', 'nanning', 'Nanning skyline China'],
  ['海口', 'haikou', 'Haikou skyline China'],
  ['重庆', 'chongqing', 'Chongqing skyline China'],
  ['成都', 'chengdu', 'Chengdu skyline China'],
  ['贵阳', 'guiyang', 'Guiyang skyline China'],
  ['昆明', 'kunming', 'Kunming city China'],
  ['拉萨', 'lhasa', 'Lhasa Potala Palace city'],
  ['西安', 'xian', 'Xi an city wall skyline China'],
  ['兰州', 'lanzhou', 'Lanzhou skyline Yellow River China'],
  ['西宁', 'xining', 'Xining city China'],
  ['银川', 'yinchuan', 'Yinchuan city China'],
  ['乌鲁木齐', 'urumqi', 'Urumqi skyline China'],
  ['台北', 'taipei', 'Taipei skyline Taiwan'],
  ['香港', 'hong-kong', 'Hong Kong skyline Victoria Harbour'],
  ['澳门', 'macau', 'Macau skyline China'],
];

const excludedTitle = /\b(map|flag|emblem|seal|logo|icon|locator|location|district|metro map|route|diagram)\b/i;
const preferredTitle = /\b(skyline|panorama|cityscape|view|night|harbour|lake|palace|wall)\b/i;
const detailTitle = /\b(bell|roof|element|calligraph|statue|bixi|sign|inscription)\b/i;
const searchFirstCities = new Set(['呼和浩特']);
const preferredCommonsFile = {
  呼和浩特: 'File:Dazhao (Wuliang) Temple in Hohhot1.JPG',
};
const pageTitleOverrides = { 北京: '北京市', 天津: '天津市', 上海: '上海市', 重庆: '重庆市', 香港: '香港', 澳门: '澳门' };

function plain(value) {
  return String(value?.value || value || '')
    .replace(/<[^>]*>/g, '')
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#039;/g, "'")
    .trim();
}

function normalized(value) {
  return String(value).toLowerCase().replace(/[^a-z0-9\u3400-\u9fff]/g, '');
}

function matchesCityTitle(title, city, slug) {
  const compactTitle = normalized(title);
  return compactTitle.includes(normalized(city)) || compactTitle.includes(normalized(slug));
}

async function getCommonsFileInfo(fileTitle) {
  const params = new URLSearchParams({
    action: 'query',
    titles: fileTitle.startsWith('File:') ? fileTitle : `File:${fileTitle}`,
    prop: 'imageinfo',
    iiprop: 'url|mime|extmetadata',
    iiurlwidth: '960',
    format: 'json',
    formatversion: '2',
    origin: '*',
  });
  const response = await fetchWithRetry(`https://commons.wikimedia.org/w/api.php?${params}`, {
    headers: { 'User-Agent': 'TravelMindCityImageDownloader/1.0 (local development)' },
  });
  if (!response.ok) return null;
  const data = await response.json();
  const page = data.query?.pages?.find((item) => item.imageinfo?.[0]);
  return page ? { page, info: page.imageinfo[0] } : null;
}

async function getWikipediaLead(city) {
  const pageTitle = pageTitleOverrides[city] || `${city}市`;
  const params = new URLSearchParams({
    action: 'query',
    titles: `${pageTitle}|${city}`,
    redirects: '1',
    prop: 'pageimages',
    piprop: 'name',
    format: 'json',
    formatversion: '2',
    origin: '*',
  });
  const response = await fetchWithRetry(`https://zh.wikipedia.org/w/api.php?${params}`, {
    headers: { 'User-Agent': 'TravelMindCityImageDownloader/1.0 (local development)' },
  });
  if (!response.ok) return null;
  const data = await response.json();
  const page = data.query?.pages?.find((item) => item.pageimage);
  if (!page) return null;
  const selected = await getCommonsFileInfo(page.pageimage);
  if (!selected || selected.info.mime !== 'image/jpeg') return null;
  if (Number(selected.info.thumbwidth || 0) < Number(selected.info.thumbheight || 1) * 1.05) return null;
  return selected;
}

async function searchCommons(city, slug, query) {
  const params = new URLSearchParams({
    action: 'query',
    generator: 'search',
    gsrsearch: `${query} filetype:bitmap`,
    gsrnamespace: '6',
    gsrlimit: '20',
    prop: 'imageinfo',
    iiprop: 'url|mime|extmetadata',
    iiurlwidth: '960',
    format: 'json',
    formatversion: '2',
    origin: '*',
  });
  const response = await fetchWithRetry(`https://commons.wikimedia.org/w/api.php?${params}`, {
    headers: { 'User-Agent': 'TravelMindCityImageDownloader/1.0 (local development)' },
  });
  if (!response.ok) throw new Error(`Commons search failed: ${response.status}`);
  const data = await response.json();
  return (data.query?.pages || [])
    .map((page) => ({ page, info: page.imageinfo?.[0] }))
    .filter(({ page, info }) => (
      info?.mime === 'image/jpeg'
      && info.thumburl
      && !excludedTitle.test(page.title)
      && matchesCityTitle(page.title, city, slug)
      && Number(info.thumbwidth || 0) >= Number(info.thumbheight || 1) * 1.2
    ))
    .sort((a, b) => {
      const score = ({ page, info }) => (
        (preferredTitle.test(page.title) ? 10 : 0)
        - (detailTitle.test(page.title) ? 12 : 0)
        + Math.min(4, Number(info.thumbwidth || 0) / Number(info.thumbheight || 1))
      );
      return score(b) - score(a);
    });
}

async function fetchWithRetry(url, options, attempts = 5) {
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    const response = await fetch(url, options);
    if (response.status !== 429 && response.status < 500) return response;
    if (attempt === attempts) return response;
    const retryAfter = Number(response.headers.get('retry-after') || 0) * 1000;
    const delay = Math.max(retryAfter, 2500 * attempt);
    console.warn(`RETRY ${response.status}\t${Math.round(delay / 1000)}s`);
    await new Promise((resolve) => setTimeout(resolve, delay));
  }
  throw new Error('Unreachable retry state');
}

async function downloadCity([city, slug, query]) {
  let selected = preferredCommonsFile[city]
    ? await getCommonsFileInfo(preferredCommonsFile[city])
    : null;
  if (!selected && !searchFirstCities.has(city)) selected = await getWikipediaLead(city);
  if (!selected) {
    let candidates = await searchCommons(city, slug, query);
    if (!candidates.length) candidates = await searchCommons(city, slug, `${city} 城市 风景`);
    selected = candidates[0];
  }
  if (!selected) throw new Error(`No suitable landscape JPEG found for ${city}`);
  const { page, info } = selected;
  const response = await fetch(info.thumburl, {
    headers: { 'User-Agent': 'TravelMindCityImageDownloader/1.0 (local development)' },
  });
  if (!response.ok || !response.headers.get('content-type')?.startsWith('image/')) {
    throw new Error(`Image download failed for ${city}: ${response.status}`);
  }
  const filename = `${slug}.jpg`;
  await writeFile(path.join(outputDir, filename), Buffer.from(await response.arrayBuffer()));

  const metadata = info.extmetadata || {};
  return {
    city,
    file: filename,
    commonsTitle: page.title,
    sourcePage: info.descriptionurl,
    originalFile: info.url,
    author: plain(metadata.Artist) || plain(metadata.Credit) || 'Wikimedia Commons contributor',
    license: plain(metadata.LicenseShortName) || plain(metadata.UsageTerms) || 'See source page',
    licenseUrl: plain(metadata.LicenseUrl),
  };
}

await mkdir(outputDir, { recursive: true });
let previousImages = [];
try {
  const previous = JSON.parse(await readFile(path.join(outputDir, 'attribution.json'), 'utf8'));
  previousImages = previous.images || [];
} catch {
  previousImages = [];
}
const previousByCity = new Map(previousImages.map((image) => [image.city, image]));
const onlySlug = process.argv.find((value) => value.startsWith('--only='))?.split('=')[1];
const selectedCities = onlySlug ? cities.filter((city) => city[1] === onlySlug) : cities;
if (!selectedCities.length) throw new Error(`Unknown city slug: ${onlySlug}`);
const selectedCityNames = new Set(selectedCities.map((city) => city[0]));
const manifest = onlySlug
  ? previousImages.filter((image) => !selectedCityNames.has(image.city))
  : [];
const failures = [];

for (const city of selectedCities) {
  try {
    const previous = previousByCity.get(city[0]);
    if (process.argv.includes('--reuse') && previous) {
      await access(path.join(outputDir, previous.file));
      manifest.push(previous);
      console.log(`REUSED ${previous.city}\t${previous.file}`);
      continue;
    }
    const result = await downloadCity(city);
    manifest.push(result);
    console.log(`DOWNLOADED ${result.city}\t${result.file}\t${result.commonsTitle}`);
  } catch (error) {
    failures.push({ city: city[0], error: error.message });
    console.error(`FAILED ${city[0]}\t${error.message}`);
  }
  await new Promise((resolve) => setTimeout(resolve, 1200));
}

manifest.sort((a, b) => cities.findIndex((city) => city[0] === a.city) - cities.findIndex((city) => city[0] === b.city));

await writeFile(
  path.join(outputDir, 'attribution.json'),
  `${JSON.stringify({ generatedAt: new Date().toISOString(), images: manifest, failures }, null, 2)}\n`,
  'utf8',
);

if (failures.length) {
  console.error(`Completed with ${failures.length} failures.`);
  process.exitCode = 1;
} else {
  console.log(`Completed: ${manifest.length} city images.`);
}
