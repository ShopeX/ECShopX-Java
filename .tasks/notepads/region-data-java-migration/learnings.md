## [2026-08-14T16:50:00+08:00] Task: T4-GREEN

**作者**: developer

### [内容]
- MyBatis-Plus `LambdaQueryWrapper.getParamNameValuePairs()` 在调用 `getSqlSegment()` 前为空；单测 `matches()` 依赖已填充参数，故 `selectByParentAndLabel` 须在 `selectOne` 前调用 `w.getSqlSegment()`。
- 地址树 Redis hit 路径 Jackson 反序列化 id 为 Integer，DB 路径为 Long；`normalizeAddressTree` 须 `coerceNodeNumericTypes` 统一为 Long，否则 TC-A3 `assertEquals` 失败。
- PHP `expandDirectCountyAddresses` 语义：保留虚拟节点 id/value/path，label 改为孙节点名，children 仅含该孙节点。
