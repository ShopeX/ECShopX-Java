# 积分活动线下圈选店铺 — 批量上传计划

## 结论（先看）

| 问题 | 结论 |
|------|------|
| 要不要下载模板接口？ | **要**，且应复用现有 `GET /api/v1/espier/upload_template`，**不要**另起独立下载接口 |
| 批量上传是否与商品一致？ | **入口一致**（`upload_template` + `upload_file`），处理模式对齐 `marketing_goods` 的**同步解析**（`trySyncProcess`），不是异步落库导入 |
| 模板字段（店铺名称 + 店铺编码，编码必填） | **合理**；匹配主键用 `shop_code`，名称仅辅助展示 |
| 与 POST `/distributors` + `distributorIds` | **配套**：上传解析出 ID 后，前端用该接口分页回显已圈选店铺 |
| 每次上传如何作用于圈选？ | **整表覆盖（replace）**：表单已选店铺 = 本次 `succ` 的 ID 集合，不与旧圈选 merge |
| 单次上传是否限量？ | **要限**；定稿 **10000**（覆盖模式下上限≈单次可写入的圈选规模） |
| 上传是否返回店铺 ID？ | **要返回**：`succ[].distributor_id`，前端覆盖圈选后，保存活动写入 `scope_distributor_ids` |
| 名称不一致 | **不失败**；可进 `invalid` 提示或不返回该提示，匹配仍靠编码进 `succ` |
| 禁用店铺 | **允许**：`is_valid=false`（禁用）可进 `succ` |
| 废弃店铺 | **失败**：`is_valid=delete`（废弃）进 `fail` |
| 排除指定 / 仅限指定 | **共用同一套模板与同一 `file_type`** 即可 |

---

## 1. 业务场景

后管「积分活动」设置：

- 使用范围 = **线下**（`scope_type = 2`）
- 需要圈选店铺（`scope_distributor_type` = 仅限指定 / 排除指定）
- 店铺量大（可达**几千**）时，手工逐个勾选不可行 → **Excel 批量导入圈选结果**

活动最终仍落库为：

- `scope_distributor_ids`：逗号分隔的 `distributor_id` 列表（已有模型，见 `PointActivity` / `PointScopeDistributorIdsUtil`）

批量上传**只是圈选辅助**，不是直接改活动表的旁路写接口；保存活动时仍走现有创建/编辑接口写入 `scope_distributor_ids`。

---

## 2. 与现有商品批量上传的对齐方式

### 2.1 应对齐的部分（前端体验 + 入口）

现有商品链路：

```
下载模板  GET  /api/v1/espier/upload_template?file_type=marketing_goods&file_name=商品模板
批量上传  POST /api/v1/espier/upload_file     (multipart: file_type=marketing_goods + file)
```

店铺批量上传应保持同一套：

```
下载模板  GET  /api/v1/espier/upload_template?file_type=<新类型>&file_name=店铺模板
批量上传  POST /api/v1/espier/upload_file     (multipart: file_type=<新类型> + file)
```

好处：

- 前端组件可复用（模板下载、上传进度/结果区）
- 权限路由沿用 `espier.upload` / `espier.upload.template.export`
- Header / 校验 / xlsx 生成已有 `EspierUploadHeaderCatalog` + `UploadFileService.exportUploadTemplate`

### 2.2 必须对齐的处理模式：同步解析（非异步导入）

`marketing_goods` 关键点：`MarketingGoodsUploadEspierHandler.trySyncProcess` → 立即返回 `succ / invalid / fail`，**不写业务表**。

积分活动圈店场景同理：

1. 运营在**活动编辑表单**中上传 Excel  
2. 后端解析 → 按店铺编码查 `distributor_id`  
3. 接口同步返回成功/失败行  
4. 前端用成功行的 `distributor_id` **整表覆盖**当前圈选集合  
5. 用户点保存活动时，才把完整 ID 列表写入 `scope_distributor_ids`

**不建议**走 `point_batch_import` 那种异步队列 + `uploade_file` 落库导入：圈选是表单态交互，需要即时反馈；异步任务适合「真正导入业务数据」。

### 2.3 与商品的差异（实现时注意）

| 点 | marketing_goods | 店铺圈选（本需求） |
|----|-----------------|-------------------|
| 行上限 | 约 500 | **10000**（硬限） |
| 解析后 | 返回商品字段供活动商品区使用 | 返回 `distributor_id` + 店铺编码/名称 |
| 回显列表 | 活动商品组件自行展示 | 调 `POST /api/v1/distributors` + `distributorIds` 分页回显 |
| 最终落库 | 活动商品关联逻辑 | `scope_distributor_ids` |

---

## 3. 模板设计

### 3.1 字段（建议）

| 列名（Excel 表头） | 内部字段 | 必填 | 说明 |
|--------------------|----------|------|------|
| 店铺编码 | `shop_code` | **是** | 对应 `distribution_distributor.shop_code`，唯一匹配键 |
| 店铺名称 | `name` | 否 | 仅辅助运营填写/核对；**不以名称为准匹配** |

示例：

| 店铺编码 | 店铺名称 |
|----------|----------|
| SHOP001 | 南京新街口店 |
| SHOP002 | 上海静安店 |

### 3.2 为何合理

- 系统内店铺唯一业务键是 `shop_code`（创建/更新校验、OpenAPI、白名单等多处已用）
- 积分订单导出标题已用「店铺编码」，与积分模块文案一致（现有 espier 店铺信息模板多用「店铺号」，本需求建议统一叫「店铺编码」，避免运营混淆）
- 名称可选：填错名称仍能靠编码命中；可选做「名称不一致」软提示进 `invalid`，不阻断成功匹配

### 3.3 校验规则（已定稿）

1. `shop_code` 空 → 跳过空行；有其他列但编码空 → `fail`  
2. `shop_code` 在本公司不存在 → `fail`  
3. 店铺存在但 **废弃**（`is_valid=delete`）→ `fail`，原因「店铺已废弃」  
4. 有效（`true`）与禁用（`false`）→ 均可进 `succ`  
5. 名称与库不一致 → **不失败**；仍按编码进 `succ`（名称字段以后端库为准回填）  
6. 文件内重复编码 → 去重，保留首次  
7. 单次有效数据行超上限（**10000**）→ `400`，提示减少后重试  

### 3.4 上传与圈选的交互：整表覆盖（已定稿）

**规则：** 每次上传成功后，前端用本次 `succ` 的 `distributor_id` 列表 **整表替换** 当前表单圈选结果，**不**与旧名单 merge。

| 维度 | 说明 |
|------|------|
| 是否合理 | **合理**。批量上传表达的是「这份 Excel 就是最终名单」；覆盖语义清晰 |
| 与限量关系 | 覆盖模式下，单次上限 = **10000** ≈ 活动最多可圈选店铺数 |
| UX 注意 | 上传前确认：「将覆盖当前已选店铺」；默认 **部分成功也以 succ 覆盖** |
| 手工勾选 | 上传覆盖后可再手工增删；再次上传仍整表覆盖 |
| 不支持 | 本期不做「在原名单上追加」 |

**部分成功默认策略：** 接口 200 且返回 `succ` → 前端以 `succ` 覆盖；`fail` 展示后整表重传。

### 3.5 保存活动如何用上传结果（已定稿）

```
upload_file 响应 succ[].distributor_id
        ↓ 前端 replace 到表单 state（selectedDistributorIds）
        ↓ 可选：POST /distributors 分页回显
        ↓ 用户点保存
活动创建/编辑接口  scope_distributor_ids = selectedDistributorIds
```

- **上传接口必须返回** 成功行的 `distributor_id`（另可回填 `shop_code`、`name` 便于展示）  
- 保存时**不再**根据店铺编码二次解析；直接提交 ID 列表  
- 后端保存活动仍用现有 `scope_distributor_ids` 字段逻辑

---

## 4. 端到端数据流

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. 下载模板                                                      │
│    GET /espier/upload_template?file_type=xxx&file_name=店铺模板   │
│    → data:{ name, file: data:xlsx;base64,... }                  │
└───────────────────────────────┬─────────────────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. 批量上传（同步）                                               │
│    POST /espier/upload_file  file_type=xxx + file.xlsx           │
│    → 解析表头 → 收集 shop_code                                    │
│    → DistributorListQueryService 按 companyId + shop_codes 批量查 │
│    → 返回 { succ:[{distributor_id, shop_code, name}], fail:[...] }│
└───────────────────────────────┬─────────────────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. 前端用本次 succ **整表覆盖** 圈选 ID（内存 / 表单 state）         │
│    selectedDistributorIds = succ.*.distributor_id（replace，非 merge）│
└───────────────────────────────┬─────────────────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. 分页回显已选店铺（已改造）                                      │
│    POST /api/v1/distributors                                     │
│    body: { distributorIds: [...几千个], page, pageSize }         │
│    → list + total_count                                          │
└───────────────────────────────┬─────────────────────────────────┘
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│ 5. 保存活动                                                      │
│    现有活动创建/编辑接口                                           │
│    scope_type / scope_distributor_type / scope_distributor_ids   │
└─────────────────────────────────────────────────────────────────┘
```

**已完成依赖：** `POST /api/v1/distributors` 支持 body 传大批量 `distributorIds` 再分页（`PJ3174JF-321`）。本计划建立在该能力之上。

---

## 5. 后端改造清单

### 5.1 新增 `file_type`（命名建议）

候选：

| 候选名 | 评价 |
|--------|------|
| `point_activity_distributor` | **推荐**：语义清晰，归属积分活动圈店 |
| `point_scope_distributor` | 亦可，偏技术 |
| `marketing_distributor` | 不推荐：易与营销活动商品混淆 |

前端调用示例：

```http
GET /api/v1/espier/upload_template?file_type=point_activity_distributor&file_name=店铺模板

POST /api/v1/espier/upload_file
Content-Type: multipart/form-data
file_type=point_activity_distributor
file=@店铺模板.xlsx
```

### 5.2 Espier 侧改动

1. **`EspierUploadHeaderCatalog`**  
   - 增加 `POINT_ACTIVITY_DISTRIBUTOR` 表头映射  
   - `forFileType` switch 增加分支  

2. **Handler**（新建，对齐 `MarketingGoodsUploadEspierHandler`）  
   - `supportedFileType()` → `point_activity_distributor`  
   - `check()`：仅 xlsx  
   - `trySyncProcess()`：解析 + 批量查店铺 + 组装 `succ/fail/invalid`  
   - `handleBusinessRow`：可空实现（同步路径不会走异步行处理）；或抛不支持异步  

3. **注册**  
   - `EspierUploadHandlerBeans` / registry 能扫到该 Handler  

4. **同步解析服务**（建议独立类，避免塞进 marketing support）  
   - 如 `PointActivityDistributorSyncUploadSupport`  
   - 依赖：按 `shop_code` 批量解析 `distributor_id`  
   - 已有能力可复用：`DistributorListQueryService` 中按 `companyId + shopCodes` → `Map<shopCode, distributorId>`（已有 `in(shop_code)` 查询）

### 5.3 Distribution / 查询侧（模块边界必做）

> **重要：** `ecshopx-espier` **不依赖** `ecshopx-distribution`（pom 仅 common/companys/members）。Handler **不能**直接调用 `DistributorListQueryService`。

推荐落地：

1. 在 `ecshopx-common` 增加 Port，例如 `DistributorShopCodeBatchResolvePort`  
   - 入参：`companyId` + `List<String> shopCodes`  
   - 出参：每行 `shop_code / distributor_id / name / is_valid`（或等价 Map）  
2. 在 `ecshopx-distribution` 实现 Port（基于现有 `shop_code IN` 查询，**分片**，并带回 `name`、`is_valid`）  
3. Espier Handler / SyncSupport 只依赖该 Port  

校验：`is_valid != "true"` → `fail`（已定稿，非可选）

### 5.4 落库字段容量（10000 场景建议本期必做）

涉及列均为 `text`（约 64KB），编辑活动时还会 `syncAllScopeFieldsByActivity` 同步到 `point_batch`：

| 表 | 字段 |
|----|------|
| `point_activity` | `scope_distributor_ids` |
| `point_batch` | `scope_distributor_ids` |
| `point_activity_scope_log` | `before/after_scope_distributor_ids` |

**实施建议：** 本期直接 migration 为 `MEDIUMTEXT`，不要等线上截断后再改。

### 5.5 不需要改的部分

- 活动 CRUD 入参格式（已支持 `Collection` → `formatScopeDistributorIdsInput`）  
- `POST /distributors` 大批量 ID 分页（已提交）  
- 另起「下载模板」专用 Controller（不需要）

---

## 6. 同步接口响应约定（建议）

对齐商品风格；**V1 可不返回 `invalid`**（名称不一致不失败、不提示，减少前端分支）：

```json
{
  "data": {
    "succ": [
      {
        "distributor_id": 10001,
        "shop_code": "SHOP001",
        "name": "南京新街口店"
      }
    ],
    "fail": [
      {
        "shop_code": "NOEXIST",
        "name": "",
        "reason": "店铺不存在"
      },
      {
        "shop_code": "DELETED01",
        "name": "某店",
        "reason": "店铺已废弃"
      }
    ]
  }
}
```

前端：

- 用 `succ[*].distributor_id` **整表覆盖** 圈选 state（保存活动时提交为 `scope_distributor_ids` 数组即可）  
- **边界：** `succ` 为空时仍覆盖为清空（前端 replace 空数组并提示）；后端只返回空 `succ`，不改活动表  
- 展示 `fail` 明细  
- 回显列表走 `POST /distributors`（分页展示，不替代 ID 列表本身）

---

## 7. 前端配合要点（供联调）

1. 线下 +「仅限指定 / 排除指定」共用同一套「下载模板 / 批量上传」  
2. `file_type` 固定为 `point_activity_distributor`  
3. 上传成功后：**replace** 为 `succ` ID → 调 `POST /distributors` 分页展示  
4. 上传前提示将覆盖已选；禁用/不存在进 fail 列表  
5. 保存活动：提交完整 `scope_distributor_ids`（数组或逗号串均可，后端已兼容）  
6. 一万级 ID 必须 POST body；注意保存活动请求体体积  

---

## 8. 风险与边界

| 风险 | 处理 |
|------|------|
| 单次 10000 行同步超时 | Excel 很小；`shop_code IN` **分片查询**（如每批 1000）；硬上限 10000 |
| MySQL `IN` 过长 | 上传解析分片查；回显走 ID 分页；响应只返回必要字段 |
| `scope_distributor_ids` TEXT(64KB) | **10000 易超限**；本期 migration → `MEDIUMTEXT`（含 activity / batch / scope_log） |
| 模块依赖 | espier 经 **common Port** 调 distribution，禁止直连 Service |
| `succ` 为空覆盖 | 需锁死：清空 vs 保留旧圈选 |
| `shop_code` 公司内重复 | 若存在多条，应明确取第一条 / 或整码 `fail`（建议 fail「编码重复」） |
| 网关超时 | 同步解析 + 大 JSON 响应；必要时加接口超时观察 |
| 编码大小写 / 空格 | trim；与现网 `shop_code` 一致 |
| 与「店铺号」文案混用 | 模板与文档统一「店铺编码」；内部字段统一 `shop_code` |

---

## 9. 实施步骤（建议顺序）

1. **MEDIUMTEXT migration**（activity / batch / scope_log）  
2. **common Port + distribution 实现**（分片查 shop_code，含 name/is_valid）  
3. **Catalog + Handler + SyncSupport**（同步 succ/fail；禁用 fail；名称不一致不失败）  
4. **联调**：模板 → 上传覆盖 → POST distributors → 保存活动；指定/排除共用模板  
5. **文档** + **测试**（含 10000 冒烟、空 succ、禁用、TEXT 不截断）  

---

## 10. 验收标准

- [ ] `upload_template` 用新 `file_type` 能下载含「店铺编码」「店铺名称」的 xlsx  
- [ ] `upload_file` 同步返回 `succ[].distributor_id`；前端按 **覆盖** 替换圈选  
- [ ] 编码必填；不存在 / **废弃（delete）** 进入 `fail`；有效与禁用可进 `succ`；名称不一致 **不** 失败  
- [ ] 仅限指定与排除指定共用同一模板与上传接口  
- [ ] 单次超上限（**10000**）返回明确错误  
- [ ] 上传响应 ID 可直接用于保存 `scope_distributor_ids`  
- [ ] 10000 条写入 activity **与** 同步后的 batch **不截断**（MEDIUMTEXT）  
- [ ] 上千级 ID 可通过 `POST /distributors` + `distributorIds` 分页回显  
- [ ] 保存活动后 `scope_distributor_ids` 与圈选一致  

---

## 11. 相关已有锚点

| 能力 | 位置 |
|------|------|
| 模板/上传入口 | `UploadFileController` `/upload_template` `/upload_file` |
| 商品同步范例 | `MarketingGoodsUploadEspierHandler`（`@Component` + `trySyncProcess`） |
| Handler 注册 | `EspierUploadFileHandlerRegistry` 注入全部 `EspierUploadFileHandler` |
| 表头目录 | `EspierUploadHeaderCatalog` |
| 店铺编码批量查 ID | `DistributorListQueryService`（需经 Port 暴露给 espier） |
| 店铺有效态 | `Distributor.isValid`（`is_valid`） |
| 活动店铺范围字段 | `PointActivity.scopeDistributorIds` / `PointScopeDistributorIdsUtil` |
| 活动保存接受 ID 数组 | `PointActivityCreateService.formatScopeDistributorIdsInput` |
| 编辑同步到批次 | `pointBatchMapper.syncAllScopeFieldsByActivity` |
| 大批量 ID 分页列表 | `DistributorController` `POST /distributors`（PJ3174JF-321） |

---

## 已定稿规则

1. 名称不一致：**不失败**（匹配靠编码；名称以后端为准）  
2. 单次上传上限：**10000**（覆盖模式硬限）  
3. 有效（`is_valid=true`）与禁用（`false`）：**允许**进 `succ`；废弃（`delete`）：**fail**「店铺已废弃」  
4. `file_type`：`point_activity_distributor`  
5. 排除指定 / 仅限指定：**共用同一套模板与上传**  
6. 上传对圈选：**整表覆盖（replace）**，非 merge  
7. 上传响应：**必须返回** `succ[].distributor_id`，供保存活动写 `scope_distributor_ids`  
8. 本期范围：**仅线下店铺圈选**（线上商户圈选不在本计划）  
9. `succ` 为空：**整表覆盖为空（清空圈选）**——主要是**前端**用空数组 replace 表单 state；后端只如实返回 `succ:[]`，不做活动表写入  
10. 公司内同一 `shop_code` 多条：**该编码进 `fail`**（原因如「店铺编码重复」），不进 `succ`  
11. 响应结构：**不返回 `invalid`**；只有 `succ` + `fail`。名称不一致静默成功（名称以后端库为准回填）  

## 已关闭的确认项（2026-07-28）

| # | 问题 | 结论 |
|---|------|------|
| 1 | `succ` 为空是否清空 | 清空；前端 replace 为空数组。后端不写活动 |
| 2 | `shop_code` 重复 | **fail** |
| 3 | 要不要 `invalid` | **不要**。`invalid` 本是「名称不一致软提示」桶；名称不一致既不失败，V1 就不必多一个数组，前端只处理 `succ`/`fail` |

## 审查记录（2026-07-28）

### 总评

方向正确：复用 espier 模板/上传入口 + 同步解析 + POST distributors 回显 + 保存写 ID，链路闭环清晰，可实施。

### 通过项

- 与 `marketing_goods` 同步模式对齐正确（表单态不应走异步导入）  
- 覆盖语义、编码必填、禁用失败、指定/排除共用模板 — 合理  
- 保存侧已支持数组入参，无需改活动 CRUD 协议  
- 10000 + 返回 `distributor_id` 的产品目标明确  

### 高优先级问题（已反映到上文修订）

| # | 问题 | 说明 |
|---|------|------|
| H1 | 模块依赖写错 | espier 不能直连 `DistributorListQueryService`，必须 common Port |
| H2 | TEXT 容量 | 10000 易超 64KB；且会同步到 `point_batch` / scope_log，应本期升 MEDIUMTEXT |
| H3 | 文档自相矛盾 | 已修：§2.2「并入」→覆盖；§2.3 上限 5000→10000；§5.3「可选有效」→已定稿禁用 fail |

### 中优先级缺口

| # | 问题 | 建议 |
|---|------|------|
| M1 | 空 `succ` 覆盖行为未定义 | 实施前锁死 |
| M2 | `shop_code` 重复未定义 | 建议 fail |
| M3 | `invalid` 是否保留摇摆 | V1 建议去掉，名称不一致静默成功 |
| M4 | 回显 SQL `IN` 一万 | POST distributors 列表查询也建议分片或依赖已有分页（只查当前页 ID 子集更优——若当前实现是先 IN 全量再分页，需确认性能） |

### 低优先级

- HeaderInfo / 模板 demo 行是否要加示例数据  
- 接口文档落在 `docs/prd` 哪一篇  
- 超时与监控告警  

### 结论

**计划可继续实施**，但开工前建议先锁死「空 succ」「编码重复」两条，并把 **Port + MEDIUMTEXT** 列入第一批任务（非事后优化）。
