const tileJsonPromises = new Map();

export function lngLatToTile([lng, lat], zoom) {
  const n = 2 ** zoom;
  const safeLat = Math.max(-85.05112878, Math.min(85.05112878, lat));
  const x = Math.floor(((lng + 180) / 360) * n);
  const latRad = (safeLat * Math.PI) / 180;
  const y = Math.floor(
    ((1 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI) / 2) * n,
  );
  return [Math.max(0, Math.min(n - 1, x)), Math.max(0, Math.min(n - 1, y))];
}

export function fillTileTemplate(template, zoom, x, y) {
  return template
    .replaceAll('{z}', String(zoom))
    .replaceAll('{x}', String(x))
    .replaceAll('{y}', String(y));
}

function addTile(urls, template, zoom, x, y) {
  const max = 2 ** zoom - 1;
  if (x < 0 || y < 0 || x > max || y > max) return;
  urls.add(fillTileTemplate(template, zoom, x, y));
}

export function collectFlightTileUrls(template, route, destination) {
  const urls = new Set();

  // A handful of z6 tiles keeps the cross-city overview continuous.
  for (const point of route) {
    const [x, y] = lngLatToTile(point, 6);
    addTile(urls, template, 6, x, y);
  }

  // Parent tiles remain visible while the final z13 detail arrives.
  for (const zoom of [10, 12]) {
    const [centerX, centerY] = lngLatToTile(destination, zoom);
    for (let x = centerX - 1; x <= centerX + 1; x += 1) {
      for (let y = centerY - 1; y <= centerY + 1; y += 1) {
        addTile(urls, template, zoom, x, y);
      }
    }
  }

  return [...urls];
}

async function resolveTileTemplate(style) {
  const vectorSource = Object.values(style?.sources || {}).find((source) => source.type === 'vector');
  if (!vectorSource) return null;
  if (vectorSource.tiles?.[0]) return vectorSource.tiles[0];
  if (!vectorSource.url) return null;

  if (!tileJsonPromises.has(vectorSource.url)) {
    const request = fetch(vectorSource.url, {
      cache: 'force-cache',
      credentials: 'omit',
      mode: 'cors',
    })
      .then((response) => {
        if (!response.ok) throw new Error(`tilejson ${response.status}`);
        return response.json();
      })
      .catch(() => null);
    tileJsonPromises.set(vectorSource.url, request);
  }
  const tileJson = await tileJsonPromises.get(vectorSource.url);
  return tileJson?.tiles?.[0] || null;
}

async function warmUrls(urls, signal) {
  let cursor = 0;
  const worker = async () => {
    while (cursor < urls.length) {
      const url = urls[cursor];
      cursor += 1;
      try {
        const response = await fetch(url, {
          cache: 'force-cache',
          credentials: 'omit',
          mode: 'cors',
          signal,
        });
        if (response.ok) await response.arrayBuffer();
      } catch {
        // MapLibre will retry missing tiles through its normal request path.
      }
    }
  };
  await Promise.all(Array.from({ length: Math.min(4, urls.length) }, worker));
}

export async function prefetchFlightTiles(style, route, destination, timeoutMs = 1800) {
  const template = await resolveTileTemplate(style);
  if (!template || !template.includes('{z}') || !template.includes('{x}') || !template.includes('{y}')) {
    return;
  }

  const urls = collectFlightTileUrls(template, route, destination);
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), timeoutMs);
  try {
    await warmUrls(urls, controller.signal);
  } finally {
    window.clearTimeout(timer);
  }
}
