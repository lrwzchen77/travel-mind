import { onMounted, onUnmounted } from 'vue';

/**
 * useMagnetic — 磁吸交互
 * 为根节点下带 data-magnetic 的元素添加指针跟随位移：
 * 指针在元素上方时，元素被"吸"向指针方向偏移，离开后弹簧回弹归位。
 * 仅精确指针设备启用，尊重 prefers-reduced-motion。
 */
export function useMagnetic(rootRef, { strength = 0.32, radius = 1.0 } = {}) {
  let cleanups = [];
  let mutationObserver;

  function attach(el) {
    if (el.dataset.magneticBound) return;
    el.dataset.magneticBound = '1';

    let raf = 0;
    let tx = 0;
    let ty = 0;
    let cx = 0;
    let cy = 0;

    function render() {
      cx += (tx - cx) * 0.18;
      cy += (ty - cy) * 0.18;
      el.style.translate = `${cx.toFixed(2)}px ${cy.toFixed(2)}px`;
      if (Math.abs(tx - cx) > 0.1 || Math.abs(ty - cy) > 0.1) {
        raf = window.requestAnimationFrame(render);
      } else {
        raf = 0;
        if (tx === 0 && ty === 0) el.style.translate = '';
      }
    }

    function kick() {
      if (!raf) raf = window.requestAnimationFrame(render);
    }

    function onMove(e) {
      const rect = el.getBoundingClientRect();
      const dx = e.clientX - (rect.left + rect.width / 2);
      const dy = e.clientY - (rect.top + rect.height / 2);
      tx = dx * strength * radius;
      ty = dy * strength * radius;
      kick();
    }

    function onLeave() {
      tx = 0;
      ty = 0;
      kick();
    }

    el.addEventListener('pointermove', onMove, { passive: true });
    el.addEventListener('pointerleave', onLeave, { passive: true });
    cleanups.push(() => {
      el.removeEventListener('pointermove', onMove);
      el.removeEventListener('pointerleave', onLeave);
      if (raf) window.cancelAnimationFrame(raf);
      el.style.translate = '';
      delete el.dataset.magneticBound;
    });
  }

  function scan(root) {
    root.querySelectorAll?.('[data-magnetic]').forEach(attach);
  }

  onMounted(() => {
    const fine = window.matchMedia?.('(pointer: fine)').matches;
    const reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    if (!fine || reduced) return;

    const root = rootRef?.value || document;
    scan(root);

    mutationObserver = new MutationObserver((mutations) => {
      mutations.forEach((m) => {
        m.addedNodes.forEach((node) => {
          if (node.nodeType !== 1) return;
          if (node.hasAttribute?.('data-magnetic')) attach(node);
          scan(node);
        });
      });
    });
    mutationObserver.observe(root === document ? document.body : root, { childList: true, subtree: true });
  });

  onUnmounted(() => {
    cleanups.forEach((fn) => fn());
    cleanups = [];
    mutationObserver?.disconnect();
  });
}
