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

package cn.shopex.ecshopx.goods.service.operatorcart;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.operatorcart.OperatorCartSkuLoadFacade;
import cn.shopex.ecshopx.common.operatorcart.dto.OperatorCartSkuRowDto;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsRelTags;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemLogisticsStoreEnricher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OperatorCartSkuLoadFacadeImpl implements OperatorCartSkuLoadFacade {

	private static final int MAX_TAG_IDS = 100;

	private final ItemsMapper itemsMapper;
	private final DistributorItemsMapper distributorItemsMapper;
	private final ItemsRelTagsRepository itemsRelTagsRepository;
	private final OperatorCartMemberPriceApplicator operatorCartMemberPriceApplicator;
	private final ItemLogisticsStoreEnricher itemLogisticsStoreEnricher;

	public OperatorCartSkuLoadFacadeImpl(ItemsMapper itemsMapper, DistributorItemsMapper distributorItemsMapper,
			ItemsRelTagsRepository itemsRelTagsRepository,
			OperatorCartMemberPriceApplicator operatorCartMemberPriceApplicator,
			ItemLogisticsStoreEnricher itemLogisticsStoreEnricher) {
		this.itemsMapper = itemsMapper;
		this.distributorItemsMapper = distributorItemsMapper;
		this.itemsRelTagsRepository = itemsRelTagsRepository;
		this.operatorCartMemberPriceApplicator = operatorCartMemberPriceApplicator;
		this.itemLogisticsStoreEnricher = itemLogisticsStoreEnricher;
	}

	@Override
	public List<OperatorCartSkuRowDto> loadSkus(long companyId, long distributorId, long targetUserId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemId, itemIds);
		List<Items> rows = itemsMapper.selectList(w);
		Map<Long, Items> byId = rows.stream().collect(Collectors.toMap(Items::getItemId, x -> x, (a, b) -> a));
		List<OperatorCartSkuRowDto> out = new ArrayList<>(itemIds.size());
		for (Long id : itemIds) {
			Items it = byId.get(id);
			if (it == null) {
				throw new ResourceException("商品已失效");
			}
			OperatorCartSkuRowDto dto = new OperatorCartSkuRowDto();
			dto.setItemId(id);
			long def = it.getDefaultItemId() != null && it.getDefaultItemId() > 0 ? it.getDefaultItemId() : id;
			dto.setDefaultItemId(def);
			dto.setItemName(it.getItemName());
			dto.setUnitPriceFen(it.getPrice() != null ? it.getPrice() : 0);
			dto.setStore(it.getStore() != null ? it.getStore() : 0);
			dto.setStore(itemLogisticsStoreEnricher.combineEffectiveStore(
					companyId, distributorId, dto.getStore(), it));
			dto.setSaleDisabled(false);
			dto.setItemCategory(it.getItemCategory());
			dto.setBrandId(it.getBrandId());
			String st = it.getSpecialType();
			dto.setSpecialType(st != null && !st.isBlank() ? st : "normal");
			dto.setGoodsType(it.getType());
			dto.setItemType(it.getItemType());
			dto.setItemApproveStatus(it.getApproveStatus());
			dto.setIsGift(it.getIsGift());
			dto.setMarketPriceFen(it.getMarketPrice());
			dto.setBrief(it.getBrief());
			dto.setPics(it.getPics());
			dto.setCrossborderTaxRate(it.getCrossborderTaxRate());
			dto.setTaxstrategyId(it.getTaxstrategyId());
			dto.setTaxationNum(it.getTaxationNum());
			dto.setOrigincountryId(it.getOrigincountryId());
			dto.setIsMedicine(it.getIsMedicine());
			dto.setIsPrescription(it.getIsPrescription());
			dto.setStartNum(it.getStartNum());
			dto.setGoodsId(it.getGoodsId());
			out.add(dto);
		}
		return out;
	}

	@Override
	public List<OperatorCartSkuRowDto> applyDistributorSkuReplace(long companyId, long distributorId, String productModel,
			List<OperatorCartSkuRowDto> rows) {
		if (rows == null || rows.isEmpty() || distributorId <= 0 || !"standard".equals(productModel)) {
			return rows;
		}
		List<Long> ids = rows.stream().map(OperatorCartSkuRowDto::getItemId).toList();
		LambdaQueryWrapper<DistributorItems> w = new LambdaQueryWrapper<>();
		w.eq(DistributorItems::getCompanyId, companyId).eq(DistributorItems::getDistributorId, distributorId).in(DistributorItems::getItemId, ids);
		List<DistributorItems> dist = distributorItemsMapper.selectList(w);
		Map<Long, DistributorItems> bySku = new HashMap<>();
		for (DistributorItems di : dist) {
			if (di.getItemId() != null) {
				bySku.put(di.getItemId(), di);
			}
		}
		Map<Long, Integer> logisticsByItem =
				itemLogisticsStoreEnricher.mapLogisticsStoreByPlatformItemIds(companyId, ids);
		for (OperatorCartSkuRowDto r : rows) {
			DistributorItems di = bySku.get(r.getItemId());
			if (di != null && Boolean.FALSE.equals(di.getIsCanSale())) {
				r.setSaleDisabled(true);
			}
			if (di != null && di.getPrice() != null && di.getPrice() > 0) {
				r.setUnitPriceFen(di.getPrice().intValue());
			}
			if (di != null && di.getStore() != null) {
				int local = di.getStore().intValue();
				int logistics = logisticsByItem.getOrDefault(r.getItemId(), 0);
				r.setStore(itemLogisticsStoreEnricher.combineEffectiveStore(
						companyId, distributorId, local, logistics));
			}
		}
		return rows;
	}

	@Override
	public void applyMemberPrices(long companyId, long targetUserId, List<OperatorCartSkuRowDto> rows) {
		operatorCartMemberPriceApplicator.apply(companyId, targetUserId, rows);
	}

	@Override
	public List<Long> listTagIdsByItemIds(long companyId, Collection<Long> defaultItemIds) {
		if (defaultItemIds == null || defaultItemIds.isEmpty()) {
			return List.of();
		}
		List<ItemsRelTags> rels = itemsRelTagsRepository.getLists(companyId, defaultItemIds);
		Set<Long> tagIds = new LinkedHashSet<>();
		for (ItemsRelTags rt : rels) {
			if (rt.getTagId() != null) {
				tagIds.add(rt.getTagId());
			}
		}
		List<Long> list = new ArrayList<>(tagIds);
		if (list.size() > MAX_TAG_IDS) {
			return list.subList(0, MAX_TAG_IDS);
		}
		return list;
	}

	@Override
	public void fillCouponGoodsFilterLists(long companyId, List<Long> cartItemIds, Map<String, Object> filter) {
		if (cartItemIds == null || cartItemIds.isEmpty() || filter == null) {
			return;
		}
		if (hasCompleteGoodsScopeKeys(filter)) {
			return;
		}
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).in(Items::getItemId, cartItemIds);
		List<Items> rows = itemsMapper.selectList(w);
		List<Long> defaultIds = new ArrayList<>();
		List<Long> mainCats = new ArrayList<>();
		List<Integer> brands = new ArrayList<>();
		for (Items it : rows) {
			long def = it.getDefaultItemId() != null && it.getDefaultItemId() > 0 ? it.getDefaultItemId() : it.getItemId();
			defaultIds.add(def);
			String cat = it.getItemCategory();
			if (cat != null && !cat.isBlank()) {
				try {
					mainCats.add(Long.parseLong(cat.trim()));
				} catch (NumberFormatException ignored) {
				}
			}
			if (it.getBrandId() != null && it.getBrandId() > 0) {
				brands.add(it.getBrandId());
			}
		}
		filter.put("default_item_id", distinctLong(defaultIds));
		filter.put("item_main_cat_id", distinctLong(mainCats));
		filter.put("brand_id", distinctInt(brands));
	}

	private static List<Long> distinctLong(List<Long> in) {
		return new ArrayList<>(new LinkedHashSet<>(in.stream().filter(Objects::nonNull).toList()));
	}

	private static List<Integer> distinctInt(List<Integer> in) {
		return new ArrayList<>(new LinkedHashSet<>(in.stream().filter(Objects::nonNull).toList()));
	}

	@Override
	public OperatorCartSkuRowDto resolveSkuForOperatorCartAdd(long companyId, long distributorId, long itemId,
			String productModel) {
		LambdaQueryWrapper<Items> w = new LambdaQueryWrapper<>();
		w.eq(Items::getCompanyId, companyId).eq(Items::getItemId, itemId);
		Items it = itemsMapper.selectOne(w);
		if (it == null) {
			throw new ResourceException("商品不存在");
		}
		OperatorCartSkuRowDto dto = new OperatorCartSkuRowDto();
		dto.setItemId(itemId);
		long def = it.getDefaultItemId() != null && it.getDefaultItemId() > 0 ? it.getDefaultItemId() : itemId;
		dto.setDefaultItemId(def);
		dto.setItemName(it.getItemName());
		dto.setUnitPriceFen(it.getPrice() != null ? it.getPrice() : 0);
		int localStore = it.getStore() != null ? it.getStore() : 0;
		String st = it.getSpecialType();
		dto.setSpecialType(st != null && !st.isBlank() ? st : "normal");
		dto.setSaleDisabled(false);
		dto.setItemCategory(it.getItemCategory());
		dto.setBrandId(it.getBrandId());

		if (distributorId == 0L || (productModel != null && "platform".equalsIgnoreCase(productModel))) {
			dto.setStore(itemLogisticsStoreEnricher.combineEffectiveStore(
					companyId, distributorId, localStore, it));
			return dto;
		}

		LambdaQueryWrapper<DistributorItems> dw = new LambdaQueryWrapper<>();
		dw.eq(DistributorItems::getCompanyId, companyId).eq(DistributorItems::getDistributorId, distributorId)
				.eq(DistributorItems::getItemId, itemId);
		DistributorItems di = distributorItemsMapper.selectOne(dw);
		if (di != null) {
			boolean totalStore = Boolean.TRUE.equals(di.getIsTotalStore());
			if (!totalStore) {
				if (di.getStore() != null) {
					long s = di.getStore();
					if (s > Integer.MAX_VALUE) {
						localStore = Integer.MAX_VALUE;
					} else {
						localStore = (int) s;
					}
				}
				if (di.getPrice() != null && di.getPrice() > 0) {
					long p = di.getPrice();
					dto.setUnitPriceFen(p > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) p);
				}
			}
			if (Boolean.FALSE.equals(di.getIsCanSale())) {
				dto.setStore(0);
				return dto;
			}
			dto.setStore(itemLogisticsStoreEnricher.combineEffectiveStore(
					companyId, distributorId, localStore, it));
			return dto;
		}

		Integer itemDistId = it.getDistributorId();
		if (itemDistId != null && itemDistId > 0) {
			if (itemDistId.longValue() == distributorId) {
				dto.setStore(itemLogisticsStoreEnricher.combineEffectiveStore(
						companyId, distributorId, localStore, it));
				return dto;
			}
			throw new ResourceException("无效商品");
		}
		throw new ResourceException("无效商品");
	}
}
