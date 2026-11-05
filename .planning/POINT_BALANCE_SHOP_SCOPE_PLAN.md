# 线上积分可用余额按店铺使用范围过滤 — 实现计划

## 需求理解

当前 `getPointBalance` 返回所有 scope_type IN (1, 3) 的积分总和，不区分店铺。新需求要求：

| 下单场景 | 可用积分的活动范围 |
|---------|-----------------|
| 平台商品下单（无 distributorId） | 线上-通用 + 通用 |
| 店铺下单，店铺范围=线上 | 线上-通用 + 线上-仅前置仓 + 通用 |
| 店铺下单，店铺范围=通用 | 线上-通用 + 通用 |

本质差异：**只有"店铺范围=线上"时才包含仅前置仓（online_scope_type=1）的批次**。

## 需要修改的文件

### 1. 新建 Port 接口：`DistributorPointScopePort`

位置：`ecshopx-common`（被 point 和 orders 模块共同依赖的接口层）

```java
public interface DistributorPointScopePort {
    /**
     * 获取店铺的积分使用范围。
     * @return "online" / "universal"，null 按 universal 处理
     */
    String getPointUsageScope(long companyId, long distributorId);
}
```

### 2. Port 实现：`DistributorPointScopePortImpl`

位置：`ecshopx-distribution` 模块

- 读取店铺的某个字段（字段名 TBD，先预留为常量占位）
- 当前阶段返回 null（即默认按"通用"处理），等字段确认后填入真实逻辑

### 3. 改造 `PointMemberBalanceReadService`

新增重载方法：
```java
public long getPointBalance(long companyId, long userId, long distributorId)
```

逻辑：
1. 如果 `distributorId <= 0`（平台订单）→ 排除仅前置仓
2. 如果 `distributorId > 0` → 调 `DistributorPointScopePort.getPointUsageScope()`
   - "online" → 包含仅前置仓
   - 其他 → 排除仅前置仓

### 4. `PointBatchMapper` 新增 SQL

新增方法 `sumOnlineAvailablePointExcludeWarehouse` — 排除 `online_scope_type=1` 的批次：
```sql
SELECT COALESCE(SUM(b.remain_point), 0) FROM point_batch b
INNER JOIN point_activity a ON b.activity_id = a.id
WHERE ... (同现有)
  AND b.scope_type IN (1, 3)
  AND (b.scope_type = 3 OR a.online_scope_type = 0)
```

### 5. 改造 `PointBatchDeductService.deductByRule` 线上路径

当前线上路径只排除 scope_type=2。新增逻辑：
- 接收一个 `includeWarehouse` 标记（或从 Port 读取）
- `includeWarehouse=false` 时，额外排除 scope_type=1 & online_scope_type=1 的批次

### 6. 改造调用方 `WxappNormalOrderTempInfoEnrichmentService`

将 `distributorId` 传入 `getPointBalance(companyId, userId, distributorId)`。

### 7. 改造 `PointOnlineOrderDeductService.deductForOrder`

传递 `includeWarehouse` 信息给 `deductByRule`，让扣减逻辑与查询逻辑一致。

## 改动文件清单

| 文件 | 操作 |
|------|------|
| `ecshopx-common/.../point/service/DistributorPointScopePort.java` | 新建接口 |
| `ecshopx-distribution/.../impl/DistributorPointScopePortImpl.java` | 新建实现（预留占位） |
| `ecshopx-point/.../service/PointMemberBalanceReadService.java` | 新增重载方法 |
| `ecshopx-point/.../mapper/PointBatchMapper.java` | 新增 SQL 方法 |
| `ecshopx-point/.../service/activity/PointBatchDeductService.java` | 线上路径增加 warehouse 过滤 |
| `ecshopx-point/.../service/activity/PointOnlineOrderDeductService.java` | 传递 warehouse 标记 |
| `ecshopx-orders/.../WxappNormalOrderTempInfoEnrichmentService.java` | 传入 distributorId |

## 设计要点

- Port 接口隔离：point 模块不直接依赖 distribution 模块，通过 Port 接口解耦
- 预留占位：`DistributorPointScopePortImpl` 当前返回 null（按通用处理），字段确认后只需改这一处
- 向后兼容：无 distributorId 的旧调用走原逻辑不变
- 扣减与查询一致：两处都用同一个 Port 判断，避免"看得到但扣不了"
