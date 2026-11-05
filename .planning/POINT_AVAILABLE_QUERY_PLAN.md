# 积分可用查询 — 实现计划

## Context（背景）

线下核销流程中，用户扫店铺核销码进入 H5 页面，需要展示：会员信息、当前店铺可用积分汇总、可用活动列表（含每个活动的可用积分、适用范围、可用商户数等），以及某个活动的适用商户详情列表。

现有 `GET /wxapp/point/offline/available`（7bd45667 提交，2026-07-09）是与 `/offline/consume` 一起添加的，定位为"选中活动后核销前查询单活动可用积分"。新流程改为扫码落地页直接展示活动列表及每个活动的可用积分，原接口不再需要，将被废弃。

本计划废弃现有 `/offline/available` 接口，用新的聚合接口替代，同时新增活动适用商户列表：

1. **扫码核销页信息接口**（替代 `/offline/available`）— 聚合展示所有可用活动及其积分
2. **活动适用商户列表接口** — 按活动 scope 规则列出可用商户，支持属性/类目筛选

---

## 与其他计划的关系

| 项 | 归属 |
|----|------|
| 线下核销扣减、point_order 生成 | `POINT_ORDER_PLAN` |
| 线上订单抵扣改造 | `POINT_ONLINE_DEDUCT_PLAN` |
| 扫码页信息聚合查询、活动适用商户列表 | **本计划** |

**前置依赖：** 无硬依赖。本计划的查询逻辑基于现有 `point_batch`、`point_activity`、`PointBatchScopeMatcher` 等已就绪的基础设施，可独立实施。

---

## 设计决策

### AQ1：废弃 `/offline/available`，新接口完全替代

**决定：** 废弃现有 `GET /wxapp/point/offline/available` 接口，新建 `GET /wxapp/point/offline/scan-page` 替代。新接口一次性返回会员信息 + 活动列表（含每个活动的可用积分）+ 积分规则。

**原因：**
- 新流程中扫码落地页已展示每个活动的 `available_point`，覆盖了原接口的展示需求
- `consume` 接口内部有余额校验兜底，不需要额外的前置查询保障一致性
- 保留一个无调用场景的接口只增加维护成本
- 如果未来确认页需要刷新单活动积分，可给新接口加 `activity_id` 可选参数（传了只返回单活动数据）

**废弃方式：**
- 新接口上线后，标记 `/offline/available` 为 `@Deprecated`
- 确认前端切换完成后移除

### AQ2：可用活动列表的产生逻辑

**决定：** 可用活动 = 该会员在该店铺有 `remain_point > 0` 的有效批次所关联的活动集合。

**逻辑步骤：**
1. 查询该用户所有有效批次：`status=1`、`remain_point > 0`、未过期
2. 过滤 `scope_type IN (2, 3)`（线下可用 or 全场景）
3. 过滤店铺匹配：`PointBatchScopeMatcher.distributorMatches(batch, distributorId)`
4. 按 `activity_id` 分组，sum `remain_point` 得到每个活动的可用积分
5. 关联 `point_activity` 获取活动名称、scope 描述、有效期等

**关于 `activity_id = 0`：** 改造后确保所有批次都关联活动（D19 约定），不会出现此情况，无需额外处理。

### AQ3：活动有效期描述的生成规则

**决定：** 按 `expire_type` 生成人可读描述：

| expire_type | 描述格式 |
|-------------|----------|
| 0 | "永不过期" |
| 1 | "有效期至 {end_time 格式化}" |
| 2 | "发放后 {expire_days}{unit} 内有效"（unit: 小时/天/月/年） |

展示的是活动级别的有效期规则，不是单个批次的实际过期时间。

### AQ4：活动可用商户总数的计算方式

**决定：** 根据活动的 `scope_distributor_type` 分别处理：

| scope_distributor_type | 可用商户总数 |
|------------------------|-------------|
| 0（全部） | 公司下有效店铺总数 |
| 1（仅限指定） | `scope_distributor_ids` 中有效店铺数量 |
| 2（排除指定） | 公司有效店铺总数 - `scope_distributor_ids` 中有效店铺数量 |

**注意：** 这里需要校验 `scope_distributor_ids` 中的店铺是否仍然有效（`is_valid = true`），避免计数包含已关闭的店铺。

### AQ5：适用商户列表接口的筛选策略

**决定：** 新建 `GET /wxapp/point/offline/activity-distributors` 接口，采用「先定范围，再加筛选」策略：

1. 根据活动 scope 规则确定基础商户集合（同 AQ4 逻辑）
2. 在基础集合上叠加可选筛选条件：
   - 店铺属性（动态，有多少展示多少，按 `attribute_value_id` 筛选）
   - 类目（传任意层级类目 ID，递归展开所有子类目后筛选）
3. 分页返回

**筛选 SQL 策略：**
- 属性筛选：复用现有 `DistributorAdminListMapper` 的 EXISTS 子查询模式
- 类目筛选：递归展开子树（需改造 `collectSubtreeCategoryIds` 支持无限层级）后 EXISTS

### AQ6：商户列表中"位置"字段的拼接规则

**决定：** "位置"= 按 `attribute_sort` 排序后，取该店铺的所有属性值依次拼接。

**具体逻辑：**
1. 查询该店铺的 `DistributorRelAttributes`，关联 `DistributorAttributes` 获取排序
2. 按 `attributeSort` ASC 排列
3. 取 `attributeValue`（select 类型从 `DistributorAttributeValues` 拿值文本；input 类型取 `customAttributeValue`）
4. 直接拼接为字符串，无分隔符

### AQ7：商户列表中"类目"字段的展示

**决定：** 展示该店铺关联的全部分类，格式同 `GET /api/v1/distributors/info` 返回的 `categories` 字段（flat list，每项含 `category_id`、`category_name`、`category_level`）。

**逻辑：**
- 复用 `DistributorRelCategoriesQueryService.listCategoriesForDistributor()` 获取该店铺关联的所有类目
- 直接返回完整列表，不限制层级，不做路径拼接
- 筛选项中的类目树同样不限层级，复用 `GET /api/v1/distributor/category?tree=1` 的树形结构

### AQ8：会员信息的获取方式

**决定：** 复用现有能力，通过 token 获取当前登录会员信息：

- 会员名称：`members.username` 或 `members_info.name`
- 会员头像：`members_info.avatar`（wechat_user 头像兜底）
- 会员卡等级：`WxappMemberInfoKaquanPort.applyGradeAndNext()` 获取 grade 名称

不新建查询通道，直接调用 `MemberAccountService` 已有方法。

---

## 接口设计

### 接口一：扫码核销页信息

**Endpoint:** `GET /api/v1/h5app/wxapp/point/offline/scan-page`

**Auth:** `@FrontAuth`（需登录）

**Request Params:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| distributor_id | long | 是 | 当前扫码的店铺 ID |

**Response:**

```json
{
  "member_info": {
    "username": "张三",
    "avatar": "https://...",
    "grade_name": "黄金会员"
  },
  "distributor_name": "朝阳门店",
  "total_available_point": 1500,
  "activity_list": [
    {
      "activity_id": 101,
      "title": "开业大促积分",
      "available_point": 800,
      "scope_desc": "仅限指定门店使用",
      "scope_distributor_count": 5,
      "expire_desc": "有效期至 2026-12-31"
    },
    {
      "activity_id": 102,
      "title": "消费返积分",
      "available_point": 700,
      "scope_desc": "全部门店可用",
      "scope_distributor_count": 120,
      "expire_desc": "永不过期"
    }
  ],
  "rule": {
    "rule_title": "积分使用规则",
    "offline_rule_desc": "100积分=1元，可用于线下核销..."
  }
}
```

**字段说明：**
- `total_available_point`：所有活动的 `available_point` 求和
- `activity_list`：按 `activity_id` ASC 排序
- `scope_distributor_count`：该活动的可用商户总数（AQ4 逻辑）
- `expire_desc`：AQ3 规则生成的人可读描述

---

### 接口二：活动适用商户列表

**Endpoint:** `GET /api/v1/h5app/wxapp/point/offline/activity-distributors`

**Auth:** `@FrontAuth`（需登录）

**Request Params:**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| activity_id | long | 是 | 活动 ID |
| attribute_value_ids | string | 否 | 属性值 ID，逗号分隔，多个为 AND 关系 |
| category_id | long | 否 | 类目 ID（一级或二级均可） |
| page_no | int | 否 | 页码，默认 1 |
| page_size | int | 否 | 每页数量，默认 20 |

**筛选逻辑：**
- `category_id`：传任意层级类目 ID，展开该类目下所有子树（递归所有后代），按子树筛选
- `attribute_value_ids`：多个值 AND 关系（店铺需同时满足所有指定属性值）

**Response:**

```json
{
  "total_count": 45,
  "list": [
    {
      "distributor_id": 201,
      "name": "朝阳门店",
      "location": "北京朝阳区三层",
      "categories": [
        { "category_id": 1, "category_name": "直营店", "category_level": 1 },
        { "category_id": 11, "category_name": "旗舰店", "category_level": 2 }
      ]
    },
    {
      "distributor_id": 202,
      "name": "西单门店",
      "location": "北京西城区一层",
      "categories": [
        { "category_id": 1, "category_name": "直营店", "category_level": 1 },
        { "category_id": 12, "category_name": "标准店", "category_level": 2 }
      ]
    }
  ],
  "filters": {
    "attributes": [
      {
        "attribute_id": 1,
        "attribute_name": "城市",
        "values": [
          { "attribute_value_id": 10, "attribute_value": "北京" },
          { "attribute_value_id": 11, "attribute_value": "上海" }
        ]
      },
      {
        "attribute_id": 2,
        "attribute_name": "楼层",
        "values": [
          { "attribute_value_id": 20, "attribute_value": "一层" },
          { "attribute_value_id": 21, "attribute_value": "二层" }
        ]
      }
    ],
    "categories": [
      {
        "category_id": 1,
        "category_name": "直营店",
        "category_level": 1,
        "children": [
          {
            "category_id": 11,
            "category_name": "旗舰店",
            "category_level": 2,
            "children": []
          },
          {
            "category_id": 12,
            "category_name": "标准店",
            "category_level": 2,
            "children": []
          }
        ]
      }
    ]
  }
}
```

**字段说明：**
- `location`：按属性 sort 排序后直接拼接的属性值字符串，无分隔符（AQ6）
- `categories`：该店铺关联的全部分类列表，不限层级（AQ7），格式同 `/distributors/info` 的 `categories` 字段
- `filters`：返回当前公司所有可用的筛选项（属性+类目），方便前端渲染筛选 UI
  - `attributes`：该公司定义的所有店铺属性及其预设值（仅 `attribute_show=select` 类型）
  - `categories`：完整类目树，不限层级，格式同 `GET /api/v1/distributor/category?tree=1`

---

## 实现计划

### Phase AQ-A：扫码页信息接口

**范围：** 接口一的完整实现

**新增文件：**

| 文件 | 说明 |
|------|------|
| `PointOfflineScanPageController.java` | 或在现有 `PointOfflineConsumeController` 中新增 endpoint |
| `PointOfflineScanPageService.java` | 扫码页聚合服务 |
| `PointBatchActivitySummaryService.java` | 按活动分组汇总可用积分 |

**核心实现步骤：**

1. **新建 `PointBatchActivitySummaryService`**
   - 方法：`List<ActivityAvailableSummary> getAvailableByActivity(companyId, userId, distributorId)`
   - 逻辑：
     - 调用 `PointBatchMapper.selectAvailableForDeduct(userId, companyId, now)` 获取所有有效批次
     - 过滤 `scope_type IN (2, 3)`
     - 过滤 `PointBatchScopeMatcher.distributorMatches(batch, distributorId)`
     - 按 `activity_id` 分组，sum `remain_point`
   - 返回结构：`{ activityId, availablePoint }`

2. **新建 `PointOfflineScanPageService`**
   - 组装逻辑：
     - 调用 `PointBatchActivitySummaryService` 获取活动可用积分列表
     - 批量查询 `point_activity` 补充活动信息（名称、scope_desc、有效期）
     - 计算每个活动的 `scope_distributor_count`（AQ4）
     - 生成 `expire_desc`（AQ3）
     - 查询店铺名称（`Distributor.name`）
     - 查询会员信息（复用 `MemberAccountService`）
     - 查询积分规则（复用 `PointMemberRuleReadService`）
     - 汇总 `total_available_point`

3. **新建 Controller endpoint**
   - `GET /api/v1/h5app/wxapp/point/offline/scan-page`
   - `@FrontAuth`，接收 `distributor_id`

**跨模块依赖：**
- `ecshopx-point` → `ecshopx-distribution`：复用现有 `DistributorNameBatchReadPort`（已在 `PointOfflineConsumeService` 中注入）获取店铺名称
- `ecshopx-point` → `ecshopx-members`：复用现有 `PointMemberDmPointMemberInfoReadPort`（已有实现 `MemberAccountPointMemberInfoAdapter`）获取会员 username、avatar、grade_id
- 会员等级名称（grade_name）：需通过 grade_id 查询等级配置获取，可新增方法到现有 port 或新增一个小粒度 port

**现有可复用的 Port/Service：**
- `DistributorNameBatchReadPort`：按 ID 批量查店铺名称
- `PointMemberDmPointMemberInfoReadPort`：获取会员基本信息（username、avatar、grade_id）
- `DistributorAttributeFilterPort.getFilterAttributes(companyId)`：获取公司属性+预设值列表，可直接用于 filters.attributes 响应
- `DistributorRelAttributesQueryService.listParamsForDistributor()`：查店铺属性值（已按 sort 排序），可用于构建 location 字段
- `DistributorRelCategoriesQueryService.listCategoriesForDistributor()`：查店铺关联分类，可直接用于 categories 字段
- `DistributorCategoryQueryService.list(companyId, tree=true, ...)`：获取类目树，可用于 filters.categories
- `PointScopeDistributorIdsUtil.parseIds()`：解析 `scope_distributor_ids` 逗号分隔字符串为 `List<Long>`

---

### Phase AQ-B：活动适用商户列表接口

**范围：** 接口二的完整实现

**新增文件：**

| 文件 | 说明 |
|------|------|
| `PointActivityDistributorListService.java` | 活动适用商户查询服务（point 模块，负责读取活动 scope 规则、解析 IDs、委托 distribution port 查询） |
| `PointActivityDistributorListPort.java` | Port 接口（common 模块）— 按 scope 规则 + 属性/类目筛选分页查询商户 |
| `PointActivityDistributorListPortImpl.java` | Port 实现（distribution 模块，含 Mapper XML） |

**核心实现步骤：**

1. **确定基础商户集合**
   - 查询活动的 `scope_distributor_type` 和 `scope_distributor_ids`
   - `type=0`：基础集合 = 公司所有有效店铺
   - `type=1`：基础集合 = `scope_distributor_ids` 中有效店铺
   - `type=2`：基础集合 = 公司所有有效店铺 排除 `scope_distributor_ids`

2. **叠加筛选条件**
   - 属性筛选：对基础集合 + EXISTS `distribution_distributor_rel_attributes`（AND 逻辑）
   - 类目筛选：递归展开子树（不限层级）→ EXISTS `distribution_distributor_rel_categories`

3. **分页查询与数据补充**
   - 分页查询匹配的店铺列表
   - 批量查询每个店铺的属性值，拼接 `location`（AQ6，无分隔符直接拼接）
   - 批量查询每个店铺的关联类目，返回完整 `categories` 列表（AQ7）

4. **返回可用筛选项**
   - 查询公司所有 `select` 类型属性及其 values
   - 查询完整类目树（不限层级，格式同 `category?tree=1`）

5. **新建 Controller endpoint**
   - `GET /api/v1/h5app/wxapp/point/offline/activity-distributors`
   - `@FrontAuth`，接收参数如接口设计所述

**SQL 策略（在 distribution 模块实现）：**

**注意：** `scope_distributor_ids` 存储为逗号分隔字符串，需在 Java 层通过 `PointScopeDistributorIdsUtil.parseIds()` 解析为 `List<Long>` 后传入 SQL。

```sql
SELECT d.distributor_id, d.name
FROM distribution_distributor d
WHERE d.company_id = #{companyId}
  AND d.is_valid = 'true'
  AND d.is_distributor = true
  -- scope 过滤（type=0 时不加此条件）
  AND d.distributor_id [IN / NOT IN] (#{parsedDistributorIds})
  -- 属性筛选（每个 attribute_value_id 一个 EXISTS）
  AND EXISTS (SELECT 1 FROM distribution_distributor_rel_attributes ra
              WHERE ra.distributor_id = d.distributor_id
                AND ra.attribute_value_id = #{avid1})
  AND EXISTS (SELECT 1 FROM distribution_distributor_rel_attributes ra
              WHERE ra.distributor_id = d.distributor_id
                AND ra.attribute_value_id = #{avid2})
  -- 类目筛选（categoryIds 为递归展开后的完整子树 ID 列表）
  AND EXISTS (SELECT 1 FROM distribution_distributor_rel_categories rc
              WHERE rc.distributor_id = d.distributor_id
                AND rc.category_id IN (#{categoryIds}))
ORDER BY d.distributor_id ASC
LIMIT #{offset}, #{pageSize}
```

**`scope_distributor_type = 0`（全部）时不加 IN/NOT IN 条件。**

---

## 性能考虑

1. **活动可用积分汇总（AQ-A）**：一次 DB 查询拿全部有效批次，内存分组。批次数量通常有限（一个用户不会有成百上千个未用完的批次），不需要 DB 端 GROUP BY。

2. **可用商户总数（AQ4）**：
   - `type=0`：缓存公司有效店铺总数（可用 Redis，短 TTL）
   - `type=1/2`：`scope_distributor_ids` 列表一般不大（几十到几百），直接 `SELECT COUNT(*) WHERE distributor_id IN (...) AND is_valid = 'true'`

3. **商户列表分页（AQ-B）**：走分页 + 索引，`distribution_distributor` 表有 `ix_company_id_shop_code` 索引。属性/类目 EXISTS 子查询利用各自的联合索引。

4. **筛选项（filters）**：属性和类目是公司级配置，数量有限，可考虑短时间缓存。

---

## 实现备注

1. **类目子树展开：** 现有 `DistributorCategoryQueryService.collectSubtreeCategoryIds()` 硬编码只支持 3 级。本计划要求不限层级，建议改为基于 `path` 字段的 LIKE 查询（`WHERE path LIKE '{parentPath}%'`），比递归查 `parent_id` 简单高效。

2. **会员等级名称：** 现有 `PointMemberDmPointMemberInfoReadPort` 只返回 `grade_id`，获取 `grade_name` 需额外查询等级配置表。实现时可在现有 adapter 中补充此逻辑，或在 `PointOfflineScanPageService` 中单独查一次。

3. **`scope_distributor_count` 优化：** 对 `scope_distributor_type=0`（全部店铺）的活动，公司有效店铺总数只需查一次即可复用于多个活动，避免重复 COUNT 查询。

4. **`is_distributor` 列类型：** 实际存储为 boolean，MyBatis-Plus 映射时需注意条件写法，确保与 DB 驱动的布尔处理一致（不要写 `= 1`）。
