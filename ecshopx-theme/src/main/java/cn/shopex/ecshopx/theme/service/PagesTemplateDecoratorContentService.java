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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.service.DistributorBatchApiRowQueryService;
import cn.shopex.ecshopx.wechat.domain.WeappSetting;
import cn.shopex.ecshopx.wechat.mapper.WeappSettingMapper;
import cn.shopex.ecshopx.wechat.support.WeappSettingLegacySerializeCodec;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PagesTemplateDecoratorContentService {

	private final WeappSettingMapper weappSettingMapper;
	private final WeappSettingParamsI18nReader weappSettingParamsI18nReader;
	private final PagesTemplateItemsInfoService pagesTemplateItemsInfoService;
	private final DistributorBatchApiRowQueryService distributorBatchApiRowQueryService;

	public PagesTemplateDecoratorContentService(
			WeappSettingMapper weappSettingMapper,
			WeappSettingParamsI18nReader weappSettingParamsI18nReader,
			PagesTemplateItemsInfoService pagesTemplateItemsInfoService,
			DistributorBatchApiRowQueryService distributorBatchApiRowQueryService) {
		this.weappSettingMapper = weappSettingMapper;
		this.weappSettingParamsI18nReader = weappSettingParamsI18nReader;
		this.pagesTemplateItemsInfoService = pagesTemplateItemsInfoService;
		this.distributorBatchApiRowQueryService = distributorBatchApiRowQueryService;
	}

	public List<Map<String, Object>> buildDecoratedComponentRows(
			long companyId,
			Long pagesTemplateId,
			String templateName,
			String version,
			String requestLocaleTag) {
		if (pagesTemplateId == null) {
			return new ArrayList<>();
		}
		return buildDecoratedComponentRows(
				companyId,
				pagesTemplateId.longValue(),
				templateName,
				version,
				requestLocaleTag,
				0L,
				0,
				0L,
				null,
				null,
				1,
				Integer.MAX_VALUE);
	}

	public List<Map<String, Object>> buildDecoratedComponentRows(
			long companyId,
			long pagesTemplateId,
			String templateName,
			String version,
			String requestLocaleTag,
			long userId,
			int distributorId,
			long eActivityId,
			Long weappSettingId,
			Long goodsGridTabId,
			int page,
			int pageSize) {
		String tn = templateName == null ? "" : templateName;
		String v = version == null || version.isBlank() ? "v1.0.2" : version.trim();
		int templateIdInt = Math.toIntExact(pagesTemplateId);
		LambdaQueryWrapper<WeappSetting> w =
				new LambdaQueryWrapper<WeappSetting>()
						.eq(WeappSetting::getCompanyId, companyId)
						.eq(WeappSetting::getTemplateName, tn)
						.eq(WeappSetting::getPageName, "index")
						.eq(WeappSetting::getVersion, v)
						.eq(WeappSetting::getPagesTemplateId, templateIdInt)
						.orderByAsc(WeappSetting::getSortBy)
						.orderByAsc(WeappSetting::getId);
		if (weappSettingId != null && weappSettingId > 0L) {
			w.eq(WeappSetting::getId, weappSettingId);
		}
		List<WeappSetting> rows = weappSettingMapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return new ArrayList<>();
		}
		List<Long> ids = rows.stream().map(WeappSetting::getId).filter(Objects::nonNull).toList();
		Map<Long, String> langById = weappSettingParamsI18nReader.findParamsByLocale(companyId, ids, requestLocaleTag);
		List<Map<String, Object>> list = new ArrayList<>();
		for (WeappSetting row : rows) {
			Object paramsObj = WeappSettingLegacySerializeCodec.decode(row.getParams());
			String langRaw = langById.get(row.getId());
			if (StringUtils.hasText(langRaw)) {
				paramsObj = WeappSettingLegacySerializeCodec.decode(langRaw);
			}
			if (!(paramsObj instanceof Map<?, ?> rawMap)) {
				throw new ResourceException("装修配置格式无效");
			}
			LinkedHashMap<String, Object> rowParams = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : rawMap.entrySet()) {
				rowParams.put(String.valueOf(e.getKey()), e.getValue());
			}
			rowParams.put("user_id", userId);
			rowParams.put("distributor_id", distributorId);
			rowParams.put("e_activity_id", eActivityId);
			String rowName = row.getName() == null ? "" : row.getName();
			switch (rowName) {
				case "goodsScroll", "goodsGrid" ->
						applyGoodsScrollOrGrid(companyId, rowName, rowParams, page, pageSize);
				case "goodsGridTab" ->
						applyGoodsGridTab(
								companyId, rowParams, weappSettingId, goodsGridTabId, page, pageSize);
				case "contentpart" -> applyContentpart(companyId, rowParams);
				case "shop" -> applyShop(companyId, rowParams);
				case "store" -> applyStore(companyId, rowParams);
				default -> {}
			}
			String pageName =
					row.getPageName() == null || row.getPageName().isBlank() ? "index" : row.getPageName();
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", row.getId());
			item.put("template_name", row.getTemplateName());
			item.put("company_id", row.getCompanyId());
			item.put("name", row.getName());
			item.put("page_name", pageName);
			item.put("params", rowParams);
			list.add(item);
		}
		return list;
	}

	public void enrichAdminDecodedWidgetParams(
			long companyId,
			String widgetName,
			LinkedHashMap<String, Object> params,
			Boolean merchantStatusPerItemOrNull) {
		String n = widgetName == null ? "" : widgetName;
		switch (n) {
			case "goodsScroll", "goodsGrid" -> applyGoodsScrollOrGrid(
					companyId, n, params, 1, Integer.MAX_VALUE, merchantStatusPerItemOrNull);
			case "goodsGridTab" -> applyGoodsGridTab(
					companyId, params, null, null, 1, Integer.MAX_VALUE, merchantStatusPerItemOrNull);
			default -> {}
		}
	}

	private void applyGoodsScrollOrGrid(
			long companyId, String configName, LinkedHashMap<String, Object> rowParams, int page, int pageSize) {
		applyGoodsScrollOrGrid(companyId, configName, rowParams, page, pageSize, null);
	}

	private void applyGoodsScrollOrGrid(
			long companyId,
			String configName,
			LinkedHashMap<String, Object> rowParams,
			int page,
			int pageSize,
			Boolean merchantStatusPerItemOrNull) {
		Object dataObj = rowParams.get("data");
		if (!(dataObj instanceof List<?> fullList)) {
			return;
		}
		int from = (page - 1) * pageSize;
		int fullSize = fullList.size();
		List<?> slice =
				from >= fullSize
						? List.of()
						: fullList.subList(from, Math.min(from + pageSize, fullSize));
		int currentCount = from + slice.size();
		rowParams.put("more", currentCount < fullSize);
		List<Long> itemIds = collectItemIdsFromElements(slice);
		Map<Long, Map<String, Object>> itemData =
				pagesTemplateItemsInfoService.getItemsInfo(companyId, configName, itemIds, rowParams);
		List<Object> reordered = new ArrayList<>();
		for (Object o : slice) {
			if (!(o instanceof Map<?, ?> raw)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> datum = (Map<String, Object>) (Map<?, ?>) raw;
			long lookupId = resolveItemLookupId(datum);
			Map<String, Object> goodsValue = lookupId > 0L ? itemData.get(lookupId) : null;
			if (goodsValue == null || goodsValue.isEmpty()) {
				continue;
			}
			mergeGoodsPresentation(datum, goodsValue, intOrZero(rowParams.get("distributor_id")));
			if (merchantStatusPerItemOrNull != null) {
				datum.put("merchant_status", merchantStatusPerItemOrNull.booleanValue());
			}
			reordered.add(datum);
		}
		rowParams.put("data", new ArrayList<>(reordered));
	}

	private void applyGoodsGridTab(
			long companyId,
			LinkedHashMap<String, Object> rowParams,
			Long weappSettingId,
			Long goodsGridTabId,
			int page,
			int pageSize) {
		applyGoodsGridTab(
				companyId, rowParams, weappSettingId, goodsGridTabId, page, pageSize, null);
	}

	private void applyGoodsGridTab(
			long companyId,
			LinkedHashMap<String, Object> rowParams,
			Long weappSettingId,
			Long goodsGridTabId,
			int page,
			int pageSize,
			Boolean merchantStatusPerItemOrNull) {
		Object listObj = rowParams.get("list");
		if (listObj == null) {
			return;
		}
		boolean filterTabs = weappSettingId != null && weappSettingId > 0L;
		String tabTarget = goodsGridTabId == null ? null : String.valueOf(goodsGridTabId);
		if (listObj instanceof Map<?, ?> map) {
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : map.entrySet()) {
				String gridKey = String.valueOf(e.getKey());
				if (filterTabs && !Objects.equals(gridKey, tabTarget)) {
					continue;
				}
				if (e.getValue() instanceof Map<?, ?> tabMap) {
					@SuppressWarnings("unchecked")
					Map<String, Object> t = (Map<String, Object>) (Map<?, ?>) tabMap;
					applyGoodsGridTabSlice(
							companyId, rowParams, t, page, pageSize, merchantStatusPerItemOrNull);
					out.put(gridKey, t);
				}
			}
			rowParams.put("list", out);
		} else if (listObj instanceof List<?> tabs) {
			List<Object> out = new ArrayList<>();
			for (int i = 0; i < tabs.size(); i++) {
				String gridKey = String.valueOf(i);
				if (filterTabs && !Objects.equals(gridKey, tabTarget)) {
					continue;
				}
				Object tabObj = tabs.get(i);
				if (tabObj instanceof Map<?, ?> tabMap) {
					@SuppressWarnings("unchecked")
					Map<String, Object> t = (Map<String, Object>) (Map<?, ?>) tabMap;
					applyGoodsGridTabSlice(
							companyId, rowParams, t, page, pageSize, merchantStatusPerItemOrNull);
					out.add(t);
				}
			}
			rowParams.put("list", new ArrayList<>(out));
		}
	}

	private void applyGoodsGridTabSlice(
			long companyId,
			LinkedHashMap<String, Object> rowParams,
			Map<String, Object> tab,
			int page,
			int pageSize,
			Boolean merchantStatusPerItemOrNull) {
		Object gl = tab.get("goodsList");
		if (!(gl instanceof List<?> fullGoods)) {
			return;
		}
		int from = (page - 1) * pageSize;
		int fullSize = fullGoods.size();
		List<?> slice =
				from >= fullSize
						? List.of()
						: fullGoods.subList(from, Math.min(from + pageSize, fullSize));
		int currentCount = from + slice.size();
		tab.put("more", currentCount < fullSize);
		List<Long> itemIds = collectItemIdsFromElements(slice);
		Map<Long, Map<String, Object>> itemData =
				pagesTemplateItemsInfoService.getItemsInfo(companyId, "goodsGridTab", itemIds, rowParams);
		List<Object> reordered = new ArrayList<>();
		for (Object o : slice) {
			if (!(o instanceof Map<?, ?> raw)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> datum = (Map<String, Object>) (Map<?, ?>) raw;
			long lookupId = resolveItemLookupId(datum);
			Map<String, Object> goodsValue = lookupId > 0L ? itemData.get(lookupId) : null;
			if (goodsValue == null || goodsValue.isEmpty()) {
				continue;
			}
			mergeGoodsPresentation(datum, goodsValue, intOrZero(rowParams.get("distributor_id")));
			if (merchantStatusPerItemOrNull != null) {
				datum.put("merchant_status", merchantStatusPerItemOrNull.booleanValue());
			}
			reordered.add(datum);
		}
		tab.put("goodsList", new ArrayList<>(reordered));
	}

	private void applyContentpart(long companyId, LinkedHashMap<String, Object> rowParams) {
		Object dataRoot = rowParams.get("data");
		if (!(dataRoot instanceof Map<?, ?> dm)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dataMap = (Map<String, Object>) (Map<?, ?>) dm;
		Object inner = dataMap.get("data");
		if (!(inner instanceof List<?> partsList)) {
			return;
		}
		List<Long> distributorIds = new ArrayList<>();
		for (Object navObj : partsList) {
			if (!(navObj instanceof Map<?, ?> navV)) {
				continue;
			}
			Object childrenObj = navV.get("children");
			if (!(childrenObj instanceof List<?> children)) {
				continue;
			}
			for (Object childObj : children) {
				if (!(childObj instanceof Map<?, ?> childV)) {
					continue;
				}
				if (!"shop".equals(String.valueOf(childV.get("name")))) {
					continue;
				}
				Object shopData = childV.get("data");
				if (!(shopData instanceof List<?> sd)) {
					continue;
				}
				for (Object row : sd) {
					if (row instanceof Map<?, ?> m) {
						long did = longOrZero(m.get("distributor_id"));
						if (did > 0L) {
							distributorIds.add(did);
						}
					}
				}
			}
		}
		if (distributorIds.isEmpty()) {
			return;
		}
		Map<Long, Map<String, Object>> distRows =
				distributorBatchApiRowQueryService.loadForPagesTemplateDecoration(companyId, distributorIds);
		long nowSec = System.currentTimeMillis() / 1000L;
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> parts = (List<Map<String, Object>>) (List<?>) partsList;
		for (Map<String, Object> navV : parts) {
			if (navV == null) {
				continue;
			}
			Object childrenObj = navV.get("children");
			if (!(childrenObj instanceof List<?> childrenRaw)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> children = (List<Map<String, Object>>) (List<?>) childrenRaw;
			for (Map<String, Object> childV : children) {
				if (childV == null) {
					continue;
				}
				if (!"shop".equals(String.valueOf(childV.get("name")))) {
					continue;
				}
				Object shopData = childV.get("data");
				if (!(shopData instanceof List<?> shopRowsRaw)) {
					continue;
				}
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> shopRows = (List<Map<String, Object>>) (List<?>) shopRowsRaw;
				for (int i = 0; i < shopRows.size(); i++) {
					Map<String, Object> childData = shopRows.get(i);
					if (childData == null) {
						continue;
					}
					long did = longOrZero(childData.get("distributor_id"));
					Map<String, Object> distributorInfo = distRows.get(did);
					if (distributorInfo == null || distributorInfo.isEmpty()) {
						continue;
					}
					mergeDecorationDistributorIntoRow(childData, distributorInfo, nowSec);
					shopRows.set(i, childData);
				}
			}
		}
	}

	private void applyShop(long companyId, LinkedHashMap<String, Object> rowParams) {
		Object dataObj = rowParams.get("data");
		if (!(dataObj instanceof List<?> rows)) {
			return;
		}
		List<Long> distributorIds = new ArrayList<>();
		for (Object o : rows) {
			if (o instanceof Map<?, ?> m) {
				long did = longOrZero(m.get("distributor_id"));
				if (did > 0L) {
					distributorIds.add(did);
				}
			}
		}
		if (distributorIds.isEmpty()) {
			return;
		}
		Map<Long, Map<String, Object>> distRows =
				distributorBatchApiRowQueryService.loadForPagesTemplateDecoration(companyId, distributorIds);
		long nowSec = System.currentTimeMillis() / 1000L;
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> dataList = (List<Map<String, Object>>) (List<?>) rows;
		for (int i = 0; i < dataList.size(); i++) {
			Map<String, Object> tmp = dataList.get(i);
			if (tmp == null) {
				continue;
			}
			long did = longOrZero(tmp.get("distributor_id"));
			Map<String, Object> distributorInfo = distRows.get(did);
			if (distributorInfo == null || distributorInfo.isEmpty()) {
				continue;
			}
			mergeDecorationDistributorIntoRow(tmp, distributorInfo, nowSec);
			dataList.set(i, tmp);
		}
	}

	private static void mergeDecorationDistributorIntoRow(
			Map<String, Object> row, Map<String, Object> distributorInfo, long nowSec) {
		Map<String, Object> overlay = new LinkedHashMap<>(distributorInfo);
		for (Map.Entry<String, Object> e : overlay.entrySet()) {
			row.put(e.getKey(), e.getValue());
		}
	}

	private void applyStore(long companyId, LinkedHashMap<String, Object> params) {
		Object dataObj = params.get("data");
		if (!(dataObj instanceof List<?> dataList)) {
			return;
		}
		List<Long> distributorIds = new ArrayList<>();
		for (Object o : dataList) {
			if (o instanceof Map<?, ?> m) {
				long id = longOrZero(m.get("id"));
				if (id > 0L) {
					distributorIds.add(id);
				}
			}
		}
		Map<Long, Map<String, Object>> distRows =
				distributorBatchApiRowQueryService.loadByCompanyAndDistributorIds(companyId, distributorIds);
		for (Object o : dataList) {
			if (!(o instanceof Map<?, ?> raw)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> datum = (Map<String, Object>) (Map<?, ?>) raw;
			long distId = longOrZero(datum.get("id"));
			Map<String, Object> dr = distRows.get(distId);
			if (dr != null) {
				Object nm = dr.get("name");
				if (nm == null) {
					nm = dr.get("distributorName");
				}
				datum.put("name", nm != null ? nm.toString() : "");
				Object lg = dr.get("logo");
				if (lg == null) {
					lg = dr.get("distributorLogo");
				}
				datum.put("logo", lg != null ? lg.toString() : "");
			}
			Object itemsObj = datum.get("items");
			if (itemsObj instanceof List<?> itemsList) {
				List<Long> itemIds = collectItemIdsFromElements(itemsList);
				Map<Long, Map<String, Object>> itemData =
						pagesTemplateItemsInfoService.getItemsInfo(companyId, "store", itemIds, params);
				List<Object> reordered = new ArrayList<>();
				for (Object el : itemsList) {
					if (!(el instanceof Map<?, ?> im)) {
						continue;
					}
					@SuppressWarnings("unchecked")
					Map<String, Object> itemEl = (Map<String, Object>) (Map<?, ?>) im;
					long lookupId = resolveItemLookupId(itemEl);
					Map<String, Object> goodsValue = lookupId > 0L ? itemData.get(lookupId) : null;
					if (goodsValue == null || goodsValue.isEmpty()) {
						continue;
					}
					mergeGoodsPresentation(itemEl, goodsValue, intOrZero(params.get("distributor_id")));
					reordered.add(itemEl);
				}
				datum.put("items", new ArrayList<>(reordered));
			}
		}
	}

	private static void mergeGoodsPresentation(
			Map<String, Object> datum, Map<String, Object> goodsValue, int outerDistributorId) {
		datum.put("price", goodsValue.get("price"));
		datum.put("imgUrl", firstPicOrImgUrl(goodsValue));
		datum.put("title", goodsValue.get("item_name"));
		datum.put("brand", goodsValue.get("brand"));
		datum.put("nospec", goodsValue.get("nospec"));
		datum.put("special_type", goodsValue.get("special_type"));
		datum.put("member_price", goodsValue.get("member_price"));
		datum.put("market_price", goodsValue.get("market_price"));
		datum.put("act_price", goodsValue.get("activity_price"));
		datum.put("vip_price", goodsValue.get("vip_price"));
		datum.put("svip_price", goodsValue.get("svip_price"));
		datum.put("promotion_activity", goodsValue.get("promotion_activity"));
		datum.put("promotionActivity", goodsValue.get("promotionActivity"));
		datum.put("cross_border_tax", goodsValue.get("cross_border_tax"));
		datum.put("cross_border_tax_rate", goodsValue.get("cross_border_tax_rate"));
		int gd = intOrZero(goodsValue.get("distributor_id"));
		if (gd == 0) {
			datum.put("distributor_id", outerDistributorId);
		} else {
			datum.put("distributor_id", gd);
		}
	}

	private static String firstPicOrImgUrl(Map<String, Object> goodsValue) {
		Object pics = goodsValue.get("pics");
		if (pics instanceof List<?> pl && !pl.isEmpty()) {
			Object z = pl.get(0);
			return z != null ? z.toString() : "";
		}
		Object img = goodsValue.get("imgUrl");
		return img != null ? img.toString() : "";
	}

	private static List<Long> collectItemIdsFromElements(Iterable<?> dataList) {
		List<Long> out = new ArrayList<>();
		for (Object o : dataList) {
			if (o instanceof Map<?, ?> m) {
				long id = resolveItemLookupId((Map<?, ?>) m);
				if (id > 0L) {
					out.add(id);
				}
			}
		}
		return out;
	}

	private static long resolveItemLookupId(Map<?, ?> datum) {
		long v = longOrZero(datum.get("goods_id"));
		if (v > 0L) {
			return v;
		}
		v = longOrZero(datum.get("goodsId"));
		if (v > 0L) {
			return v;
		}
		return longOrZero(datum.get("item_id"));
	}

	private static int intOrZero(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v == null) {
			return 0;
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long longOrZero(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
