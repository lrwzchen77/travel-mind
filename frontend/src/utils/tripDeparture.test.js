import { describe, expect, it } from 'vitest';
import { currentTripDayIndex, tripCalendar } from './tripDeparture.js';

describe('trip departure helpers', () => {
  const days = [{ date: '2026-08-01' }, { date: '2026-08-02' }];

  it('opens today, then the next planned day, then the last day', () => {
    expect(currentTripDayIndex(days, new Date('2026-08-02T10:00:00Z'))).toBe(1);
    expect(currentTripDayIndex(days, new Date('2026-07-30T10:00:00Z'))).toBe(0);
    expect(currentTripDayIndex(days, new Date('2026-08-04T10:00:00Z'))).toBe(1);
  });

  it('exports one all-day event per trip day', () => {
    const output = tripCalendar({ city: '杭州', days: [{ date: '2026-08-01', attractions: [{ name: '西湖;北门' }] }, { description: '日期待定' }] });
    expect(output).toContain('DTSTART;VALUE=DATE:20260801');
    expect(output).toContain('SUMMARY:Day 1 · 西湖\\;北门');
    expect(output).not.toContain('日期待定');
  });
});
