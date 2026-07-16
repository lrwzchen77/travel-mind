import { flushPromises, mount } from '@vue/test-utils';
import { createMemoryHistory, createRouter } from 'vue-router';
import { describe, expect, it } from 'vitest';
import PageTransition from './PageTransition.vue';

const FragmentView = {
  props: ['title'],
  template: '<h1>{{ title }}</h1><p>fragment content</p>',
};

describe('page transition', () => {
  it('wraps fragment route views in an animatable element during navigation', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/one', component: FragmentView, props: { title: 'One' } },
        { path: '/two', component: FragmentView, props: { title: 'Two' } },
      ],
    });
    await router.push('/one');
    await router.isReady();

    const wrapper = mount(PageTransition, { global: { plugins: [router] } });
    expect(wrapper.get('.page-transition-shell').text()).toContain('One');

    await router.push('/two');
    await flushPromises();

    expect(wrapper.get('.page-transition-shell').text()).toContain('Two');
  });
});
