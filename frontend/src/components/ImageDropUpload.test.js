import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ImageDropUpload from './ImageDropUpload.vue';

const image = vi.hoisted(() => vi.fn());
vi.mock('../api/upload.js', () => ({ uploadApi: { image } }));

async function selectFile(wrapper, file) {
  const input = wrapper.find('input[type="file"]');
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true });
  await input.trigger('change');
  await flushPromises();
}

beforeEach(() => {
  image.mockReset();
});

describe('图片拖拽上传', () => {
  it.each([
    ['拒绝非图片文件', new File(['text'], 'notes.txt', { type: 'text/plain' })],
    ['拒绝超过 8MB 的图片', new File([new Uint8Array(8 * 1024 * 1024 + 1)], 'large.png', { type: 'image/png' })],
  ])('%s', async (_, file) => {
    const wrapper = mount(ImageDropUpload);

    await selectFile(wrapper, file);

    expect(image).not.toHaveBeenCalled();
    expect(wrapper.find('.error-line').text()).toBe('请选择不超过 8MB 的 JPG、PNG 或 WebP 图片');
  });

  it('拖入合法图片后上传、回写并预览', async () => {
    image.mockResolvedValue({ url: '/private-uploads/1001/trip.webp' });
    let wrapper;
    wrapper = mount(ImageDropUpload, {
      props: {
        modelValue: '',
        'onUpdate:modelValue': (value) => wrapper.setProps({ modelValue: value }),
      },
    });
    const file = new File(['webp'], 'trip.webp', { type: 'image/webp' });

    await wrapper.trigger('dragover');
    expect(wrapper.classes()).toContain('is-dragging');
    await wrapper.trigger('drop', { dataTransfer: { files: [file] } });
    await flushPromises();

    expect(image).toHaveBeenCalledWith(file);
    expect(wrapper.emitted('update:modelValue')[0]).toEqual(['/private-uploads/1001/trip.webp']);
    expect(wrapper.find('img').attributes('src')).toMatch(/^blob:/);
    expect(wrapper.find('img').attributes('alt')).toBe('已上传图片预览');
    expect(wrapper.classes()).toContain('has-image');
    expect(wrapper.classes()).not.toContain('is-dragging');
  });

  it('展示上传失败原因且不回写地址', async () => {
    image.mockRejectedValue(new Error('图片服务暂时不可用'));
    const wrapper = mount(ImageDropUpload);

    await selectFile(wrapper, new File(['jpg'], 'trip.jpg', { type: 'image/jpeg' }));

    expect(wrapper.find('.error-line').text()).toBe('图片服务暂时不可用');
    expect(wrapper.emitted('update:modelValue')).toBeUndefined();
    expect(wrapper.find('img').exists()).toBe(false);
  });
});
