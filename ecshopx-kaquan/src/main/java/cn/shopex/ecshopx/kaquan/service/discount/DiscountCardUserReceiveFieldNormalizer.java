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

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.RelItems;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.util.UserDiscountUseConditionLegacyCodec;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * Normalizes discount card template fields for a single user receive (same rules as package grant flow).
 *
 * <p>Aligns with PHP {@code UserDiscountService::__getCardInfo} → {@code getKaquanDetail} +
 * {@code _handlerCardInfo}: load item/category scope from {@code kaquan_rel_items}, then format
 * {@code rel_item_ids} by {@code use_bound}.
 */
public final class DiscountCardUserReceiveFieldNormalizer {

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private DiscountCardUserReceiveFieldNormalizer() {}

	public static Map<String, Object> normalize(
			DiscountCardsRowMapperService rowMapper, ObjectMapper objectMapper, DiscountCards entity) {
		return normalize(rowMapper, objectMapper, entity, null);
	}

	public static Map<String, Object> normalize(
			DiscountCardsRowMapperService rowMapper,
			ObjectMapper objectMapper,
			DiscountCards entity,
			RelItemsMapper relItemsMapper) {
		Map<String, Object> cardInfo = new LinkedHashMap<>(rowMapper.toSnakeCaseMap(entity));

		int discount = intFrom(cardInfo.get("discount"), 0);
		int least = intFrom(cardInfo.get("least_cost"), 0);
		int reduce = intFrom(cardInfo.get("reduce_cost"), 0);
		int most = intFrom(cardInfo.get("most_cost"), 0);
		if (most <= 0) {
			most = 99999900;
		}
		cardInfo.put("discount", discount);
		cardInfo.put("least_cost", least);
		cardInfo.put("reduce_cost", reduce);
		cardInfo.put("most_cost", most);

		Map<String, Object> useCondition = UserDiscountUseConditionLegacyCodec.buildFromCardInfo(cardInfo, least);
		cardInfo.put("use_condition", UserDiscountUseConditionLegacyCodec.serialize(useCondition));

		parseRelDistributorIds(cardInfo);
		normalizeRelShopsIdsToList(cardInfo);
		applyUseAllShops(cardInfo);

		fillRelScopeFromKaquanRelItems(cardInfo, entity, relItemsMapper);
		applyUseBoundRelItemIds(cardInfo);

		if ("new_gift".equals(String.valueOf(cardInfo.get("card_type")))) {
			cardInfo.put("distributor_id", "");
			cardInfo.put("rel_item_ids", "");
		}

		applyFixTermDates(cardInfo);
		return cardInfo;
	}

	/**
	 * PHP {@code DiscountCardService::getKaquanDetail}: load normal item ids and category ids from
	 * {@code kaquan_rel_items} onto the card snapshot before {@code _handlerCardInfo} formatting.
	 */
	public static void fillRelScopeFromKaquanRelItems(
			Map<String, Object> cardInfo, DiscountCards entity, RelItemsMapper relItemsMapper) {
		if (cardInfo == null || entity == null || relItemsMapper == null) {
			return;
		}
		Long companyId = entity.getCompanyId();
		Long cardId = entity.getCardId();
		if (companyId == null || cardId == null || companyId <= 0L || cardId <= 0L) {
			return;
		}

		List<Long> normalItemIds = listRelItemIds(relItemsMapper, companyId, cardId, "normal");
		cardInfo.put("rel_item_ids", normalItemIds);

		List<Long> categoryIds = listRelItemIds(relItemsMapper, companyId, cardId, "category");
		cardInfo.put("item_category", categoryIds);
		cardInfo.put("rel_category_ids", new ArrayList<>(categoryIds));
	}

	/** PHP {@code UserDiscountService::_handlerCardInfo} use_bound switch for {@code rel_item_ids}. */
	public static void applyUseBoundRelItemIds(Map<String, Object> cardInfo) {
		int ubInt = intFrom(cardInfo.get("use_bound"), 0);
		cardInfo.put("use_bound", ubInt);
		switch (ubInt) {
			case 0 -> cardInfo.put("rel_item_ids", "all");
			case 1, 5 -> cardInfo.put("rel_item_ids", commaJoin(cardInfo.get("rel_item_ids")));
			case 2 -> cardInfo.put("rel_item_ids", commaJoin(cardInfo.get("item_category")));
			case 3 -> cardInfo.put("rel_item_ids", commaJoin(cardInfo.get("tag_ids")));
			case 4 -> cardInfo.put("rel_item_ids", commaJoin(cardInfo.get("brand_ids")));
			default -> cardInfo.put("rel_item_ids", commaJoin(cardInfo.get("rel_item_ids")));
		}
	}

	private static List<Long> listRelItemIds(
			RelItemsMapper relItemsMapper, long companyId, long cardId, String itemType) {
		List<RelItems> rows =
				relItemsMapper.selectList(
						new LambdaQueryWrapper<RelItems>()
								.eq(RelItems::getCompanyId, companyId)
								.eq(RelItems::getCardId, cardId)
								.eq(RelItems::getItemType, itemType));
		List<Long> ids = new ArrayList<>();
		if (rows == null || rows.isEmpty()) {
			return ids;
		}
		for (RelItems row : rows) {
			if (row.getItemId() != null && row.getItemId() > 0L) {
				ids.add(row.getItemId());
			}
		}
		return ids;
	}

	private static void parseRelDistributorIds(Map<String, Object> cardInfo) {
		Object distRaw = cardInfo.get("distributor_id");
		if (distRaw == null || !StringUtils.hasText(String.valueOf(distRaw))) {
			return;
		}
		List<String> relDistributorIds = new ArrayList<>();
		for (String part : String.valueOf(distRaw).split(",")) {
			String t = part.trim();
			if (StringUtils.hasText(t) && t.chars().allMatch(Character::isDigit)) {
				relDistributorIds.add(t);
			}
		}
		cardInfo.put("rel_distributor_ids", relDistributorIds);
	}

	private static void normalizeRelShopsIdsToList(Map<String, Object> cardInfo) {
		Object relShops = cardInfo.get("rel_shops_ids");
		if (relShops instanceof List<?>) {
			return;
		}
		if (relShops instanceof String rs && StringUtils.hasText(rs)) {
			List<String> ids = new ArrayList<>();
			for (String part : rs.split(",")) {
				String t = part.trim();
				if (StringUtils.hasText(t)) {
					ids.add(t);
				}
			}
			cardInfo.put("rel_shops_ids", ids.isEmpty() ? List.of() : ids);
			return;
		}
		if (relShops == null || !StringUtils.hasText(String.valueOf(relShops))) {
			cardInfo.put("rel_shops_ids", List.of());
		}
	}

	private static void applyUseAllShops(Map<String, Object> cardInfo) {
		if (isUseAllShopsTrue(cardInfo.get("use_all_shops"))) {
			cardInfo.put("rel_shops_ids", "all");
			cardInfo.put("distributor_id", "all");
			return;
		}
		Object shopIdsRaw = cardInfo.get("rel_shops_ids");
		List<?> shopIds = shopIdsRaw instanceof List<?> list ? list : List.of();
		cardInfo.put(
				"rel_shops_ids",
				!shopIds.isEmpty()
						? "," + shopIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ","
						: "all");

		Object distIdsRaw = cardInfo.get("rel_distributor_ids");
		List<?> distributorIds = distIdsRaw instanceof List<?> list ? list : List.of();
		cardInfo.put(
				"distributor_id",
				!distributorIds.isEmpty()
						? "," + distributorIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ","
						: "all");
	}

	private static boolean isUseAllShopsTrue(Object raw) {
		if (raw == null) {
			return false;
		}
		String s = String.valueOf(raw).trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
	}

	private static void applyFixTermDates(Map<String, Object> cardInfo) {
		String dateType = String.valueOf(cardInfo.getOrDefault("date_type", ""));
		if (!"DATE_TYPE_FIX_TERM".equals(dateType)
				&& !DiscountCardActionValidationService.DATE_TYPE_LONG.equals(dateType)) {
			return;
		}
		int beginDate = intFrom(cardInfo.get("begin_date"), 0);
		if (beginDate >= 10000) {
			return;
		}
		long dayOffset = beginDate;
		ZonedDateTime beginDay =
				LocalDate.now(SHANGHAI).plusDays(dayOffset).atStartOfDay(SHANGHAI);
		int beginTs = (int) Math.min(beginDay.toEpochSecond(), Integer.MAX_VALUE);
		cardInfo.put("begin_date", beginTs);

		int endDate = intFrom(cardInfo.get("end_date"), 0);
		if (endDate > 0) {
			return;
		}
		int fixedTerm = intFrom(cardInfo.get("fixed_term"), 0);
		Instant beginInstant = Instant.ofEpochSecond(beginTs);
		LocalDate endDay = beginInstant.atZone(SHANGHAI).toLocalDate().plusDays(fixedTerm);
		ZonedDateTime endOfDay = endDay.atTime(23, 59, 59).atZone(SHANGHAI);
		cardInfo.put("end_date", (int) Math.min(endOfDay.toEpochSecond(), Integer.MAX_VALUE));
	}

	private static String commaJoin(Object raw) {
		if (raw == null) {
			return "," + ",";
		}
		if (raw instanceof List<?> list) {
			return "," + list.stream().map(String::valueOf).collect(Collectors.joining(",")) + ",";
		}
		String s = String.valueOf(raw).trim();
		if (!s.startsWith(",")) {
			s = "," + s;
		}
		if (!s.endsWith(",")) {
			s = s + ",";
		}
		return s;
	}

	private static int intFrom(Object raw, int def) {
		if (raw == null) {
			return def;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
