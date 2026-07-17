export const cityImageSlugs = {
  北京: 'beijing', 天津: 'tianjin', 石家庄: 'shijiazhuang', 太原: 'taiyuan',
  呼和浩特: 'hohhot', 沈阳: 'shenyang', 长春: 'changchun', 哈尔滨: 'harbin',
  上海: 'shanghai', 南京: 'nanjing', 杭州: 'hangzhou', 合肥: 'hefei',
  福州: 'fuzhou', 南昌: 'nanchang', 济南: 'jinan', 郑州: 'zhengzhou',
  厦门: 'xiamen', 青岛: 'qingdao', 桂林: 'guilin',
  武汉: 'wuhan', 长沙: 'changsha', 广州: 'guangzhou', 南宁: 'nanning',
  海口: 'haikou', 重庆: 'chongqing', 成都: 'chengdu', 贵阳: 'guiyang',
  昆明: 'kunming', 大理: 'dali', 丽江: 'lijiang', 拉萨: 'lhasa', 西安: 'xian', 兰州: 'lanzhou',
  西宁: 'xining', 银川: 'yinchuan', 乌鲁木齐: 'urumqi', 台北: 'taipei',
  香港: 'hong-kong', 澳门: 'macau',
};

export const cityImageByName = Object.fromEntries(
  Object.entries(cityImageSlugs).map(([city, slug]) => [city, `/city-images/${slug}.jpg`]),
);
