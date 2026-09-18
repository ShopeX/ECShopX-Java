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

package cn.shopex.ecshopx.promotions.service.wxapp;

import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.service.SeckillActivityCreateService;
import cn.shopex.ecshopx.promotions.service.multilang.SeckillActivityOutsideMultiLangReadService;
import cn.shopex.ecshopx.promotions.service.support.SeckillActivityPayloadSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappSeckillActivityListService {

	private final SeckillActivityMapper seckillActivityMapper;
	private final SeckillRelGoodsMapper seckillRelGoodsMapper;
	private final SeckillActivityCreateService seckillActivityCreateService;
	private final SeckillActivityOutsideMultiLangReadService seckillActivityOutsideMultiLangReadService;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;

	public WxappSeckillActivityListService(
			SeckillActivityMapper seckillActivityMapper,
			SeckillRelGoodsMapper seckillRelGoodsMapper,
			SeckillActivityCreateService seckillActivityCreateService,
			SeckillActivityOutsideMultiLangReadService seckillActivityOutsideMultiLangReadService,
			MarketingActivityCatalogAccess marketingActivityCatalogAccess,
			CompanyDefaultCurrencyService companyDefaultCurrencyService) {
		this.seckillActivityMapper = seckillActivityMapper;
		this.seckillRelGoodsMapper = seckillRelGoodsMapper;
		this.seckillActivityCreateService = seckillActivityCreateService;
		this.seckillActivityOutsideMultiLangReadService = seckillActivityOutsideMultiLangReadService;
		this.marketingActivityCatalogAccess = marketingActivityCatalogAccess;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
	}

	public Map<String, Object> getSeckillList(
			long companyId,
			long userId,
			String statusRaw,
			String itemTypeRaw,
			String seckillTypeRaw,
			int page,
			int pageSize,
			String acceptLanguage,
			String requestLangTag) {
		String status = (statusRaw == null) ? "" : statusRaw.trim();
		String itemType =
				(!StringUtils.hasText(itemTypeRaw)) ? "normal" : itemTypeRaw.trim();
		String seckillType =
				(!StringUtils.hasText(seckillTypeRaw)) ? "normal" : seckillTypeRaw.trim();

		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaQueryWrapper<SeckillActivity> wrapper =
				new LambdaQueryWrapper<SeckillActivity>()
						.eq(SeckillActivity::getCompanyId, companyId)
						.eq(SeckillActivity::getItemType, itemType)
						.eq(SeckillActivity::getSeckillType, seckillType);
		if ("notice".equals(status)) {
			wrapper
					.le(SeckillActivity::getActivityReleaseTime, now)
					.gt(SeckillActivity::getActivityStartTime, now)
					.eq(SeckillActivity::getDisabled, false);
		} else if ("valid".equals(status)) {
			wrapper
					.le(SeckillActivity::getActivityStartTime, now)
					.gt(SeckillActivity::getActivityEndTime, now)
					.eq(SeckillActivity::getDisabled, false);
		} else {
			wrapper
					.le(SeckillActivity::getActivityReleaseTime, now)
					.gt(SeckillActivity::getActivityEndTime, now)
					.eq(SeckillActivity::getDisabled, false);
		}
		wrapper.orderByAsc(SeckillActivity::getActivityStartTime);

		Page<SeckillActivity> p = new Page<>(page, pageSize);
		seckillActivityMapper.selectPage(p, wrapper);
		long totalCount = p.getTotal();
		List<SeckillActivity> records = p.getRecords();

		List<Map<String, Object>> listRows = new ArrayList<>();
		if (!records.isEmpty()) {
			LinkedHashSet<Long> idSet = new LinkedHashSet<>();
			for (SeckillActivity act : records) {
				Long sid = act.getSeckillId();
				if (sid != null && sid > 0L) {
					idSet.add(sid);
				}
			}
			List<Long> pageSeckillIds = new ArrayList<>(idSet);

			List<SeckillRelGoods> allRel = List.of();
			if (!pageSeckillIds.isEmpty()) {
				LambdaQueryWrapper<SeckillRelGoods> relW =
						new LambdaQueryWrapper<SeckillRelGoods>()
								.eq(SeckillRelGoods::getCompanyId, companyId)
								.in(SeckillRelGoods::getSeckillId, pageSeckillIds)
								.orderByDesc(SeckillRelGoods::getSort)
								.orderByDesc(SeckillRelGoods::getItemId)
								.orderByDesc(SeckillRelGoods::getActivityStartTime);
				allRel = seckillRelGoodsMapper.selectList(relW);
			}

			Map<Long, List<Map<String, Object>>> relBySid = new LinkedHashMap<>();
			for (SeckillRelGoods r : allRel) {
				relBySid
						.computeIfAbsent(r.getSeckillId(), k -> new ArrayList<>())
						.add(SeckillActivityCreateService.relGoodsEntityToAdminRow(r));
			}

			for (SeckillActivity act : records) {
				List<Map<String, Object>> relMaps =
						relBySid.getOrDefault(act.getSeckillId(), List.of());
				listRows.add(seckillActivityCreateService.assembleWxappH5GetListRowPayload(act, relMaps));
			}
		}

		seckillActivityOutsideMultiLangReadService.applyBatch(companyId, listRows, requestLangTag);
		for (Map<String, Object> row : listRows) {
			SeckillActivityPayloadSupport.applySeckillActivityBooleanStringFieldsForApi(row);
		}

		if (!listRows.isEmpty()) {
			List<Long> orderedItemIds = new ArrayList<>();
			LinkedHashSet<Long> seen = new LinkedHashSet<>();
			for (Map<String, Object> row : listRows) {
				if (!row.containsKey("items") || !(row.get("items") instanceof List<?> rawItems)) {
					continue;
				}
				for (Object el : rawItems) {
					if (!(el instanceof Map<?, ?>)) {
						continue;
					}
					@SuppressWarnings("unchecked")
					Map<String, Object> m = (Map<String, Object>) el;
					Long itemId = parsePositiveItemId(m.get("item_id"));
					if (itemId != null && seen.add(itemId)) {
						orderedItemIds.add(itemId);
					}
				}
			}
			if (!orderedItemIds.isEmpty()) {
				Map<String, Object> itemPack =
						marketingActivityCatalogAccess.loadWxappItemListDataForSeckillGetInfo(
								companyId, userId, orderedItemIds, acceptLanguage);
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> itemList =
						(List<Map<String, Object>>) itemPack.getOrDefault("list", List.of());
				Map<Long, Map<String, Object>> byItemId = new LinkedHashMap<>();
				for (Map<String, Object> itemRow : itemList) {
					Long id = parsePositiveItemId(itemRow.get("item_id"));
					if (id != null) {
						byItemId.put(id, itemRow);
					}
				}
				for (Map<String, Object> row : listRows) {
					if (!row.containsKey("items") || !(row.get("items") instanceof List<?> rawItems)) {
						continue;
					}
					List<Map<String, Object>> mergedItems = new ArrayList<>();
					for (Object el : rawItems) {
						if (!(el instanceof Map<?, ?> rm)) {
							continue;
						}
						@SuppressWarnings("unchecked")
						Map<String, Object> relRow = (Map<String, Object>) rm;
						Long relItemId = parsePositiveItemId(relRow.get("item_id"));
						LinkedHashMap<String, Object> merged = new LinkedHashMap<>(relRow);
						Map<String, Object> enrich = relItemId != null ? byItemId.get(relItemId) : null;
						if (enrich != null) {
							merged.putAll(enrich);
						}
						mergedItems.add(merged);
					}
					row.put("items", mergedItems);
				}
			}
		}

		CurrencyExchangeRate curRow = companyDefaultCurrencyService.getCur(companyId);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", listRows);
		out.put("cur", companyDefaultCurrencyService.toCurResponseMap(curRow));
		return out;
	}

	private static Long parsePositiveItemId(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			long x = n.longValue();
			return x > 0L ? x : null;
		}
		try {
			long x = Long.parseLong(Objects.toString(v, "").trim());
			return x > 0L ? x : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
