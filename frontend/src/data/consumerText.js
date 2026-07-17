const translations = {
  nature: '自然风光', culture: '人文古迹', family: '亲子友好', lake: '湖景', museum: '博物馆', temple: '寺庙',
  comfort: '舒适型', boutique: '精品住宿', local: '本地风味', hotpot: '火锅', food: '美食', tea: '茶食', spicy: '辛辣',
  'All day': '全天开放', '500-800': '约 ¥500–800', '600-900': '约 ¥600–900', '400-700': '约 ¥400–700',
  'West Lake': '西湖', 'Lingyin Temple': '灵隐寺', 'Forbidden City': '故宫博物院',
  'Chengdu Research Base of Giant Panda Breeding': '成都大熊猫繁育研究基地',
  'West Lake Comfort Hotel': '西湖舒适酒店', 'Hutong Boutique Stay': '胡同精品住宿', 'Chengdu Food Street Hotel': '成都美食街酒店',
  'Longjing Tea House': '龙井茶舍', 'Hutong Noodle Kitchen': '胡同面馆', 'Chengdu Hotpot Demo': '成都火锅店（演示内容）',
  'Hangzhou cuisine': '杭帮菜', 'Beijing cuisine': '北京风味', 'Sichuan cuisine': '川菜',
  'Xihu District, Hangzhou': '杭州市西湖区', 'Fayun Lane, Hangzhou': '杭州市法云弄', 'Dongcheng District, Beijing': '北京市东城区',
  'Chenghua District, Chengdu': '成都市成华区', 'Near West Lake, Hangzhou': '杭州西湖附近', 'Jinjiang District, Chengdu': '成都市锦江区', 'Meijiawu, Hangzhou': '杭州梅家坞',
  'Classic lakeside walking and cycling area.': '经典的湖畔步行与骑行区域。', 'Historic temple near forested hills.': '依山而建的历史寺庙。',
  'Imperial palace museum and landmark.': '明清皇家宫殿博物馆与地标。', 'Panda conservation and visitor park.': '大熊猫保护、科研与参观园区。',
  'Convenient hotel for lake walks.': '便于沿湖游览的住宿。', 'Small hotel close to historic streets.': '靠近历史街区的小型住宿。',
  'Good base for food exploration.': '便于探索当地美食的落脚点。', 'Tea village dishes and green tea.': '茶村菜肴与绿茶。',
  'Noodles and Beijing snacks.': '北京风味面食与小吃。', 'Classic spicy hotpot.': '经典麻辣火锅。',
};

export function consumerText(value) {
  const text = String(value || '');
  if (text.includes('Demo Planner')) return '当前为演示规划，预算未接入实时交通、价格和库存，请勿作为预订依据。';
  return translations[text] || text;
}
