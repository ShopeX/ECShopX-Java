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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.service.EmployeePurchaseActivityItemsPriceOverlayService;
import cn.shopex.ecshopx.goods.service.ItemsGroupGetGroupItemsService;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListQueryOrchestrator;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import cn.shopex.ecshopx.pointsmall.service.PointsmallItemsAdminListService;
import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import cn.shopex.ecshopx.theme.service.dto.PagesTemplateWidgetItemsQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PagesTemplateWidgetItemsService {

	private final ObjectMapper objectMapper;
	private final ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	private final SeckillActivityMapper seckillActivityMapper;
	private final SeckillRelGoodsMapper seckillRelGoodsMapper;
	private final ItemsGroupGetGroupItemsService itemsGroupGetGroupItemsService;
	private final WxappGoodsItemsListQueryOrchestrator wxappGoodsItemsListQueryOrchestrator;
	private final GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService;
	private final WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;
	private final PointsmallItemsMapper pointsmallItemsMapper;
	private final PointsmallItemsAdminListService pointsmallItemsAdminListService;
	private final ActivitiesMapper activitiesMapper;
	private final EmployeePurchaseActivityItemsPriceOverlayService
			employeePurchaseActivityItemsPriceOverlayService;

	public Map<String, Object> getWidgetItems(long companyId, String acceptLanguage, PagesTemplateWidgetItemsQuery q) {
		return getWidgetItems(companyId, acceptLanguage, q, null);
	}

	public Map<String, Object> getWidgetItems(
			long companyId, String acceptLanguage, PagesTemplateWidgetItemsQuery q, Long memberPriceUserIdOrNull) {
		if (!"sales".equals(q.getDataType()) && isEmptyDataValueForNonSalesEarlyExit(q)) {
			return emptyWidgetBody();
		}

		String dt = q.getDataType();
		if ("pointsmall_items".equals(dt)) {
			return buildPointsmallWidgetItems(companyId, q);
		}

		if (!isKnownWidgetDataType(dt)) {
			return emptyWidgetBody();
		}

		LinkedHashMap<String, Object> p = new LinkedHashMap<>();
		LinkedHashMap<String, Object> newFilter = new LinkedHashMap<>();
		p.put("company_id", companyId);
		p.put("approve_status", "onsale");
		p.put("audit_status", "approved");
		p.put("type", 0);
		p.put("item_type", "normal");
		p.put("is_gift", Boolean.FALSE);
		p.put("is_default", Boolean.TRUE);
		p.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_ACCEPT_LANGUAGE, acceptLanguage);
		if (q.isSortGtePresent()) {
			p.put("sort|gte", q.getSortGte());
		}
		Integer effectiveDistributorId = resolveEffectiveDistributorId(companyId, q);
		if (effectiveDistributorId != null && effectiveDistributorId > 0) {
			p.put("distributor_id", effectiveDistributorId.longValue());
			if (q.isApplyStoreOnsaleFilter()) {
				p.put("is_can_sale", Boolean.TRUE);
			}
		}

		SeckillActivity seckillAct = null;

		if ("main_category".equals(dt)) {
			long leafMainCatId = parseLeafCategoryIdFromCommaSeparated(q.getDataValueRaw());
			List<String> itemCategoryKeys =
					itemsCategoryItemIdResolver.expandMainCategoryIdsToItemCategoryKeys(companyId, List.of(leafMainCatId));
			p.put("item_category", itemCategoryKeys);
			newFilter.put("main_category", leafMainCatId);
		} else if ("category".equals(dt)) {
			long leafSaleCatId = parseLeafCategoryIdFromCommaSeparated(q.getDataValueRaw());
			List<Long> resolved = itemsCategoryItemIdResolver.getItemIdsByCategoryTree(companyId, leafSaleCatId);
			p.put("category_resolved_item_ids", resolved);
			newFilter.put("category_id", String.valueOf(leafSaleCatId));
		} else if ("sales".equals(dt)) {
			p.put("goodsSort", "1");
		} else if ("items".equals(dt)) {
			List<Long> goodsIds = resolveWidgetItemsGoodsIds(q);
			p.put("goods_id", goodsIds);
			List<String> filterGoodsIds = goodsIds.stream().map(String::valueOf).collect(Collectors.toList());
			newFilter.put("goods_id", filterGoodsIds);
		} else if ("distributor".equals(dt)) {
			long distData = parseLongStrict(q.getDataValueRaw(), "data_value 无效");
			p.put("distributor_id", Long.valueOf(distData));
			newFilter.put("distributor_id", distData);
		} else if ("items_group".equals(dt)) {
			long groupId = parseLongStrict(q.getDataValueRaw(), "data_value 无效");
			List<Long> goodsIds = itemsGroupGetGroupItemsService.listAllGoodsIdsByGroupId(groupId);
			if (goodsIds.isEmpty()) {
				p.put("goods_id", List.of(-1L));
				newFilter.put("goods_id", new ArrayList<>());
			} else {
				p.put("goods_id", goodsIds);
				newFilter.put("goods_id", new ArrayList<>(goodsIds));
			}
		} else if ("seckill".equals(dt)) {
			long seckillId = parseLongStrict(q.getDataValueRaw(), "data_value 无效");
			seckillAct = seckillActivityMapper.selectOne(Wrappers.<SeckillActivity>lambdaQuery()
					.eq(SeckillActivity::getCompanyId, companyId)
					.eq(SeckillActivity::getSeckillId, seckillId));
			int nowSec = (int) (System.currentTimeMillis() / 1000L);
			if (seckillAct == null
					|| seckillAct.getActivityEndTime() == null
					|| seckillAct.getActivityEndTime() < nowSec) {
				return emptyWidgetBody();
			}
			List<SeckillRelGoods> rels = seckillRelGoodsMapper.selectList(Wrappers.<SeckillRelGoods>lambdaQuery()
					.eq(SeckillRelGoods::getCompanyId, companyId)
					.eq(SeckillRelGoods::getSeckillId, seckillId));
			List<Long> ids = rels.stream()
					.map(SeckillRelGoods::getItemId)
					.filter(Objects::nonNull)
					.filter(id -> id > 0L)
					.distinct()
					.collect(Collectors.toList());
			if (ids.isEmpty()) {
				return emptyWidgetBody();
			}
			p.put("item_id", ids);
			newFilter.put("seckill_id", seckillId);
		} else if ("group".equals(dt)) {
			String groupIdEchoString = q.getDataValueRaw() == null ? "" : q.getDataValueRaw().trim();
			p.put("group_id", groupIdEchoString);
			newFilter.put("group_id", groupIdEchoString);
		} else if ("price".equals(dt)) {
			applyPriceBranch(q, p, newFilter);
		} else {
			return emptyWidgetBody();
		}

		int effectivePageSize = (q.getPageSizeOverride() != null) ? q.getPageSizeOverride() : q.getNum();
		p.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_LIST_PAGE, q.getPage());
		p.put(WxappGoodsItemsListQueryOrchestrator.KEY_INTERNAL_LIST_PAGE_SIZE, effectivePageSize);

		Map<String, Object> orchestratorResult =
				wxappGoodsItemsListQueryOrchestrator.queryItemListData(companyId, p, List.of());
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list =
				(List<Map<String, Object>>) orchestratorResult.getOrDefault("list", List.of());

		if ("seckill".equals(dt) && !list.isEmpty() && seckillAct != null) {
			Integer start = seckillAct.getActivityStartTime();
			Integer end = seckillAct.getActivityEndTime();
			int startSec = start == null ? 0 : start;
			int endSec = end == null ? 0 : end;
			for (Map<String, Object> row : list) {
				row.put("activity_start_time", startSec);
				row.put("activity_end_time", endSec);
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("data", list);
		if ("sales".equals(dt)) {
			out.put("filter", List.of());
		} else {
			LinkedHashMap<String, Object> filterOut = new LinkedHashMap<>(newFilter);
			if ("group".equals(dt)) {
				String groupIdEchoString = q.getDataValueRaw() == null ? "" : q.getDataValueRaw().trim();
				filterOut.put("group_id", groupIdEchoString);
			}
			if ("items_group".equals(dt)) {
				applySortedNumericStringGoodsIdsForWidgetFilter(filterOut, "goods_id");
			}
			out.put("filter", filterOut);
		}
		out.put("goodsSort", "sales".equals(dt) ? Integer.valueOf(1) : null);

		for (Map<String, Object> row : list) {
			if (!row.containsKey("company_id") || row.get("company_id") == null) {
				row.put("company_id", companyId);
			}
		}
		if (memberPriceUserIdOrNull != null && list != null && !list.isEmpty()) {
			wxappGoodsItemsListMemberPriceApplyService.applyForRows(
					companyId, memberPriceUserIdOrNull.longValue(), list, acceptLanguage);
		}
		goodsItemsListPromotionEnrichmentService.enrich(list);
		long eActivityId = q.getEActivityId();
		if (eActivityId > 0L && list != null && !list.isEmpty()) {
			employeePurchaseActivityItemsPriceOverlayService.overlayActivityPrice(
					list, companyId, eActivityId);
		}
		out.put("data", list);

		return out;
	}

	private Integer resolveEffectiveDistributorId(long companyId, PagesTemplateWidgetItemsQuery q) {
		Integer distributorId = q.getDistributorId();
		long eActivityId = q.getEActivityId();
		if (eActivityId <= 0L || companyId <= 0L) {
			return distributorId;
		}
		Activities actRow =
				activitiesMapper.selectOne(
						Wrappers.<Activities>lambdaQuery()
								.eq(Activities::getCompanyId, companyId)
								.eq(Activities::getId, eActivityId)
								.last("LIMIT 1"));
		if (actRow != null && actRow.getDistributorId() != null && actRow.getDistributorId() > 0) {
			return actRow.getDistributorId();
		}
		return distributorId;
	}

	private void applyPriceBranch(
			PagesTemplateWidgetItemsQuery q, LinkedHashMap<String, Object> p, LinkedHashMap<String, Object> newFilter) {
		JsonNode arr = resolvePriceDataValueArray(q);
		BigDecimal gteYuan = arr.size() >= 1 ? yuanBigDecimalFromPriceJsonElement(arr.get(0)) : null;
		BigDecimal lteYuan = arr.size() >= 2 ? yuanBigDecimalFromPriceJsonElement(arr.get(1)) : null;
		if (gteYuan != null) {
			int gteCents = centsFromYuanBigDecimal(gteYuan);
			p.put("price|gte", gteCents);
			newFilter.put("start_price", gteCents);
		}
		if (lteYuan != null) {
			int lteCents = centsFromYuanBigDecimal(lteYuan);
			p.put("price|lte", lteCents);
			newFilter.put("end_price", lteCents);
		}
	}

	private Map<String, Object> buildPointsmallWidgetItems(long companyId, PagesTemplateWidgetItemsQuery q) {
		List<Long> psIds = resolvePointsmallItemIds(q);
		if (psIds.isEmpty()) {
			return emptyWidgetBody();
		}
		int psPage = q.getPage();
		int psPageSize = (q.getPageSizeOverride() != null) ? q.getPageSizeOverride() : q.getNum();
		Page<PointsmallItems> page = new Page<>(psPage, psPageSize);
		LambdaQueryWrapper<PointsmallItems> w = Wrappers.<PointsmallItems>lambdaQuery()
				.eq(PointsmallItems::getCompanyId, companyId)
				.in(PointsmallItems::getItemId, psIds)
				.in(PointsmallItems::getApproveStatus, List.of("onsale", "only_show"))
				.eq(PointsmallItems::getAuditStatus, "approved")
				.orderByDesc(PointsmallItems::getSort)
				.orderByDesc(PointsmallItems::getItemId);
		IPage<PointsmallItems> hit = pointsmallItemsMapper.selectPage(page, w);
		List<Map<String, Object>> rows = new ArrayList<>();
		for (PointsmallItems ent : hit.getRecords()) {
			Map<String, Object> row = new LinkedHashMap<>(pointsmallItemsAdminListService.toListRowMap(ent));
			com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PointsmallItems> sumQw =
					Wrappers.<PointsmallItems>query()
							.select("COALESCE(SUM(store),0)")
							.eq("company_id", companyId)
							.eq("default_item_id", row.get("default_item_id"));
			List<Object> sumObjs = pointsmallItemsMapper.selectObjs(sumQw);
			int goodsStore = sumObjs.isEmpty() || sumObjs.get(0) == null ? 0 : ((Number) sumObjs.get(0)).intValue();
			row.put("goods_store", goodsStore);
			rows.add(row);
		}
		LinkedHashMap<String, Object> f = new LinkedHashMap<>();
		f.put("pointsmall_item_id", new ArrayList<>(psIds));
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("data", rows);
		out.put("filter", f);
		out.put("goodsSort", null);
		return out;
	}

	private static List<Long> resolvePointsmallItemIds(PagesTemplateWidgetItemsQuery q) {
		if (!q.getDataValueIds().isEmpty()) {
			return new ArrayList<>(q.getDataValueIds());
		}
		return parsePointsmallItemIds(q.getDataValueRaw());
	}

	private static List<Long> parsePointsmallItemIds(String raw) {
		if (raw == null || raw.isBlank()) {
			return new ArrayList<>();
		}
		String[] parts = raw.split(",", -1);
		LinkedHashSet<Long> set = new LinkedHashSet<>();
		for (String part : parts) {
			String seg = part == null ? "" : part.trim();
			if (!StringUtils.hasText(seg)) {
				continue;
			}
			try {
				long v = Long.parseLong(seg);
				if (v > 0L) {
					set.add(v);
				}
			} catch (NumberFormatException ignored) {
				// skip invalid segment
			}
		}
		return new ArrayList<>(set);
	}

	private static boolean isKnownWidgetDataType(String dt) {
		return "main_category".equals(dt)
				|| "category".equals(dt)
				|| "sales".equals(dt)
				|| "items".equals(dt)
				|| "distributor".equals(dt)
				|| "items_group".equals(dt)
				|| "seckill".equals(dt)
				|| "group".equals(dt)
				|| "price".equals(dt);
	}

	private static Map<String, Object> emptyWidgetBody() {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("data", List.of());
		m.put("filter", List.of());
		m.put("goodsSort", null);
		return m;
	}

	private static void applySortedNumericStringGoodsIdsForWidgetFilter(LinkedHashMap<String, Object> filterOut, String key) {
		Object v = filterOut.get(key);
		if (v == null) {
			return;
		}
		List<Long> ids = new ArrayList<>();
		if (v instanceof Collection<?> c) {
			for (Object o : c) {
				Long boxed = toLongOrNull(o);
				if (boxed != null) {
					ids.add(boxed);
				}
			}
		} else {
			Long boxed = toLongOrNull(v);
			if (boxed != null) {
				ids.add(boxed);
			}
		}
		Collections.sort(ids);
		List<String> asStrings = ids.stream().map(String::valueOf).collect(Collectors.toList());
		filterOut.put(key, asStrings);
	}

	private static Long toLongOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static List<Long> resolveWidgetItemsGoodsIds(PagesTemplateWidgetItemsQuery q) {
		if (!q.getDataValueIds().isEmpty()) {
			return new ArrayList<>(q.getDataValueIds());
		}
		long goodsId = parseLongStrict(q.getDataValueRaw(), "data_value 无效");
		return List.of(goodsId);
	}

	private JsonNode resolvePriceDataValueArray(PagesTemplateWidgetItemsQuery q) {
		String raw = q.getDataValueRaw();
		if (StringUtils.hasText(raw)) {
			try {
				JsonNode node = objectMapper.readTree(raw.trim());
				if (node != null && node.isArray()) {
					return node;
				}
			} catch (JsonProcessingException ex) {
				throw new BadRequestException("data_value 格式错误");
			}
			throw new BadRequestException("data_value 格式错误");
		}
		List<String> segs = q.getDataValueOrderedSegments();
		if (!segs.isEmpty()) {
			ArrayNode arr = objectMapper.createArrayNode();
			for (String s : segs) {
				arr.add(s);
			}
			return arr;
		}
		throw new BadRequestException("data_value 格式错误");
	}

	private static boolean isEmptyDataValueForNonSalesEarlyExit(PagesTemplateWidgetItemsQuery q) {
		if (!q.getDataValueOrderedSegments().isEmpty()) {
			return false;
		}
		if (!q.getDataValueIds().isEmpty()) {
			return false;
		}
		String raw = q.getDataValueRaw();
		return raw == null || raw.trim().isEmpty() || "0".equals(raw.trim());
	}

	private static long parseLeafCategoryIdFromCommaSeparated(String dataValueRaw) {
		if (dataValueRaw == null || !StringUtils.hasText(dataValueRaw.trim())) {
			throw new BadRequestException("data_value 无效");
		}
		String raw = dataValueRaw.trim();
		String[] arr = raw.split(",", -1);
		ArrayList<String> copy = new ArrayList<>();
		for (String s : arr) {
			copy.add(s);
		}
		String lastNonEmpty = null;
		for (int i = copy.size() - 1; i >= 0; i--) {
			String t = copy.get(i) == null ? "" : copy.get(i).trim();
			if (StringUtils.hasText(t)) {
				lastNonEmpty = t;
				break;
			}
		}
		if (lastNonEmpty == null) {
			throw new BadRequestException("data_value 无效");
		}
		try {
			return Long.parseLong(lastNonEmpty);
		} catch (NumberFormatException ex) {
			throw new BadRequestException("data_value 无效");
		}
	}

	private static long parseLongStrict(String raw, String invalidMsg) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			throw new BadRequestException(invalidMsg);
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException ex) {
			throw new BadRequestException(invalidMsg);
		}
	}

	private static BigDecimal yuanBigDecimalFromPriceJsonElement(JsonNode n) {
		if (n == null || n.isNull() || n.isMissingNode()) {
			return null;
		}
		if (!n.isValueNode()) {
			throw new BadRequestException("data_value 格式错误");
		}
		try {
			return new BigDecimal(n.asText());
		} catch (NumberFormatException ex) {
			throw new BadRequestException("data_value 格式错误");
		}
	}

	private static int centsFromYuanBigDecimal(BigDecimal yuan) {
		BigDecimal cents = yuan.multiply(BigDecimal.valueOf(100));
		if (cents.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
			return Integer.MAX_VALUE;
		}
		if (cents.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) < 0) {
			return Integer.MIN_VALUE;
		}
		return cents.intValue();
	}
}
