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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorListRowFormatService;
import cn.shopex.ecshopx.distribution.service.SelfDeliverySettingReadService;
import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.port.LimitPromotionAdminGoodsSupportPort;
import cn.shopex.ecshopx.promotions.service.multilang.SeckillActivityOutsideMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SeckillActivityInfoService {

	private static final ZoneId DATETIME_ZONE = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter LISTING_DATETIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(DATETIME_ZONE);

	private final SeckillActivityMapper seckillActivityMapper;
	private final SeckillRelGoodsMapper seckillRelGoodsMapper;
	private final SeckillActivityCreateService seckillActivityCreateService;
	private final SeckillActivityOutsideMultiLangReadService seckillActivityOutsideMultiLangReadService;
	private final DistributorListQueryService distributorListQueryService;
	private final SelfDeliverySettingReadService selfDeliverySettingReadService;
	private final DistributorListRowFormatService distributorListRowFormatService;
	private final LimitPromotionAdminGoodsSupportPort limitPromotionAdminGoodsSupportPort;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;

	public SeckillActivityInfoService(
			SeckillActivityMapper seckillActivityMapper,
			SeckillRelGoodsMapper seckillRelGoodsMapper,
			SeckillActivityCreateService seckillActivityCreateService,
			SeckillActivityOutsideMultiLangReadService seckillActivityOutsideMultiLangReadService,
			DistributorListQueryService distributorListQueryService,
			SelfDeliverySettingReadService selfDeliverySettingReadService,
			DistributorListRowFormatService distributorListRowFormatService,
			LimitPromotionAdminGoodsSupportPort limitPromotionAdminGoodsSupportPort,
			ObjectMapper objectMapper,
			MessageSource messageSource) {
		this.seckillActivityMapper = seckillActivityMapper;
		this.seckillRelGoodsMapper = seckillRelGoodsMapper;
		this.seckillActivityCreateService = seckillActivityCreateService;
		this.seckillActivityOutsideMultiLangReadService = seckillActivityOutsideMultiLangReadService;
		this.distributorListQueryService = distributorListQueryService;
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
		this.distributorListRowFormatService = distributorListRowFormatService;
		this.limitPromotionAdminGoodsSupportPort = limitPromotionAdminGoodsSupportPort;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getSeckillActivityInfo(long companyId, long seckillId, String requestLangTag) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);

		SeckillActivity activity =
				seckillActivityMapper.selectOne(
						new LambdaQueryWrapper<SeckillActivity>()
								.eq(SeckillActivity::getCompanyId, companyId)
								.eq(SeckillActivity::getSeckillId, seckillId));
		if (activity == null) {
			throw new ResourceException(
					messageSource.getMessage("promotions.seckill.no_update_data_found", null, locale));
		}

		LambdaQueryWrapper<SeckillRelGoods> rw =
				new LambdaQueryWrapper<SeckillRelGoods>()
						.eq(SeckillRelGoods::getCompanyId, companyId)
						.eq(SeckillRelGoods::getSeckillId, seckillId)
						.orderByDesc(SeckillRelGoods::getSort)
						.orderByDesc(SeckillRelGoods::getItemId)
						.orderByDesc(SeckillRelGoods::getActivityStartTime);
		Page<SeckillRelGoods> relPage = new Page<>(1, 10000);
		List<SeckillRelGoods> relRecords = seckillRelGoodsMapper.selectPage(relPage, rw).getRecords();

		List<Map<String, Object>> itemMaps = new ArrayList<>();
		for (SeckillRelGoods r : relRecords) {
			itemMaps.add(SeckillActivityCreateService.relGoodsEntityToAdminRow(r));
		}

		Map<String, Object> result =
				seckillActivityCreateService.assembleAdminSeckillReadPayload(activity, itemMaps);
		result.put("total_count", relPage.getTotal());

		seckillActivityOutsideMultiLangReadService.apply(companyId, seckillId, result, requestLangTag);

		result.put(
				"is_activity_rebate",
				Boolean.TRUE.equals(result.get("is_activity_rebate")) ? "true" : "false");
		result.put(
				"is_free_shipping",
				Boolean.TRUE.equals(result.get("is_free_shipping")) ? "true" : "false");

		Object rawDist = result.get("distributor_id");
		if (!distributorIdTruthy(rawDist)) {
			result.put("distributor_info", List.of());
		} else {
			List<Long> distIds = parseDistributorIds(rawDist);
			List<Distributor> distributors =
					distIds.isEmpty()
							? List.of()
							: distributorListQueryService.listByIdsAndCompany(companyId, distIds);
			Map<Long, Distributor> byId = new LinkedHashMap<>();
			for (Distributor d : distributors) {
				if (d.getDistributorId() != null) {
					byId.put(d.getDistributorId(), d);
				}
			}
			List<Map<String, Object>> distInfo = new ArrayList<>();
			for (Long did : distIds) {
				Distributor d = byId.get(did);
				if (d == null) {
					continue;
				}
				int ds = d.getDistributorSelf() != null ? d.getDistributorSelf() : 0;
				Map<String, Object> setting = selfDeliverySettingReadService.getSetting(companyId, did, ds);
				distInfo.add(distributorListRowFormatService.formatStoreRow(d, setting, objectMapper));
			}
			result.put("distributor_info", distInfo);
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
		if (items == null) {
			items = List.of();
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		List<Long> itemIds = new ArrayList<>();
		LinkedHashSet<Long> seenItemIds = new LinkedHashSet<>();
		for (Map<String, Object> row : items) {
			row.remove("id");
			applyRelGoodsListingStatusAndDates(row, now);
			Long iid = toLongObject(row.get("item_id"));
			if (iid != null && iid > 0L && seenItemIds.add(iid)) {
				itemIds.add(iid);
			}
			Object ap = row.get("activity_price");
			BigDecimal yuan =
					(ap instanceof Number n && n.longValue() != 0L)
							? BigDecimal.valueOf(n.longValue())
									.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
							: BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
			row.put("activity_price", yuan);
		}

		if (!itemIds.isEmpty()) {
			Map<String, Object> skuPack =
					limitPromotionAdminGoodsSupportPort.loadSkuItemsListForPromotionDetail(companyId, itemIds);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> skuList = (List<Map<String, Object>>) skuPack.get("list");
			if (skuList == null) {
				skuList = List.of();
			}
			Map<Long, Map<String, Object>> skuById = new LinkedHashMap<>();
			for (Map<String, Object> skuRow : skuList) {
				Long iid = toLongObject(skuRow.get("item_id"));
				if (iid != null) {
					skuById.put(iid, skuRow);
				}
			}
			List<Map<String, Object>> orderedSkuRows = new ArrayList<>();
			for (Long id : itemIds) {
				Map<String, Object> one = skuById.get(id);
				if (one != null) {
					orderedSkuRows.add(one);
				}
			}
			List<Map<String, Object>> itemTreeLists =
					orderedSkuRows.isEmpty()
							? List.of()
							: limitPromotionAdminGoodsSupportPort.formatItemsList(orderedSkuRows);
			result.put("itemTreeLists", itemTreeLists);

			List<Long> treeItemIds = new ArrayList<>();
			for (Map<String, Object> tr : itemTreeLists) {
				Long tid = toLongObject(tr.get("item_id"));
				if (tid != null && tid > 0L) {
					treeItemIds.add(tid);
				}
			}
			List<Map<String, Object>> tagJoinRows =
					treeItemIds.isEmpty()
							? List.of()
							: limitPromotionAdminGoodsSupportPort.loadItemsRelTagJoinRows(companyId, treeItemIds);

			Map<Long, List<Map<String, Object>>> byItemId = new LinkedHashMap<>();
			for (Map<String, Object> tagRow : tagJoinRows) {
				Long iid = toLongObject(tagRow.get("item_id"));
				if (iid == null) {
					continue;
				}
				byItemId.computeIfAbsent(iid, k -> new ArrayList<>()).add(tagRow);
			}

			for (Map<String, Object> value : itemTreeLists) {
				Long topId = toLongObject(value.get("item_id"));
				List<Map<String, Object>> tags = topId == null ? List.of() : byItemId.getOrDefault(topId, List.of());
				value.put("tagList", tags);
				inheritTagListToChildren(value, tags);
			}
		}

		return result;
	}

	private static void inheritTagListToChildren(Map<String, Object> parent, List<Map<String, Object>> tagList) {
		if (!isMultiSpecParent(parent.get("nospec"))) {
			return;
		}
		Object specObj = parent.get("spec_items");
		if (!(specObj instanceof List<?> specList)) {
			return;
		}
		for (Object el : specList) {
			if (el instanceof Map<?, ?> sm) {
				@SuppressWarnings("unchecked")
				Map<String, Object> spec = (Map<String, Object>) sm;
				spec.put("tagList", tagList);
			}
		}
	}

	private static boolean isMultiSpecParent(Object nospec) {
		if (nospec == null) {
			return false;
		}
		String s = nospec.toString().trim().toLowerCase(Locale.ROOT);
		return "false".equals(s) || "0".equals(s);
	}

	private static boolean distributorIdTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Collection<?> c) {
			for (Object el : c) {
				if (el != null && StringUtils.hasText(el.toString().trim())) {
					return true;
				}
			}
			return false;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return false;
			}
			return StringUtils.hasText(t.replace(",", "").trim());
		}
		return false;
	}

	private static List<Long> parseDistributorIds(Object raw) {
		List<Long> out = new ArrayList<>();
		if (raw instanceof Collection<?> c) {
			for (Object el : c) {
				Long id = toLongObject(el);
				if (id != null && id > 0L) {
					out.add(id);
				}
			}
			return out;
		}
		if (raw instanceof String s && StringUtils.hasText(s.trim())) {
			for (String p : s.split(",")) {
				Long id = toLongObject(p);
				if (id != null && id > 0L) {
					out.add(id);
				}
			}
		}
		return out;
	}

	private static void applyRelGoodsListingStatusAndDates(Map<String, Object> row, int nowTime) {
		int activityEnd = intEpoch(row.get("activity_end_time"));
		int activityStart = intEpoch(row.get("activity_start_time"));
		int activityRelease = intEpoch(row.get("activity_release_time"));
		row.remove("last_seconds");
		if (nowTime >= activityEnd) {
			row.put("status", "it_has_ended");
		} else if (nowTime >= activityStart && nowTime < activityEnd) {
			row.put("status", "in_sale");
			row.put("last_seconds", Math.max(0, activityEnd - nowTime));
		} else if (nowTime >= activityRelease && nowTime < activityStart) {
			row.put("status", "in_the_notice");
			row.put("last_seconds", Math.max(0, activityStart - nowTime));
		} else if (nowTime < activityRelease) {
			row.put("status", "waiting");
		}
		row.put("created_date", formatListingDateTime(intEpoch(row.get("created"))));
		row.put("updated_date", formatListingDateTime(intEpoch(row.get("updated"))));
	}

	private static int intEpoch(Object o) {
		Long v = toLongObject(o);
		return v == null ? 0 : v.intValue();
	}

	private static String formatListingDateTime(int epochSec) {
		if (epochSec <= 0) {
			return "";
		}
		return LISTING_DATETIME.format(Instant.ofEpochSecond(epochSec));
	}

	private static Long toLongObject(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return null;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
