function dateKey(value = new Date()) {
  if (!(value instanceof Date)) return String(value || '').slice(0, 10);
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`;
}

export function currentTripDayIndex(days = [], today = new Date()) {
  if (!days.length) return -1;
  const key = dateKey(today);
  const todayIndex = days.findIndex((day) => day.date === key);
  if (todayIndex >= 0) return todayIndex;
  const nextIndex = days.findIndex((day) => day.date && day.date > key);
  return nextIndex >= 0 ? nextIndex : days.length - 1;
}

function escapeIcs(value) {
  return String(value || '').replace(/\\/g, '\\\\').replace(/;/g, '\\;').replace(/,/g, '\\,').replace(/\r?\n/g, '\\n');
}

export function tripCalendar(plan = {}) {
  const events = (plan.days || []).filter((day) => /^\d{4}-\d{2}-\d{2}$/.test(day.date || '')).flatMap((day, index) => {
    const stops = [...(day.attractions || []), ...(day.meals || [])];
    if (day.hotel?.name) stops.push(day.hotel);
    const summary = stops.map((stop) => stop.name).filter(Boolean).join('、') || day.description || '旅行安排';
    return [
      'BEGIN:VEVENT',
      `UID:travel-mind-${escapeIcs(plan.city)}-${index}@local`,
      `DTSTART;VALUE=DATE:${String(day.date || '').replaceAll('-', '')}`,
      `SUMMARY:${escapeIcs(`Day ${index + 1} · ${summary}`)}`,
      `LOCATION:${escapeIcs(day.city || plan.city)}`,
      'END:VEVENT',
    ];
  });
  return ['BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//Travel Mind//Trip//ZH', ...events, 'END:VCALENDAR', ''].join('\r\n');
}
