## [2026-08-14T16:50:00+08:00] Task: T4-GREEN

**作者**: developer

### [内容]
- `EspierAddressTreeApplicationService.get()`：Redis hit/miss 均在返回前调用 `normalizeAddressTree`（展开省直辖 + 数值类型归一）；写 Redis 仍用 DB 原始树 JSON。
- `EspierRegionLabelResolveService.resolveIds`：对齐 PHP `getLocalRegionV2`——市精确/去「市」/city==region 回退「省直辖县级行政区划」；区精确/去「区/市/县」；任一级未命中抛 `IllegalArgumentException("地区不存在")`。
- 空白/null 输入同样抛「地区不存在」，禁止默认 id=1。
