# 积分活动功能 — 实现计划

## Context（背景）

当前 ecshopx 的积分系统极其简单：`point_member` 只有一个 `point` 余额字段，`point_member_log` 记录流水。没有积分过期、没有批次概念、没有活动管理、没有批量发放。

PRD 要求新增"积分活动"功能，核心定位是**积分规则模板**：
- 配置使用范围（线上/线下/全场景，圈选店铺）
- 配置有效期规则（特定时间点失效 / 特定时长后失效）
- 被各业务场景关联引用：导入积分、下单送积分、注册送积分等

用户获取到的每一笔积分：
1. 只能在活动指定范围内使用
2. 按照活动设置的失效规则到期自动失效

从积分明细 Excel 看到：积分有唯一编码（活动前缀+用户编码）、有对应金额（仅展示/对账用）、有消费/退款/结算状态管理。

**关键业务澄清：**
- 积分是**虚拟积分**，不是预付卡/代金券
- "结算"只是标记：已结算 = 不可退款，已退款 = 不可结算。不涉及和商户结算金额
- `point_value` 金额字段保留做展示/对账，不参与业务逻辑
- 结算标记在 Phase 1 就加入 `point_batch_consume` 表

---

## 设计决策记录

### D1：余额实时计算，废弃 point_member.point 同步（2026-07-09 确定）

**决定：** 用户可用积分余额改为实时聚合查询，不再依赖 `point_member.point` 字段同步。

**原因：**
- 积分过期时间可能精确到秒，定时回收任务无法做到实时准确
- 用户看到的积分必须是实时的
- 去掉双写减少一致性问题

**实现方式：**
```sql
SELECT SUM(remain_point) FROM point_batch
WHERE user_id=? AND company_id=? AND remain_point > 0
  AND (expire_time=0 OR expire_time > UNIX_TIMESTAMP())
```

**影响：**
- Phase 3 扣减前做实时可用余额校验（聚合查询）
- Phase 5/6/7 发放时不再更新 `point_member.point`
- `point_member.point` 字段保留但不再维护，旧逻辑暂不改动

### D2：过期回收延后，等到期时间粒度确认后再做（2026-07-09 确定）

**决定：** Phase 4 的过期回收定时任务（`PointBatchExpireService` + `SchedulePointBatchExpireHandler`）整体延后，等确认到期时间粒度（到小时 or 到秒）后新建独立任务处理。

**原因：**
- 余额改为实时计算后，过期批次天然被查询过滤掉，定时任务不再是余额准确性的保障
- 到期粒度尚未确定，先预留

**过期日志规则（已确定）：**
- 活动过期后，需要在 `point_member_log` 中写入一条过期日志
- 一个活动产生一条过期日志（SUM 该活动下所有过期积分）
- journal_type = 17（积分过期）

**`point_batch.status` 简化：**
- `1` = 有余额（remain_point > 0）
- `2` = 已用完（remain_point = 0）
- 不再主动标记 `status=3`（过期），改为查询时通过 `expire_time` 实时判断

### D3：Phase 4 缩小为仅退款返还（2026-07-09 确定）

**决定：** Phase 4 原"过期回收 + 退款原路返还"缩小为仅"退款原路返还"。过期相关逻辑单独拆出，待到期粒度确认后处理。

### D4：活动状态处理 — 绑定的活动被停止/过期时的行为（2026-07-09 确定）

**决定：**
- 活动已停止（`activity_status = 3`）→ 下单不送积分，跳过发放
- 活动已过期（`expire_time` 已过）→ 跳过发放，记录文件日志（warn 级别）

**原因：**
- 停止的活动不应继续产出新积分，运营停止即意味着中止发放
- 过期是异常情况（运营应及时更换活动），记日志便于排查

**实现方式：**
- `PointActivityDistributeService.grantBatch()` 保持严格模式：活动非 PROCESSING 时**抛 BadRequestException**
- Phase 5 导入场景：sink 内 try-catch 捕获异常，写入失败文件（正确行为）
- Phase 6 下单场景：`addPointForNormalOrderBonus()` 在调用 grant **之前**自行查询活动状态，非 PROCESSING 时跳过并记 warn 日志
- Phase 7 注册场景：同 Phase 6，调用前检查
- 调用方（orders 模块）无需感知此逻辑

### D5：activity_id 读取职责 — 由发放方法内部读取（2026-07-09 确定）

**决定：** `addPointForNormalOrderBonus()` 内部读取 `activity_id`，对调用方（orders 模块）透明。

**原因：**
- 下单送积分有两条路径（定时任务 + 即时发放），调用方在不同模块
- 由 point 模块内部统一处理，避免 orders 模块需要知道 activity 的存在
- 调用方签名不变，向后兼容

**实现方式：**
- `addPointForNormalOrderBonus()` 内部调用 `PointMemberRuleReadService.getPointRule(companyId)` 取 `activity_id`
- 有值 → 调用 `PointBatchGrantService.grant()`
- 无值 → 走旧逻辑

### D6：旧积分处理 — 完全作废，后续视需要迁移（2026-07-09 确定）

**决定：** 旧积分（只存在于 `point_member.point`、无 batch 记录）完全作废。如后续确认有需要保留的历史积分，再做一次 `point_batch` 初始化迁移。

**原因：**
- 余额查询改为 `SUM(point_batch.remain_point)` 后，旧积分天然不可见
- 大多数公司切换前积分已消耗或不再需要
- 避免复杂的双源聚合逻辑

**影响：**
- 切换到新模式后，用户看到的可用余额 = 仅新积分（batch 中的）
- 如有公司存量积分需保留，单独做迁移脚本写入 `point_batch`（不在本次范围内）

### D7：point_value 来源 — 下单送积分场景传 0（2026-07-09 确定）

**决定：** 下单送积分场景创建 `point_batch` 时，`point_value` 传 0。

**原因：**
- 下单送积分的积分数是动态计算的（按比例或按商品配置），每单不同
- 活动的 `point_type=2`（不定额）天然适用此场景，活动级 `point_value` 无意义
- `point_value` 仅用于展示/对账，下单场景可通过 `point_member_log` 的 `order_id` 回溯到订单金额

**实现方式：**
- `PointActivityDistributeService.buildBatchFromActivity()` 中 `point_value` 已硬编码为 0
- 所有通过 `PointBatchGrantService.grant()` 发放的积分，batch 的 `point_value` 均为 0
- 如后续有场景需要传入自定义 `point_value`（如导入定额积分），再加重载方法

### D8：total_issued/total_users 统计字段暂不维护（2026-07-09 确定）

**决定：** `point_activity` 表的 `total_issued`、`total_users` 两个统计字段暂不维护，发放时不更新。

**原因：**
- PRD 中未明确要求在活动列表展示"已发放积分数"和"已发放人数"
- 不做需求扩展，避免额外 UPDATE 开销
- 字段保留在表中不删除（删列成本高于加列），等需求明确要展示时再补上

**影响：**
- `PointActivityDistributeService.grantBatch()` 中已无更新 `total_issued`/`total_users` 的逻辑（已清理）
- 后续 Phase 5/6/7 的 `PointBatchGrantService` 发放时也不维护这两个字段
- `PointActivity` 实体保留字段定义，`point_activity` DDL 保留列定义

---

## 分阶段规划（10 个 Phase）

### Phase 1：数据模型 + 活动管理 CRUD + 积分批次表 ✅ 已完成
- 🖥️ 前端：活动列表/创建编辑/范围管理 3 个页面 + 活动选择器公共组件
- 🔀 可并行：前端页面可独立开发（API 已就位）

### Phase 2：批量发放机制（发放核心逻辑 + 范围管理） ✅ 已完成
- 🖥️ 前端：无新页面（范围管理 API 已在 Phase 1 页面中）
- 🔀 可并行：纯后端，无前端依赖

### Phase 3：批次扣减改造 + 消费范围校验 ✅ 已完成（有修改）
- 🖥️ 前端：无新页面
- 🔀 可并行：纯后端基础设施

### Phase 4：退款原路返还（按比例）← 原"过期回收+退款"，过期延后 ✅ 已完成
- 🖥️ 前端：无新页面（订单取消流程内部触发）
- 🔀 可并行：纯后端，可与 Phase 5/6/7 同时开发

### Phase 5：导入积分（关联积分活动） ✅ 已完成（后端+前端）
- 🖥️ 前端：导入积分页面（下载模板 + 上传 Excel + 导入进度） ✅ 已完成
- 🔀 可并行：与 Phase 6/7 互相独立，可同时开发

### Phase 6：下单送积分关联积分活动 ✅ 已完成（后端+前端）
- 🖥️ 前端：改造现有"积分配置"页面（加活动选择器） ✅ 已完成
- 🔀 可并行：与 Phase 5/7 互相独立，可同时开发

### Phase 7：注册送积分关联积分活动 ✅ 已完成（后端+前端）
- 🖥️ 前端：改造现有"注册赠送积分"页面（加活动选择器） ✅ 已完成
- 🔀 可并行：与 Phase 5/6 互相独立，可同时开发

### Phase 8：积分转赠
- 🖥️ 前端：无后管页面（C 端小程序操作）
- 🔀 可并行：依赖 Phase 3 扣减基础设施（已完成），可独立开发

### Phase 9：线下扫码核销积分 ✅ 已完成（后端）
- 🖥️ 前端：简 H5 核销页面（非后管，C 端）
- 🔀 可并行：依赖 Phase 3 扣减基础设施（已完成），可独立开发

### Phase 10：积分报表
- 🖥️ 前端：积分报表页面（筛选 + 列表 + 导出）
- 🔀 可并行：等其他 Phase 数据流跑通后再做，最后开发

### 待定：积分过期回收 + 过期日志（等到期时间粒度确认）
- 🖥️ 前端：无页面
- 🔀 可并行：独立任务，后续新建

---

### 并行开发建议

```
会话 1（后端主线）：Phase 4 → Phase 5 → Phase 6 → Phase 7 → Phase 8 → Phase 9 → Phase 10
会话 2（前端）：Phase 1 页面 → Phase 5 导入页面 → Phase 6/7 配置页面改造 → Phase 10 报表页面
会话 3（可选）：Phase 8 转赠 + Phase 9 核销（纯后端，与主线无冲突）
```

**可以同时开多个会话的组合：**
- Phase 5 + Phase 6 + Phase 7（都是调 GrantService 的不同入口）
- Phase 8 + Phase 9（都依赖已完成的扣减基础设施，互相独立）
- 后端任意 Phase + 对应前端页面开发

---

## Phase 1 详细设计 ✅ 已完成

### ⚠️ 修改标记

**已删除字段（活动不再自主控制发放触发）：**
- ~~`distribute_type`~~ — 发放时机由外部场景控制
- ~~`distribute_time`~~ — 同上
- ~~`trigger_type`~~ — 活动不主动触发
- ~~`trigger_condition`~~ — 同上

**已删除表：**
- ~~`point_activity_user`~~ — 不需要中间名单表，导入对齐 `EspierImportRowSink` 模式

### 1.1 数据库表（最终3张）

#### 表 1：`point_activity`（积分活动表 = 积分规则模板）

```sql
CREATE TABLE point_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL COMMENT '公司ID',
    title VARCHAR(200) NOT NULL COMMENT '活动名称（积分来源名称）',
    point_code_prefix VARCHAR(10) NOT NULL COMMENT '积分编码前缀（5位字母数字）',
    
    -- 适用范围
    scope_type TINYINT NOT NULL DEFAULT 3 COMMENT '适用范围：1=线上, 2=线下, 3=全场景',
    scope_distributor_type TINYINT NOT NULL DEFAULT 0 COMMENT '店铺圈选：0=全部, 1=仅限指定店铺, 2=排除指定店铺',
    scope_distributor_ids TEXT DEFAULT NULL COMMENT '店铺ID列表（逗号分隔）',
    
    -- 有效期
    expire_type TINYINT NOT NULL DEFAULT 0 COMMENT '失效方式：0=永不过期, 1=特定时间点失效, 2=特定时长后失效',
    expire_time INT DEFAULT NULL COMMENT '失效时间点（Unix时间戳，expire_type=1时有效）',
    expire_days INT DEFAULT NULL COMMENT '失效时长（天，expire_type=2时有效）',
    
    -- 状态
    activity_status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0=草稿, 1=进行中, 2=已完成, 3=已停止',
    
    -- 统计
    total_issued INT DEFAULT 0 COMMENT '已发放总积分',
    total_users INT DEFAULT 0 COMMENT '已发放人数',
    
    created INT NOT NULL COMMENT '创建时间',
    updated INT NOT NULL COMMENT '更新时间',
    
    INDEX idx_company_status (company_id, activity_status),
    INDEX idx_company_created (company_id, created)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分活动表';
```

#### 表 2：`point_batch`（积分批次表）

每次发放积分时写入一条批次记录。作用：
- FIFO 先过期先扣的扣减单位
- 过期回收的最小粒度
- 转赠时的流转单位
- 关联活动追溯积分来源

```sql
CREATE TABLE point_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL COMMENT '公司ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    activity_id BIGINT NOT NULL DEFAULT 0 COMMENT '关联活动ID（0=非活动发放）',
    point_code VARCHAR(50) NOT NULL COMMENT '积分唯一编码',
    
    total_point INT NOT NULL COMMENT '批次总积分',
    remain_point INT NOT NULL COMMENT '剩余可用积分',
    point_value INT NOT NULL DEFAULT 0 COMMENT '积分对应金额（分）',
    
    -- 适用范围（冗余自活动，消费时快速判断）
    scope_type TINYINT NOT NULL DEFAULT 3 COMMENT '适用范围：1=线上, 2=线下, 3=全场景',
    scope_distributor_type TINYINT NOT NULL DEFAULT 0 COMMENT '店铺圈选类型',
    scope_distributor_ids TEXT DEFAULT NULL COMMENT '适用/排除店铺ID列表',
    
    -- 有效期
    expire_time INT NOT NULL DEFAULT 0 COMMENT '过期时间（Unix时间戳，0=永不过期）',
    
    -- 状态
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=有效, 2=已用完, 3=已过期',
    
    created INT NOT NULL COMMENT '创建时间（发放时间）',
    updated INT NOT NULL COMMENT '更新时间',
    
    INDEX idx_user_company_status (user_id, company_id, status, expire_time),
    INDEX idx_activity (activity_id),
    INDEX idx_expire (company_id, status, expire_time),
    UNIQUE INDEX uk_point_code (point_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分批次表';
```

#### 表 3：`point_batch_consume`（批次消费记录）

记录每笔消费扣了哪些批次的多少积分。作用：
- 一次消费可跨多批次（FIFO），记录每个批次分别扣了多少
- 取消订单时原路返还的依据（知道退哪个batch多少积分）
- 结算标记（已结算不可退款）

```sql
CREATE TABLE point_batch_consume (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL COMMENT '关联批次ID',
    order_id VARCHAR(64) NOT NULL COMMENT '交易单号',
    
    point_consumed INT NOT NULL COMMENT '本批次扣减积分数',
    
    is_refunded TINYINT NOT NULL DEFAULT 0 COMMENT '是否已退款：0=否, 1=是',
    is_settled TINYINT NOT NULL DEFAULT 0 COMMENT '是否已结算：0=否, 1=是',
    
    created INT NOT NULL,
    
    INDEX idx_order (order_id),
    INDEX idx_batch (batch_id),
    INDEX idx_user_company (user_id, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分批次消费记录';
```

### 1.2 已实现的 Java 代码

| 层 | 文件 | 说明 |
|---|---|---|
| **domain** | `PointActivity.java` | 积分活动实体 |
| **domain** | `PointBatch.java` | 积分批次实体 |
| **domain** | `PointBatchConsume.java` | 批次消费记录实体 |
| **mapper** | `PointActivityMapper.java` | 活动 Mapper |
| **mapper** | `PointBatchMapper.java` | 批次 Mapper（含 FIFO 查询 + 范围同步） |
| **mapper** | `PointBatchConsumeMapper.java` | 消费记录 Mapper |
| **service/activity** | `PointActivityCreateService.java` | 创建/编辑活动 |
| **service/activity** | `PointActivityReadService.java` | 活动列表/详情查询 |
| **service/activity** | `PointActivityStatusService.java` | 活动状态变更（启动/停止） |
| **service/activity** | `PointCodeGenerator.java` | 积分编码生成器 |
| **api/admin/v1** | `PointActivityController.java` | 活动管理 Admin API (7个端点) |

### 1.3 Admin API

| 方法 | 路径 | 名称 |
|------|------|------|
| POST | `/api/v1/point/activity` | 创建积分活动 |
| PUT | `/api/v1/point/activity/{id}` | 编辑积分活动 |
| GET | `/api/v1/point/activity` | 积分活动列表（分页） |
| GET | `/api/v1/point/activity/{id}` | 积分活动详情 |
| PUT | `/api/v1/point/activity/{id}/start` | 启动活动 |
| PUT | `/api/v1/point/activity/{id}/stop` | 停止活动 |
| DELETE | `/api/v1/point/activity/{id}` | 删除活动（仅草稿可删） |

### 1.4 前端（ecshopx-admin）

**新增页面：** 积分活动管理

| 文件 | 说明 |
|------|------|
| `src/view/member/point/activity/list.vue` | 积分活动列表（分页、状态筛选、启动/停止/删除操作） |
| `src/view/member/point/activity/add.vue` | 创建/编辑积分活动（名称、积分编码前缀、积分类型、适用范围配置、有效期配置） |
| `src/view/member/point/activity/scope.vue` | 活动范围管理（查看/追加适用店铺、变更日志） |
| `src/api/point.js` 新增方法 | `getPointActivityList()` / `getPointActivityDetail()` / `createPointActivity()` / `updatePointActivity()` / `startPointActivity()` / `stopPointActivity()` / `deletePointActivity()` |

**路由注册：** 在 `src/router/routes/modules/point.js` 新增活动管理相关路由

**公共组件：** 新增活动选择器组件 `src/components/sp-picker/pickers/picker-pointActivity.vue`，供 Phase 5/6/7 的页面复用

---

## Phase 2 详细设计 ✅ 已完成

### ⚠️ 修改标记

**已删除（不再需要）：**
- ~~`PointActivityUserImportService`~~ — 导入积分改为 Phase 5 对齐 EspierImportRowSink
- ~~`PointActivityUserController`~~ — 不再有独立名单管理
- ~~`PointActivityDistributeJobHandler` / `SchedulePointActivityDistributeHandler`~~ — 无定时发放场景
- ~~`point_activity_user` 相关代码~~ — 表已移除

**保留的核心：**
- `PointActivityDistributeService` — 发放执行逻辑（遍历用户，创建batch）
- `PointActivityScopeService` — 范围追加 + 审计日志 + 批次同步
- `PointActivityScopeReadService` — 范围查询
- `PointCooperativeDistributorQueryService` — 店铺列表查询
- `PointActivityScopeController` — 范围管理API

### 2.1 发放核心逻辑（已实现）

发放时每个用户：
1. 生成唯一积分编码 `{prefix}{companyId}{sequence}`
2. 根据活动的 `expire_type` 计算过期时间
3. 写入 `point_batch`（继承活动的范围和有效期）
4. ~~更新 `point_member` 余额~~ — 已不再维护（D1 决策）
5. 写入 `point_member_log`（由 `PointBatchGrantService` 负责）

> ⚠️ Phase 5 变更：`PointActivityDistributeService.grantBatch()` 中的 `totalUsers += 1` 和 `totalIssued += pointAmount` 逻辑需删除，统计字段不再维护。

### 2.2 范围管理（已实现）

- 活动进行中可追加适用店铺
- 追加时同步更新该活动下所有有效 batch 的 `scope_distributor_ids`
- 写入 `point_activity_scope_log` 审计日志

---

## Phase 3 详细设计 ✅ 已完成

### ⚠️ 修改标记

**已完成部分（代码已存在）：**
- `PointBatchDeductService` — FIFO 批次扣减
- `PointBatchScopeMatcher` — 店铺范围匹配工具
- `PointScopeDistributorIdsUtil` — ID 列表解析/格式化

**设计变更（D1 余额实时计算）：**
- 扣减前增加实时可用余额校验（聚合 point_batch 未过期有余额的批次）
- 去掉对 `point_member.point` 的同步更新

### 3.1 FIFO 批次扣减逻辑（已实现）

```
扣减流程：
1. 实时校验可用余额 >= 扣减积分数（聚合查询）
2. 查询用户有效批次 WHERE remain_point > 0 AND (expire_time=0 OR expire_time > now)
   AND 店铺范围匹配
   ORDER BY expire_time ASC, id ASC  (先过期先扣)
3. 循环扣减各批次直到够数
4. 每个批次写入 point_batch_consume 记录
5. 批次 remain_point 减为 0 时标记 status=2
```

> 注意：不再同步更新 point_member.point（参见设计决策 D1）

### 3.2 商户范围校验（已实现）

消费时传入店铺 ID（distributor_id），扣减时只从匹配的批次中扣：
- `scope_distributor_type=0`（全部）→ 任何商户都可用
- `scope_distributor_type=1`（仅限指定）→ 商户必须在 `scope_distributor_ids` 中
- `scope_distributor_type=2`（排除指定）→ 商户不能在 `scope_distributor_ids` 中

### 3.3 实时可用余额查询

新增方法用于扣减前校验和用户查看可用积分：
```sql
SELECT SUM(remain_point) FROM point_batch
WHERE user_id=? AND company_id=? AND remain_point > 0
  AND (expire_time=0 OR expire_time > UNIX_TIMESTAMP())
  [AND 店铺范围过滤（可选）]
```

### 3.4 店铺类型预留说明

> **注意：** 后续店铺会拆分为"前置仓（线上）"和"线下店铺"两种类型，但该拆分不在本次任务范围内。
> 
> 当前设计保持不变：
> - `scope_type`（1=线上, 2=线下, 3=全场景）语义已正确区分
> - `scope_distributor_ids` 只存 ID，不区分店铺类型
> - 消费时由 `PointBatchScopeMatcher` 做 ID 匹配
> 
> **未来适配方式：** 店铺类型拆分后，只需改 `PointBatchScopeMatcher` 的匹配逻辑（匹配时额外检查 shop_id 对应的类型是否符合 scope_type），数据结构无需变更。

---

## Phase 4 详细设计 — 退款原路返还（按比例）

> ⚠️ 原 Phase 4 包含"过期回收 + 退款返还"，过期回收已延后（见设计决策 D2/D3）

### 4.1 退款原路返还

**业务规则：**
- 取消订单时，按比例返还该订单消费的积分
- 比例 = 退货金额 / 订单金额
- 全额退 → 全部返还
- 部分退 → 按比例计算返还积分数
- 返还的积分**恢复到原 batch**（remain_point 增加），活动关联和有效期不变
- 标记 `point_batch_consume.is_refunded = 1`
- 如果 batch 已过期（expire_time <= now），**不恢复积分**，只标记 consume.is_refunded = 1，积分作废
- 如果 `is_settled = 1` 则不允许退款

**退款入口定位：**
- 现有取消订单处理积分的入口：写 `journal_type=9` 的地方（`PointMemberAddPointService` 中的相关方法）
- 改造方式：在该入口处嵌入 `PointBatchRefundService` 调用，替换原有的直接加余额逻辑
- 需要找到具体调用链：取消订单 Service → 积分处理 → 新增 batch 退款逻辑

**过期 batch 处理规则：**
- 如果 batch 已过期（expire_time > 0 AND expire_time <= now），**不恢复积分**，只标记 `is_refunded = 1`
- 过期积分不可复活，退款时直接作废

**流程：**
```
1. 根据 order_id 查询 point_batch_consume 记录（WHERE is_refunded=0 AND is_settled=0）
2. 计算退还比例 refund_ratio = refund_amount / order_amount
3. 对每条 consume 记录：refund_point = point_consumed * refund_ratio（向下取整）
4. 标记 consume.is_refunded = 1
5. 检查关联的 batch 是否已过期（expire_time > 0 AND expire_time <= now）：
   - 如果已过期 → 跳过恢复，积分作废
   - 如果未过期 → 恢复 point_batch.remain_point += refund_point，如 status=2 则改回 1
6. 写 point_member_log（journal_type=9 取消订单返积分，income=实际恢复的积分数）
```

> 注意：不再同步更新 point_member.point（参见设计决策 D1）

### 4.2 新增文件

| 层 | 文件 | 说明 |
|---|---|---|
| **service/activity** | `PointBatchRefundService.java` | 退款原路返还 |

### 4.3 前端（ecshopx-admin）

无新增页面。退款返还由订单取消流程内部触发。

---

## Phase 5 详细设计（新）— 导入积分（关联积分活动）

### 5.1 业务流程

```
管理员下载 Excel 模板（4列：用户手机号、积分数、活动ID、活动名称）
    ↓
填写后上传，走 espier 导入链路（file_type = "point_batch_import"）
    ↓
逐行异步处理：
  1. 解析 mobile → 查找会员（失败则记入失败文件，继续下一行）
  2. 解析 activity_id → 查找活动（失败则记入失败文件）
  3. 校验活动名称：activity_name 与查到的活动标题不一致 → 该行失败
  4. 校验活动状态：必须为"进行中"（status=1），否则该行失败
  5. 解析 point_num → 校验 > 0（否则该行失败）
  6. 调用 PointBatchGrantService.grant() 创建 batch + 写 log
```

> 注意：不再更新 point_member.point 余额（参见设计决策 D1，余额走实时聚合）
> 注意：不再维护 point_activity 的 total_issued / total_users 统计字段

### 5.1.1 错误处理规则

与现有导入流程保持一致：
- 逐行处理，**单行失败不影响其他行**
- 失败原因写入失败文件（供管理员下载查看）
- 失败场景：
  - 手机号找不到对应会员 → "用户不存在"
  - activity_id 对应活动不存在 → "活动不存在"
  - activity_name 与活动实际名称不一致 → "活动名称不匹配"
  - 活动状态非进行中 → "活动未启动或已停止"
  - 积分数 ≤ 0 → "积分数无效"
- 不做防重复导入（页面层面防重复点击即可）

### 5.2 新增文件

| 层 | 文件 | 说明 |
|---|---|---|
| **service/activity** | `PointBatchEspierImportRowSink.java` | 实现 `EspierImportRowSink`，`supportedFileType()` = `"point_batch_import"` |
| **service/activity** | `PointBatchGrantService.java` | 通用发放逻辑（创建batch+写log），被导入/下单/注册共用 |

### 5.2.1 PointBatchGrantService 核心职责

该 Service 是所有"发积分关联活动"场景的统一入口：

```java
/**
 * 通用积分发放（关联活动）
 * @param userId       目标用户
 * @param companyId    公司
 * @param activityId   关联的积分活动ID
 * @param pointNum     发放积分数
 * @param journalType  日志类型（7=下单送, 1=注册送, 18=导入等）
 * @param orderId      关联单号（可选，导入场景传 null）
 */
public void grant(long userId, long companyId, long activityId, int pointNum, int journalType, String orderId)
```

内部原子操作（同一事务内）：
1. 查询 `point_activity` 获取范围 + 有效期配置
2. 生成 `point_code`（调用 `PointCodeGenerator`）
3. 计算 `expire_time`（根据 activity 的 expire_type）
4. 插入 `point_batch`（继承 activity 的 scope + expire）
5. 插入 `point_member_log`（journal_type + activity_id + batch_id）

> 注意：不再更新 point_member.point（参见设计决策 D1）
> 注意：不再更新 point_activity 的统计字段

**与已有 PointActivityDistributeService 的关系：**
- `PointBatchGrantService.grant()` 内部调用 `PointActivityDistributeService.grantBatch()` 完成 batch 创建
- `PointActivityDistributeService.grantBatch()` 中的 `totalUsers += 1` 和 `totalIssued += pointAmount` 逻辑需删除
- `PointBatchGrantService` 额外负责写 `point_member_log`（grantBatch 不写 log）

### 5.3 对齐现有导入体系

- 前端复用 `ecshopx-espier` 的上传接口（`/api/v1/espier/upload_file`），传 `file_type = "point_batch_import"`
- `EspierUploadHeaderCatalog` 新增 `point_batch_import` 的列定义
- `EspierUploadHandlerBeans` 新增对应 handler bean 注册
- 异步处理走现有 `ImportDataJobHandler` 链路，无需新写 dispatch
- `PointBatchEspierImportRowSink` 加 `@Component` 注解，自动被 `EspierImportRowSinkMapConfiguration` 发现注册

### 5.4 Excel 列定义

| 列名 | 字段 key | 必填 | 说明 |
|------|----------|------|------|
| 用户手机号 | mobile | 是 | 查找会员 |
| 积分数 | point_num | 是 | 发放的积分数量，必须 > 0 |
| 活动ID | activity_id | 是 | 关联的积分活动 |
| 活动名称 | activity_name | 是 | 用于对比校验，与 activity_id 查到的活动名称不一致则该行失败 |

### 5.5 前端（ecshopx-admin）

**新增页面：** 积分导入页面（参照 `src/view/member/members/uploade.vue` 现有会员导入模式）

| 文件 | 说明 |
|------|------|
| `src/view/member/point/pointImport.vue` | 导入积分页面：下载模板 + 上传 Excel + 查看导入进度/结果 |

**复用 espier 导入体系（无需新增 API 方法）：**
- 直接引用 `@/api/common` 中已有的四个方法：
  - `handleUploadFile({ isUploadFile: true, file_type: 'point_batch_import', file })` — 上传文件
  - `getUploadLists({ file_type: 'point_batch_import', page, pageSize })` — 获取上传记录列表
  - `exportUploadTemplate({ file_type: 'point_batch_import', file_name: '积分导入模板' })` — 下载模板
  - `exportUploadErrorFile(id, { file_type: 'point_batch_import' })` — 下载错误文件
- 不需要在 `src/api/point.js` 中新增方法

**路由注册：** 在 `src/router/routes/modules/members.js` 的 `member-benefit` 路由下新增导入页面路由（积分管理路由实际在 members.js 中，不是 point.js）

**页面入口：** 积分活动列表页（`activity/list.vue`）顶部操作栏新增"导入积分"按钮，点击跳转到导入页面

**页面功能点：**
1. 下载模板按钮 — 调用 `exportUploadTemplate`
2. 文件上传区域 — `el-upload` 组件，`auto-upload: false`，手动触发上传
3. 上传按钮 — 加 `loading` 状态防重复提交，API 返回后恢复
4. 上传记录列表 — 展示：文件名、上传时间、文件大小、处理状态(wait/processing/finish)、完成时间、成功行数、失败行数
5. 错误文件下载 — 失败行数 > 0 时展示"下载错误详情"链接
6. 分页 — 复用现有分页模式
7. 刷新 — 手动刷新（与现有导入页面保持一致，无需自动轮询）

**注意：** 页面不需要活动选择器。活动在 Excel 每行中按 `activity_id` + `activity_name` 指定，由后端逐行校验。

**菜单/权限注册：** 需在后端菜单系统中注册新页面入口，确保管理员可见

---

## Phase 6 详细设计 ✅ 已完成（后端）— 下单送积分关联积分活动

### 6.1 业务流程

改造"会员权益-积分规则"中的下单送积分逻辑，使每次下单送出的积分关联到一个积分活动。

```
订单完成 → 触发下单送积分（定时任务 or 即时）
    ↓
addPointForNormalOrderBonus() 内部读取 rule 配置中的 activity_id（参见决策 D5）
    ↓
activity_id 有值？
    ├── 是 → 检查活动状态（参见决策 D4）
    │       ├── PROCESSING → 调用 PointBatchGrantService.grant() 创建 batch
    │       ├── 已停止 → 跳过发放（不送积分）
    │       └── 已过期 → 跳过发放，记 warn 日志
    └── 否 → 走旧逻辑（加 point_member.point 余额 + 写 log）
```

### 6.2 现有下单送积分的两条路径

| 路径 | 触发时机 | 积分计算方 | 调用入口 |
|------|---------|-----------|---------|
| **定时任务（主）** | 订单完成 `gain_time` 天后 | `PointMemberService.scheduleSendMemberPoint()` 读 rule，按 `gain_point × 订单金额` 或按商品配置计算 | point 模块 |
| **即时发放（次）** | 确认收货时 | 读 order 上预存的 `bonus_points` 列 | orders 模块 `FinishOrderBatchService` / `NormalOrderConfirmReceiptAfterCommitService` |

**两条路径最终都调用 `addPointForNormalOrderBonus()`**，改造这一个方法即可覆盖全部场景。

### 6.3 改造点

| 改造对象 | 改动内容 |
|----------|---------|
| `PointMemberAddPointService.addPointForNormalOrderBonus()` | 方法内部读取 `activity_id`，有值时先检查活动状态（D4），再调 `PointBatchGrantService.grant()`，无值走旧逻辑 |
| `PointMemberRuleSaveService.saveFromAdminRequest()` | 显式处理 `activity_id`：类型校验为 Long，允许 null（清空关联） |
| `PointMemberRuleReadService.getPointRule()` | 默认配置加 `"activity_id": null`，确保 fallback |
| `PointMemberAddPointService` | 新增依赖注入：`PointBatchGrantService`、`PointActivityMapper`（用于活动状态前置检查） |

### 6.4 关联方式

**一对一：** 一个下单送积分规则绑定一个积分活动。在 Redis hash `memeberpoint:rule:{companyId}` 中加 `activity_id` 字段，关联 `point_activity.id`。

活动要求：
- 关联的活动 `point_type` 应为 `2`（不定额），因为下单送积分数量由规则动态计算
- 前端选择器可过滤只显示 `point_type=2` 的活动

### 6.5 旧数据兼容

- `activity_id` 为空/null 时，保持旧行为（只加 `point_member.point` 余额 + 写 log，不创建 batch）
- 避免存量公司因没有配置 activity_id 而报错
- `point_batch.point_value` 传 0（参见决策 D7，下单场景不需要固定金额）
- 前端配置页面提示：关联积分活动后，下单送出的积分将受活动的范围和有效期约束

### 6.6 `addPointForNormalOrderBonus()` 改造伪代码

```java
public void addPointForNormalOrderBonus(long userId, long companyId, int point, long orderId) {
    // 读取 activity_id（决策 D5：发放方法内部读取）
    Map<String, Object> rule = pointMemberRuleReadService.getPointRule(companyId);
    Long activityId = parseLongOrNull(rule.get("activity_id"));
    
    if (activityId != null && activityId > 0) {
        // 决策 D4：调用 grant 之前自行检查活动状态
        PointActivity activity = pointActivityMapper.selectById(activityId, companyId);
        if (activity == null || activity.getActivityStatus() != PointActivityStatus.PROCESSING) {
            log.warn("下单送积分跳过：activity {} 非进行中状态, userId={}, orderId={}", activityId, userId, orderId);
            return;  // 静默跳过，不影响订单流程
        }
        // 新逻辑：创建 batch + 写 log（GrantService 内部不再重复检查状态）
        pointBatchGrantService.grant(
            userId, companyId, activityId, point,
            PointMemberJournalTypeDescriptions.JOURNAL_TYPE_ORDER_BONUS,  // journal_type=7
            String.valueOf(orderId)
        );
    } else {
        // 旧逻辑：直接加余额（兼容未配置活动的存量公司）
        pointMemberMapper.addPointDelta(userId, companyId, point);
        // 写 point_member_log ... (现有逻辑保持不变)
    }
}
```

> **注意：** `PointActivityDistributeService.grantBatch()` 内部仍会校验活动状态（抛 BadRequestException），
> 这里提前检查是为了在下单场景中**静默跳过**而不是抛异常中断流程。
> Phase 5 导入场景不需要提前检查，由 sink 的 try-catch 捕获异常写入失败文件即可。

### 6.7 前端（ecshopx-admin）

**改造页面：** `src/view/member/point/index.vue`（现有"积分配置"页面）

| 改动 | 说明 |
|------|------|
| 新增"关联积分活动"表单项 | 在积分规则表单中加一个活动选择器（过滤 `point_type=2` 的活动） |
| API 调用 | `savePointRule()` 提交时带上 `activity_id` 字段 |
| 提示文案 | "关联积分活动后，下单送出的积分将受活动的范围和有效期约束" |

**对应后端 API：** `PUT /api/v1/member/point/rule` — 增加接收 `activity_id` 参数（Long，可选）

---

## Phase 7 详细设计 ✅ 已完成（后端+前端）— 注册送积分关联积分活动

### 7.1 业务流程

改造"会员营销-新客营销"中的注册送积分逻辑，使注册送出的积分关联到一个积分活动。

```
用户注册事件触发
    ↓
CreateMemberSuccessRegisterPointExecutionService.apply() 读取注册送积分配置
    ↓
配置中有 activity_id？
    ├── 是 → 检查活动状态
    │       ├── PROCESSING →
    │       │     1. point > 0 → 调 PointBatchGrantService.grant() 给注册用户本人发积分
    │       │     2. rebate > 0 且有上级 → 调 PointBatchGrantService.grant() 给推荐人发返利积分
    │       └── 非 PROCESSING（停止/完成/草稿/不存在）→ 跳过全部发放，记 warn 日志，return
    └── 否 → 走旧逻辑（加 point_member.point 余额 + 写 log，不创建 batch）
```

**关键决策：**
- 积分数量**始终以注册配置的 `point` / `rebate` 字段为准**，活动仅作为"范围+有效期"模板
- **一个 `activity_id` 同时适用于注册赠送（本人）和返上级积分（推荐人）**
- 活动非进行中时**直接跳过不发积分**（本人和推荐人都不发），不降级走旧逻辑，记 warn 日志即可
- 不考虑第三方对接（DmCRM/Shuyun 等），`apply()` 层直接做分支

### 7.2 改造点

| 改造对象 | 改动内容 |
|----------|---------|
| `PointMemberRuleSaveService.normalizeActivityId()` | 可见性从 `private static` 改为 `public static`，供跨模块复用 |
| `RegisterPointConfigService.saveRegisterPointConfig()` | 新增处理 `activity_id`：调用 `PointMemberRuleSaveService.normalizeActivityId(data)` 做类型校验，允许 null（清空关联） |
| `RegisterPointConfigService.getRegisterPointConfig()` | 默认配置加 `"activity_id": null`，确保 fallback |
| `CreateMemberSuccessRegisterPointExecutionService.apply()` | 从 config 取 `activity_id`，有值时检查活动状态 → 调 `PointBatchGrantService.grant()`，无值走旧逻辑 |
| `CreateMemberSuccessRegisterPointExecutionService` | 新增依赖注入：`PointBatchGrantService`、`PointActivityMapper`（用于活动状态前置检查） |

### 7.3 `apply()` 改造伪代码

```java
public void apply(Map<String, Object> payload) {
    long companyId = MapUtils.getLongValue(payload, "company_id");
    long userId = MapUtils.getLongValue(payload, "user_id");

    Map<String, Object> config = registerPointConfigService.getRegisterPointConfig(companyId, "point");
    
    boolean isOpen = BooleanUtils.toBoolean(MapUtils.getString(config, "is_open"));
    if (!isOpen) return;
    
    int point = MapUtils.getIntValue(config, "point", 0);
    int rebate = MapUtils.getIntValue(config, "rebate", 0);

    // 读取 activity_id（本人赠送 + 返上级积分 共用同一个活动）
    Long activityId = parseLongOrNull(config.get("activity_id"));
    
    if (activityId != null && activityId > 0) {
        // 新逻辑：检查活动状态，创建 batch
        PointActivity activity = pointActivityMapper.selectById(activityId, companyId);
        if (activity == null || activity.getActivityStatus() != PointActivityStatus.PROCESSING) {
            log.warn("注册送积分跳过：activity {} 非进行中状态, userId={}, companyId={}", activityId, userId, companyId);
            return;  // 本人和推荐人都不发积分，不降级
        }
        // 1. 注册赠送积分（给本人）
        if (point > 0) {
            pointBatchGrantService.grant(
                userId, companyId, activityId, point,
                1,  // journal_type=1 注册赠送
                null  // 无关联订单
            );
        }
        // 2. 返上级积分（给推荐人）
        if (rebate > 0) {
            Long parentUserId = getParentUserId(userId, companyId);  // 查找推荐人
            if (parentUserId != null && parentUserId > 0) {
                pointBatchGrantService.grant(
                    parentUserId, companyId, activityId, rebate,
                    1,  // journal_type=1 注册赠送（返上级）
                    null  // 无关联订单
                );
            }
        }
    } else {
        // 旧逻辑：兼容未配置活动的存量公司
        if (point > 0) {
            pointMemberAddPointService.addPointForMemberRegisterGift(userId, companyId, point);
        }
        if (rebate > 0) {
            // 旧逻辑中的返上级积分处理（保持不变）
            Long parentUserId = getParentUserId(userId, companyId);
            if (parentUserId != null && parentUserId > 0) {
                pointMemberAddPointService.addPointForMemberRegisterGift(parentUserId, companyId, rebate);
            }
        }
    }
}
```

> **注意：** `PointActivityDistributeService.grantBatch()` 内部也会校验活动状态（抛 BadRequestException），
> 这里提前检查是为了在注册场景中**静默跳过**而不是抛异常中断流程。
> 
> **`getParentUserId()` 需确认：** 当前注册送积分流程中查找推荐人的现有方法名/位置待开发时定位，伪代码中用 `getParentUserId` 代指。

### 7.4 旧数据兼容

- `activity_id` 为空/null 时，保持旧行为（只加 `point_member.point` 余额 + 写 log，不创建 batch）
- 避免存量公司因没有配置 activity_id 而报错
- `point_batch.point_value` 传 0（注册场景不需要固定金额对应关系）

### 7.5 前端（ecshopx-admin）

**改造页面：** `src/view/member/promotions/point.vue`（现有"注册赠送积分"配置页面）

**参照模式：** Phase 6 已完成的 `src/view/member/point/index.vue` 活动关联实现（同样的 picker 调用 + 回显 + 清除模式）

**具体改动：**

#### 7.5.1 新增 import

```js
import { getPointActivityDetail } from '../../../api/point'
```

#### 7.5.2 data() 扩展

```js
data() {
  return {
    form: {
      is_open: 'false',
      point: 0,
      rebate: 0,
      type: 'point',
      activity_id: null       // 新增：关联积分活动ID
    },
    activityName: ''          // 新增：活动名称（用于回显）
  }
}
```

#### 7.5.3 template 新增表单项

在"注册赠送积分"表单项之后、保存按钮之前，新增活动选择区域：

```html
<el-form-item v-if="form.is_open == 'true'" label="关联积分活动：">
  <div v-if="activityName" class="activity-selected">
    <el-tag closable @close="handleClearActivity">{{ activityName }}</el-tag>
    <el-button type="text" size="small" style="margin-left: 10px" @click="handleSelectActivity">更换</el-button>
  </div>
  <el-button v-else type="primary" size="small" @click="handleSelectActivity">选择活动</el-button>
  <div class="frm-tips">关联积分活动后，注册送出的积分将受活动的范围和有效期约束</div>
</el-form-item>
```

> 注意：仅在 `is_open == 'true'` 时显示，关闭注册送积分时无需展示活动选择器

#### 7.5.4 methods 新增 3 个方法

```js
async loadActivityName() {
  const activityId = this.form.activity_id
  if (activityId) {
    try {
      const res = await getPointActivityDetail(activityId)
      this.activityName = res.data.data.title || ''
    } catch (e) {
      this.activityName = ''
      console.error('加载关联活动失败', e)
    }
  } else {
    this.activityName = ''
  }
},
async handleSelectActivity() {
  const res = await this.$picker.pointActivity({
    data: this.form.activity_id ? [{ id: this.form.activity_id }] : [],
    multiple: false,
    params: { point_type: 2 }
  })
  if (res && res.length > 0) {
    const selected = res[0]
    this.form.activity_id = selected.id
    this.activityName = selected.title
  }
},
handleClearActivity() {
  this.form.activity_id = null
  this.activityName = ''
}
```

> picker 过滤 `point_type: 2`（不定额），因为注册积分数由配置的 `point` 字段决定，活动仅作为范围+有效期模板

#### 7.5.5 getRegisterData() 改造

```js
getRegisterData() {
  var params = { type: 'point' }
  getRegisterPoint(params).then((response) => {
    const data = response.data.data
    this.form = {
      ...this.form,
      ...data,
      activity_id: data.activity_id || null  // 确保 null fallback
    }
    this.loadActivityName()  // 新增：回显活动名称
  })
}
```

#### 7.5.6 save() 无需额外改动

`saveRegisterPoint(this.form)` 已经会把 `form` 中的 `activity_id` 一并提交，无需单独处理。

#### 7.5.7 样式（可选）

如需与 Phase 6 保持一致，追加：

```scss
.activity-selected {
  display: inline-flex;
  align-items: center;
}
```

**对应后端 API：** `POST /api/v1/promotions/point` — 增加接收 `activity_id` 参数（Long，可选）

**业务确认：** 一个配置只选一个 `activity_id`，注册赠送（本人）和返上级积分（推荐人）共用同一个活动。前端无需第二个选择器。

---

## Phase 8 详细设计（新）— 积分转赠

### 8.1 业务规则

- 用户 A 可以将积分转赠给用户 B
- 转赠后**活动关联不变**（新 batch 继承原 batch 的 activity_id）
- 转赠后**有效期不变**（新 batch 继承原 batch 的 expire_time）
- 转赠后**使用范围不变**（新 batch 继承原 batch 的 scope 配置）
- 需要生成一对明细：A 的扣减记录 + B 的增加记录

### 8.2 流程

```
用户A发起转赠（target_user_id, point_amount）
    ↓
1. 实时校验 A 可用余额 >= point_amount
2. FIFO 扣减 A 的 batch（先过期先扣，但不写 consume 记录，写专门的转赠扣减）
   - 每个被扣的 batch：remain_point 减少
   - 对应创建 B 的新 batch：
     activity_id = 原batch.activity_id
     expire_time = 原batch.expire_time
     scope_type/scope_distributor_type/scope_distributor_ids = 原batch 的值
     total_point = remain_point = 本次从该batch转出的积分数
3. 写 A 的 point_member_log（journal_type=19 转赠转出，outcome=积分数）
4. 写 B 的 point_member_log（journal_type=20 转赠转入，income=积分数）
```

> 注意：不再同步更新 point_member.point（参见设计决策 D1）

### 8.3 新增文件

| 层 | 文件 | 说明 |
|---|---|---|
| **service/activity** | `PointTransferService.java` | 转赠核心逻辑 |
| **api/app/v1** | `PointTransferController.java` | C端转赠API |

### 8.4 API

| 方法 | 路径 | 名称 |
|------|------|------|
| POST | `/api/v1/point/transfer` | 发起转赠 |
| GET | `/api/v1/point/transfer/logs` | 转赠记录（列表） |

### 8.5 前端（ecshopx-admin）

**无后管页面。** 转赠是 C 端用户操作，在小程序端完成。后管可在积分报表中查看转赠记录。

---

## Phase 9 详细设计（新）— 线下扫码核销积分

### 9.1 业务流程

```
1. 用户扫线下店铺小程序码 → 进入核销页面（H5/小程序）
2. 页面调接口：传 distributor_id → 返回用户在当前店铺可用积分总额
3. 用户填写消费积分数，点确认
4. 调扣减接口 → FIFO 扣减匹配该商户的 batch（仅 scope_type=2线下 或 3全场景）
5. 生成积分消费记录（point_batch_consume）
6. 按活动分组写 point_member_log（一个活动一条日志）
```

### 9.2 接口设计

Controller 路径遵循现有 C 端 front 模式：
- 目录：`api/front/v1/`
- 注解：`@FrontAuth`（需要 H5 JWT 登录）
- 基础路径：`/api/v1/h5app`

| 方法 | 路径 | 名称 |
|------|------|------|
| GET | `/api/v1/h5app/wxapp/point/offline/available?distributor_id=X` | 查询当前店铺可用积分总额 |
| POST | `/api/v1/h5app/wxapp/point/offline/consume` | 线下核销扣减积分 |

**用户身份获取：**
- `companyId` ← `request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID)`
- `userId` ← JWT claims 中的 `user_id`

**查询可用积分：**
```sql
SELECT SUM(remain_point) FROM point_batch
WHERE user_id = ? AND company_id = ? AND remain_point > 0
  AND (expire_time = 0 OR expire_time > UNIX_TIMESTAMP())
  AND scope_type IN (2, 3)  -- 仅线下 + 全场景
  AND scope 匹配 distributor_id（复用 PointBatchScopeMatcher 逻辑）
```

**核销扣减请求体：**
```json
{
  "distributor_id": 123,
  "point_amount": 100
}
```

**核销扣减响应：**
```json
{
  "writeoff_no": "HX20260709000001",
  "point_deducted": 100,
  "distributor_id": 123
}
```

### 9.3 distributor_id 校验

前端传的 `distributor_id` 来自 `/api/v1/distributors` 接口（即店铺）。后端需校验：
- 查 distributors 表确认该 distributor_id 存在且属于当前 company_id
- 校验失败抛 BadRequestException

### 9.4 scope_type 线上/线下过滤

`PointBatchScopeMatcher` 现有逻辑不区分线上/线下，只做店铺 ID 白名单/黑名单匹配。

**线下核销需要额外过滤：** 只扣 `scope_type = 2（线下）` 或 `scope_type = 3（全场景）` 的批次。`scope_type = 1（仅线上）` 的批次不参与线下核销。

实现方式：扩展 `PointBatchDeductService`，增加 `allowedScopeTypes` 参数：

```java
public Map<String, Object> deduct(
    long companyId, long userId, int pointToDeduct,
    long distributorId, String orderId, List<Integer> allowedScopeTypes)
```

- 线下核销传 `[2, 3]`
- 现有线上消费调用不传（默认不过滤，向后兼容）
- 避免在 PointOfflineConsumeService 中重复实现 FIFO 逻辑

### 9.5 核销单号生成

新建 `PointWriteoffNoGenerator`，参照 `PointCodeGenerator` 模式：

```java
@Service
public class PointWriteoffNoGenerator {
    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis() % 1_000_000L);

    public String nextNo(long companyId) {
        long seq = sequence.incrementAndGet();
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        return "HX" + date + companyId + seq;
    }
}
```

格式：`HX{yyyyMMdd}{companyId}{sequence}`，保证同进程内唯一。

### 9.5 point_member_log 写入规则

一次核销可能 FIFO 跨多个 batch（来自不同活动）。按 activity_id 分组，**每个活动写一条日志**：

```
扣减完成后：
1. 将 consume_records 按 batch.activity_id 分组
2. 每组 SUM(point_consumed) 作为该活动的核销积分数
3. 每组写一条 point_member_log：
   - journal_type = 21（线下核销）
   - outcome = 该活动下本次核销的积分总数
   - activity_id = 分组的 activity_id
   - batch_id = null（跨 batch，不记单个）
   - order_id = 核销单号
```

### 9.6 防重复提交

- **前端：** 按钮防抖（C 端负责）
- **后端：** 短时间内（10 秒）同一用户 + 同一店铺 + 相同积分数的请求，返回提醒
- 不做硬拦截：前端带 `confirm=true` 参数时放行执行
- 实现方式：Redis key `point:writeoff:dup:{userId}:{distributorId}:{amount}`，TTL 10秒

**响应协议：**
- 检测到疑似重复时，HTTP 200，业务响应：
```json
{
  "code": "NEED_CONFIRM",
  "message": "您刚刚已核销相同积分，是否确认再次核销？"
}
```
- 前端收到 `code=NEED_CONFIRM` 后弹确认框，用户确认后带 `confirm=true` 重新发请求
- 正常成功/失败走标准响应格式

### 9.7 新增文件

| 层 | 文件 | 说明 |
|---|---|---|
| **service/activity** | `PointOfflineConsumeService.java` | 线下核销：scope_type 过滤 + 扣减 + 按活动写 log |
| **service/activity** | `PointWriteoffNoGenerator.java` | 核销单号生成器 |
| **api/front/v1** | `PointOfflineConsumeController.java` | C端核销 API（@FrontAuth） |

### 9.8 PointOfflineConsumeService 核心流程

```java
@Transactional(rollbackFor = Exception.class)
public Map<String, Object> consume(long companyId, long userId, long distributorId, int pointAmount) {
    // 1. 校验 distributor_id 属于该 company（查 distributors 表）

    // 2. 生成核销单号
    String writeoffNo = pointWriteoffNoGenerator.nextNo(companyId);

    // 3. 调用扩展后的 DeductService（传 allowedScopeTypes=[2,3]）
    Map<String, Object> result = pointBatchDeductService.deduct(
        companyId, userId, pointAmount, distributorId, writeoffNo, Arrays.asList(2, 3));

    // 4. 从 result 取 consume_records，按 batch.activity_id 分组
    //    每组写一条 point_member_log (journal_type=21, outcome=该活动核销总积分)

    // 5. 返回核销结果 {writeoff_no, point_deducted, merchant_id}
}

public int getOfflineAvailablePoint(long companyId, long userId, long distributorId) {
    // 调用扩展后的 DeductService.getAvailablePoint(companyId, userId, distributorId, [2, 3])
}
```

### 9.9 前端

**无后管页面。** 线下核销是 C 端小程序操作。后管可在积分报表中查看核销记录。

---

## Phase 10 详细设计（新）— 积分报表

### 10.1 报表字段

按 `docs/prd/积分明细字段.xlsx` 定义，导出数据包含积分批次 + 消费明细。

### 10.2 数据来源

- 积分批次明细：`point_batch` JOIN `point_activity`
- 积分消费明细：`point_batch_consume` JOIN `point_batch`
- 用户信息：`point_member` / `members`

### 10.3 新增文件

| 层 | 文件 | 说明 |
|---|---|---|
| **service/activity** | `PointReportExportService.java` | 报表数据组装 + Excel 导出 |
| **api/admin/v1** | `PointReportController.java` | 报表导出 Admin API |

### 10.4 API

| 方法 | 路径 | 名称 |
|------|------|------|
| GET | `/api/v1/point/report/batch` | 积分批次明细导出 |
| GET | `/api/v1/point/report/consume` | 积分消费明细导出 |

### 10.5 前端（ecshopx-admin）

**新增页面：** 积分报表页面

| 文件 | 说明 |
|------|------|
| `src/view/member/point/pointReport.vue` | 积分报表：筛选条件（活动/用户/时间） + 列表展示 + Excel 导出 |
| `src/api/point.js` 新增方法 | `getPointBatchReport()` / `getPointConsumeReport()` / `exportPointReport()` |

**路由注册：** 在 `src/router/routes/modules/point.js` 新增报表页面路由

---

## 依赖关系

```
Phase 1-3 (已完成) ← 基础设施
    ↓
Phase 4 (退款返还) ← Phase 5/6/7/8 可并行
    ↓
Phase 5 (导入积分) ← 最简单的"发积分"场景
    ↓
Phase 6 (下单送积分) ← 核心业务链路
    ↓
Phase 7 (注册送积分) ← 同 Phase 6 模式
    ↓
Phase 8 (积分转赠) ← 依赖 batch 基础设施
    ↓
Phase 9 (线下核销) ← 依赖 FIFO 扣减
    ↓
Phase 10 (报表) ← 所有数据流跑通后做
    ↓
待定 (过期回收+过期日志) ← 等到期粒度确认
```

Phase 5/6/7 相对独立（都是调用 PointBatchGrantService），可并行开发。
Phase 8 和 Phase 9 相对独立，可以调换顺序或并行开发。

---

## point_member_log.journal_type 扩展

| 值 | 说明 | 新增? |
|----|------|-------|
| 1-16 | 现有类型保持不变 | |
| 17 | 积分过期（待定Phase，一个活动一条过期日志） | 待定 |
| 18 | 活动导入积分 | Phase 5 新增 |
| 19 | 积分转赠-转出 | Phase 8 新增 |
| 20 | 积分转赠-转入 | Phase 8 新增 |
| 21 | 线下核销 | Phase 9 新增 |

> 注：取消订单返积分保持现有 journal_type=9

## point_member_log 表结构变更

**新增字段（Phase 4 开始前执行）：**

```sql
ALTER TABLE point_member_log ADD COLUMN activity_id BIGINT DEFAULT NULL COMMENT '关联积分活动ID';
ALTER TABLE point_member_log ADD COLUMN batch_id BIGINT DEFAULT NULL COMMENT '关联积分批次ID';
ALTER TABLE point_member_log ADD INDEX idx_activity (activity_id);
ALTER TABLE point_member_log ADD INDEX idx_batch (batch_id);
```

**旧数据兼容：** 字段允许 NULL，存量数据不受影响。

**使用规则：**
- 所有通过 `PointBatchGrantService` 发放的积分：同时写入 `activity_id` 和 `batch_id`
- 转赠：转出/转入双方的 log 都记录 `batch_id`（各自的 batch）和 `activity_id`
- 过期：记录被过期的 `batch_id` 和 `activity_id`
- 退款：记录退回的 `batch_id` 和 `activity_id`
- 旧逻辑（无活动关联）：两个字段保持 NULL

**代码改动：**
- `PointMemberLog.java` 新增 `activityId` 和 `batchId` 字段
- `PointBatchGrantService` 写 log 时填充这两个字段
- `PointBatchRefundService` 写 log 时填充
- `PointTransferService` 写 log 时填充
- 报表查询直接从 `point_member_log` 获取活动/批次信息，无需 JOIN
- ~~`PointBatchExpireService` 写 log 时填充~~ ← 延后到过期回收任务

---

## ⚠️ 需要代码修改的已完成 Phase — ✅ 已处理

以下清理工作已在 Phase 2 阶段完成：

1. ✅ **`PointActivity.java`** — 已删除 `distributeType`, `distributeTime`, `triggerType`, `triggerCondition` 字段
2. ✅ **`PointActivityCreateService.java`** — 已删除对上述字段的赋值和返回逻辑
3. ~~**`PointActivityReadService.java`**~~ — 不涉及（ReadService 使用 CreateService 的 toResponseMap）
4. ✅ **`PointActivityUser.java`** — 已删除整个文件
5. ✅ **`PointActivityUserMapper.java`** — 已删除整个文件
6. ✅ **`PointActivityDistributeService.java`** — 已重构为 `grantBatch(userId, companyId, activityId, pointAmount)` 方法
7. ✅ **SQL** — 已更新 `docker/point_activity.sql`：删除废弃字段 + 删除 point_activity_user 表

---

## 关键参照文件

| 用途 | 参照文件 |
|------|----------|
| 实体类模式 | `ecshopx-point/.../domain/PointMemberLog.java` |
| Mapper 模式 | `ecshopx-point/.../mapper/PointMemberMapper.java` |
| Controller 模式 | `ecshopx-point/.../api/admin/v1/PointMemberController.java` |
| 导入模式 | `ecshopx-common/.../espier/upload/EspierImportRowSink.java` |
| 导入 Handler 注册 | `ecshopx-espier/.../service/upload/EspierUploadHandlerBeans.java` |
| 导入列定义 | `ecshopx-espier/.../service/upload/EspierUploadHeaderCatalog.java` |
| 异步 Job 模式 | `ecshopx-point/.../dispatch/SendMemberPointJobHandler.java` |
| XxlJob 模式 | `ecshopx-point/.../cron/ScheduleSendMemberPointHandler.java` |
| 店铺范围校验 | `ecshopx-orders/.../domain/OrderPromotionsExtraPointActivity.java` (distributorIds) |

---

## 验证方式

1. **编译验证**：每个 Phase 完成后 `mvn compile -pl ecshopx-point -am`
2. **单元测试**：各 Service 关键逻辑（已有6个测试类，每个新 Phase 补充）
3. **API 测试**：Postman 测试新增接口
4. **集成验证**：
   - Phase 4：取消订单 → 验证 batch 积分恢复 + consume.is_refunded 标记
   - Phase 5：上传 Excel → 验证 batch 创建 + log 写入
   - Phase 6：下单完成 → 验证 batch 创建 + 关联活动
   - Phase 7：注册 → 验证 batch 创建 + 关联活动
   - Phase 8：转赠 → 验证 A 扣减 B 增加 + 有效期/范围继承
   - Phase 9：扫码 → 查看可用积分 → 核销 → 验证 consume 记录
   - Phase 10：导出 Excel → 核对字段完整性
