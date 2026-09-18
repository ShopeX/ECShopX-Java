/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.supplier.repository;

import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.domain.SupplierItemsAuditPatch;
import cn.shopex.ecshopx.supplier.domain.SupplierItemsUpdatePatch;
import cn.shopex.ecshopx.supplier.mapper.SupplierItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class SupplierItemsRepository {

	private final SupplierItemsMapper mapper;

	public SupplierItemsRepository(SupplierItemsMapper mapper) {
		this.mapper = mapper;
	}

	public SupplierItems getByItemIdAndCompany(long itemId, long companyId) {
		LambdaQueryWrapper<SupplierItems> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItems::getItemId, itemId).eq(SupplierItems::getCompanyId, companyId);
		return mapper.selectOne(w);
	}

	public SupplierItems findApprovedDefaultByCompanyAndGoodsId(long companyId, long goodsId) {
		LambdaQueryWrapper<SupplierItems> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItems::getCompanyId, companyId)
				.eq(SupplierItems::getGoodsId, goodsId)
				.eq(SupplierItems::getAuditStatus, "approved")
				.eq(SupplierItems::getIsDefault, true)
				.last("LIMIT 1");
		return mapper.selectOne(w);
	}

	public SupplierItems findByItemId(long itemId) {
		LambdaQueryWrapper<SupplierItems> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItems::getItemId, itemId).last("LIMIT 1");
		return mapper.selectOne(w);
	}

	public List<SupplierItems> listByDefaultItemIdAndCompany(long defaultItemId, long companyId) {
		LambdaQueryWrapper<SupplierItems> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItems::getDefaultItemId, defaultItemId).eq(SupplierItems::getCompanyId, companyId).last("LIMIT 100");
		return mapper.selectList(w);
	}

	public void insert(SupplierItems row) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (row.getCreated() == null) {
			row.setCreated(now);
		}
		if (row.getUpdated() == null) {
			row.setUpdated(now);
		}
		mapper.insert(row);
	}

	public void updateOneByItemId(long itemId, long companyId, SupplierItems patch) {
		LambdaQueryWrapper<SupplierItems> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItems::getItemId, itemId).eq(SupplierItems::getCompanyId, companyId);
		int now = (int) (System.currentTimeMillis() / 1000L);
		patch.setUpdated(now);
		mapper.update(patch, w);
	}

	public int deleteByItemIdsAndCompany(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<SupplierItems> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItems::getCompanyId, companyId).in(SupplierItems::getItemId, itemIds);
		return mapper.delete(w);
	}

	public void updateByItemIds(Collection<Long> itemIds, long defaultItemId, long goodsId) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<SupplierItems> u = new LambdaUpdateWrapper<>();
		u.in(SupplierItems::getItemId, itemIds).set(SupplierItems::getDefaultItemId, defaultItemId).set(SupplierItems::getGoodsId, goodsId).set(SupplierItems::getUpdated, now);
		mapper.update(null, u);
	}

	public void updateIsDefaultByDefaultItemId(long companyId, long defaultItemId, boolean isDefault, Collection<Long> excludeItemId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<SupplierItems> u = new LambdaUpdateWrapper<>();
		u.eq(SupplierItems::getCompanyId, companyId).eq(SupplierItems::getDefaultItemId, defaultItemId);
		if (excludeItemId != null && !excludeItemId.isEmpty()) {
			u.notIn(SupplierItems::getItemId, excludeItemId);
		}
		u.set(SupplierItems::getIsDefault, isDefault).set(SupplierItems::getUpdated, now);
		mapper.update(null, u);
	}

	public void updateIsDefaultByItemId(long companyId, long itemId, boolean isDefault) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<SupplierItems> u = new LambdaUpdateWrapper<>();
		u.eq(SupplierItems::getCompanyId, companyId).eq(SupplierItems::getItemId, itemId).set(SupplierItems::getIsDefault, isDefault).set(SupplierItems::getUpdated, now);
		mapper.update(null, u);
	}

	public SupplierItems findByItemBnAndCompany(String itemBn, long companyId) {
		if (itemBn == null || itemBn.isEmpty()) {
			return null;
		}
		LambdaQueryWrapper<SupplierItems> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItems::getItemBn, itemBn).eq(SupplierItems::getCompanyId, companyId);
		return mapper.selectOne(w);
	}

	public List<SupplierItems> listByCompanyIdAndGoodsId(long companyId, long goodsId) {
		LambdaQueryWrapper<SupplierItems> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItems::getCompanyId, companyId).eq(SupplierItems::getGoodsId, goodsId);
		return mapper.selectList(w);
	}

	public void updateAuditByGoodsId(long companyId, long goodsId, SupplierItemsAuditPatch patch) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<SupplierItems> u = new LambdaUpdateWrapper<>();
		u.eq(SupplierItems::getCompanyId, companyId).eq(SupplierItems::getGoodsId, goodsId);
		u.set(SupplierItems::getAuditStatus, patch.auditStatus())
				.set(SupplierItems::getAuditDate, patch.auditDate())
				.set(SupplierItems::getAuditReason, patch.auditReason())
				.set(SupplierItems::getUpdated, now);
		mapper.update(null, u);
	}

	public long countSupplierItemsForDistributorUpdate(
			long companyId, boolean requireNonMarket, Collection<Long> supplierSkuItemIds) {
		if (supplierSkuItemIds == null || supplierSkuItemIds.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<SupplierItems> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItems::getCompanyId, companyId).ge(SupplierItems::getSupplierId, 1);
		if (requireNonMarket) {
			w.eq(SupplierItems::getIsMarket, 0);
		}
		w.in(SupplierItems::getItemId, supplierSkuItemIds);
		return mapper.selectCount(w);
	}

	public void updateTemplatesIdByDefaultItemIdAndCompany(long defaultItemId, long companyId, int templatesId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<SupplierItems> u = new LambdaUpdateWrapper<>();
		u.eq(SupplierItems::getDefaultItemId, defaultItemId)
				.eq(SupplierItems::getCompanyId, companyId)
				.set(SupplierItems::getTemplatesId, templatesId)
				.set(SupplierItems::getUpdated, now);
		mapper.update(null, u);
	}

	/**
	 * 按 company_id + (item_id 或 default_item_id) 更新 store。
	 *
	 * @param keyItemId 请求体中的 item_id；{@code keyIsDefaultItemId} 为 true 时表示 default_item_id
	 */
	public int updateStoreByCompanyAndProductKey(long companyId, long keyItemId, boolean keyIsDefaultItemId, int store) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<SupplierItems> u = new LambdaUpdateWrapper<>();
		u.eq(SupplierItems::getCompanyId, companyId);
		if (keyIsDefaultItemId) {
			u.eq(SupplierItems::getDefaultItemId, keyItemId);
		} else {
			u.eq(SupplierItems::getItemId, keyItemId);
		}
		u.set(SupplierItems::getStore, store).set(SupplierItems::getUpdated, now);
		return mapper.update(null, u);
	}

	public List<SupplierItems> listByCompanyAndProductKey(long companyId, long keyItemId, boolean keyIsDefaultItemId) {
		LambdaQueryWrapper<SupplierItems> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItems::getCompanyId, companyId);
		if (keyIsDefaultItemId) {
			w.eq(SupplierItems::getDefaultItemId, keyItemId);
		} else {
			w.eq(SupplierItems::getItemId, keyItemId);
		}
		return mapper.selectList(w);
	}

	/** 同一公司下按 item_id 批量查询（商品详情 intro 组件等）。 */
	public List<SupplierItems> listByCompanyAndItemIds(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<SupplierItems> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItems::getCompanyId, companyId).in(SupplierItems::getItemId, itemIds);
		return mapper.selectList(w);
	}

	public long countByCompanyGoodsOrItem(
			long companyId,
			List<Long> goodsIdsOrNull,
			List<Long> itemIdsOrNull,
			Integer isMarketEqOrNull,
			List<String> auditStatusInOrNull) {
		LambdaQueryWrapper<SupplierItems> w = new LambdaQueryWrapper<>();
		w.eq(SupplierItems::getCompanyId, companyId);
		applyGoodsOrItemIdFilter(w, goodsIdsOrNull, itemIdsOrNull);
		if (isMarketEqOrNull != null) {
			w.eq(SupplierItems::getIsMarket, isMarketEqOrNull);
		}
		if (auditStatusInOrNull != null && !auditStatusInOrNull.isEmpty()) {
			w.in(SupplierItems::getAuditStatus, auditStatusInOrNull);
		}
		return mapper.selectCount(w);
	}

	public int updateAllByCompanyGoodsOrItem(
			long companyId,
			List<Long> goodsIdsOrNull,
			List<Long> itemIdsOrNull,
			Integer isMarketEqOrNull,
			List<String> auditStatusInOrNull,
			SupplierItemsUpdatePatch patch) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<SupplierItems> u = new LambdaUpdateWrapper<>();
		u.eq(SupplierItems::getCompanyId, companyId);
		applyGoodsOrItemIdFilter(u, goodsIdsOrNull, itemIdsOrNull);
		if (isMarketEqOrNull != null) {
			u.eq(SupplierItems::getIsMarket, isMarketEqOrNull);
		}
		if (auditStatusInOrNull != null && !auditStatusInOrNull.isEmpty()) {
			u.in(SupplierItems::getAuditStatus, auditStatusInOrNull);
		}
		if (patch.getPrice() != null) {
			u.set(SupplierItems::getPrice, patch.getPrice());
		}
		if (patch.getCostPrice() != null) {
			u.set(SupplierItems::getCostPrice, patch.getCostPrice());
		}
		if (patch.getMarketPrice() != null) {
			u.set(SupplierItems::getMarketPrice, patch.getMarketPrice());
		}
		if (patch.getStore() != null) {
			u.set(SupplierItems::getStore, patch.getStore());
		}
		if (patch.getRebate() != null) {
			u.set(SupplierItems::getRebate, patch.getRebate());
		}
		if (patch.getRebateType() != null) {
			u.set(SupplierItems::getRebateType, patch.getRebateType());
		}
		if (patch.getApproveStatus() != null) {
			u.set(SupplierItems::getApproveStatus, patch.getApproveStatus());
		}
		if (patch.getIsMarket() != null) {
			u.set(SupplierItems::getIsMarket, patch.getIsMarket());
		}
		if (patch.getAuditStatus() != null) {
			u.set(SupplierItems::getAuditStatus, patch.getAuditStatus());
		}
		u.set(SupplierItems::getUpdated, now);
		return mapper.update(null, u);
	}

	private static void applyGoodsOrItemIdFilter(
			LambdaQueryWrapper<SupplierItems> w, List<Long> goodsIdsOrNull, List<Long> itemIdsOrNull) {
		if (goodsIdsOrNull != null && !goodsIdsOrNull.isEmpty()) {
			if (goodsIdsOrNull.size() == 1) {
				w.eq(SupplierItems::getGoodsId, goodsIdsOrNull.get(0));
			} else {
				w.in(SupplierItems::getGoodsId, goodsIdsOrNull);
			}
			return;
		}
		if (itemIdsOrNull != null && !itemIdsOrNull.isEmpty()) {
			if (itemIdsOrNull.size() == 1) {
				w.eq(SupplierItems::getItemId, itemIdsOrNull.get(0));
			} else {
				w.in(SupplierItems::getItemId, itemIdsOrNull);
			}
		}
	}

	private static void applyGoodsOrItemIdFilter(
			LambdaUpdateWrapper<SupplierItems> u, List<Long> goodsIdsOrNull, List<Long> itemIdsOrNull) {
		if (goodsIdsOrNull != null && !goodsIdsOrNull.isEmpty()) {
			if (goodsIdsOrNull.size() == 1) {
				u.eq(SupplierItems::getGoodsId, goodsIdsOrNull.get(0));
			} else {
				u.in(SupplierItems::getGoodsId, goodsIdsOrNull);
			}
			return;
		}
		if (itemIdsOrNull != null && !itemIdsOrNull.isEmpty()) {
			if (itemIdsOrNull.size() == 1) {
				u.eq(SupplierItems::getItemId, itemIdsOrNull.get(0));
			} else {
				u.in(SupplierItems::getItemId, itemIdsOrNull);
			}
		}
	}
}
