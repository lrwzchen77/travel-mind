import { describe, expect, it } from 'vitest';
import { createMapPerformanceProfile } from './performance.js';

describe('map performance profile', () => {
  it('limits rendering and animation work on mobile', () => {
    const profile = createMapPerformanceProfile({
      width: 390,
      devicePixelRatio: 3,
      deviceMemory: 8,
      hardwareConcurrency: 8,
    });

    expect(profile).toMatchObject({
      tier: 'low',
      mobile: true,
      antialias: false,
      pixelRatio: 1,
      maxTileCacheSize: 96,
      enable3d: false,
      animatedFlightPath: false,
    });
  });

  it('keeps 3D detail while capping desktop pixel density', () => {
    const profile = createMapPerformanceProfile({
      width: 1440,
      devicePixelRatio: 2,
      deviceMemory: 16,
      hardwareConcurrency: 12,
    });

    expect(profile).toMatchObject({
      tier: 'high',
      pixelRatio: 1.5,
      maxTileCacheSize: 256,
      enable3d: true,
      animatedFlightPath: true,
    });
  });

  it('disables expensive effects for save-data and reduced-motion users', () => {
    const profile = createMapPerformanceProfile({
      width: 1440,
      deviceMemory: 16,
      hardwareConcurrency: 12,
      saveData: true,
      reducedMotion: true,
    });

    expect(profile.tier).toBe('low');
    expect(profile.flightDuration).toBe(0);
    expect(profile.enableOrbit).toBe(false);
    expect(profile.enable3d).toBe(false);
  });
});
