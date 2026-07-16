import { describe, expect, it } from 'vitest';
import { markdownBlocks } from './markdown.js';

describe('markdownBlocks', () => {
  it('keeps text safe while recognizing common travel-answer markdown', () => {
    expect(markdownBlocks('## 杭州\n\n- **西湖** `08:00`\n- [地图](https://example.com)')).toEqual([
      { type: 'heading', level: 2, parts: [{ type: 'text', text: '杭州' }] },
      { type: 'list', items: [
        [{ type: 'strong', text: '西湖' }, { type: 'text', text: ' ' }, { type: 'code', text: '08:00' }],
        [{ type: 'link', text: '地图', href: 'https://example.com' }],
      ] },
    ]);
  });
});
