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

@Service
public class WxappSeckillActivityInfoService {

	private final SeckillActivityMapper seckillActivityMapper;
	private final SeckillRelGoodsMapper seckillRelGoodsMapper;
	private final SeckillActivityCreateService seckillActivityCreateService;
	private final SeckillActivityOutsideMultiLangReadService seckillActivityOutsideMultiLangReadService;
	private final MarketingActivityCatalogAccess marketingActivityCatalogAccess;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;

	public WxappSeckillActivityInfoService(
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

	public Map<String, Object> getSeckillInfo(
			long companyId,
			long userId,
			String seckillIdRaw,
			int page,
			int pageSize,
			String acceptLanguage,
			String requestLangTag) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();

		Long seckillIdParsed = parsePositiveSeckillIdOrNull(seckillIdRaw);
		SeckillActivity activity = null;
		if (seckillIdParsed != null) {
			activity =
					seckillActivityMapper.selectOne(
							new LambdaQueryWrapper<SeckillActivity>()
									.eq(SeckillActivity::getCompanyId, companyId)
									.eq(SeckillActivity::getSeckillId, seckillIdParsed));
		}

		if (activity != null) {
			LambdaQueryWrapper<SeckillRelGoods> wrapper =
					new LambdaQueryWrapper<SeckillRelGoods>()
							.eq(SeckillRelGoods::getCompanyId, companyId)
							.eq(SeckillRelGoods::getSeckillId, activity.getSeckillId())
							.eq(SeckillRelGoods::getIsShow, Boolean.TRUE)
							.orderByDesc(SeckillRelGoods::getSort)
							.orderByDesc(SeckillRelGoods::getItemId)
							.orderByDesc(SeckillRelGoods::getActivityStartTime);
			Page<SeckillRelGoods> relPage = new Page<>(page, pageSize);
			List<SeckillRelGoods> relRecords = seckillRelGoodsMapper.selectPage(relPage, wrapper).getRecords();
			long relTotal = relPage.getTotal();
			List<Map<String, Object>> relMaps = new ArrayList<>();
			for (SeckillRelGoods r : relRecords) {
				relMaps.add(SeckillActivityCreateService.relGoodsEntityToAdminRow(r));
			}
			Map<String, Object> body =
					seckillActivityCreateService.assembleWxappH5GetInfoPayload(activity, relMaps, relTotal);
			seckillActivityOutsideMultiLangReadService.apply(companyId, activity.getSeckillId(), body, requestLangTag);
			SeckillActivityPayloadSupport.applySeckillActivityBooleanStringFieldsForApi(body);
			result.putAll(body);
		}

		if (result.containsKey("items") && result.get("items") instanceof List<?>) {
			List<?> rawItems = (List<?>) result.get("items");
			List<Long> orderedIds = new ArrayList<>();
			LinkedHashSet<Long> seen = new LinkedHashSet<>();
			for (Object el : rawItems) {
				if (!(el instanceof Map<?, ?> rm)) {
					continue;
				}
				@SuppressWarnings("unchecked")
				Map<String, Object> row = (Map<String, Object>) rm;
				Long itemId = parsePositiveItemId(row.get("item_id"));
				if (itemId != null && seen.add(itemId)) {
					orderedIds.add(itemId);
				}
			}
			if (!orderedIds.isEmpty()) {
				Map<String, Object> itemPack =
						marketingActivityCatalogAccess.loadWxappItemListDataForSeckillGetInfo(
								companyId, userId, orderedIds, acceptLanguage);
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> itemList =
						(List<Map<String, Object>>) itemPack.getOrDefault("list", List.of());
				Map<Long, Map<String, Object>> byItemId = new LinkedHashMap<>();
				for (Map<String, Object> row : itemList) {
					Long id = parsePositiveItemId(row.get("item_id"));
					if (id != null) {
						byItemId.put(id, row);
					}
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
				result.put("items", mergedItems);
			}
		}

		CurrencyExchangeRate row = companyDefaultCurrencyService.getCur(companyId);
		result.put("cur", companyDefaultCurrencyService.toCurResponseMap(row));
		return result;
	}

	private static Long parsePositiveSeckillIdOrNull(String seckillIdRaw) {
		if (seckillIdRaw == null) {
			return null;
		}
		String t = seckillIdRaw.trim();
		if (t.isEmpty()) {
			return null;
		}
		try {
			long v = Long.parseLong(t);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
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
