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

package cn.shopex.ecshopx.goods.repository;

import cn.shopex.ecshopx.goods.domain.ItemsMedicine;
import cn.shopex.ecshopx.goods.mapper.ItemsMedicineMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ItemsMedicineRepository {

	private final ItemsMedicineMapper mapper;

	public ItemsMedicineRepository(ItemsMedicineMapper mapper) {
		this.mapper = mapper;
	}

	public void upsertByItemId(ItemsMedicine row) {
		ItemsMedicine existing = mapper.selectById(row.getItemId());
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (existing != null) {
			row.setUpdated(now);
			mapper.updateById(row);
		} else {
			if (row.getCreated() == null) {
				row.setCreated(now);
			}
			row.setUpdated(now);
			mapper.insert(row);
		}
	}

	public Map<Long, ItemsMedicine> mapByItemIds(long companyId, Collection<Long> itemIds) {
		Map<Long, ItemsMedicine> out = new LinkedHashMap<>();
		if (itemIds == null || itemIds.isEmpty()) {
			return out;
		}
		LambdaQueryWrapper<ItemsMedicine> w = new LambdaQueryWrapper<>();
		w.eq(ItemsMedicine::getCompanyId, companyId).in(ItemsMedicine::getItemId, itemIds);
		for (ItemsMedicine m : mapper.selectList(w)) {
			if (m.getItemId() != null) {
				out.put(m.getItemId(), m);
			}
		}
		return out;
	}

	public List<ItemsMedicine> listNormalByItemIds(Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsMedicine> w = new LambdaQueryWrapper<>();
		w.in(ItemsMedicine::getItemId, itemIds).eq(ItemsMedicine::getItemType, "normal");
		return new ArrayList<>(mapper.selectList(w));
	}

	public void updateAuditStatusForNormalItems(Collection<Long> itemIds, int auditStatus) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<ItemsMedicine> u = new LambdaUpdateWrapper<>();
		u.in(ItemsMedicine::getItemId, itemIds).eq(ItemsMedicine::getItemType, "normal").set(ItemsMedicine::getAuditStatus, auditStatus).set(ItemsMedicine::getUpdated, now);
		mapper.update(null, u);
	}

	/** 仅按 {@code item_id} 更新审核结果（异步失败回写）。 */
	public void updateAuditResultByItemId(long itemId, int auditStatus, String auditReason) {
		int statusDb = auditStatus == 0 ? 3 : 2;
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<ItemsMedicine> u = new LambdaUpdateWrapper<>();
		u.eq(ItemsMedicine::getItemId, itemId).set(ItemsMedicine::getAuditStatus, statusDb).set(ItemsMedicine::getAuditReason, auditReason != null ? auditReason : "")
				.set(ItemsMedicine::getUpdated, now);
		mapper.update(null, u);
	}

	public boolean existsPrescriptionByCompanyAndItemIds(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return false;
		}
		LambdaQueryWrapper<ItemsMedicine> w = new LambdaQueryWrapper<>();
		w.eq(ItemsMedicine::getCompanyId, companyId).in(ItemsMedicine::getItemId, itemIds).eq(ItemsMedicine::getIsPrescription, 1).last("LIMIT 1");
		return mapper.selectCount(w) > 0;
	}

	public Optional<ItemsMedicine> findFirstPrescriptionPendingAuditByCompanyAndItemIds(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Optional.empty();
		}
		LambdaQueryWrapper<ItemsMedicine> w = new LambdaQueryWrapper<>();
		w.eq(ItemsMedicine::getCompanyId, companyId)
				.in(ItemsMedicine::getItemId, itemIds)
				.eq(ItemsMedicine::getIsPrescription, 1)
				.in(ItemsMedicine::getAuditStatus, List.of(1, 3))
				.last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}
}
