import { mount } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';
import TravelSprite from './TravelSprite.vue';

afterEach(() => vi.unstubAllGlobals());

describe('TravelSprite', () => {
  it('animates eye tracking only after pointer movement', () => {
    const frames = [];
    vi.stubGlobal('matchMedia', vi.fn((query) => ({ matches: query === '(pointer: fine)' })));
    vi.stubGlobal('requestAnimationFrame', vi.fn((callback) => {
      frames.push(callback);
      return frames.length;
    }));
    vi.stubGlobal('cancelAnimationFrame', vi.fn());

    const wrapper = mount(TravelSprite);
    expect(requestAnimationFrame).not.toHaveBeenCalled();

    window.dispatchEvent(new MouseEvent('pointermove', { clientX: 80, clientY: 40 }));
    expect(requestAnimationFrame).toHaveBeenCalledOnce();

    frames.shift()();
    expect(requestAnimationFrame).toHaveBeenCalledTimes(2);
    wrapper.unmount();
    expect(cancelAnimationFrame).toHaveBeenCalled();
  });
});
