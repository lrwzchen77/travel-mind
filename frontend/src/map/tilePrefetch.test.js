import { describe, expect, it } from 'vitest';
import { collectFlightTileUrls, fillTileTemplate, lngLatToTile } from './tilePrefetch.js';

describe('flight tile prefetch', () => {
  it('converts known coordinates into stable web mercator tiles', () => {
    expect(lngLatToTile([0, 0], 1)).toEqual([1, 1]);
    expect(lngLatToTile([120.1551, 30.2741], 6)).toEqual([53, 26]);
  });

  it('fills a standard vector tile URL template', () => {
    expect(fillTileTemplate('https://maps.test/{z}/{x}/{y}.pbf', 6, 53, 26))
      .toBe('https://maps.test/6/53/26.pbf');
  });

  it('deduplicates route tiles and includes destination parent grids', () => {
    const urls = collectFlightTileUrls(
      'https://maps.test/{z}/{x}/{y}.pbf',
      [[120.1551, 30.2741], [120.1551, 30.2741], [104.0665, 30.5728]],
      [104.0665, 30.5728],
    );

    expect(urls.filter((url) => url.includes('/6/')).length).toBe(2);
    expect(urls.filter((url) => url.includes('/10/')).length).toBe(9);
    expect(urls.filter((url) => url.includes('/12/')).length).toBe(9);
  });
});
