import { onMounted, onUnmounted, ref } from 'vue';

/**
 * 滚动入场：为根节点下带 data-reveal 的元素添加 is-inview。
 * 尊重 prefers-reduced-motion。
 *
 * 支持异步渲染：用 MutationObserver 监听后续新增的 data-reveal 节点
 * （例如列表数据加载完成后才出现的卡片容器），自动注册到 IntersectionObserver。
 */
export function useReveal(rootRef) {
  const ready = ref(false);
  let observer;
  let mutationObserver;

  function observeEl(el) {
    if (!observer || el.dataset.revealObserved) return;
    el.dataset.revealObserved = '1';
    // 若元素未显式声明 --reveal-delay，按 DOM 顺序赋默认延迟（封顶 360ms）。
    if (!el.style.getPropertyValue('--reveal-delay')) {
      const siblings = el.parentElement
        ? Array.prototype.indexOf.call(el.parentElement.children, el)
        : 0;
      el.style.setProperty('--reveal-delay', `${Math.min(siblings * 60, 360)}ms`);
    }
    // 已在视口内的元素立即激活，避免首屏异步内容闪入失败。
    const rect = el.getBoundingClientRect();
    if (rect.top < window.innerHeight && rect.bottom > 0) {
      el.classList.add('is-inview');
      return;
    }
    observer.observe(el);
  }

  function scan(root) {
    (root.querySelectorAll ? root : document).querySelectorAll('[data-reveal]').forEach(observeEl);
  }

  onMounted(() => {
    const root = rootRef?.value || document;
    const reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;

    if (reduced || typeof IntersectionObserver === 'undefined') {
      scan(root);
      root.querySelectorAll('[data-reveal]').forEach((el) => el.classList.add('is-inview'));
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

    // 首次扫描。
    scan(root);

    // 监听后续异步渲染插入的 data-reveal 节点。
    mutationObserver = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        mutation.addedNodes.forEach((node) => {
          if (node.nodeType !== 1) return;
          if (node.hasAttribute?.('data-reveal')) observeEl(node);
          if (node.querySelectorAll) {
            node.querySelectorAll('[data-reveal]').forEach(observeEl);
          }
        });
      });
    });
    mutationObserver.observe(root, { childList: true, subtree: true });

    ready.value = true;
  });

  onUnmounted(() => {
    observer?.disconnect();
    mutationObserver?.disconnect();
  });

  return { ready };
}
