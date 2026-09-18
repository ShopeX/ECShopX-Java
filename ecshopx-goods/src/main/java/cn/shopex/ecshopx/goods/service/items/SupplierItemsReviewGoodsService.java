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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.integration.SupplierItemsReviewGoodsExecutor;
import cn.shopex.ecshopx.common.integration.SupplierItemsSyncToPoolExecutor;
import cn.shopex.ecshopx.common.integration.SupplierItemsSyncToShopExecutor;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsRelCats;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsBarcodeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.domain.SupplierItemsAttr;
import cn.shopex.ecshopx.supplier.domain.SupplierItemsAuditPatch;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import cn.shopex.ecshopx.supplier.service.SupplierItemsAttrPersistenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SupplierItemsReviewGoodsService
		implements SupplierItemsReviewGoodsExecutor, SupplierItemsSyncToPoolExecutor, SupplierItemsSyncToShopExecutor {

	private final SupplierItemsRepository supplierItemsRepository;
	private final ItemsRepository itemsRepository;
	private final ItemStoreService itemStoreService;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ItemsRelCatsRepository itemsRelCatsRepository;
	private final ItemsBarcodeRepository itemsBarcodeRepository;
	private final SupplierItemsAttrPersistenceService supplierItemsAttrPersistenceService;
	private final ObjectMapper objectMapper;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;

	public SupplierItemsReviewGoodsService(
			SupplierItemsRepository supplierItemsRepository,
			ItemsRepository itemsRepository,
			ItemStoreService itemStoreService,
			ItemRelAttributesRepository itemRelAttributesRepository,
			ItemsRelCatsRepository itemsRelCatsRepository,
			ItemsBarcodeRepository itemsBarcodeRepository,
			SupplierItemsAttrPersistenceService supplierItemsAttrPersistenceService,
			ObjectMapper objectMapper,
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver) {
		this.supplierItemsRepository = supplierItemsRepository;
		this.itemsRepository = itemsRepository;
		this.itemStoreService = itemStoreService;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.itemsRelCatsRepository = itemsRelCatsRepository;
		this.itemsBarcodeRepository = itemsBarcodeRepository;
		this.supplierItemsAttrPersistenceService = supplierItemsAttrPersistenceService;
		this.objectMapper = objectMapper;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
	}

	@Transactional(rollbackFor = Exception.class)
	public void reviewGoods(Map<String, Object> params, long pathItemId) {
		SupplierItems supplierGoods = requireSupplierItem(pathItemId);
		long companyId = supplierGoods.getCompanyId();
		String auditStatus = str(params.get("audit_status"));
		int auditDate = (int) (System.currentTimeMillis() / 1000L);
		String auditReason = str(params.get("audit_reason"));
		supplierItemsRepository.updateAuditByGoodsId(
				companyId, supplierGoods.getGoodsId(), new SupplierItemsAuditPatch(auditStatus, auditDate, auditReason));

		params.put("supplier_id", supplierGoods.getSupplierId());
		params.put("goods_id", supplierGoods.getGoodsId());
		params.put("is_market", supplierGoods.getIsMarket());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void syncToPool(long companyId, long pathItemId) {
		SupplierItems supplierGoods = requireSupplierItem(pathItemId);
		if (supplierGoods.getCompanyId() != companyId) {
			throw new ResourceException("商品不存在或无权");
		}
		if (!"approved".equals(str(supplierGoods.getAuditStatus()))) {
			throw new ResourceException("仅审核通过的商品可同步到商品池");
		}
		syncApprovedGoodsToPool(supplierGoods, pathItemId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void syncToShop(long companyId, long distributorId, long pathItemId) {
		if (distributorId <= 0L) {
			throw new ResourceException("店铺信息无效");
		}
		if (!"platform".equalsIgnoreCase(itemsCategoryDistributorIdResolver.resolveProductModel(companyId))) {
			throw new ForbiddenException("仅 Platform 版店铺可同步供应商商品");
		}
		SupplierItems supplierGoods = requireSupplierItem(pathItemId);
		if (supplierGoods.getCompanyId() != companyId) {
			throw new ResourceException("商品不存在或无权");
		}
		if (!"approved".equals(str(supplierGoods.getAuditStatus()))) {
			throw new ResourceException("仅审核通过的商品可同步到店铺");
		}
		syncApprovedGoodsToShop(supplierGoods, pathItemId, distributorId);
	}

	private void syncApprovedGoodsToPool(SupplierItems supplierGoods, long pathItemId) {
		long companyId = supplierGoods.getCompanyId();
		long supplierGoodsId = supplierGoods.getGoodsId();
		List<SupplierItems> supplierGoodsList = supplierItemsRepository.listByCompanyIdAndGoodsId(companyId, supplierGoodsId);

		Items poolAnchor = itemsRepository.getBySupplierItemIdAndCompany(supplierGoodsId, companyId);
		long itemsGoodsId = poolAnchor != null && poolAnchor.getGoodsId() != null && poolAnchor.getGoodsId() > 0
				? poolAnchor.getGoodsId()
				: 0L;
		List<Long> oldItemIds = new ArrayList<>();
		if (itemsGoodsId > 0) {
			oldItemIds = itemsRepository.listByGoodsIds(List.of(itemsGoodsId)).stream().map(Items::getItemId).filter(Objects::nonNull).toList();
		}

		List<Long> newPoolItemIds = new ArrayList<>();
		Map<Long, String> barcodeByItemId = new LinkedHashMap<>();
		long defaultItemId = 0L;
		long unifiedGoodsId = 0L;

		for (SupplierItems v : supplierGoodsList) {
			long supplierSkuId = v.getItemId();
			Items patch = patchFromSupplierRow(v, supplierSkuId);
			patch.setDistributorId(0);
			Items rsItem = itemsRepository.getBySupplierItemIdAndCompany(supplierSkuId, companyId);
			if (rsItem == null && StringUtils.hasText(v.getItemBn())) {
				Items orphan = itemsRepository.findByItemBnAndCompany(v.getItemBn().trim(), companyId);
				if (orphan != null) {
					int linkedSupplierItemId = orphan.getSupplierItemId() != null ? orphan.getSupplierItemId() : 0;
					if (linkedSupplierItemId == 0) {
						rsItem = orphan;
					}
				}
			}
			if (rsItem != null) {
				if (rsItem.getGoodsId() != null && rsItem.getGoodsId() > 0) {
					unifiedGoodsId = rsItem.getGoodsId();
				}
				itemsRepository.updateByItemId(rsItem.getItemId(), companyId, patch);
				syncItemSpec(rsItem.getItemId(), supplierSkuId, companyId);
				if (Boolean.TRUE.equals(v.getIsDefault())) {
					defaultItemId = rsItem.getItemId();
				}
				newPoolItemIds.add(rsItem.getItemId());
				barcodeByItemId.put(rsItem.getItemId(), v.getBarcode() != null ? v.getBarcode() : "");
			} else {
				Items row = mapSupplierToNewPoolItem(v, supplierSkuId, companyId);
				itemsRepository.insert(row);
				rsItem = row;
				syncItemSpec(rsItem.getItemId(), supplierSkuId, companyId);
				if (Boolean.TRUE.equals(v.getIsDefault())) {
					defaultItemId = rsItem.getItemId();
				}
				newPoolItemIds.add(rsItem.getItemId());
				barcodeByItemId.put(rsItem.getItemId(), v.getBarcode() != null ? v.getBarcode() : "");
				if (rsItem.getGoodsId() != null && rsItem.getGoodsId() > 0) {
					unifiedGoodsId = rsItem.getGoodsId();
				}
			}
		}

		if (unifiedGoodsId == 0L) {
			unifiedGoodsId = defaultItemId;
		}
		if (defaultItemId > 0L && !newPoolItemIds.isEmpty()) {
			itemsRepository.updateByItemIds(companyId, newPoolItemIds, defaultItemId, unifiedGoodsId);
		}
		saveSyncedBarcodes(companyId, 0L, defaultItemId, barcodeByItemId);

		if (!oldItemIds.isEmpty()) {
			Set<Long> keep = new HashSet<>(newPoolItemIds);
			List<Long> removeIds = oldItemIds.stream().filter(id -> !keep.contains(id)).toList();
			if (!removeIds.isEmpty()) {
				itemsBarcodeRepository.deleteByItemIds(companyId, removeIds);
				itemsRepository.deleteByItemIdsAndCompanyId(companyId, removeIds);
				for (Long rid : removeIds) {
					itemStoreService.deleteItemStore(rid, 0L);
				}
			}
		}

		long supplierDefaultItemId = supplierGoods.getDefaultItemId() != null && supplierGoods.getDefaultItemId() > 0
				? supplierGoods.getDefaultItemId()
				: pathItemId;
		if (defaultItemId > 0L) {
			syncRelCats(companyId, defaultItemId, supplierDefaultItemId);
			syncRelBrand(companyId, defaultItemId, supplierDefaultItemId);
			syncRelParams(companyId, defaultItemId, supplierDefaultItemId);
		}
	}

	private void syncApprovedGoodsToShop(SupplierItems supplierGoods, long pathItemId, long distributorId) {
		long companyId = supplierGoods.getCompanyId();
		long supplierGoodsId = supplierGoods.getGoodsId();
		List<SupplierItems> supplierGoodsList = supplierItemsRepository.listByCompanyIdAndGoodsId(companyId, supplierGoodsId);

		Items shopAnchor = itemsRepository.getBySupplierItemIdCompanyAndDistributor(supplierGoodsId, companyId, distributorId);
		long itemsGoodsId = shopAnchor != null && shopAnchor.getGoodsId() != null && shopAnchor.getGoodsId() > 0
				? shopAnchor.getGoodsId()
				: 0L;
		List<Long> oldItemIds = new ArrayList<>();
		if (itemsGoodsId > 0) {
			oldItemIds = itemsRepository.listByGoodsIds(List.of(itemsGoodsId)).stream()
					.filter(it -> it.getDistributorId() != null && it.getDistributorId() == (int) distributorId)
					.map(Items::getItemId)
					.filter(Objects::nonNull)
					.toList();
		}

		List<Long> newShopItemIds = new ArrayList<>();
		Map<Long, String> barcodeByItemId = new LinkedHashMap<>();
		long defaultItemId = 0L;
		long unifiedGoodsId = 0L;

		for (SupplierItems v : supplierGoodsList) {
			long supplierSkuId = v.getItemId();
			Items patch = patchFromSupplierRow(v, supplierSkuId);
			patch.setDistributorId((int) distributorId);
			Items rsItem = itemsRepository.getBySupplierItemIdCompanyAndDistributor(supplierSkuId, companyId, distributorId);
			if (rsItem != null) {
				if (rsItem.getGoodsId() != null && rsItem.getGoodsId() > 0) {
					unifiedGoodsId = rsItem.getGoodsId();
				}
				itemsRepository.updateByItemId(rsItem.getItemId(), companyId, patch);
				syncItemSpec(rsItem.getItemId(), supplierSkuId, companyId);
				if (Boolean.TRUE.equals(v.getIsDefault())) {
					defaultItemId = rsItem.getItemId();
				}
				newShopItemIds.add(rsItem.getItemId());
				barcodeByItemId.put(rsItem.getItemId(), v.getBarcode() != null ? v.getBarcode() : "");
			} else {
				Items row = mapSupplierToNewShopItem(v, supplierSkuId, companyId, distributorId);
				itemsRepository.insert(row);
				rsItem = row;
				syncItemSpec(rsItem.getItemId(), supplierSkuId, companyId);
				if (Boolean.TRUE.equals(v.getIsDefault())) {
					defaultItemId = rsItem.getItemId();
				}
				newShopItemIds.add(rsItem.getItemId());
				barcodeByItemId.put(rsItem.getItemId(), v.getBarcode() != null ? v.getBarcode() : "");
				if (rsItem.getGoodsId() != null && rsItem.getGoodsId() > 0) {
					unifiedGoodsId = rsItem.getGoodsId();
				}
			}
		}

		if (unifiedGoodsId == 0L) {
			unifiedGoodsId = defaultItemId;
		}
		if (defaultItemId > 0L && !newShopItemIds.isEmpty()) {
			itemsRepository.updateByItemIds(companyId, newShopItemIds, defaultItemId, unifiedGoodsId);
		}
		saveSyncedBarcodes(companyId, distributorId, defaultItemId, barcodeByItemId);

		if (!oldItemIds.isEmpty()) {
			Set<Long> keep = new HashSet<>(newShopItemIds);
			List<Long> removeIds = oldItemIds.stream().filter(id -> !keep.contains(id)).toList();
			if (!removeIds.isEmpty()) {
				itemsBarcodeRepository.deleteByItemIds(companyId, removeIds);
				itemsRepository.deleteByItemIdsAndCompanyId(companyId, removeIds);
				for (Long rid : removeIds) {
					itemStoreService.deleteItemStore(rid, distributorId);
				}
			}
		}

		long supplierDefaultItemId = supplierGoods.getDefaultItemId() != null && supplierGoods.getDefaultItemId() > 0
				? supplierGoods.getDefaultItemId()
				: pathItemId;
		if (defaultItemId > 0L) {
			syncRelCats(companyId, defaultItemId, supplierDefaultItemId);
			syncRelBrand(companyId, defaultItemId, supplierDefaultItemId);
			syncRelParams(companyId, defaultItemId, supplierDefaultItemId);
		}
	}

	private void saveSyncedBarcodes(
			long companyId, long distributorId, long defaultItemId, Map<Long, String> barcodeByItemId) {
		if (barcodeByItemId == null || barcodeByItemId.isEmpty()) {
			return;
		}
		long defaultForBarcode = defaultItemId > 0L ? defaultItemId : 0L;
		for (Map.Entry<Long, String> e : barcodeByItemId.entrySet()) {
			Long itemId = e.getKey();
			if (itemId == null || itemId <= 0L) {
				continue;
			}
			long def = defaultForBarcode > 0L ? defaultForBarcode : itemId;
			itemsBarcodeRepository.saveBarcode(companyId, distributorId, itemId, def, e.getValue());
		}
	}

	private Items mapSupplierToNewShopItem(SupplierItems v, long supplierSkuId, long companyId, long distributorId) {
		Items row = patchFromSupplierRow(v, supplierSkuId);
		row.setCompanyId(companyId);
		row.setDistributorId((int) distributorId);
		row.setItemId(null);
		row.setGoodsId(0L);
		row.setDefaultItemId(0L);
		return row;
	}

	private Items patchFromSupplierRow(SupplierItems v, long supplierSkuId) {
		Items p = new Items();
		p.setItemType(v.getItemType());
		p.setItemCategory(v.getItemCategory());
		p.setConsumeType("every");
		boolean market = v.getIsMarket() != null && v.getIsMarket() == 1;
		p.setApproveStatus(market ? "onsale" : "instock");
		p.setItemName(v.getItemName());
		p.setItemBn(v.getItemBn());
		p.setBarcode(v.getBarcode());
		p.setBrief(v.getBrief());
		p.setPrice(v.getPrice());
		p.setCostPrice(v.getCostPrice());
		p.setMarketPrice(v.getMarketPrice());
		p.setSales(v.getSales());
		p.setRebate(v.getRebate());
		p.setRebateType(v.getRebateType());
		p.setRebateConf(v.getRebateConf());
		p.setAuditStatus(v.getAuditStatus());
		p.setAuditReason(v.getAuditReason());
		p.setGoodsFunction(v.getGoodsFunction());
		p.setGoodsSeries(v.getGoodsSeries());
		p.setGoodsColor(v.getGoodsColor());
		p.setGoodsBrand(v.getGoodsBrand());
		p.setIsDefault(v.getIsDefault());
		p.setDefaultItemId(v.getDefaultItemId());
		p.setGoodsId(v.getGoodsId());
		p.setNospec(itemsNospecString(v.getNospec()));
		p.setWeight(v.getWeight());
		p.setSort(v.getSort());
		p.setIsEpidemic(v.getIsEpidemic());
		p.setTemplatesId(v.getTemplatesId());
		p.setPics(v.getPics());
		p.setPicsCreateQrcode(v.getPicsCreateQrcode());
		p.setVideoType(v.getVideoType());
		p.setVideos(v.getVideos());
		p.setVideoPicUrl(v.getVideoPicUrl());
		p.setIntro(v.getIntro());
		p.setPurchaseAgreement(v.getPurchaseAgreement());
		p.setIsShowSpecimg(v.getIsShowSpecimg());
		p.setEnableAgreement(v.getEnableAgreement());
		p.setDateType(v.getDateType());
		p.setBeginDate(v.getBeginDate());
		p.setEndDate(v.getEndDate());
		p.setFixedTerm(v.getFixedTerm());
		p.setBrandLogo(v.getBrandLogo());
		p.setIsPoint(v.getIsPoint());
		p.setPoint(v.getPoint());
		p.setDistributorId(v.getDistributorId());
		p.setVolume(v.getVolume());
		p.setItemSource(v.getItemSource());
		p.setBrandId(v.getBrandId());
		p.setTaxRate(v.getTaxRate());
		p.setCrossborderTaxRate(v.getCrossborderTaxRate());
		p.setProfitType(v.getProfitType());
		p.setOrigincountryId(v.getOrigincountryId());
		p.setTaxstrategyId(v.getTaxstrategyId());
		p.setTaxationNum(v.getTaxationNum());
		p.setProfitFee(v.getProfitFee());
		p.setType(v.getType());
		p.setIsProfit(v.getIsProfit());
		p.setIsMedicine(v.getIsMedicine());
		p.setIsPrescription(v.getIsPrescription());
		p.setIsGift(v.getIsGift());
		p.setIsPackage(v.getIsPackage());
		p.setTdkContent(v.getTdkContent());
		p.setSupplierId(v.getSupplierId());
		p.setSupplierItemId((int) supplierSkuId);
		p.setIsMarket(v.getIsMarket());
		p.setGoodsBn(v.getGoodsBn());
		p.setSupplierGoodsBn(v.getSupplierGoodsBn());
		p.setAuditDate(v.getAuditDate());
		p.setStartNum(v.getStartNum());
		p.setSpecialType(v.getSpecialType());
		p.setItemUnit(v.getItemUnit());
		return p;
	}

	private Items mapSupplierToNewPoolItem(SupplierItems v, long supplierSkuId, long companyId) {
		Items row = patchFromSupplierRow(v, supplierSkuId);
		row.setCompanyId(companyId);
		row.setDistributorId(0);
		row.setItemId(null);
		row.setGoodsId(0L);
		row.setDefaultItemId(0L);
		return row;
	}

	private static String itemsNospecString(String raw) {
		return isNospecTrue(raw) ? "true" : "false";
	}

	private static boolean isNospecTrue(Object nospec) {
		if (nospec instanceof Boolean b) {
			return b;
		}
		if (nospec == null) {
			return false;
		}
		String s = nospec.toString();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private void syncItemSpec(long platformItemId, long supplierItemId, long companyId) {
		List<SupplierItemsAttr> rs = supplierItemsAttrPersistenceService.listByCompanyIdAndItemIdAndAttributeType(companyId, supplierItemId, "item_spec");
		if (rs == null || rs.isEmpty()) {
			return;
		}
		List<ItemRelAttributes> existing = itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, List.of(platformItemId), "item_spec");
		List<Long> oldAttrIds = existing.stream().map(ItemRelAttributes::getId).filter(Objects::nonNull).toList();
		List<Long> newAttrIds = new ArrayList<>();
		for (SupplierItemsAttr attrRow : rs) {
			if (!StringUtils.hasText(attrRow.getAttrData())) {
				continue;
			}
			Map<String, Object> attrData;
			try {
				attrData = objectMapper.readValue(attrRow.getAttrData(), new TypeReference<Map<String, Object>>() {});
			} catch (Exception e) {
				throw new ResourceException("规格数据解析失败");
			}
			Object specObj = attrData.get("item_spec");
			if (!(specObj instanceof Map<?, ?> rawSpec)) {
				continue;
			}
			Map<String, Object> itemSpec = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : rawSpec.entrySet()) {
				itemSpec.put(String.valueOf(e.getKey()), e.getValue());
			}
			long attributeId = attrRow.getAttributeId() != null && attrRow.getAttributeId() > 0
					? attrRow.getAttributeId()
					: toLong(itemSpec.containsKey("attribute_id") ? itemSpec.get("attribute_id") : itemSpec.get("spec_id"));
			if (attributeId <= 0) {
				continue;
			}
			Long specValueId = itemSpec.get("attribute_value_id") != null ? toLong(itemSpec.get("attribute_value_id"))
					: (itemSpec.get("spec_value_id") != null ? toLong(itemSpec.get("spec_value_id")) : null);
			String imageUrl = itemSpecRelImageUrlOrNull(itemSpec.get("image_url"));
			int sort = itemSpec.get("attribute_sort") != null ? toInt(itemSpec.get("attribute_sort")) : 0;
			Optional<ItemRelAttributes> found = itemRelAttributesRepository.getOne(companyId, platformItemId, attributeId, "item_spec");
			if (found.isPresent()) {
				ItemRelAttributes u = new ItemRelAttributes();
				u.setAttributeValueId(specValueId);
				if (itemSpec.get("custom_attribute_value") != null) {
					u.setCustomAttributeValue(itemSpec.get("custom_attribute_value").toString());
				}
				if (imageUrl != null) {
					u.setImageUrl(imageUrl);
				}
				u.setAttributeSort(sort);
				itemRelAttributesRepository.updateById(found.get().getId(), u);
				newAttrIds.add(found.get().getId());
			} else {
				ItemRelAttributes ins = new ItemRelAttributes();
				ins.setCompanyId(companyId);
				ins.setItemId(platformItemId);
				ins.setAttributeId(attributeId);
				ins.setAttributeType("item_spec");
				ins.setAttributeValueId(specValueId);
				ins.setAttributeSort(sort);
				ins.setImageUrl(imageUrl);
				if (itemSpec.get("custom_attribute_value") != null) {
					ins.setCustomAttributeValue(itemSpec.get("custom_attribute_value").toString());
				}
				itemRelAttributesRepository.insert(ins);
				if (ins.getId() != null) {
					newAttrIds.add(ins.getId());
				}
			}
		}
		if (!oldAttrIds.isEmpty()) {
			Set<Long> keep = new HashSet<>(newAttrIds);
			List<Long> remove = oldAttrIds.stream().filter(id -> !keep.contains(id)).toList();
			if (!remove.isEmpty()) {
				itemRelAttributesRepository.deleteByIds(remove);
			}
		}
	}

	private void syncRelCats(long companyId, long defaultItemId, long supplierDefaultItemId) {
		List<Long> newCategoryIds = loadSupplierCategoryIds(companyId, supplierDefaultItemId);
		List<ItemsRelCats> existing = itemsRelCatsRepository.listByCompanyIdAndItemId(companyId, defaultItemId);
		List<Long> oldCategoryIds = existing.stream().map(ItemsRelCats::getCategoryId).filter(Objects::nonNull).toList();
		Set<Long> newSet = new LinkedHashSet<>(newCategoryIds);
		Set<Long> oldSet = new LinkedHashSet<>(oldCategoryIds);
		for (Long cid : newSet) {
			if (cid != null && cid > 0 && !oldSet.contains(cid)) {
				int now = (int) (System.currentTimeMillis() / 1000L);
				ItemsRelCats row = new ItemsRelCats();
				row.setCompanyId(companyId);
				row.setItemId(defaultItemId);
				row.setCategoryId(cid);
				row.setCreated(now);
				row.setUpdated(now);
				itemsRelCatsRepository.insert(row);
			}
		}
		List<Long> remove = oldCategoryIds.stream().filter(id -> id != null && !newSet.contains(id)).distinct().toList();
		if (!remove.isEmpty()) {
			itemsRelCatsRepository.deleteByCompanyIdAndItemIdAndCategoryIdIn(companyId, defaultItemId, remove);
		}
	}

	private List<Long> loadSupplierCategoryIds(long companyId, long supplierDefaultItemId) {
		List<SupplierItemsAttr> rows = supplierItemsAttrPersistenceService.listByCompanyIdAndItemIdAndAttributeType(companyId, supplierDefaultItemId, "category");
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		for (SupplierItemsAttr row : rows) {
			if (!StringUtils.hasText(row.getAttrData())) {
				continue;
			}
			try {
				Map<String, Object> root = objectMapper.readValue(row.getAttrData(), new TypeReference<Map<String, Object>>() {});
				addLongIds(root.get("category"), ids);
			} catch (Exception ignored) {
			}
		}
		return new ArrayList<>(ids);
	}

	private void syncRelBrand(long companyId, long defaultItemId, long supplierDefaultItemId) {
		long brandId = loadSupplierBrandId(companyId, supplierDefaultItemId);
		if (brandId <= 0) {
			return;
		}
		ItemRelAttributes save = new ItemRelAttributes();
		save.setCompanyId(companyId);
		save.setItemId(defaultItemId);
		save.setAttributeId(brandId);
		save.setAttributeValueId(brandId);
		save.setAttributeType("brand");
		Optional<ItemRelAttributes> rs = itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, List.of(defaultItemId), "brand").stream()
				.findFirst();
		if (rs.isPresent()) {
			ItemRelAttributes patch = new ItemRelAttributes();
			patch.setAttributeId(brandId);
			patch.setAttributeValueId(brandId);
			itemRelAttributesRepository.updateById(Objects.requireNonNull(rs.get().getId()), patch);
		} else {
			itemRelAttributesRepository.insert(save);
		}
	}

	private long loadSupplierBrandId(long companyId, long supplierDefaultItemId) {
		List<SupplierItemsAttr> rows = supplierItemsAttrPersistenceService.listByCompanyIdAndItemIdAndAttributeType(companyId, supplierDefaultItemId, "brand");
		for (SupplierItemsAttr row : rows) {
			if (!StringUtils.hasText(row.getAttrData())) {
				continue;
			}
			try {
				Map<String, Object> root = objectMapper.readValue(row.getAttrData(), new TypeReference<Map<String, Object>>() {});
				Object brandVal = root.get("brand");
				if (brandVal == null) {
					brandVal = root.get("brand_id");
				}
				long id = toLong(brandVal);
				if (id > 0) {
					return id;
				}
			} catch (Exception ignored) {
			}
		}
		return 0L;
	}

	private void syncRelParams(long companyId, long defaultItemId, long supplierDefaultItemId) {
		List<Map<String, Object>> supplierParams = loadSupplierItemParams(companyId, supplierDefaultItemId);
		List<Long> newAttrIds = supplierParams.stream().map(m -> toLong(m.get("attribute_id"))).filter(id -> id > 0).toList();
		List<ItemRelAttributes> platformRows = itemRelAttributesRepository.listByCompanyItemIdsAndAttributeType(companyId, List.of(defaultItemId), "item_params");
		List<Long> oldAttrIds = platformRows.stream().map(ItemRelAttributes::getAttributeId).filter(Objects::nonNull).toList();
		for (Map<String, Object> v : supplierParams) {
			long aid = toLong(v.get("attribute_id"));
			if (aid <= 0) {
				continue;
			}
			Long valueId = v.get("attribute_value_id") != null ? toLong(v.get("attribute_value_id")) : null;
			String custom = v.get("custom_attribute_value") != null ? v.get("custom_attribute_value").toString()
					: (v.get("attribute_value_name") != null ? v.get("attribute_value_name").toString() : null);
			Optional<ItemRelAttributes> found = itemRelAttributesRepository.getOne(companyId, defaultItemId, aid, "item_params");
			if (found.isPresent()) {
				ItemRelAttributes patch = new ItemRelAttributes();
				patch.setAttributeValueId(valueId);
				patch.setCustomAttributeValue(custom);
				itemRelAttributesRepository.updateById(found.get().getId(), patch);
			} else {
				ItemRelAttributes ins = new ItemRelAttributes();
				ins.setCompanyId(companyId);
				ins.setItemId(defaultItemId);
				ins.setAttributeId(aid);
				ins.setAttributeType("item_params");
				ins.setAttributeValueId(valueId);
				ins.setCustomAttributeValue(custom);
				itemRelAttributesRepository.insert(ins);
			}
		}
		Set<Long> newIdSet = new HashSet<>(newAttrIds);
		List<Long> remove = oldAttrIds.stream().filter(id -> id != null && !newIdSet.contains(id)).distinct().toList();
		if (!remove.isEmpty()) {
			for (Long aid : remove) {
				itemRelAttributesRepository.getOne(companyId, defaultItemId, aid, "item_params").ifPresent(row -> itemRelAttributesRepository.deleteByIds(List.of(row.getId())));
			}
		}
	}

	private List<Map<String, Object>> loadSupplierItemParams(long companyId, long supplierDefaultItemId) {
		List<SupplierItemsAttr> rows = supplierItemsAttrPersistenceService.listByCompanyIdAndItemIdAndAttributeType(companyId, supplierDefaultItemId, "item_params");
		List<Map<String, Object>> out = new ArrayList<>();
		for (SupplierItemsAttr row : rows) {
			if (!StringUtils.hasText(row.getAttrData())) {
				continue;
			}
			try {
				Map<String, Object> root = objectMapper.readValue(row.getAttrData(), new TypeReference<Map<String, Object>>() {});
				Object p = root.get("params");
				if (p instanceof List<?> list) {
					for (Object el : list) {
						if (el instanceof Map<?, ?> m) {
							Map<String, Object> mm = new LinkedHashMap<>();
							for (Map.Entry<?, ?> e : m.entrySet()) {
								mm.put(String.valueOf(e.getKey()), e.getValue());
							}
							out.add(mm);
						}
					}
				}
				Object ip = root.get("item_params");
				if (ip instanceof List<?> list2) {
					for (Object el : list2) {
						if (el instanceof Map<?, ?> m) {
							Map<String, Object> mm = new LinkedHashMap<>();
							for (Map.Entry<?, ?> e : m.entrySet()) {
								mm.put(String.valueOf(e.getKey()), e.getValue());
							}
							out.add(mm);
						}
					}
				}
			} catch (Exception ignored) {
			}
		}
		return out;
	}

	private static void addLongIds(Object raw, Set<Long> ids) {
		if (raw instanceof List<?> list) {
			for (Object o : list) {
				if (o != null) {
					ids.add(toLong(o));
				}
			}
		} else if (raw != null) {
			long v = toLong(raw);
			if (v > 0) {
				ids.add(v);
			}
		}
	}

	/**
	 * {@code items_rel_attributes.image_url} is a JSON array column; serialize lists/maps with Jackson, not {@code List#toString()}.
	 */
	private String itemSpecRelImageUrlOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof List<?> list) {
			if (list.isEmpty()) {
				return null;
			}
			try {
				return objectMapper.writeValueAsString(list);
			} catch (JsonProcessingException e) {
				throw new ResourceException("规格图片数据解析失败");
			}
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty() || "[]".equals(t)) {
				return null;
			}
			if (t.startsWith("[")) {
				return t;
			}
			try {
				return objectMapper.writeValueAsString(List.of(t));
			} catch (JsonProcessingException e) {
				throw new ResourceException("规格图片数据解析失败");
			}
		}
		try {
			return objectMapper.writeValueAsString(raw);
		} catch (JsonProcessingException e) {
			throw new ResourceException("规格图片数据解析失败");
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static int toInt(Object o) {
		return (int) toLong(o);
	}

	private SupplierItems requireSupplierItem(long pathItemId) {
		SupplierItems supplierGoods = supplierItemsRepository.findByItemId(pathItemId);
		if (supplierGoods == null) {
			throw new ResourceException("商品不存在或无权");
		}
		return supplierGoods;
	}
}
