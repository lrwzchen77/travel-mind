import { mockData, getMockResource } from '../data/mockData.js';

function ok(data) {
  return { status: 200, data: { code: 0, message: 'success', data } };
}

function delay(ms = 200) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function parsePath(url) {
  const clean = url.split('?')[0].replace(/^\/+/, '').replace(/\/+$/, '');
  return clean.split('/');
}

function parseParams(url) {
  const query = url.split('?')[1];
  if (!query) return {};
  return Object.fromEntries(new URLSearchParams(query));
}

async function mockAdapter(config) {
  const method = (config.method || 'get').toLowerCase();
  const url = config.url || '';
  const segments = parsePath(url);
  const params = parseParams(url);

  await delay(150 + Math.random() * 200);

  if (method === 'post' && segments[1] === 'auth' && segments[2] === 'login') {
    const portal = segments[0];
    return ok(portal === 'admin' ? mockData.demoAdminSession : mockData.demoSession);
  }

  if (method === 'post' && segments[1] === 'auth' && segments[2] === 'register') {
    return ok(mockData.demoSession);
  }

  if (method === 'get' && segments[1] === 'auth' && segments[2] === 'me') {
    return ok(segments[0] === 'admin' ? mockData.demoAdmin : mockData.demoUser);
  }

  if (method === 'post' && segments[1] === 'auth' && segments[2] === 'logout') {
    return ok(null);
  }

  if (method === 'get' && segments[0] === 'public' && segments[1] === 'resources') {
    const key = segments[2];
    let items = getMockResource(key) || [];
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      items = items.filter((item) => Object.values(item).some((v) => String(v).toLowerCase().includes(kw)));
    }
    if (params.city) items = items.filter((item) => item.city === params.city);
    if (params.category && items[0]?.category) items = items.filter((item) => item.category === params.category);
    return ok(items);
  }

  if (method === 'get' && segments[0] === 'public' && segments[1] === 'resources' && segments[3]) {
    const key = segments[2];
    const id = Number(segments[3]);
    const item = (getMockResource(key) || []).find((r) => r.id === id);
    return ok(item || null);
  }

  if (method === 'get' && segments[0] === 'public' && segments[1] === 'inspirations') {
    if (segments[2] && segments[3] === 'comments') {
      const id = Number(segments[2]);
      const allComments = mockData.communityComments[id] || [];
      return ok({ records: allComments, total: allComments.length });
    }
    if (segments[2]) {
      const id = Number(segments[2]);
      return ok(mockData.communityPosts.find((p) => p.id === id) || null);
    }
    let posts = mockData.communityPosts;
    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      posts = posts.filter((p) => p.title.toLowerCase().includes(kw) || String(p.content || '').toLowerCase().includes(kw) || String(p.tags || '').toLowerCase().includes(kw));
    }
    if (params.city) posts = posts.filter((p) => p.city === params.city);
    if (params.topic) posts = posts.filter((p) => p.topic === params.topic);
    return ok({ records: posts, total: posts.length });
  }

  if (method === 'get' && segments[0] === 'public' && segments[1] === 'travel-map') {
    return ok({
      pois: mockData.adminMapPois.filter((p) => p.city === params.city),
      city: params.city,
    });
  }

  if (method === 'get' && segments[0] === 'poi' && segments[1] === 'photo') {
    return ok({ url: '' });
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'library') {
    const key = segments[2];
    if (key === 'favorites') return ok([]);
    if (key === 'travel-notes') return ok([]);
    if (key === 'ai-records') return ok(mockData.adminAiRecords.filter((r) => r.user_id === 1));
    return ok([]);
  }

  if (method === 'post' && segments[0] === 'user' && segments[1] === 'library') {
    return ok({ id: Date.now(), ...JSON.parse(config.data || '{}') });
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'profile') {
    return ok(mockData.profile);
  }

  if (method === 'put' && segments[0] === 'user' && segments[1] === 'profile') {
    return ok({ ...mockData.profile, ...JSON.parse(config.data || '{}') });
  }

  if (method === 'put' && segments[0] === 'user' && segments[1] === 'account' && segments[2] === 'password') {
    return ok(null);
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'account' && segments[2] === 'export') {
    return ok({ data: '导出数据（演示模式）' });
  }

  if (method === 'delete' && segments[0] === 'user' && segments[1] === 'account') {
    return ok(null);
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'trip' && segments[2] === 'history') {
    return ok({ items: mockData.trips, total: mockData.trips.length });
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'trip' && segments[3] === 'expenses') {
    return ok({ items: [], total: 0 });
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'trip' && segments[2] && !isNaN(Number(segments[2]))) {
    const id = Number(segments[2]);
    return ok(mockData.trips.find((t) => t.id === id) || null);
  }

  if (method === 'post' && segments[0] === 'user' && segments[1] === 'trip' && segments[2] === 'plan') {
    return ok({ task_id: 'mock-task-1', status: 'completed' });
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'trip' && segments[2] === 'status') {
    return ok(mockData.tripPlanResult);
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'memories') {
    if (segments[2]) {
      const id = Number(segments[2]);
      return ok(mockData.memories.find((m) => m.id === id) || null);
    }
    return ok(mockData.memories);
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'journals') {
    if (segments[2]) {
      const id = Number(segments[2]);
      return ok(mockData.journals.find((j) => j.id === id) || null);
    }
    return ok(mockData.journals);
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'notifications') {
    return ok(mockData.notifications);
  }

  if (method === 'post' && segments[0] === 'user' && segments[1] === 'notifications') {
    return ok(null);
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'recommendations') {
    const type = params.type || 'city';
    return ok(mockData.recommendations[type] || mockData.recommendations.city);
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'inspirations' && segments[2] === 'bag') {
    return ok(mockData.inspirationBag);
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'inspirations' && segments[2] === 'posts') {
    return ok([]);
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'assistant' && segments[2] === 'conversations') {
    if (segments[3]) return ok(mockData.assistantConversations[0] || null);
    return ok(mockData.assistantConversations);
  }

  if (method === 'get' && segments[0] === 'user' && segments[1] === 'ai') {
    if (segments[2] === 'trip' && segments[4] === 'comfort') {
      if (segments[5] === 'feedback') return ok({});
      return ok(mockData.comfortResult);
    }
    return ok({});
  }

  if (method === 'post' && segments[0] === 'user' && segments[1] === 'ai') {
    if (segments[2] === 'vision' && segments[3] === 'detect') return ok(mockData.visionResult);
    if (segments[2] === 'content' && segments[3] === 'analyze') return ok({ summary: '内容分析（演示模式）', labels: [] });
    if (segments[2] === 'trip' && segments[4] === 'comfort' && segments[5] === 'feedback') return ok(null);
    return ok({});
  }

  if (method === 'post' && segments[0] === 'user' && segments[1] === 'uploads' && segments[2] === 'images') {
    return ok({ url: 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg"/%3E' });
  }

  if (method === 'get' && segments[0] === 'admin' && segments[1] === 'resources') {
    const key = segments[2];
    return ok(getMockResource(key) || []);
  }

  if (method === 'get' && segments[0] === 'admin' && segments[1] === 'users') {
    return ok(mockData.adminUsers);
  }

  if (method === 'put' && segments[0] === 'admin' && segments[1] === 'users' && segments[3] === 'password') {
    return ok(null);
  }

  if (method === 'put' && segments[0] === 'admin' && segments[1] === 'users' && segments[3] === 'role') {
    return ok(null);
  }

  if (method === 'post' && segments[0] === 'admin' && segments[1] === 'ai') {
    if (segments[2] === 'trip' && segments[3] === 'evaluate') return ok(mockData.comfortResult);
    return ok({});
  }

  if (method === 'get' && segments[0] === 'admin' && segments[1] === 'ai') {
    if (segments[2] === 'travel-comfort' && segments[3] === 'feedback' && segments[4] === 'stats') {
      return ok({ total: 42, labels: { relaxed: 20, balanced: 15, intense: 7 } });
    }
    return ok({});
  }

  if (method === 'post' && segments[0] === 'admin' && segments[1] === 'inspirations') {
    return ok(null);
  }

  return ok([]);
}

export default mockAdapter;
