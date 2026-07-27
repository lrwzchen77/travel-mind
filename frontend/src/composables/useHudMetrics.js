import { onMounted, onUnmounted, ref } from 'vue';

const clockFormatter = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'Asia/Shanghai',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hourCycle: 'h23',
});

export function useHudMetrics() {
  const scrolled = ref(false);
  const scrollProgress = ref(0);
  const clock = ref('');
  let clockTimer;

  function onScroll() {
    const max = document.documentElement.scrollHeight - window.innerHeight;
    scrolled.value = window.scrollY > 12;
    scrollProgress.value = max > 0 ? Math.min(1, window.scrollY / max) : 0;
  }

  function tickClock() {
    clock.value = clockFormatter.format(new Date());
  }

  onMounted(() => {
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
    tickClock();
    clockTimer = window.setInterval(tickClock, 1000);
  });

  onUnmounted(() => {
    window.removeEventListener('scroll', onScroll);
    if (clockTimer) window.clearInterval(clockTimer);
  });

  return { clock, scrolled, scrollProgress };
}
