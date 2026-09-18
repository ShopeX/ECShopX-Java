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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsRelGoods;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsRelGoodsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromotionGroupsRelGoodsWriteService {

	public record NormalizedGroupSkuItem(
			long itemId,
			long activityPriceFen,
			long activityStore,
			String title,
			String pic,
			String specDesc) {}

	private final PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper;
	private final PromotionGroupsRelGoodsReadService promotionGroupsRelGoodsReadService;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final PromotionGroupsActivityGroupParamValidationService promotionGroupsActivityGroupParamValidationService;
	private final MessageSource messageSource;
	private final StringRedisTemplate companysRedisTemplate;

	public PromotionGroupsRelGoodsWriteService(
			PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper,
			PromotionGroupsRelGoodsReadService promotionGroupsRelGoodsReadService,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			PromotionGroupsActivityGroupParamValidationService promotionGroupsActivityGroupParamValidationService,
			MessageSource messageSource,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.promotionGroupsRelGoodsMapper = promotionGroupsRelGoodsMapper;
		this.promotionGroupsRelGoodsReadService = promotionGroupsRelGoodsReadService;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.promotionGroupsActivityGroupParamValidationService = promotionGroupsActivityGroupParamValidationService;
		this.messageSource = messageSource;
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public List<NormalizedGroupSkuItem> normalizeAndValidateItems(
			Map<String, Object> params, long companyId, Locale locale) {
		List<RawItemInput> rawItems = parseRawItemInputs(params, locale);
		if (rawItems.isEmpty()) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.invalid_product", null, locale));
		}

		boolean legacySingleRow = !(params.get("items") instanceof List<?>);

		List<Long> requestedItemIds =
				rawItems.stream().map(RawItemInput::itemId).distinct().toList();
		if (requestedItemIds.size() != rawItems.size()) {
			throw new BadRequestException(messageSource.getMessage("promotions.groups.invalid_product", null, locale));
		}

		long catalogAnchor =
				params.get("goods_id") != null
						? PromotionGroupsActivityGroupParamValidationService.readLong(params.get("goods_id"))
						: requestedItemIds.get(0);

		// loadSkuItemsList(anchor) 只返回命中行；多规格必须用货品 goods_id / default_item_id 展开兄弟 SKU
		Set<Long> siblingItemIds = resolveSiblingItemIds(companyId, catalogAnchor, locale);
		if (siblingItemIds.isEmpty()) {
			siblingItemIds.add(catalogAnchor);
		}

		if (legacySingleRow) {
			RawItemInput legacy = rawItems.get(0);
			long resolvedItemId =
					siblingItemIds.contains(legacy.itemId())
							? legacy.itemId()
							: siblingItemIds.iterator().next();
			if (resolvedItemId != legacy.itemId()) {
				rawItems = List.of(new RawItemInput(resolvedItemId, legacy.activityPriceFen(), legacy.activityStore()));
				requestedItemIds = List.of(resolvedItemId);
			}
		}

		for (long itemId : requestedItemIds) {
			if (!siblingItemIds.contains(itemId)) {
				throw new ResourceException(messageSource.getMessage("promotions.groups.invalid_product", null, locale));
			}
		}

		Map<String, Object> requestedPack =
				marketingActivityCatalogAccess.loadSkuItemsList(companyId, requestedItemIds);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> requestedSkuList =
				(List<Map<String, Object>>) requestedPack.get("list");
		if (requestedSkuList == null || requestedSkuList.isEmpty()) {
			throw new ResourceException(messageSource.getMessage("promotions.groups.product_not_found", null, locale));
		}

		Map<Long, Map<String, Object>> skuByItemId = new LinkedHashMap<>();
		for (Map<String, Object> skuRow : requestedSkuList) {
			long itemId = PromotionGroupsActivityGroupParamValidationService.readLong(skuRow.get("item_id"));
			if (itemId > 0L) {
				skuByItemId.put(itemId, skuRow);
			}
		}
		if (skuByItemId.size() != requestedItemIds.size()) {
			throw new ResourceException(messageSource.getMessage("promotions.groups.product_not_found", null, locale));
		}

		Long goodsId = null;
		for (long itemId : requestedItemIds) {
			Map<String, Object> skuRow = skuByItemId.get(itemId);
			long rowCompanyId =
					PromotionGroupsActivityGroupParamValidationService.readLong(skuRow.get("company_id"));
			if (rowCompanyId != companyId) {
				throw new ResourceException(messageSource.getMessage("promotions.groups.product_not_found", null, locale));
			}
			Object goodsIdObj = skuRow.get("goods_id");
			if (goodsIdObj == null) {
				continue;
			}
			long rowGoodsId = PromotionGroupsActivityGroupParamValidationService.readLong(goodsIdObj);
			if (goodsId == null) {
				goodsId = rowGoodsId;
			} else if (!Objects.equals(goodsId, rowGoodsId)) {
				throw new ResourceException(messageSource.getMessage("promotions.groups.invalid_product", null, locale));
			}
		}

		if (marketingActivityCatalogAccess.anyGiftItem(companyId, requestedItemIds)) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.gift_product_check_error", null, locale));
		}

		List<NormalizedGroupSkuItem> normalized = new ArrayList<>();
		for (RawItemInput raw : rawItems) {
			Map<String, Object> skuRow = skuByItemId.get(raw.itemId());
			long salePriceFen = PromotionGroupsActivityGroupParamValidationService.readLong(skuRow.get("price"));
			if (raw.activityPriceFen() > salePriceFen) {
				throw new ResourceException(
						messageSource.getMessage(
								"promotions.groups.group_price_cannot_greater_than_sale_price", null, locale));
			}
			normalized.add(
					new NormalizedGroupSkuItem(
							raw.itemId(),
							raw.activityPriceFen(),
							raw.activityStore(),
							resolveTitle(skuRow),
							resolvePic(skuRow),
							resolveSpecDesc(skuRow)));
		}
		return normalized;
	}

	/**
	 * Hard-deletes all rel rows and per-SKU (plus legacy) Redis store keys for an activity.
	 */
	public void cleanupRelRowsAndRedisKeys(long companyId, long actId) {
		List<PromotionGroupsRelGoods> relRows =
				promotionGroupsRelGoodsReadService.listByActivityId(companyId, actId);
		for (PromotionGroupsRelGoods rel : relRows) {
			Long itemId = rel.getItemId();
			if (itemId != null && itemId > 0L) {
				companysRedisTemplate.delete(redisStoreKey(actId, itemId));
			}
		}
		companysRedisTemplate.delete(legacyRedisStoreKey(actId));
		if (!relRows.isEmpty()) {
			promotionGroupsRelGoodsMapper.delete(
					new LambdaQueryWrapper<PromotionGroupsRelGoods>()
							.eq(PromotionGroupsRelGoods::getCompanyId, companyId)
							.eq(PromotionGroupsRelGoods::getGroupsActivityId, actId));
		}
	}

	/**
	 * Replaces all rel rows for an activity and refreshes per-SKU Redis store keys.
	 * <p>Callers must invoke within {@link org.springframework.transaction.annotation.Transactional}
	 * so DB writes roll back atomically on failure. Redis updates are best-effort after successful DB
	 * writes (order: DB first, then Redis); no distributed transaction is required.
	 */
	public void replaceRelGoods(long actId, long companyId, List<NormalizedGroupSkuItem> items, int now) {
		List<PromotionGroupsRelGoods> existing =
				promotionGroupsRelGoodsMapper.selectList(
						new LambdaQueryWrapper<PromotionGroupsRelGoods>()
								.eq(PromotionGroupsRelGoods::getCompanyId, companyId)
								.eq(PromotionGroupsRelGoods::getGroupsActivityId, actId));

		Set<Long> oldItemIds =
				existing.stream()
						.map(PromotionGroupsRelGoods::getItemId)
						.filter(Objects::nonNull)
						.collect(Collectors.toCollection(LinkedHashSet::new));
		Set<Long> newItemIds =
				items.stream()
						.map(NormalizedGroupSkuItem::itemId)
						.collect(Collectors.toCollection(LinkedHashSet::new));
		Set<Long> removedItemIds = new LinkedHashSet<>(oldItemIds);
		removedItemIds.removeAll(newItemIds);

		promotionGroupsRelGoodsMapper.delete(
				new LambdaQueryWrapper<PromotionGroupsRelGoods>()
						.eq(PromotionGroupsRelGoods::getCompanyId, companyId)
						.eq(PromotionGroupsRelGoods::getGroupsActivityId, actId));

		for (NormalizedGroupSkuItem item : items) {
			PromotionGroupsRelGoods rel = new PromotionGroupsRelGoods();
			rel.setGroupsActivityId(actId);
			rel.setCompanyId(companyId);
			rel.setItemId(item.itemId());
			rel.setItemTitle(item.title());
			rel.setItemPic(item.pic());
			rel.setItemSpecDesc(item.specDesc());
			rel.setActivityPrice(item.activityPriceFen());
			rel.setActivityStore(item.activityStore());
			rel.setSalesStore(0L);
			rel.setIsShow(true);
			rel.setCreated(now);
			rel.setUpdated(now);
			promotionGroupsRelGoodsMapper.insert(rel);

			companysRedisTemplate
					.opsForValue()
					.set(redisStoreKey(actId, item.itemId()), String.valueOf(item.activityStore()));
		}

		for (long removedItemId : removedItemIds) {
			companysRedisTemplate.delete(redisStoreKey(actId, removedItemId));
		}

		companysRedisTemplate.delete(legacyRedisStoreKey(actId));
	}

	/**
	 * Updates activity price and store in place for an unchanged SKU set.
	 * <p>Callers must invoke within {@link org.springframework.transaction.annotation.Transactional}
	 * so DB writes roll back atomically on failure. Redis updates are best-effort after successful DB
	 * writes (order: DB first, then Redis); no distributed transaction is required.
	 */
	public void updatePricesAndStoresInPlace(
			long actId, long companyId, List<NormalizedGroupSkuItem> items, int now, Locale locale) {
		List<PromotionGroupsRelGoods> existing =
				promotionGroupsRelGoodsMapper.selectList(
						new LambdaQueryWrapper<PromotionGroupsRelGoods>()
								.eq(PromotionGroupsRelGoods::getCompanyId, companyId)
								.eq(PromotionGroupsRelGoods::getGroupsActivityId, actId));

		Set<Long> existingIds =
				existing.stream()
						.map(PromotionGroupsRelGoods::getItemId)
						.filter(Objects::nonNull)
						.collect(Collectors.toCollection(LinkedHashSet::new));
		Set<Long> requestedIds =
				items.stream().map(NormalizedGroupSkuItem::itemId).collect(Collectors.toCollection(LinkedHashSet::new));
		if (!existingIds.equals(requestedIds)) {
			throw new ResourceException(
					messageSource.getMessage("promotions.groups.invalid_product", null, locale));
		}

		Map<Long, NormalizedGroupSkuItem> byItemId = new LinkedHashMap<>();
		for (NormalizedGroupSkuItem item : items) {
			byItemId.put(item.itemId(), item);
		}

		for (PromotionGroupsRelGoods row : existing) {
			long itemId = row.getItemId() == null ? 0L : row.getItemId();
			NormalizedGroupSkuItem item = byItemId.get(itemId);
			if (item == null) {
				continue;
			}
			promotionGroupsRelGoodsMapper.update(
					null,
					new LambdaUpdateWrapper<PromotionGroupsRelGoods>()
							.eq(PromotionGroupsRelGoods::getId, row.getId())
							.set(PromotionGroupsRelGoods::getActivityPrice, item.activityPriceFen())
							.set(PromotionGroupsRelGoods::getActivityStore, item.activityStore())
							.set(PromotionGroupsRelGoods::getUpdated, now));

			companysRedisTemplate
					.opsForValue()
					.set(redisStoreKey(actId, itemId), String.valueOf(item.activityStore()));
		}

		companysRedisTemplate.delete(legacyRedisStoreKey(actId));
	}

	private Set<Long> resolveSiblingItemIds(long companyId, long catalogAnchor, Locale locale) {
		Map<String, Object> anchorPack =
				marketingActivityCatalogAccess.loadSkuItemsList(companyId, List.of(catalogAnchor));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> anchorList =
				anchorPack == null ? null : (List<Map<String, Object>>) anchorPack.get("list");
		if (anchorList == null || anchorList.isEmpty()) {
			throw new ResourceException(messageSource.getMessage("promotions.groups.product_not_found", null, locale));
		}
		Map<String, Object> anchorRow = anchorList.get(0);
		long spuGoodsId =
				PromotionGroupsActivityGroupParamValidationService.readLong(anchorRow.get("goods_id"));
		if (spuGoodsId <= 0L) {
			spuGoodsId =
					PromotionGroupsActivityGroupParamValidationService.readLong(anchorRow.get("default_item_id"));
		}
		if (spuGoodsId <= 0L) {
			spuGoodsId = catalogAnchor;
		}

		Set<Long> siblingItemIds = new LinkedHashSet<>();
		List<Long> byGoodsId =
				marketingActivityCatalogAccess.listItemIdsByGoodsId(companyId, spuGoodsId);
		if (byGoodsId != null) {
			for (Long id : byGoodsId) {
				if (id != null && id > 0L) {
					siblingItemIds.add(id);
				}
			}
		}
		if (siblingItemIds.isEmpty()) {
			// fallback: seed by default_item_id family via SKU list when goods_id query returns empty
			long defaultItemId =
					PromotionGroupsActivityGroupParamValidationService.readLong(anchorRow.get("default_item_id"));
			if (defaultItemId <= 0L) {
				defaultItemId = catalogAnchor;
			}
			Map<String, Object> familyPack =
					marketingActivityCatalogAccess.loadSkuItemsList(companyId, List.of(defaultItemId));
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> familyList =
					familyPack == null ? null : (List<Map<String, Object>>) familyPack.get("list");
			if (familyList != null) {
				for (Map<String, Object> skuRow : familyList) {
					long siblingItemId =
							PromotionGroupsActivityGroupParamValidationService.readLong(skuRow.get("item_id"));
					if (siblingItemId > 0L) {
						siblingItemIds.add(siblingItemId);
					}
				}
			}
		}
		if (siblingItemIds.isEmpty()) {
			long anchorItemId =
					PromotionGroupsActivityGroupParamValidationService.readLong(anchorRow.get("item_id"));
			siblingItemIds.add(anchorItemId > 0L ? anchorItemId : catalogAnchor);
		}
		return siblingItemIds;
	}

	private List<RawItemInput> parseRawItemInputs(Map<String, Object> params, Locale locale) {
		Object itemsRaw = params.get("items");
		if (itemsRaw instanceof List<?> itemsList) {
			if (itemsList.isEmpty()) {
				throw new BadRequestException(messageSource.getMessage("promotions.groups.invalid_product", null, locale));
			}
			List<RawItemInput> parsed = new ArrayList<>();
			for (Object entry : itemsList) {
				if (!(entry instanceof Map<?, ?> rowMap)) {
					throw new BadRequestException(
							messageSource.getMessage("promotions.groups.invalid_product", null, locale));
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> row = (Map<String, Object>) rowMap;
				long itemId =
						promotionGroupsActivityGroupParamValidationService.readPositiveLongParam(
								row.get("item_id"), "promotions.groups.goods_id_required", locale);
				BigDecimal actPriceYuan =
						promotionGroupsActivityGroupParamValidationService.requireActPriceYuan(
								row.get("act_price"), locale);
				long actPriceFen =
						actPriceYuan.movePointRight(2).setScale(0, RoundingMode.DOWN).longValueExact();
				long store =
						promotionGroupsActivityGroupParamValidationService.readNonNegativeLong(
								row.get("store"), "promotions.groups.store_invalid", locale);
				parsed.add(new RawItemInput(itemId, actPriceFen, store));
			}
			return parsed;
		}

		long goodsId =
				promotionGroupsActivityGroupParamValidationService.readPositiveLongParam(
						params.get("goods_id"), "promotions.groups.goods_id_required", locale);
		BigDecimal actPriceYuan =
				promotionGroupsActivityGroupParamValidationService.requireActPriceYuan(params.get("act_price"), locale);
		long actPriceFen = actPriceYuan.movePointRight(2).setScale(0, RoundingMode.DOWN).longValueExact();
		long store =
				promotionGroupsActivityGroupParamValidationService.readNonNegativeLong(
						params.get("store"), "promotions.groups.store_invalid", locale);
		return List.of(new RawItemInput(goodsId, actPriceFen, store));
	}

	private static String resolveTitle(Map<String, Object> skuRow) {
		String title = Objects.toString(skuRow.get("item_name"), "").trim();
		return StringUtils.hasText(title) ? title : "";
	}

	private static String resolvePic(Map<String, Object> skuRow) {
		String pic = Objects.toString(skuRow.get("pics"), "").trim();
		return StringUtils.hasText(pic) ? pic : "";
	}

	private static String resolveSpecDesc(Map<String, Object> skuRow) {
		String specDesc = Objects.toString(skuRow.get("item_spec_desc"), "").trim();
		if (StringUtils.hasText(specDesc)) {
			return specDesc;
		}
		return Objects.toString(skuRow.get("spec_desc"), "").trim();
	}

	private static String redisStoreKey(long actId, long itemId) {
		return "group_item_store:" + actId + ":" + itemId;
	}

	private static String legacyRedisStoreKey(long actId) {
		return "group_item_store:" + actId;
	}

	private record RawItemInput(long itemId, long activityPriceFen, long activityStore) {}
}
