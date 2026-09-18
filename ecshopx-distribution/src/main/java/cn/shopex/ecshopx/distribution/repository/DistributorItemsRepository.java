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

package cn.shopex.ecshopx.distribution.repository;

import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.distribution.service.distributor.dto.DistributorItemsUpdateColumnPatch;
import cn.shopex.ecshopx.distribution.service.distributor.dto.DistributorItemsUpdateRowFilter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class DistributorItemsRepository {

	private final DistributorItemsMapper mapper;

	public DistributorItemsRepository(DistributorItemsMapper mapper) {
		this.mapper = mapper;
	}

	public int deleteByCompanyDistributorAndGoodsIds(long companyId, long distributorId, List<Long> goodsIds) {
		if (goodsIds == null || goodsIds.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.eq(DistributorItems::getCompanyId, companyId).eq(DistributorItems::getDistributorId, distributorId);
		if (goodsIds.size() == 1) {
			w.eq(DistributorItems::getGoodsId, goodsIds.get(0));
		} else {
			w.in(DistributorItems::getGoodsId, goodsIds);
		}
		LambdaQueryWrapper<DistributorItems> forSelect = new LambdaQueryWrapper<>();
		forSelect.eq(DistributorItems::getCompanyId, companyId).eq(DistributorItems::getDistributorId, distributorId);
		if (goodsIds.size() == 1) {
			forSelect.eq(DistributorItems::getGoodsId, goodsIds.get(0));
		} else {
			forSelect.in(DistributorItems::getGoodsId, goodsIds);
		}
		forSelect.select(DistributorItems::getGoodsId, DistributorItems::getDistributorId);
		List<DistributorItems> rows = mapper.selectList(forSelect);
		Set<String> seenKeys = new LinkedHashSet<>();
		List<long[]> distinctPairs = new ArrayList<>();
		for (DistributorItems r : rows) {
			Long gid = r.getGoodsId();
			Long did = r.getDistributorId();
			if (gid == null || did == null) {
				continue;
			}
			String key = gid + ":" + did;
			if (seenKeys.add(key)) {
				distinctPairs.add(new long[] {did, gid});
			}
		}
		int deleted = mapper.delete(w);
		for (long[] pair : distinctPairs) {
			applyGoodsCanSaleSync(pair[0], pair[1]);
		}
		return deleted;
	}

	public int deleteByItemIdsAndCompany(long companyId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return 0;
		}
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.eq(DistributorItems::getCompanyId, companyId).in(DistributorItems::getItemId, itemIds);
		return mapper.delete(w);
	}

	public int deleteByCompanyIdAndDefaultItemId(long companyId, long defaultItemId) {
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.eq(DistributorItems::getCompanyId, companyId).eq(DistributorItems::getDefaultItemId, defaultItemId);
		return mapper.delete(w);
	}

	public void insert(DistributorItems row) {
		long now = System.currentTimeMillis() / 1000L;
		if (row.getCreated() == null) {
			row.setCreated(now);
		}
		if (row.getUpdated() == null) {
			row.setUpdated(now);
		}
		mapper.insert(row);
	}

	public boolean existsByDistributorCompanyItem(long distributorId, long companyId, long itemId) {
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.eq(DistributorItems::getDistributorId, distributorId).eq(DistributorItems::getCompanyId, companyId).eq(DistributorItems::getItemId, itemId);
		return mapper.selectCount(w) > 0;
	}

	/**
	 * 同一公司下，给定店铺 id 列表是否在 {@code distributor_items} 中存在任意一行。
	 */
	public boolean existsAnyForCompanyAndDistributorIds(long companyId, Collection<Long> distributorIds) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return false;
		}
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.eq(DistributorItems::getCompanyId, companyId).in(DistributorItems::getDistributorId, distributorIds);
		return mapper.selectCount(w) > 0;
	}

	/**
	 * 店铺商品覆盖行：同一主商品下所有 SKU 的 item_id 及与 default_item_id 关联的行。
	 */
	public List<DistributorItems> listByCompanyDistributorAndSkuScope(long companyId, long distributorId, long defaultItemId, Collection<Long> skuItemIds) {
		if (skuItemIds == null || skuItemIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.eq(DistributorItems::getCompanyId, companyId).eq(DistributorItems::getDistributorId, distributorId)
				.and(q -> q.in(DistributorItems::getItemId, skuItemIds).or().eq(DistributorItems::getDefaultItemId, defaultItemId));
		return mapper.selectList(w);
	}

	public List<DistributorItems> listByDistributorAndItemIds(long companyId, long distributorId, Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.eq(DistributorItems::getCompanyId, companyId)
				.eq(DistributorItems::getDistributorId, distributorId)
				.in(DistributorItems::getItemId, itemIds);
		return mapper.selectList(w);
	}

	public List<DistributorItems> listByCompanyDistributorAndDefaultItemIdIn(
			long companyId, long distributorId, Collection<Long> defaultItemIds) {
		if (defaultItemIds == null || defaultItemIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.eq(DistributorItems::getCompanyId, companyId)
				.eq(DistributorItems::getDistributorId, distributorId)
				.in(DistributorItems::getDefaultItemId, defaultItemIds);
		return mapper.selectList(w);
	}

	public void updateColumnsByItemIdAndDistributor(
			long companyId,
			long distributorId,
			long itemId,
			Long defaultItemId,
			Long goodsId,
			Boolean isShow) {
		LambdaUpdateWrapper<DistributorItems> uw = new LambdaUpdateWrapper<>();
		uw.eq(DistributorItems::getCompanyId, companyId)
				.eq(DistributorItems::getDistributorId, distributorId)
				.eq(DistributorItems::getItemId, itemId);
		boolean any = false;
		if (defaultItemId != null) {
			uw.set(DistributorItems::getDefaultItemId, defaultItemId);
			any = true;
		}
		if (goodsId != null) {
			uw.set(DistributorItems::getGoodsId, goodsId);
			any = true;
		}
		if (isShow != null) {
			uw.set(DistributorItems::getIsShow, isShow);
			any = true;
		}
		if (!any) {
			return;
		}
		long now = System.currentTimeMillis() / 1000L;
		uw.set(DistributorItems::getUpdated, now);
		mapper.update(null, uw);
	}

	public void applyGoodsCanSaleSync(long distributorId, long goodsId) {
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.eq(DistributorItems::getDistributorId, distributorId)
				.eq(DistributorItems::getGoodsId, goodsId)
				.eq(DistributorItems::getIsCanSale, true);
		long cnt = mapper.selectCount(w);
		boolean goodsCanSale = cnt > 0;
		LambdaUpdateWrapper<DistributorItems> uw = new LambdaUpdateWrapper<>();
		uw.eq(DistributorItems::getDistributorId, distributorId)
				.eq(DistributorItems::getGoodsId, goodsId)
				.set(DistributorItems::getGoodsCanSale, goodsCanSale)
				.set(DistributorItems::getUpdated, System.currentTimeMillis() / 1000L);
		mapper.update(null, uw);
	}

	public List<DistributorItems> listForUpdateByFilter(DistributorItemsUpdateRowFilter filter) {
		LambdaQueryWrapper<DistributorItems> w = buildUpdateScopeWrapper(filter);
		w.select(DistributorItems::getId, DistributorItems::getDistributorId, DistributorItems::getItemId, DistributorItems::getGoodsId);
		return mapper.selectList(w);
	}

	public void updateByFilterWithGoodsCanSaleSync(DistributorItemsUpdateRowFilter filter, DistributorItemsUpdateColumnPatch patch) {
		if (!patch.anyPresent()) {
			return;
		}
		List<DistributorItems> rows = listForUpdateByFilter(filter);
		long now = System.currentTimeMillis() / 1000L;
		LinkedHashSet<String> pairKeys = new LinkedHashSet<>();
		for (DistributorItems row : rows) {
			LambdaUpdateWrapper<DistributorItems> uw = new LambdaUpdateWrapper<>();
			uw.eq(DistributorItems::getId, row.getId());
			if (patch.isCanSale().isPresent()) {
				uw.set(DistributorItems::getIsCanSale, patch.isCanSale().get());
			}
			if (patch.isTotalStore().isPresent()) {
				uw.set(DistributorItems::getIsTotalStore, patch.isTotalStore().get());
			}
			if (patch.store().isPresent()) {
				uw.set(DistributorItems::getStore, patch.store().get());
			}
			if (patch.price().isPresent()) {
				uw.set(DistributorItems::getPrice, patch.price().get());
			}
			uw.set(DistributorItems::getUpdated, now);
			mapper.update(null, uw);
			Long gid = row.getGoodsId();
			Long did = row.getDistributorId();
			if (patch.isCanSale().isPresent() && gid != null && gid > 0 && did != null) {
				pairKeys.add(did + ":" + gid);
			}
		}
		if (patch.isCanSale().isPresent()) {
			for (String key : pairKeys) {
				int colon = key.indexOf(':');
				long did = Long.parseLong(key.substring(0, colon));
				long gid = Long.parseLong(key.substring(colon + 1));
				applyGoodsCanSaleSync(did, gid);
			}
		}
	}

	public Optional<DistributorItems> findFirstNonTotalStorePriceBelowMember(
			long companyId, List<Long> distributorIds, long itemId, long memberPriceFen) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return Optional.empty();
		}
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.eq(DistributorItems::getCompanyId, companyId)
				.in(DistributorItems::getDistributorId, distributorIds)
				.eq(DistributorItems::getItemId, itemId)
				.lt(DistributorItems::getPrice, memberPriceFen)
				.eq(DistributorItems::getIsTotalStore, Boolean.FALSE)
				.orderByDesc(DistributorItems::getCreated)
				.last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	private static LambdaQueryWrapper<DistributorItems> buildUpdateScopeWrapper(DistributorItemsUpdateRowFilter filter) {
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.eq(DistributorItems::getCompanyId, filter.companyId());
		if (filter.distributorIds().size() == 1) {
			w.eq(DistributorItems::getDistributorId, filter.distributorIds().get(0));
		} else {
			w.in(DistributorItems::getDistributorId, filter.distributorIds());
		}
		if (filter.goodsId() != null) {
			w.eq(DistributorItems::getGoodsId, filter.goodsId());
		} else if (filter.goodsIds() != null && !filter.goodsIds().isEmpty()) {
			w.in(DistributorItems::getGoodsId, filter.goodsIds());
		} else if (filter.itemId() != null) {
			w.eq(DistributorItems::getItemId, filter.itemId());
		}
		return w;
	}

	public Optional<DistributorItems> findByDistributorIdAndCompanyIdAndItemId(long distributorId, long companyId, long itemId) {
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.eq(DistributorItems::getDistributorId, distributorId)
				.eq(DistributorItems::getCompanyId, companyId)
				.eq(DistributorItems::getItemId, itemId)
				.last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	/**
	 * distributor_id 为 null 时按 company_id + item_id 查询一行。
	 */
	public Optional<DistributorItems> findByDistributorIdIsNullAndCompanyIdAndItemId(long companyId, long itemId) {
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.isNull(DistributorItems::getDistributorId)
				.eq(DistributorItems::getCompanyId, companyId)
				.eq(DistributorItems::getItemId, itemId)
				.last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	public void updateColumnsByDistributorCompanyItem(
			long distributorId,
			long companyId,
			long itemId,
			Boolean isCanSale,
			Boolean isTotalStore,
			Long store,
			Long price) {
		long now = System.currentTimeMillis() / 1000L;
		LambdaUpdateWrapper<DistributorItems> uw = new LambdaUpdateWrapper<>();
		uw.eq(DistributorItems::getDistributorId, distributorId)
				.eq(DistributorItems::getCompanyId, companyId)
				.eq(DistributorItems::getItemId, itemId);
		if (isCanSale != null) {
			uw.set(DistributorItems::getIsCanSale, isCanSale);
		}
		if (isTotalStore != null) {
			uw.set(DistributorItems::getIsTotalStore, isTotalStore);
		}
		if (store != null) {
			uw.set(DistributorItems::getStore, store);
		}
		if (price != null) {
			uw.set(DistributorItems::getPrice, price);
		}
		uw.set(DistributorItems::getUpdated, now);
		mapper.update(null, uw);
	}

	public void syncGoodsCanSaleByCompanyDistributorAndDefaultItemId(
			long companyId, long distributorId, long defaultItemId) {
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.eq(DistributorItems::getCompanyId, companyId)
				.eq(DistributorItems::getDistributorId, distributorId)
				.eq(DistributorItems::getDefaultItemId, defaultItemId);
		List<DistributorItems> rows = mapper.selectList(w);
		boolean goodsCanSale = false;
		for (DistributorItems row : rows) {
			if (Boolean.TRUE.equals(row.getIsCanSale())) {
				goodsCanSale = true;
				break;
			}
		}
		long now = System.currentTimeMillis() / 1000L;
		LambdaUpdateWrapper<DistributorItems> uw = new LambdaUpdateWrapper<>();
		uw.eq(DistributorItems::getCompanyId, companyId)
				.eq(DistributorItems::getDistributorId, distributorId)
				.eq(DistributorItems::getDefaultItemId, defaultItemId)
				.set(DistributorItems::getGoodsCanSale, goodsCanSale)
				.set(DistributorItems::getUpdated, now);
		mapper.update(null, uw);
	}

	public void updateIsTotalStoreByDistributorAndGoodsGroup(long distributorId, long goodsGroupId, boolean isTotalStore) {
		long now = System.currentTimeMillis() / 1000L;
		LambdaUpdateWrapper<DistributorItems> uw = new LambdaUpdateWrapper<>();
		uw.eq(DistributorItems::getDistributorId, distributorId)
				.eq(DistributorItems::getGoodsId, goodsGroupId)
				.set(DistributorItems::getIsTotalStore, isTotalStore)
				.set(DistributorItems::getUpdated, now);
		mapper.update(null, uw);
	}

	/**
	 * 按公司与 SKU（item_id）批量更新可售标记与时间戳；{@code isCanSaleOrNullToSkip} 为 {@code null} 时不修改 {@code is_can_sale}。
	 */
	public int updateIsCanSaleAndUpdatedByCompanyAndItemIds(
			long companyId, Collection<Long> itemIds, Boolean isCanSaleOrNullToSkip, long updatedEpochSeconds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return 0;
		}
		LambdaUpdateWrapper<DistributorItems> uw = new LambdaUpdateWrapper<>();
		uw.eq(DistributorItems::getCompanyId, companyId).in(DistributorItems::getItemId, itemIds);
		if (isCanSaleOrNullToSkip != null) {
			uw.set(DistributorItems::getIsCanSale, isCanSaleOrNullToSkip);
		}
		uw.set(DistributorItems::getUpdated, updatedEpochSeconds);
		return mapper.update(null, uw);
	}
}
