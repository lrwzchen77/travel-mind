const SLOW_CONNECTIONS = new Set(['slow-2g', '2g']);

function numberOr(value, fallback) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

/**
 * Select rendering limits without depending on browser globals so the policy
 * remains deterministic and testable.
 */
export function createMapPerformanceProfile(input = {}) {
  const width = numberOr(input.width, 1280);
  const devicePixelRatio = numberOr(input.devicePixelRatio, 1);
  const deviceMemory = numberOr(input.deviceMemory, 8);
  const hardwareConcurrency = numberOr(input.hardwareConcurrency, 8);
  const mobile = width <= 768;
  const saveData = Boolean(input.saveData);
  const reducedMotion = Boolean(input.reducedMotion);
  const slowConnection = SLOW_CONNECTIONS.has(input.effectiveType);
  const constrainedHardware = deviceMemory <= 4 || hardwareConcurrency <= 4;
  const lowPower = mobile || saveData || slowConnection || constrainedHardware;
  const tier = lowPower ? 'low' : deviceMemory >= 8 && hardwareConcurrency >= 8 ? 'high' : 'balanced';

  if (tier === 'low') {
    return {
      tier,
      mobile,
      lowPower,
      reducedMotion,
      antialias: false,
      pixelRatio: Math.min(devicePixelRatio, 1),
      maxTileCacheSize: mobile ? 96 : 128,
      maxPitch: 55,
      enable3d: false,
      maxPoiMarkers: 6,
      flightDuration: reducedMotion ? 0 : 900,
      flightSamples: 24,
      animatedFlightPath: false,
      enableOrbit: false,
    };
  }

  const high = tier === 'high';
  return {
    tier,
    mobile,
    lowPower,
    reducedMotion,
    antialias: high,
    pixelRatio: Math.min(devicePixelRatio, 1.5),
    maxTileCacheSize: high ? 256 : 192,
    maxPitch: high ? 70 : 65,
    enable3d: true,
    maxPoiMarkers: high ? 12 : 8,
    flightDuration: reducedMotion ? 0 : high ? 2400 : 1800,
    flightSamples: high ? 56 : 40,
    animatedFlightPath: !reducedMotion,
    enableOrbit: !reducedMotion,
  };
}

export function detectMapPerformanceProfile() {
  if (typeof window === 'undefined') return createMapPerformanceProfile();
  const connection = navigator.connection || navigator.mozConnection || navigator.webkitConnection;
  return createMapPerformanceProfile({
    width: window.innerWidth,
    devicePixelRatio: window.devicePixelRatio,
    deviceMemory: navigator.deviceMemory,
    hardwareConcurrency: navigator.hardwareConcurrency,
    saveData: connection?.saveData,
    effectiveType: connection?.effectiveType,
    reducedMotion: window.matchMedia?.('(prefers-reduced-motion: reduce)').matches,
  });
}
