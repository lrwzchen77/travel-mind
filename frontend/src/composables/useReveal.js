import { onMounted, onUnmounted, ref } from 'vue';

/**
 * 滚动入场：为根节点下带 data-reveal 的元素添加 is-inview。
 * 尊重 prefers-reduced-motion。
 */
export function useReveal(rootRef) {
  const ready = ref(false);
  let observer;

  onMounted(() => {
    const root = rootRef?.value || document;
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const nodes = (root.querySelectorAll ? root : document).querySelectorAll('[data-reveal]');

    if (reduced) {
      nodes.forEach((el) => el.classList.add('is-inview'));
      ready.value = true;
      return;
    }

    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-inview');
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -40px 0px' },
    );

    nodes.forEach((el, i) => {
      el.style.setProperty('--reveal-delay', `${Math.min(i * 60, 360)}ms`);
      observer.observe(el);
    });
    ready.value = true;
  });

  onUnmounted(() => observer?.disconnect());

  return { ready };
}
