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

package cn.shopex.ecshopx.goods.service.discount;

import cn.shopex.ecshopx.common.discount.DiscountCardKaquanDetailForUserLoadService;
import cn.shopex.ecshopx.common.discount.DiscountCardKaquanDetailLoadService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsListFilter;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.RelItems;
import cn.shopex.ecshopx.kaquan.domain.RelMemberTags;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelMemberTagsMapper;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardActionValidationService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardSerializedFieldDecodeService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardsMultiLangReadService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardsRowMapperService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountNewGiftCardCreateService;
import cn.shopex.ecshopx.kaquan.service.discount.KaquanDiscountCardMessages;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardKaquanDetailFacadeService
		implements DiscountCardKaquanDetailLoadService, DiscountCardKaquanDetailForUserLoadService {

	private static final int FOR_TAG_ITEMS = 3;
	private static final int FOR_BRAND_ITEMS = 4;

	private final DiscountCardsMapper discountCardsMapper;
	private final RelItemsMapper relItemsMapper;
	private final RelMemberTagsMapper relMemberTagsMapper;
	private final DiscountCardsRowMapperService discountCardsRowMapperService;
	private final DiscountCardSerializedFieldDecodeService discountCardSerializedFieldDecodeService;
	private final DiscountCardsMultiLangReadService discountCardsMultiLangReadService;
	private final DistributorListQueryService distributorListQueryService;
	private final DiscountCardKaquanDetailItemsService discountCardKaquanDetailItemsService;
	private final ItemsTagsRepository itemsTagsRepository;
	private final ItemsAttributesRepository itemsAttributesRepository;

	public DiscountCardKaquanDetailFacadeService(
			DiscountCardsMapper discountCardsMapper,
			RelItemsMapper relItemsMapper,
			RelMemberTagsMapper relMemberTagsMapper,
			DiscountCardsRowMapperService discountCardsRowMapperService,
			DiscountCardSerializedFieldDecodeService discountCardSerializedFieldDecodeService,
			DiscountCardsMultiLangReadService discountCardsMultiLangReadService,
			DistributorListQueryService distributorListQueryService,
			DiscountCardKaquanDetailItemsService discountCardKaquanDetailItemsService,
			ItemsTagsRepository itemsTagsRepository,
			ItemsAttributesRepository itemsAttributesRepository) {
		this.discountCardsMapper = discountCardsMapper;
		this.relItemsMapper = relItemsMapper;
		this.relMemberTagsMapper = relMemberTagsMapper;
		this.discountCardsRowMapperService = discountCardsRowMapperService;
		this.discountCardSerializedFieldDecodeService = discountCardSerializedFieldDecodeService;
		this.discountCardsMultiLangReadService = discountCardsMultiLangReadService;
		this.distributorListQueryService = distributorListQueryService;
		this.discountCardKaquanDetailItemsService = discountCardKaquanDetailItemsService;
		this.itemsTagsRepository = itemsTagsRepository;
		this.itemsAttributesRepository = itemsAttributesRepository;
	}

	@Override
	public Map<String, Object> loadDetailForAdmin(long companyId, String cardIdRaw, String distributorIdRaw) {
		long cardId = Long.parseLong(cardIdRaw.trim());
		String distRaw = StringUtils.hasText(distributorIdRaw) ? distributorIdRaw.trim() : null;
		return loadDetailInternal(companyId, cardId, distRaw);
	}

	@Override
	public Map<String, Object> loadDetailForUserCard(long companyId, long cardId) {
		return loadDetailInternal(companyId, cardId, null);
	}

	private Map<String, Object> loadDetailInternal(long companyId, long cardId, String adminDistributorIdRawOrNull) {
		LambdaQueryWrapper<DiscountCards> w = new LambdaQueryWrapper<>();
		w.eq(DiscountCards::getCompanyId, companyId).eq(DiscountCards::getCardId, cardId);
		if (StringUtils.hasText(adminDistributorIdRawOrNull)) {
			w.eq(DiscountCards::getDistributorId, "," + adminDistributorIdRawOrNull + ",");
		}
		DiscountCards entity = discountCardsMapper.selectOne(w);
		if (entity == null) {
			throw new ResourceException(KaquanDiscountCardMessages.COUPON_INVALID);
		}

		Map<String, Object> detail = new LinkedHashMap<>(discountCardsRowMapperService.toSnakeCaseMap(entity));

		Object decodedText = discountCardSerializedFieldDecodeService.decodeTextImageList(entity.getTextImageList());
		detail.put("text_image_list", decodedText);
		List<Map<String, Object>> decodedTl = discountCardSerializedFieldDecodeService.decodeTimeLimit(entity.getTimeLimit());
		detail.put("time_limit", decodedTl.isEmpty() ? null : decodedTl);

		detail.put("can_share", isOne(detail.get("can_share")));
		detail.put("can_give_friend", isOne(detail.get("can_give_friend")));
		detail.put("can_use_with_other_discount", isOne(detail.get("can_use_with_other_discount")) ? "true" : "false");

		Object recvRaw = detail.get("receive");
		boolean receiveBool =
				(recvRaw instanceof Number n && n.intValue() == 1)
						|| "1".equals(recvRaw == null ? null : String.valueOf(recvRaw).trim());
		detail.put("receive", receiveBool);

		List<String> relShopList = splitCommaStrings(entity.getRelShopsIds());
		detail.remove("rel_shops_ids");
		detail.put("rel_shops_ids", relShopList);

		String distributorRaw = entity.getDistributorId() == null ? "" : entity.getDistributorId();
		List<String> distributorTokens = splitCommaPreserveOrder(distributorRaw);
		List<String> relDistributorIds = new ArrayList<>();
		for (String v : distributorTokens) {
			if (v != null && isNumericToken(v)) {
				relDistributorIds.add(v.trim());
			}
		}
		detail.remove("distributor_id");
		detail.put("rel_distributor_ids", relDistributorIds.isEmpty() ? null : relDistributorIds);

		detail.put("use_all_shops", useAllShopsToTriStateString(detail.get("use_all_shops")));

		discountCardsMultiLangReadService.overlay(companyId, cardId, detail);

		List<Object> distributorInfo = new ArrayList<>();
		if (!relDistributorIds.isEmpty()) {
			List<Long> idLongs = relDistributorIds.stream().map(Long::parseLong).toList();
			List<Distributor> distRows = distributorListQueryService.listByIdsAndCompany(companyId, idLongs);
			Map<Long, Distributor> byId =
					distRows.stream().collect(Collectors.toMap(Distributor::getDistributorId, d -> d, (a, b) -> a));
			for (String token : distributorTokens) {
				if (!isNumericToken(token)) {
					continue;
				}
				long did = Long.parseLong(token.trim());
				Distributor d = byId.get(did);
				if (d != null) {
					distributorInfo.add(distributorToSnakeRow(d));
				}
			}
			if (distributorInfo.isEmpty()) {
				distributorInfo.add(List.of());
			}
		}
		detail.put("distributor_info", distributorInfo);
		detail.put("use_all_distributor", distributorInfo.isEmpty());

		List<Long> itemIds = new ArrayList<>();
		Map<Long, Integer> itemIdToUseLimit = new LinkedHashMap<>();
		LambdaQueryWrapper<RelItems> relW = new LambdaQueryWrapper<>();
		relW.eq(RelItems::getCompanyId, companyId)
				.eq(RelItems::getCardId, entity.getCardId())
				.eq(RelItems::getItemType, "normal")
				.orderByAsc(RelItems::getItemId);
		List<RelItems> relNormal = relItemsMapper.selectList(relW);
		if (!relNormal.isEmpty()) {
			for (RelItems ri : relNormal) {
				if (ri.getItemId() != null && ri.getItemId() > 0) {
					itemIds.add(ri.getItemId());
					itemIdToUseLimit.put(ri.getItemId(), ri.getUseLimit() != null ? ri.getUseLimit() : 0);
				}
			}
		}
		if (!itemIds.isEmpty()) {
			List<String> relItemIdStrings = new ArrayList<>(itemIds.size());
			for (Long itemId : itemIds) {
				relItemIdStrings.add(String.valueOf(itemId));
			}
			detail.put("rel_item_ids", relItemIdStrings);
			detail.put(
					"itemTreeLists",
					discountCardKaquanDetailItemsService.buildItemTreeLists(companyId, itemIds, itemIdToUseLimit));
		} else {
			detail.put("rel_item_ids", List.of());
		}
		String useAllItems = itemIds.isEmpty() ? "true" : "false";
		detail.put("use_all_items", useAllItems);

		LambdaQueryWrapper<RelMemberTags> tagW = new LambdaQueryWrapper<>();
		tagW.eq(RelMemberTags::getCompanyId, companyId).eq(RelMemberTags::getCardId, entity.getCardId());
		List<RelMemberTags> memberTags = relMemberTagsMapper.selectList(tagW);
		List<Long> userTagIds = new ArrayList<>();
		for (RelMemberTags mt : memberTags) {
			if (mt.getTagId() != null) {
				userTagIds.add(mt.getTagId());
			}
		}
		detail.put("user_tag_ids", userTagIds);

		List<Long> categoryIds = new ArrayList<>();
		LambdaQueryWrapper<RelItems> catW = new LambdaQueryWrapper<>();
		catW.eq(RelItems::getCompanyId, companyId)
				.eq(RelItems::getCardId, entity.getCardId())
				.eq(RelItems::getItemType, "category")
				.orderByAsc(RelItems::getItemId);
		List<RelItems> relCat = relItemsMapper.selectList(catW);
		for (RelItems ri : relCat) {
			if (ri.getItemId() != null) {
				categoryIds.add(ri.getItemId());
			}
		}
		if (!categoryIds.isEmpty()) {
			detail.put("rel_category_ids", new ArrayList<>(categoryIds));
			detail.put("item_category", new ArrayList<>(categoryIds));
		}
		if (!categoryIds.isEmpty()) {
			detail.put("use_all_items", "category");
		}

		Integer useBound = entity.getUseBound();
		if (useBound != null && useBound == FOR_TAG_ITEMS) {
			detail.put("use_all_items", "tag");
		} else if (useBound != null && useBound == FOR_BRAND_ITEMS) {
			detail.put("use_all_items", "brand");
		}

		Object useScenesObj = detail.get("use_scenes");
		String useScenesStr = useScenesObj == null ? "" : String.valueOf(useScenesObj);
		if (useScenesStr.split(",", -1).length > 1) {
			detail.put("use_scenes", "QUICK");
		}

		List<String> tagIdStrs = normalizeIdStringList(detail.get("tag_ids"));
		List<Long> tagIdLongs = parseLongIds(tagIdStrs);
		detail.put("tag_ids", tagIdStrs);
		detail.put("rel_tag_ids", tagIdStrs);
		detail.put("tag_list", loadTagList(companyId, tagIdLongs));

		List<String> brandIdStrs = normalizeIdStringList(detail.get("brand_ids"));
		List<Long> brandIdLongs = parseLongIds(brandIdStrs);
		detail.put("brand_ids", brandIdStrs);
		detail.put("rel_brand_ids", brandIdStrs);
		detail.put("brand_list", loadBrandList(companyId, brandIdLongs));

		detail.put("is_active", computeIsActive(detail, entity));

		detail.put("grade_ids", normalizeCsvIdList(entity.getGradeIds()));
		detail.put("vip_grade_ids", normalizeCsvIdList(entity.getVipGradeIds()));

		return detail;
	}

	private List<Map<String, Object>> loadTagList(long companyId, List<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return List.of();
		}
		ItemsTagsListFilter filter = new ItemsTagsListFilter();
		filter.setCompanyId(companyId);
		filter.setTagIdsIn(tagIds);
		Page<ItemsTags> page = new Page<>(1, 500);
		IPage<ItemsTags> p = itemsTagsRepository.selectPageByFilter(page, filter);
		List<ItemsTags> rows = p.getRecords();
		if (rows.isEmpty()) {
			return List.of();
		}
		List<Long> distIds =
				rows.stream().map(ItemsTags::getDistributorId).filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
		Map<Long, Distributor> distById = Map.of();
		if (!distIds.isEmpty()) {
			distById = distributorListQueryService.listByIdsAndCompany(companyId, distIds).stream()
					.collect(Collectors.toMap(Distributor::getDistributorId, d -> d, (a, b) -> a));
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (ItemsTags t : rows) {
			out.add(itemsTagToRow(t, distById));
		}
		return out;
	}

	private List<Map<String, Object>> loadBrandList(long companyId, List<Long> brandIds) {
		if (brandIds == null || brandIds.isEmpty()) {
			return List.of();
		}
		List<ItemsAttributes> attrs = itemsAttributesRepository.listByCompanyAndAttributeIdsIn(companyId, brandIds);
		List<Map<String, Object>> out = new ArrayList<>();
		for (ItemsAttributes a : attrs) {
			if (a.getAttributeType() != null && "brand".equalsIgnoreCase(a.getAttributeType().trim())) {
				out.add(itemsAttributeToRow(a));
			}
		}
		return out;
	}

	private static Map<String, Object> itemsTagToRow(ItemsTags t, Map<Long, Distributor> distById) {
		Map<String, Object> m = new LinkedHashMap<>();
		putLong(m, "tag_id", t.getTagId());
		putLong(m, "company_id", t.getCompanyId());
		m.put("tag_name", t.getTagName());
		m.put("tag_color", t.getTagColor());
		putLong(m, "distributor_id", t.getDistributorId());
		m.put("font_color", t.getFontColor());
		m.put("description", t.getDescription());
		m.put("tag_icon", t.getTagIcon());
		m.put("front_show", t.getFrontShow());
		m.put("created", t.getCreated());
		m.put("updated", t.getUpdated());
		long did = t.getDistributorId() != null ? t.getDistributorId() : 0L;
		if (did > 0) {
			Distributor d = distById.get(did);
			m.put("distributor_name", d != null && StringUtils.hasText(d.getName()) ? d.getName() : "");
			m.put("is_platform", false);
		} else {
			m.put("distributor_name", "平台");
			m.put("is_platform", true);
		}
		return m;
	}

	private static Map<String, Object> itemsAttributeToRow(ItemsAttributes a) {
		Map<String, Object> m = new LinkedHashMap<>();
		putLong(m, "attribute_id", a.getAttributeId());
		putLong(m, "company_id", a.getCompanyId());
		m.put("attribute_type", a.getAttributeType());
		m.put("attribute_name", a.getAttributeName());
		m.put("attribute_memo", a.getAttributeMemo());
		m.put("attribute_sort", a.getAttributeSort());
		putLong(m, "shop_id", a.getShopId());
		return m;
	}

	private static void putLong(Map<String, Object> m, String key, Long v) {
		if (v != null) {
			m.put(key, v);
		}
	}

	private static Map<String, Object> distributorToSnakeRow(Distributor d) {
		Map<String, Object> m = new LinkedHashMap<>();
		if (d.getDistributorId() != null) {
			m.put("distributor_id", d.getDistributorId());
		}
		if (d.getCompanyId() != null) {
			m.put("company_id", d.getCompanyId());
		}
		m.put("name", d.getName());
		m.put("mobile", d.getMobile());
		m.put("address", d.getAddress());
		m.put("logo", d.getLogo());
		m.put("province", d.getProvince());
		m.put("city", d.getCity());
		m.put("area", d.getArea());
		m.put("shop_id", d.getShopId());
		m.put("is_valid", d.getIsValid());
		m.put("created", d.getCreated());
		m.put("updated", d.getUpdated());
		return m;
	}

	private static boolean computeIsActive(Map<String, Object> detail, DiscountCards entity) {
		boolean active = false;
		if ("new_gift".equals(detail.get("card_type"))) {
			String dateType = detail.get("date_type") == null ? "" : detail.get("date_type").toString();
			long now = System.currentTimeMillis() / 1000L;
			Integer sendBegin = entity.getSendBeginTime();
			Object beginDateObj = detail.get("begin_date");
			int beginDateInt = beginDateObj instanceof Number n ? n.intValue() : 0;
			if (DiscountCardActionValidationService.DATE_TYPE_LONG.equals(dateType)) {
				int sbt = sendBegin != null ? sendBegin : 0;
				active = (long) sbt + 86400L * (long) beginDateInt < now;
			} else if (DiscountCardActionValidationService.DATE_TYPE_SHORT.equals(dateType)) {
				active = (long) beginDateInt < now;
			}
		}
		Integer kq = entity.getKqStatus();
		return active && !Objects.equals(kq, Integer.valueOf(DiscountNewGiftCardCreateService.STATUS_INIT));
	}

	private static List<String> normalizeCsvIdList(String raw) {
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
		if (!StringUtils.hasText(t)) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (String p : t.split(",")) {
			if (StringUtils.hasText(p)) {
				out.add(p.trim());
			}
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static List<String> normalizeIdStringList(Object raw) {
		if (raw == null) {
			return new ArrayList<>();
		}
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				String s = o.toString().trim();
				if (StringUtils.hasText(s)) {
					out.add(s);
				}
			}
			return out;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return new ArrayList<>();
		}
		return new ArrayList<>(splitCommaStrings(s));
	}

	private static List<Long> parseLongIds(List<String> ids) {
		List<Long> out = new ArrayList<>();
		for (String s : ids) {
			try {
				out.add(Long.parseLong(s.trim()));
			} catch (NumberFormatException ignored) {
			}
		}
		return out;
	}

	private static List<String> splitCommaStrings(String raw) {
		if (!StringUtils.hasText(raw)) {
			return new ArrayList<>();
		}
		List<String> out = new ArrayList<>();
		for (String p : raw.split(",")) {
			if (StringUtils.hasText(p)) {
				out.add(p.trim());
			}
		}
		return out;
	}

	private static List<String> splitCommaPreserveOrder(String raw) {
		if (raw == null || raw.isEmpty()) {
			return new ArrayList<>();
		}
		String[] parts = raw.split(",", -1);
		List<String> out = new ArrayList<>();
		for (String p : parts) {
			out.add(p);
		}
		return out;
	}

	private static boolean isNumericToken(String v) {
		if (v == null) {
			return false;
		}
		String t = v.trim();
		if (t.isEmpty()) {
			return false;
		}
		for (int i = 0; i < t.length(); i++) {
			if (!Character.isDigit(t.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isOne(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() == 1;
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}

	/** API string {@code "true"} only when the stored value compares equal to numeric one (word {@code "true"} maps to {@code "false"}). */
	private static String useAllShopsToTriStateString(Object v) {
		if (v == null) {
			return "false";
		}
		if (v instanceof Boolean b) {
			return b ? "true" : "false";
		}
		if (v instanceof Number n) {
			return n.intValue() == 1 ? "true" : "false";
		}
		String s = v.toString().trim();
		if ("1".equals(s)) {
			return "true";
		}
		if ("true".equalsIgnoreCase(s)) {
			return "false";
		}
		try {
			if (!s.isEmpty() && s.matches("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?")) {
				return Double.parseDouble(s) == 1.0 ? "true" : "false";
			}
		} catch (NumberFormatException ignored) {
		}
		return "false";
	}
}
