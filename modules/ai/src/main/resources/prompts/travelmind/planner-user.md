请为下面的用户生成旅行计划 JSON。

【基本信息】
- 途经城市：{{city_names}}
- 城市停留：
{{city_stays}}
- 开始日期：{{start_date}}
- 结束日期：{{end_date}}
- 总天数：{{travel_days}}
- 交通方式：{{transportation}}
- 住宿偏好：{{accommodation}}
- 预算：{{budget}}
- 偏好：{{preferences}}
- 额外要求：{{free_text_input}}
- 输出语言：{{language}}

【用户在地图上绘制的路线】
{{route_context}}

【地图/POI/天气上下文】
{{map_context}}

【小红书游记上下文】
{{content_context}}

【结构化输出格式】
{{format}}

【要求】
1. days 数组长度必须等于总天数。
2. 每天安排 2-3 个景点，移动日可以 1-2 个。
3. 每天的 meals 至少安排一餐，优先给出午餐和晚餐；早餐可按住宿含早或附近简餐灵活处理。
4. 每天必须有一个具体 hotel。
5. location 必须给出合理经纬度，没有精确信息时给城市附近的近似坐标。
6. 多城市时，每天 city 字段必须正确，切换城市当天设置 is_transfer_day=true。
7. 只输出 JSON 对象，不要输出 markdown。
8. 如果地图上下文里有 POI、酒店、餐饮、天气，请优先使用其中的真实名称、地址和经纬度；数量不足时再由你补齐。
9. 景点顺序要按同城就近、少走回头路的原则安排；移动日行程要轻。
10. 如果小红书游记上下文里有景点候选、预约提醒、避坑建议，请优先吸收进 attractions.description，并保留 reservation_required / reservation_tips 含义。
11. weather_info 必须覆盖每天；上下文没有对应日期预报时，明确写“待临近出发确认”，不得留空或编造天气。
12. 地图路线是用户明确选择，优先级高于普通候选数据。每个节点都必须在 days 中体现：poi 节点的原始名称要出现在 attractions.name 或 description；free_point 的原始名称要出现在当天 description 中，作为途经区域而不是虚构店铺；节点备注和偏好必须落实到当天 description 或对应景点 description。soft_order 只可因距离、营业时间或节奏小幅调序，并在 overall_suggestions 说明；strict_order 不得调换命名地点顺序。无法满足的节点也要在 overall_suggestions 明确解释，不能静默忽略。

