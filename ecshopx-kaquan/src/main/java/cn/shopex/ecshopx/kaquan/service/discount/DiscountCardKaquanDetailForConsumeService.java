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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.RelItems;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardKaquanDetailForConsumeService {

	private final DiscountCardsMapper discountCardsMapper;
	private final RelItemsMapper relItemsMapper;
	private final DiscountCardsRowMapperService discountCardsRowMapperService;
	private final UserDiscountExchangeCardGoodsLookupService goodsLookupService;
	private final ObjectMapper objectMapper;

	public DiscountCardKaquanDetailForConsumeService(
			DiscountCardsMapper discountCardsMapper,
			RelItemsMapper relItemsMapper,
			DiscountCardsRowMapperService discountCardsRowMapperService,
			UserDiscountExchangeCardGoodsLookupService goodsLookupService,
			ObjectMapper objectMapper) {
		this.discountCardsMapper = discountCardsMapper;
		this.relItemsMapper = relItemsMapper;
		this.discountCardsRowMapperService = discountCardsRowMapperService;
		this.goodsLookupService = goodsLookupService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> loadDetail(long companyId, long cardId) {
		DiscountCards card = discountCardsMapper.selectOne(new LambdaQueryWrapper<DiscountCards>()
				.eq(DiscountCards::getCompanyId, companyId)
				.eq(DiscountCards::getCardId, cardId)
				.last("LIMIT 1"));
		if (card == null) {
			throw new ResourceException(KaquanDiscountCardMessages.COUPON_INVALID);
		}
		Map<String, Object> detail = new LinkedHashMap<>(discountCardsRowMapperService.toSnakeCaseMap(card));
		detail.put("text_image_list", tryParseJsonListOrEmpty(card.getTextImageList()));
		detail.put("time_limit", tryParseJsonListOrEmpty(card.getTimeLimit()));
		normalizeBoolFlags(detail);
		detail.put("rel_shops_ids", splitCommaToStringList(card.getRelShopsIds()));
		detail.put("rel_distributor_ids", splitCommaNumericList(card.getDistributorId()));
		detail.put("distributor_info", List.of());
		boolean hasRelDist = !((List<?>) detail.get("rel_distributor_ids")).isEmpty();
		detail.put("use_all_distributor", !hasRelDist);

		List<RelItems> normalRows = relItemsMapper.selectList(new LambdaQueryWrapper<RelItems>()
				.eq(RelItems::getCompanyId, companyId)
				.eq(RelItems::getCardId, cardId)
				.eq(RelItems::getItemType, "normal"));
		List<Long> normalItemIds = new ArrayList<>();
		Map<Long, Integer> useLimitByItem = new LinkedHashMap<>();
		for (RelItems r : normalRows) {
			if (r.getItemId() != null && r.getItemId() > 0L) {
				normalItemIds.add(r.getItemId());
				useLimitByItem.put(r.getItemId(), r.getUseLimit() != null ? r.getUseLimit() : 0);
			}
		}
		detail.put("rel_item_ids", new ArrayList<>(normalItemIds));
		detail.put("itemTreeLists", List.of());
		detail.put("use_all_items", normalItemIds.isEmpty() ? "true" : "false");

		List<RelItems> catRows = relItemsMapper.selectList(new LambdaQueryWrapper<RelItems>()
				.eq(RelItems::getCompanyId, companyId)
				.eq(RelItems::getCardId, cardId)
				.eq(RelItems::getItemType, "category"));
		List<Long> categoryIds = new ArrayList<>();
		for (RelItems cr : catRows) {
			if (cr.getItemId() != null) {
				categoryIds.add(cr.getItemId());
			}
		}
		detail.put("rel_category_ids", new ArrayList<>(categoryIds));
		detail.put("item_category", new ArrayList<>(categoryIds));
		if (!categoryIds.isEmpty()) {
			detail.put("use_all_items", "category");
		}

		int ub = card.getUseBound() == null ? 0 : card.getUseBound();
		if (ub == 3) {
			detail.put("use_all_items", "tag");
		} else if (ub == 4) {
			detail.put("use_all_items", "brand");
		}

		String useScenes = stringVal(detail.get("use_scenes"));
		if (useScenes.contains(",")) {
			String[] parts = useScenes.split(",");
			if (parts.length > 1) {
				detail.put("use_scenes", "QUICK");
			}
		}

		detail.put("receive", "1".equals(String.valueOf(detail.get("receive"))) || Integer.valueOf(1).equals(detail.get("receive")));

		List<String> tagIdsStr = splitCommaToStringListPreserve(card.getTagIds());
		detail.put("tag_ids", tagIdsStr);
		detail.put("rel_tag_ids", tagIdsStr);
		detail.put("tag_list", List.of());

		List<String> brandIdsStr = splitCommaToStringListPreserve(card.getBrandIds());
		detail.put("brand_ids", brandIdsStr);
		detail.put("rel_brand_ids", brandIdsStr);
		detail.put("brand_list", List.of());

		detail.put("is_active", computeIsActive(card));
		detail.put("grade_ids", splitCommaToStringListPreserve(card.getGradeIds()));
		detail.put("vip_grade_ids", splitCommaToStringListPreserve(card.getVipGradeIds()));

		applyUseBoundItemEnumeration(companyId, card, detail, ub, normalRows, useLimitByItem);

		return detail;
	}

	private void applyUseBoundItemEnumeration(
			long companyId,
			DiscountCards card,
			Map<String, Object> detail,
			int ub,
			List<RelItems> normalRows,
			Map<Long, Integer> useLimitByItem) {
		List<Map<String, Object>> relItems = new ArrayList<>();
		List<Long> itemIds = new ArrayList<>();
		switch (ub) {
			case 0 -> {
				// 全场
			}
			case 1 -> {
				for (RelItems r : normalRows) {
					if (r.getItemId() == null) {
						continue;
					}
					itemIds.add(r.getItemId());
					relItems.add(relItemRow(r.getItemId(), r.getUseLimit()));
				}
			}
			case 2 -> {
				List<RelItems> catR = relItemsMapper.selectList(new LambdaQueryWrapper<RelItems>()
						.eq(RelItems::getCompanyId, companyId)
						.eq(RelItems::getCardId, card.getCardId())
						.eq(RelItems::getItemType, "category"));
				Set<Long> uniq = new LinkedHashSet<>();
				for (RelItems cr : catR) {
					if (cr.getItemId() == null) {
						continue;
					}
					List<Long> ids = goodsLookupService.getItemIdsByCategoryTree(companyId, cr.getItemId());
					uniq.addAll(ids);
				}
				itemIds.addAll(uniq);
				int ul = card.getUseLimit() != null ? card.getUseLimit() : 0;
				for (Long id : uniq) {
					relItems.add(relItemRow(id, ul));
				}
			}
			case 3 -> {
				List<Long> tagNums = parseCommaLongs(card.getTagIds());
				if (!tagNums.isEmpty()) {
					itemIds.addAll(goodsLookupService.listItemIdsByTagIds(companyId, tagNums));
				}
				int ul = card.getUseLimit() != null ? card.getUseLimit() : 0;
				for (Long id : itemIds) {
					relItems.add(relItemRow(id, ul));
				}
			}
			case 4 -> {
				List<Long> brandNums = parseCommaLongs(card.getBrandIds());
				if (!brandNums.isEmpty()) {
					itemIds.addAll(goodsLookupService.listAllItemIdsByBrandAttributeIds(companyId, brandNums));
				}
				int ul = card.getUseLimit() != null ? card.getUseLimit() : 0;
				for (Long id : itemIds) {
					relItems.add(relItemRow(id, ul));
				}
			}
			case 5 -> {
				List<Long> excluded = new ArrayList<>();
				for (RelItems r : normalRows) {
					if (r.getItemId() != null) {
						excluded.add(r.getItemId());
					}
				}
				itemIds.addAll(goodsLookupService.listDefaultNormalItemIdsExcluding(companyId, excluded));
				int ul = card.getUseLimit() != null ? card.getUseLimit() : 0;
				for (Long id : itemIds) {
					relItems.add(relItemRow(id, ul));
				}
			}
			default -> {
				// unknown
			}
		}
		if (ub > 0 && itemIds.isEmpty()) {
			itemIds.add(-1L);
			relItems.clear();
			relItems.add(relItemRow(-1L, 0));
		}
		detail.put("rel_item_ids", itemIds.isEmpty() ? List.of() : new ArrayList<>(itemIds));
		detail.put("rel_items", relItems);
		if (ub == 0) {
			detail.put("use_all_items", "true");
		} else if (ub == 1 && !itemIds.isEmpty()) {
			detail.put("use_all_items", "false");
		}
	}

	private static Map<String, Object> relItemRow(long itemId, Integer useLimit) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("item_id", itemId);
		row.put("use_limit", useLimit != null ? useLimit : 0);
		return row;
	}

	private static boolean computeIsActive(DiscountCards card) {
		boolean active = false;
		long now = System.currentTimeMillis() / 1000L;
		if ("new_gift".equals(card.getCardType())) {
			if (DiscountCardActionValidationService.DATE_TYPE_LONG.equals(card.getDateType())) {
				int sendBegin = card.getSendBeginTime() != null ? card.getSendBeginTime() : 0;
				int beginDays = card.getBeginDate() != null ? card.getBeginDate() : 0;
				active = (long) sendBegin + 3600L * 24 * beginDays < now;
			} else if (DiscountCardActionValidationService.DATE_TYPE_SHORT.equals(card.getDateType())) {
				int bd = card.getBeginDate() != null ? card.getBeginDate() : 0;
				active = (long) bd < now;
			}
		}
		int kq = card.getKqStatus() != null ? card.getKqStatus() : 0;
		return active && kq != cn.shopex.ecshopx.kaquan.service.discount.DiscountNewGiftCardUpdateService.STATUS_INIT;
	}

	private void normalizeBoolFlags(Map<String, Object> detail) {
		detail.put("can_share", intTruthy(detail.get("can_share")));
		detail.put("can_give_friend", intTruthy(detail.get("can_give_friend")));
		String c = stringVal(detail.get("can_use_with_other_discount"));
		detail.put("can_use_with_other_discount", "true".equalsIgnoreCase(c) || "1".equals(c) ? "true" : "false");
	}

	private static boolean intTruthy(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() == 1;
		}
		String s = String.valueOf(v);
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	private List<Object> tryParseJsonListOrEmpty(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim();
		try {
			Object v = objectMapper.readValue(t, new TypeReference<Object>() {});
			if (v instanceof List<?> list) {
				return new ArrayList<>(list);
			}
			if (v instanceof Map<?, ?> m) {
				return List.of(m);
			}
		} catch (Exception ignored) {
			// legacy serialized blob — 无法解析时返回空列表
		}
		return List.of();
	}

	private static List<String> splitCommaToStringList(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim();
		if (t.startsWith(",")) {
			t = t.substring(1);
		}
		if (t.endsWith(",")) {
			t = t.substring(0, t.length() - 1);
		}
		List<String> out = new ArrayList<>();
		for (String p : t.split(",")) {
			if (StringUtils.hasText(p.trim())) {
				out.add(p.trim());
			}
		}
		return out;
	}

	private static List<String> splitCommaToStringListPreserve(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim().replaceAll("^,+|,+$", "");
		if (!StringUtils.hasText(t)) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (String p : t.split(",")) {
			if (StringUtils.hasText(p.trim())) {
				out.add(p.trim());
			}
		}
		return out;
	}

	private static List<String> splitCommaNumericList(String raw) {
		List<String> parts = splitCommaToStringList(raw);
		List<String> nums = new ArrayList<>();
		for (String p : parts) {
			try {
				Long.parseLong(p);
				nums.add(p);
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return nums;
	}

	private static List<Long> parseCommaLongs(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim().replaceAll("^,+|,+$", "");
		List<Long> out = new ArrayList<>();
		for (String p : t.split(",")) {
			if (!StringUtils.hasText(p)) {
				continue;
			}
			try {
				out.add(Long.parseLong(p.trim()));
			} catch (NumberFormatException ignored) {
				// skip
			}
		}
		return out;
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
