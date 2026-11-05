# Scope 命名统一重构计划

## 背景

积分模块中 `scope_shop_ids` 实际存储的是 `distributor_id`（店铺表 `distribution_distributor` 的主键），但字段名和相关 Service 命名使用了 `shop` / `merchant` 等混淆概念。需要统一为 `distributor` 命名。

> **独立任务：** 本计划不包含业务逻辑变更，纯重命名重构。在另一个会话中处理。
> **执行顺序：本计划先于积分流水改造（POINT_LOG_FIELDS_PLAN.md）执行。** 命名统一后，流水改造可以直接复用已重命名的 `distributorId` 参数，不需要额外的重载方法。

---

## 重命名清单

### 1. 数据库字段

| 表 | 旧字段名 | 新字段名 |
|----|---------|---------|
| `point_activity` | `scope_shop_type` | `scope_distributor_type` |
| `point_activity` | `scope_shop_ids` | `scope_distributor_ids` |
| `point_batch` | `scope_shop_type` | `scope_distributor_type` |
| `point_batch` | `scope_shop_ids` | `scope_distributor_ids` |
| `point_activity_scope_log` | `merchant_ids` | `distributor_ids` |
| `point_activity_scope_log` | `before_scope_shop_ids` | `before_scope_distributor_ids` |
| `point_activity_scope_log` | `after_scope_shop_ids` | `after_scope_distributor_ids` |

**DDL 方式：** 使用 MySQL 8.0+ 的 `RENAME COLUMN`（instant DDL，不锁表）：

```sql
ALTER TABLE point_activity RENAME COLUMN scope_shop_type TO scope_distributor_type;
ALTER TABLE point_activity RENAME COLUMN scope_shop_ids TO scope_distributor_ids;
ALTER TABLE point_batch RENAME COLUMN scope_shop_type TO scope_distributor_type;
ALTER TABLE point_batch RENAME COLUMN scope_shop_ids TO scope_distributor_ids;
ALTER TABLE point_activity_scope_log RENAME COLUMN merchant_ids TO distributor_ids;
ALTER TABLE point_activity_scope_log RENAME COLUMN before_scope_shop_ids TO before_scope_distributor_ids;
ALTER TABLE point_activity_scope_log RENAME COLUMN after_scope_shop_ids TO after_scope_distributor_ids;
```

> ⚠️ `RENAME COLUMN` 是 metadata-only 操作，不重建表，不锁写。比 `CHANGE COLUMN` 安全。

### 2. Java 实体字段

| 文件 | 旧字段 | 新字段 |
|------|--------|--------|
| `PointActivity.java` | `scopeShopType` | `scopeDistributorType` |
| `PointActivity.java` | `scopeShopIds` | `scopeDistributorIds` |
| `PointBatch.java` | `scopeShopType` | `scopeDistributorType` |
| `PointBatch.java` | `scopeShopIds` | `scopeDistributorIds` |
| `PointActivityScopeLog.java` | `merchantIds` | `distributorIds` |
| `PointActivityScopeLog.java` | `beforeScopeShopIds` | `beforeScopeDistributorIds` |
| `PointActivityScopeLog.java` | `afterScopeShopIds` | `afterScopeDistributorIds` |

> 同步更新 `@MpField` 注解中的 comment，"商户" → "店铺"

### 3. Service / 工具类重命名

| 旧名 | 新名 | 说明 |
|------|------|------|
| `PointCooperativeMerchantQueryService` | `PointCooperativeDistributorQueryService` | 查询合作店铺列表 |
| `PointScopeShopIdsUtil` | `PointScopeDistributorIdsUtil` | ID 列表解析/合并工具 |

### 4. Service 方法/变量重命名

| 文件 | 旧名 | 新名 |
|------|------|------|
| `PointActivityScopeService` | `appendMerchants()` | `appendDistributors()` |
| `PointActivityScopeService` | `validateMerchantsExist()` | `validateDistributorsExist()` |
| `PointActivityScopeReadService` | `getScopeMerchants()` | `getScopeDistributors()` |
| `PointBatchScopeMatcher` | `merchantMatches()` | `distributorMatches()` |
| `PointBatchDeductService` | `merchantId` 参数 | `distributorId` 参数 |
| `PointOfflineConsumeService` | `merchantId` 参数 | `distributorId` 参数 |
| `PointBatchMapper` | `syncScopeShopIdsByActivity()` | `syncScopeDistributorIdsByActivity()` |
| `PointBatchMapper` | `@Param("scopeShopIds")` | `@Param("scopeDistributorIds")` |
| `PointActivityScopeController` | `extractMerchantIds()` | `extractDistributorIds()` |

> ⚠️ `PointBatchMapper.syncScopeShopIdsByActivity()` 内有**原生 SQL**（`UPDATE point_batch SET scope_shop_ids = ...`），必须同步修改 SQL 字符串。编译器不会检测 SQL 中的列名错误。

### 5. Controller / API 路径 + routeAlias

| 旧路径 | 新路径 | 说明 |
|--------|--------|------|
| `PUT /api/v1/point/activity/{id}/scope/merchants` | `PUT /api/v1/point/activity/{id}/scope/distributors` | 追加范围店铺 |
| `GET /api/v1/point/activity/{id}/scope/merchants` | `GET /api/v1/point/activity/{id}/scope/distributors` | 查询范围店铺 |

**routeAlias 更新：**

| 旧值 | 新值 |
|------|------|
| `point.activity.scope.merchants` | `point.activity.scope.distributors` |
| `point.activity.scope.merchants.append` | `point.activity.scope.distributors.append` |

> ⚠️ routeAlias 关联权限菜单系统。如果管理后台的权限配置引用了旧 alias，需要同步更新权限数据（或保留旧 alias 做兼容映射）。

> ⚠️ API 路径变更需要前端同步修改。过渡期保留旧路径（@Deprecated 注解），前端部署后再删除。

### 6. 请求/响应字段

| 位置 | 旧名 | 新名 |
|------|------|------|
| Scope API 请求体 | `merchant_ids` | `distributor_ids` |
| Scope API 响应 | `merchant_id` / `merchant_name` | `distributor_id` / `distributor_name` |
| 线下核销请求体 | `merchant_id` | `distributor_id` |

### 7. 前端改动（ecshopx-admin）

| 文件 | 变更 |
|------|------|
| `src/api/point.js` | API 路径 merchants → distributors |
| `src/view/member/point/activity/scope.vue` | 字段名对齐 |
| C 端核销页面 | `merchant_id` → `distributor_id` |

### 8. 测试文件重命名

| 旧名 | 新名 |
|------|------|
| `PointScopeShopIdsUtilTest.java` | `PointScopeDistributorIdsUtilTest.java` |

> 其他测试文件中引用的类名/方法名随之自动更新。

---

## 实施策略

### 方案 A：一次性重命名（推荐）

1. DB migration：`RENAME COLUMN`（MySQL 8.0+ instant DDL，不锁表）
2. Java 实体 + Service 全量替换（含原生 SQL 字符串）
3. API 路径新旧并存（旧路径 @Deprecated，过渡期后删除）
4. 前端同步修改

### 方案 B：渐进式（字段别名）

1. Java 实体用 `@MpField(value = "scope_shop_ids")` 保持旧列名映射
2. 只改 Java 层命名，不改 DB 列名
3. 后续有 DB migration 机会时再改列名

**建议用方案 A**，一步到位，避免 Java 层和 DB 层命名不一致的长期维护成本。

---

## 部署顺序

```
Step 1: DB migration — RENAME COLUMN（instant，无风险）
Step 2: Backend 部署 — 新旧 API 路径并存，旧路径 @Deprecated
Step 3: Frontend 部署 — 改用新路径
Step 4: 下一迭代 — 删除 @Deprecated 旧路径
```

> **回滚方案：** 如果 Step 2 出问题，用 `RENAME COLUMN ... TO` 恢复旧列名 + 回滚后端代码。RENAME COLUMN 是 instant 操作，双向都安全。

---

## 与积分流水改造（POINT_LOG_FIELDS_PLAN）的协调

**本计划先执行。** 原因：

1. 命名重构完成后，`PointBatchDeductService.deduct()` 的参数已从 `merchantId` 改为 `distributorId`
2. 流水改造可以**直接用这个已有参数**填充 consume/log 的 `distributor_id` 字段
3. 不需要重载方法、不需要审计调用方 — 实现最简

**执行顺序：**
```
命名重构（本计划）合并 → 积分流水改造（POINT_LOG_FIELDS_PLAN）开新分支开发
```

---

## 影响范围

- **不影响已有数据**：只改列名/类名，值不变
- **需要前后端同步**：API 路径和字段名变更
- **需要回归测试**：
  - 活动创建/编辑（范围配置）
  - 活动范围追加
  - 线下核销
  - 积分扣减（scope 匹配）

---

## 验证方式

1. `mvn compile` 全量编译通过
2. 活动范围 CRUD 接口正常
3. 线下核销接口正常
4. 积分扣减 scope 匹配逻辑不变
5. **grep 扫描确认无遗漏：**
   ```bash
   grep -rn "scope_shop\|scopeShop\|ScopeShop\|Merchant" ecshopx-point/src/ --include="*.java" | grep -v "// deprecated"
   ```
   结果中不应有未重命名的残留（排除无关的 merchant 引用）

---

## Review 反馈（2026-07-15 via Claude CLI）

**总体评估：MEDIUM 风险**（补全遗漏项后降为 LOW）

### 已采纳的改进

| 问题 | 处理方式 |
|------|---------|
| 遗漏 `PointActivityScopeLog` 3 个字段 | ✅ 补充到重命名清单 |
| 遗漏 `PointBatchMapper` 原生 SQL + 方法名 | ✅ 补充并标注 SQL 字符串风险 |
| 遗漏 Controller `extractMerchantIds` 方法 | ✅ 补充到方法重命名清单 |
| `CHANGE COLUMN` 可能锁表 | ✅ 改为 `RENAME COLUMN`（instant DDL） |
| 缺少部署顺序 | ✅ 新增 4 步部署顺序 + 回滚方案 |
| 与积分流水计划的协调 | ✅ 明确先做流水改造、后做重命名 |
| routeAlias 需要同步更新 | ✅ 列出旧值→新值映射 |
| 测试文件遗漏 | ✅ 补充测试文件重命名 |
| 实体 comment 仍说"商户" | ✅ 标注需同步更新注解 comment |
| 缺少 grep 验证步骤 | ✅ 新增 grep 扫描确认 |
| 缺少回滚策略 | ✅ 部署顺序中说明回滚方式 |
