# 积分流水字段改造计划

## 背景

当前 `point_member_log` 表字段较少，仅记录基本的积分变动信息。PRD 要求积分流水展示更丰富的维度数据，包括店铺信息、积分状态、有效期等。

本计划解决：
1. 数据模型补充（新增 `distributor_id` 字段）
2. 各场景写入 `distributor_id` 的改造
3. 积分流水查询接口改造（字段丰富化）

> **依赖：** Phase 1-9 基础设施已就绪。本计划独立于 Phase 10（报表），报表需求待确认后再做展示/导出层。
> **前置条件：** Scope 命名重构（SCOPE_NAMING_REFACTOR_PLAN.md）已完成。重构后 `PointBatchDeductService.deduct()` 的参数已从 `merchantId` 改为 `distributorId`，本计划直接复用。

---

## 积分流水目标字段

| 字段 | 数据来源 | 说明 |
|------|---------|------|
| 积分活动名称 | `point_activity.title` via `log.activityId` | |
| 类型 | `income > 0` = 获取, `outcome > 0` = 支出 | |
| 积分来源 | `journal_type` 映射 | 导入/转赠/注册赠送/消费购物/初始 |
| 店铺名称 | `distribution_distributor.name` via `log.distributorId` | 空则不展示 |
| 使用范围 | `point_activity.scope_type` via `log.activityId` | 1=线上, 2=线下, 3=全场景 |
| 手机号 | `members.mobile` via `log.userId` | |
| 积分变动 | `income` 或 `outcome` | +N / -N |
| 变动类型 | `journal_type` 描述文案 | 例如"注册赠送积分" |
| 当前剩余积分 | `point_batch.remain_point` via `log.batchId` | 这笔积分还剩多少未使用 |
| 过期积分 | batch 已过期时的 `remain_point` | 过期时未使用的部分作废 |
| 积分状态 | 从 batch 推导 | 有效/已使用/已过期 |
| 记录 | `point_member_log.point_desc` | 保持现有字段 |
| 有效期 | `point_batch.expire_time` via `log.batchId` | 0=永不过期 |
| 订单号 | `point_member_log.order_id` | |
| 大区/区域/楼层/街道/类目 | `DistributorRelAttributes` via `log.distributorId` | 店铺动态属性 |
| 用户名 | `members_info.username` via `log.userId` | |
| 昵称 | `members_wechatusers.nickname` via `log.userId` | |

### 积分状态推导规则（三种，无"已冻结"）

- **有效** — `batch.remain_point > 0` 且 (`batch.expire_time = 0` 或 `batch.expire_time > now`)
- **已使用** — `batch.remain_point = 0`
- **已过期** — `batch.expire_time > 0` 且 `batch.expire_time <= now` 且 `batch.remain_point > 0`

> ⚠️ **注意：** 积分状态是**实时推导**的，同一条流水在不同时间查询可能返回不同状态（如从"有效"变为"已过期"）。这是预期行为 — 反映的是这笔积分的当前真实状态。

### 过期积分字段推导规则

- batch 未过期（有效或已使用）→ `expire_point = 0`
- batch 已过期（`expire_time > 0` 且 `expire_time <= now`）→ `expire_point = remain_point`（过期时未使用的部分作废）

### 使用范围来源说明

使用范围从**积分活动**（`point_activity`）上获取，通过 `log.activityId` 关联查询 `point_activity.scope_type`。不从 batch 上取（batch 的 scope 是冗余快照，活动才是权威来源）。

---

## Phase A：数据模型变更

### A.1 `point_member_log` 新增字段

```sql
ALTER TABLE point_member_log ADD COLUMN distributor_id BIGINT DEFAULT NULL COMMENT '关联店铺ID（消费/核销时的店铺）';
```

> **索引暂不加。** `distributor_id` 大部分行为 NULL（获取类场景远多于支出类），且查询时是先分页取 log 再按 distributorId 批量 enrich，不需要按 distributor_id 做 WHERE 过滤。后续如果确认需要按店铺筛选再加索引。

### A.2 `point_batch_consume` 新增字段

```sql
ALTER TABLE point_batch_consume ADD COLUMN distributor_id BIGINT DEFAULT NULL COMMENT '消费店铺ID';
```

> **同理不加索引。** consume 记录通过 order_id 或 batch_id 查询，不需要按 distributor_id 检索。

### A.3 旧数据处理

**决策：不回填。** 已有的 log/consume 记录 `distributor_id` 保持 NULL。原因：
- 旧的线下核销日志没有单独存 merchant_id，无法可靠回填
- 报表/导出中店铺列为空是可接受的（老数据本身就没有这个维度）
- 避免复杂的数据迁移脚本引入风险

### A.3 Java 实体变更

| 文件 | 变更 |
|------|------|
| `PointMemberLog.java` | 新增 `distributorId` 字段（Long） |
| `PointBatchConsume.java` | 新增 `distributorId` 字段（Long） |

---

## Phase B：各场景写入 distributor_id

> **前提：** Scope 命名重构已完成，`PointBatchDeductService.deduct()` 参数已是 `distributorId`，`PointOfflineConsumeService` 内部变量已是 `distributorId`。

### B.1 线下核销（journal_type=21）

**来源：** 前端请求参数中的 `distributor_id`（命名重构后已统一）

**改造点：**
- `PointOfflineConsumeService.consume()` — 写 log 时设置 `log.distributorId = distributorId`
- `PointBatchDeductService.deduct()` — 创建 consume 记录时直接用已有的 `distributorId` 参数填充 `consume.distributorId`

**事务保证：** 现有 `PointOfflineConsumeService.consume()` 已有 `@Transactional`，log 和 consume 写入在同一事务内，distributor_id 一致性有保障。

**改造文件：**
- `PointOfflineConsumeService.java` — writeLogsByActivity 方法中设置 log.distributorId
- `PointBatchDeductService.java` — 创建 consume 时填充已有的 distributorId 参数到记录中

### B.2 线上订单积分抵扣

**来源：** 订单数据中的 `distributor_id`（已存在于订单上下文）

**改造点：**
- 积分抵扣调用链需要透传 `distributorId`：
  - `OrdersPaymentDoPaymentService.doPayment()` 中 `data.get("distributor_id")` 已有值
  - 需要传给 `pointMemberAddPointService` 的扣减方法
  - 最终由 `PointBatchDeductService.deduct()` 内部将已有的 `distributorId` 参数写入 consume 记录

**改造文件：**
- `PointMemberAddPointService.java` — 扣减方法透传 distributorId（命名重构后参数名已统一）
- orders 模块调用方 — 透传 distributorId

### B.3 获取类场景（无店铺）

| 场景 | distributor_id |
|------|---------------|
| 导入积分（journal_type=18） | NULL |
| 注册赠送（journal_type=1） | NULL |
| 下单送积分（journal_type=7） | NULL（送积分无特定店铺） |
| 转赠转出/转入（journal_type=19/20） | NULL |
| 退款返还（journal_type=9） | NULL（退款恢复积分无店铺概念） |

不改动，字段保持 NULL。

---

## Phase C：积分流水查询改造

### C.1 列表接口改造

**现有接口：** `GET /api/v1/point/member`

**新增返回字段（在现有 enrichment 基础上扩展）：**

```json
{
  "activity_name": "618大促活动",
  "scope_type": 3,
  "scope_type_desc": "全场景",
  "distributor_id": 123,
  "distributor_name": "朝阳门店",
  "batch_remain_point": 80,
  "batch_expire_time": 1720000000,
  "batch_status": "valid",
  "batch_status_desc": "有效",
  "distributor_attributes": {
    "大区": "华北",
    "区域": "北京",
    "楼层": "B1",
    "街道": "朝阳路",
    "类目": "餐饮"
  }
}
```

### C.2 数据组装逻辑

查询流水列表后，批量补充关联数据：

```
1. 收集所有 activityId（非null） → 批量查 point_activity → Map<activityId, activity>
2. 收集所有 batchId（非null） → 批量查 point_batch → Map<batchId, batch>
3. 收集所有 distributorId（非null，去重） → 批量查 distributor name → Map<distributorId, name>
4. 同上去重后的 distributorId → 批量查动态属性 → Map<distributorId, attributes>
5. 收集所有 userId → 批量查会员信息（现有逻辑，补充 nickname）
6. 遍历列表，组装完整字段
```

**性能考虑：**
- 批量查询，避免 N+1
- **distributor 数据按 ID 去重** — 同一页内多条日志可能来自同一店铺，只查一次
- **request 级 Map 缓存** — EnrichService 内部用 `Map<Long, DistributorInfo>` 做 per-request 去重缓存
- 如列表中 distributor_id 大多为 NULL（获取类日志），则实际查询量不大
- 动态属性查询用 `DistributorRelAttributesQueryService` 已有的批量方法（如无批量方法则按 ID 循环，但有 Map 去重保底）

### C.3 新增 Service

| 文件 | 说明 |
|------|------|
| `PointMemberLogEnrichService.java` | 流水列表字段丰富化服务 — **仅做数据组装调度**，具体查询委托给各领域 Service（ActivityReadService、DistributorQueryService 等），避免成为 god-service |

### C.4 新增筛选条件

**本次实现：**

| 参数 | 说明 | 实现方式 |
|------|------|---------|
| `activity_id` | 按积分活动筛选 | `point_member_log.activity_id` 直接 WHERE 过滤（字段已有索引 idx_activity） |

**后端改造：**
- `PointMemberController.java` — 列表接口新增 `activity_id` 可选参数
- `PointMemberListService.java` — `buildLogWrapper` 方法中增加 `activity_id` 过滤条件：
  ```java
  if (activityId != null && activityId > 0) {
      wrapper.eq(PointMemberLog::getActivityId, activityId);
  }
  ```
- 导出接口同步支持 `activity_id` 过滤（`buildLogWrapperForExport` 同步加）
- `PointMemberLogExportContext` record 新增 `Long activityId` 字段

**⚠️ Deferred（不在本次范围）：**

| 参数 | 说明 | 延后原因 |
|------|------|---------|
| `journal_type` | 按积分来源类型筛选 | 需求未确认 |
| `distributor_id` | 按店铺筛选 | 需要加索引才有意义，待确认 |
| `batch_status` | 按积分状态筛选（有效/已使用/已过期） | 需要 JOIN + 派生条件做 WHERE，实现复杂度高，单独任务处理 |

---

## Phase D：导出改造

在现有 `PointMemberLogCsvExportService` 基础上扩展导出列：

| 新增列 | 来源 |
|--------|------|
| 积分活动 | activity.title |
| 使用范围 | activity.scope_type 描述 |
| 店铺名称 | distributor.name |
| 当前剩余积分 | batch.remain_point（替代从 point_desc 解析的旧逻辑） |
| 积分状态 | batch 推导 |
| 有效期 | batch.expire_time 格式化 |
| 大区/区域/楼层/街道/类目 | 店铺动态属性 |
| 昵称 | wechat nickname |

---

## Phase E：后管页面改造（ecshopx-admin） ✅ 已完成（2026-07-16，commit 59f7a1041 @ ecshopx-admin tp-junfa）

### E.1 现有页面结构

| 文件 | 说明 |
|------|------|
| `src/view/member/point/overview.vue` | 容器页面，两个 tab：总览 + 积分记录 |
| `src/view/member/point/pointrecord.vue` | **积分流水列表**（主要改造对象） |
| `src/view/member/point/totalview.vue` | 积分总览（汇总数据 + 简化列表） |
| `src/view/member/members/comps/pointList.vue` | 会员详情中的积分记录（SpFinder 组件） |

**路由：** `members/member-benefit/point-overview`

### E.1.1 隐藏"总览" tab，只保留"积分记录"

`overview.vue` 当前有两个 tab（总览 + 积分记录），改造为**隐藏总览 tab，只展示积分记录**：

- 移除 `el-tabs` 组件或将其隐藏（保留代码不删除，避免后续需要恢复）
- 页面直接渲染 `pointrecord` 组件，不再展示 `totalview`
- 如果 `totalview.vue` 中有后续需要的汇总数据，可以考虑将汇总数字搬到 `pointrecord.vue` 页面顶部作为统计卡片（待确认）

**改造方式：**

```vue
<!-- overview.vue 改造前 -->
<el-tabs v-model="activeName">
  <el-tab-pane label="总览" name="totalview">
    <totalview />
  </el-tab-pane>
  <el-tab-pane label="积分记录" name="pointRecord">
    <pointrecord />
  </el-tab-pane>
</el-tabs>

<!-- overview.vue 改造后 — 直接展示积分记录，隐藏总览 -->
<pointrecord />
```

### E.2 现有表格列

`pointrecord.vue` 当前展示：

| 列 prop | 表头 | 备注 |
|---------|------|------|
| `created` | 创建时间 | 格式化为 YYYY-MM-DD HH:mm:ss |
| `username` | 昵称 | |
| `name` | 用户名 | |
| `mobile` | 手机号 | |
| income/outcome | 积分变动 | 显示 +N 或 -N |
| `point_desc` | 变动类型 | |
| `s_point` | 当前剩余积分 | |
| `point_desc` | 记录 | 和变动类型重复使用同字段 |
| `order_id` | 订单号 | |

### E.3 改造后的表格列

| 列 prop | 表头 | 新增? | 数据来源 |
|---------|------|-------|---------|
| `created` | 时间 | | 现有 |
| `activity_name` | 积分活动 | ✅ | 后端 enrich |
| `outin_type` | 类型 | | 获取/支出 |
| `journal_type_desc` | 积分来源 | ✅ 改列名 | 导入/转赠/注册/消费/初始 |
| `distributor_name` | 店铺名称 | ✅ | 后端 enrich，空则显示 `-` |
| `scope_type_desc` | 使用范围 | ✅ | 线上/线下/全场景 |
| `mobile` | 手机号 | | 现有 |
| `username` | 用户名 | | 现有（members_info.username） |
| `nickname` | 昵称 | ✅ 区分开 | 微信昵称 |
| income/outcome | 积分变动 | | +N / -N |
| `point_desc` | 变动类型描述 | | 现有 |
| `batch_remain_point` | 当前剩余积分 | ✅ | batch.remain_point |
| `expire_point` | 过期积分 | ✅ | 后端计算 |
| `batch_status_desc` | 积分状态 | ✅ | 有效/已使用/已过期 |
| `point_desc` | 记录 | | 现有 |
| `batch_expire_time` | 有效期 | ✅ | 格式化展示，0=永久 |
| `order_id` | 订单号 | | 现有 |
| `distributor_attributes` | 大区/区域/楼层/街道/类目 | ✅ | 动态属性，按列展开或合并展示 |

### E.4 筛选条件改造

**现有筛选：**
- 时间范围（date_begin / date_end）
- 手机号（mobile）
- 昵称（username）

**新增筛选：**
- **积分活动**（activity_id）— 使用积分活动选择器组件

**前端实现：**

```vue
<!-- pointrecord.vue 筛选区域新增 -->
<el-form-item label="积分活动：">
  <div v-if="activityName" class="activity-selected">
    <el-tag closable @close="handleClearActivityFilter">{{ activityName }}</el-tag>
  </div>
  <el-button v-else type="text" @click="handleSelectActivityFilter">选择活动</el-button>
</el-form-item>
```

**新增方法：**

```javascript
data() {
  return {
    params: {
      // ...现有参数
      activity_id: ''  // 新增
    },
    activityName: ''   // 活动名称回显
  }
},
methods: {
  async handleSelectActivityFilter() {
    const res = await this.$picker.pointActivity({
      data: this.params.activity_id ? [{ id: this.params.activity_id }] : [],
      multiple: false
    })
    if (res && res.length > 0) {
      this.params.activity_id = res[0].id
      this.activityName = res[0].title
      this.getList()  // 选择后立即刷新列表
    }
  },
  handleClearActivityFilter() {
    this.params.activity_id = ''
    this.activityName = ''
    this.getList()  // 清除后刷新列表
  }
}
```

> 复用 Phase 1 已创建的活动选择器组件 `src/components/sp-picker/pickers/picker-pointActivity.vue`，无需新建组件。

### E.5 导出按钮

保持现有导出逻辑不变（调 `GET /point/member/export`）。后端 Phase D 会扩展导出列，前端无需改动导出逻辑 — CSV 列由后端控制。

### E.6 店铺动态属性展示方案

店铺动态属性（大区/区域/楼层/街道/类目）数据格式：

```json
{
  "distributor_attributes": {
    "大区": "华北",
    "区域": "北京",
    "楼层": "B1",
    "街道": "朝阳路",
    "类目": "餐饮"
  }
}
```

**展示方式（二选一，待确认）：**

- **方案 1：每个属性一列** — 大区、区域、楼层、街道、类目 各占独立列。列数固定但表格较宽。
- **方案 2：合并为一列** — "店铺属性" 列内以 tag 或分行展示所有属性。节省横向空间。

> 暂定方案 1（每个属性一列），与导出 CSV 列保持一致。如果表格过宽再调整。

### E.7 前端改动文件清单

| 文件 | 变更 |
|------|------|
| `src/view/member/point/overview.vue` | 隐藏"总览"tab，只保留积分记录 |
| `src/view/member/point/pointrecord.vue` | 表格列改造：新增列、调整列顺序 |
| `src/view/member/point/totalview.vue` | 不改动（隐藏但保留代码） |
| `src/view/member/members/comps/pointList.vue` | SpFinder 配置调整（如需要） |
| `src/api/point.js` | 无需新增 API（后端返回字段扩展，前端直接读） |

### E.8 `pointrecord.vue` 改造要点

```vue
<!-- 新增列示例 -->
<el-table-column prop="activity_name" label="积分活动" min-width="120">
  <template slot-scope="scope">
    {{ scope.row.activity_name || '-' }}
  </template>
</el-table-column>

<el-table-column prop="distributor_name" label="店铺名称" min-width="120">
  <template slot-scope="scope">
    {{ scope.row.distributor_name || '-' }}
  </template>
</el-table-column>

<el-table-column prop="scope_type_desc" label="使用范围" min-width="80">
  <template slot-scope="scope">
    {{ scope.row.scope_type_desc || '-' }}
  </template>
</el-table-column>

<el-table-column prop="batch_status_desc" label="积分状态" min-width="80">
  <template slot-scope="scope">
    <el-tag :type="batchStatusTagType(scope.row.batch_status)">
      {{ scope.row.batch_status_desc }}
    </el-tag>
  </template>
</el-table-column>

<el-table-column prop="batch_expire_time" label="有效期" min-width="120">
  <template slot-scope="scope">
    {{ scope.row.batch_expire_time === 0 ? '永久' : formatDate(scope.row.batch_expire_time) }}
  </template>
</el-table-column>

<!-- 动态属性列（方案1：每个属性一列） -->
<el-table-column label="大区" min-width="80">
  <template slot-scope="scope">
    {{ (scope.row.distributor_attributes || {})['大区'] || '-' }}
  </template>
</el-table-column>
<!-- 区域、楼层、街道、类目 同上模式 -->
```

### E.9 新增辅助方法

```javascript
methods: {
  batchStatusTagType(status) {
    const map = {
      'valid': 'success',     // 有效 → 绿色
      'used': 'info',         // 已使用 → 灰色
      'expired': 'warning'    // 已过期 → 橙色
    }
    return map[status] || 'info'
  }
}
```

---

## 实施顺序

```
Phase A（模型变更）→ Phase B（写入改造）→ Phase C（查询改造）→ Phase D（导出改造）→ Phase E（后管页面）
```

- Phase A + B 可以先做，确保新数据开始落库
- Phase C + D 是后端查询/导出层改造
- Phase E（前端）依赖 Phase C 完成（需要后端返回新字段），可在 C 完成后独立会话开发
- Phase D + E 可以并行（导出是后端改动，页面是前端改动）

---

## 改造文件清单

| 模块 | 文件 | 变更类型 |
|------|------|---------|
| point/domain | `PointMemberLog.java` | 新增字段 |
| point/domain | `PointBatchConsume.java` | 新增字段 |
| point/service | `PointBatchDeductService.java` | 创建 consume 时填充已有 distributorId 参数 |
| point/service | `PointOfflineConsumeService.java` | 写 log 时填充 distributorId |
| point/service | `PointMemberAddPointService.java` | 透传 distributorId |
| point/service | `PointMemberLogEnrichService.java` | **新建** — 流水字段丰富化调度 |
| point/service | `PointMemberListService.java` | 调用 EnrichService 补充字段 |
| point/service/export | `PointMemberLogCsvExportService.java` | 扩展导出列 |
| orders/service | `OrdersPaymentDoPaymentService.java` | 透传 distributorId |
| SQL | migration | 两张表各加一个 nullable 字段（不加索引） |
| **前端** | `src/view/member/point/overview.vue` | 隐藏总览 tab，只保留积分记录 |
| **前端** | `src/view/member/point/pointrecord.vue` | 表格列改造 |
| **前端** | `src/view/member/members/comps/pointList.vue` | SpFinder 配置调整（如需要） |
| point/service | `PointMemberListService.java` | 调用 EnrichService 补充字段 |
| point/service/export | `PointMemberLogCsvExportService.java` | 扩展导出列 |
| orders/service | `OrdersPaymentDoPaymentService.java` | 透传 distributorId |
| SQL | migration | 两张表各加一个 nullable 字段（不加索引） |

---

## Review 反馈（2026-07-15 via Claude CLI）

**总体评估：MEDIUM 风险**

### 已采纳的改进

| 问题 | 处理方式 |
|------|---------|
| deduct() 签名变更是 breaking change | ✅ 命名重构先做后，deduct() 参数已是 distributorId，流水改造直接复用，无需重载方法 |
| 需要审计所有 deduct 调用方 | ✅ 命名重构时已全量审计过，流水改造无需重复 |
| log + consume 写入需同事务 | ✅ 确认现有 @Transactional 覆盖 |
| 旧数据 distributor_id 为 NULL | ✅ 明确"不回填"决策 |
| batch_status 是实时推导 | ✅ 文档标注为预期行为 |
| EnrichService 可能成 god-service | ✅ 明确只做调度，委托各领域 Service |
| 动态属性查询较重 | ✅ per-request Map 去重缓存 |
| 过期积分字段推导不明确 | ✅ 新增明确的推导规则 |
| Phase C.4 筛选条件复杂 | ✅ 标记为 deferred，不在本次实现 |
| distributor_id 索引不一定需要 | ✅ 暂不加索引 |

---

## 验证方式

1. **编译验证：** `mvn compile -pl ecshopx-point -am`
2. **写入验证：** 线下核销后检查 `point_member_log.distributor_id` 和 `point_batch_consume.distributor_id` 有值
3. **查询验证：** 调用列表接口确认新字段正确返回
4. **导出验证：** 导出 CSV 检查新增列数据完整
