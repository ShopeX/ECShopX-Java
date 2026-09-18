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

package cn.shopex.ecshopx.goods.service.wxapp;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.core.domain.PageResult;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.service.redis.CompanyTradeRateItemDisplayRedisReadService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.goods.integration.kujiale.KujialeDesignerWorksByItemIdPort;
import cn.shopex.ecshopx.goods.service.items.ItemsMedicineService;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardKaquanListByItemIdQueryService;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import cn.shopex.ecshopx.orders.domain.dto.ShippingTemplateRow;
import cn.shopex.ecshopx.goods.service.pointsmall.PointsmallFrontTdkGivenRenderService;
import cn.shopex.ecshopx.promotions.service.WxappGoodsDetailMemberpreferenceActivityService;
import cn.shopex.ecshopx.tdkset.service.TdkGivenSaveService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappGoodsItemsDetailPostEnrichmentService {

	private final WxappGoodsDetailMemberpreferenceActivityService wxappGoodsDetailMemberpreferenceActivityService;
	private final DiscountCardKaquanListByItemIdQueryService discountCardKaquanListByItemIdQueryService;
	private final ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;
	private final CompanyTradeRateItemDisplayRedisReadService companyTradeRateItemDisplayRedisReadService;
	private final TdkGivenSaveService tdkGivenSaveService;
	private final PointsmallFrontTdkGivenRenderService pointsmallFrontTdkGivenRenderService;
	private final ItemsMedicineService itemsMedicineService;
	private final ObjectProvider<KujialeDesignerWorksByItemIdPort> kujialeDesignerWorksByItemIdPort;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public WxappGoodsItemsDetailPostEnrichmentService(WxappGoodsDetailMemberpreferenceActivityService wxappGoodsDetailMemberpreferenceActivityService,
			DiscountCardKaquanListByItemIdQueryService discountCardKaquanListByItemIdQueryService,
			ShippingTemplatesQueryRepository shippingTemplatesQueryRepository,
			CompanyTradeRateItemDisplayRedisReadService companyTradeRateItemDisplayRedisReadService,
			TdkGivenSaveService tdkGivenSaveService, PointsmallFrontTdkGivenRenderService pointsmallFrontTdkGivenRenderService,
			ItemsMedicineService itemsMedicineService, ObjectProvider<KujialeDesignerWorksByItemIdPort> kujialeDesignerWorksByItemIdPort,
			ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.wxappGoodsDetailMemberpreferenceActivityService = wxappGoodsDetailMemberpreferenceActivityService;
		this.discountCardKaquanListByItemIdQueryService = discountCardKaquanListByItemIdQueryService;
		this.shippingTemplatesQueryRepository = shippingTemplatesQueryRepository;
		this.companyTradeRateItemDisplayRedisReadService = companyTradeRateItemDisplayRedisReadService;
		this.tdkGivenSaveService = tdkGivenSaveService;
		this.pointsmallFrontTdkGivenRenderService = pointsmallFrontTdkGivenRenderService;
		this.itemsMedicineService = itemsMedicineService;
		this.kujialeDesignerWorksByItemIdPort = kujialeDesignerWorksByItemIdPort;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	public void runPostSteps16Through28(Map<String, Object> result, long companyId, long userId, long distributorId, long gradeId, boolean needTdk,
			Map<String, Object> promotionActivityData, long currentItemId, Object groupMinBaselineStore, HttpServletRequest request) {
		post16Memberpreference(result, companyId, userId);
		post17Kaquan(result, companyId, userId, distributorId, gradeId, currentItemId);
		post18NoPost(result, companyId);
		post19LimitedBuyGrade(result, promotionActivityData, gradeId);
		post20StoreSalesAlign(result);
		post21GroupStoreMin(result, promotionActivityData, currentItemId, groupMinBaselineStore);
		post22RateStatus(result, companyId);
		post23SalesStoreSettings(result, companyId);
		post24Tdk(result, companyId, needTdk, request);
		post25MedicineAgain(result, companyId);
		post26SpecItemsCustom(result);
		post27ItemName(result);
		post28DesignWorks(result, currentItemId);
	}

	private void post16Memberpreference(Map<String, Object> result, long companyId, long userId) {
		long goodsId = toLong(result.get("goods_id"));
		List<Map<String, Object>> list = wxappGoodsDetailMemberpreferenceActivityService.listValidMemberpreferenceForGoodsDetail(companyId, userId, goodsId);
		if (!list.isEmpty()) {
			result.put("memberpreference_activity", list.get(0));
		}
	}

	private void post17Kaquan(Map<String, Object> result, long companyId, long userId, long distributorId, long gradeId, long currentItemId) {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("default_item_id", result.get("default_item_id"));
		filter.put("item_main_cat_id", result.get("item_main_cat_id"));
		filter.put("brand_id", result.get("brand_id"));
		filter.put("company_id", companyId);
		filter.put("item_id", currentItemId);
		filter.put("receive", "true");
		long dist = distributorId > 0 ? distributorId : toLong(result.get("distributor_id"));
		filter.put("distributor_id", dist);
		if (gradeId > 0) {
			filter.put("grade_id", (int) Math.min(gradeId, Integer.MAX_VALUE));
		}
		if (userId > 0) {
			filter.put("user_id", userId);
		}
		PageResult<Map<String, Object>> page = discountCardKaquanListByItemIdQueryService.query(filter, 1, 500);
		long totalCount = page.getTotal();
		String totalStr = String.valueOf(totalCount);
		Map<String, Object> kaquanList = new LinkedHashMap<>();
		kaquanList.put("total_count", totalStr);
		Map<String, Object> pagers = new LinkedHashMap<>();
		pagers.put("total", totalStr);
		kaquanList.put("pagers", pagers);
		kaquanList.put("list", page.getList() != null ? page.getList() : List.of());
		result.put("kaquan_list", kaquanList);
	}

	private void post18NoPost(Map<String, Object> result, long companyId) {
		result.put("no_post", List.of());
		long templatesId = toLong(result.get("templates_id"));
		if (templatesId <= 0) {
			return;
		}
		shippingTemplatesQueryRepository.selectTemplateById(companyId, templatesId).map(ShippingTemplateRow::getNopostConf).filter(StringUtils::hasText)
				.ifPresent(raw -> {
					try {
						Object parsed = objectMapper.readValue(raw, Object.class);
						if (parsed instanceof List<?> || parsed instanceof Map<?, ?>) {
							result.put("no_post", parsed);
						}
					} catch (Exception ignored) {
					}
				});
	}

	private void post19LimitedBuyGrade(Map<String, Object> result, Map<String, Object> promotionActivityData, long gradeId) {
		if (promotionActivityData == null) {
			return;
		}
		Object activityType = promotionActivityData.get("activity_type");
		if (!"limited_buy".equals(activityType == null ? null : activityType.toString())) {
			return;
		}
		Object info = promotionActivityData.get("info");
		if (!(info instanceof Map<?, ?> infoMap)) {
			return;
		}
		Object validGradeRaw = infoMap.get("valid_grade");
		if (!(validGradeRaw instanceof List<?> validList)) {
			return;
		}
		boolean hit = false;
		for (Object g : validList) {
			Long ng = normalizeToLong(g);
			if (ng != null && Objects.equals(ng, gradeId)) {
				hit = true;
				break;
			}
		}
		if (hit) {
			result.put("activity_type", activityType);
			@SuppressWarnings("unchecked")
			Map<String, Object> infoCast = (Map<String, Object>) info;
			result.put("activity_info", infoCast);
		}
	}

	private static Long normalizeToLong(Object g) {
		if (g == null) {
			return null;
		}
		if (g instanceof Number n) {
			return n.longValue();
		}
		String s = g.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void post20StoreSalesAlign(Map<String, Object> result) {
		Object v = result.get("item_total_store");
		result.put("store", v != null ? v : result.get("store"));
		Object vs = result.get("item_total_sales");
		result.put("sales", vs != null ? vs : result.get("sales"));
	}

	private void post21GroupStoreMin(Map<String, Object> result, Map<String, Object> promotionActivityData, long currentItemId,
			Object groupMinBaselineStore) {
		if (promotionActivityData == null) {
			return;
		}
		if (!"group".equals(String.valueOf(promotionActivityData.get("activity_type")))) {
			return;
		}
		Object listObj = promotionActivityData.get("list");
		if (!(listObj instanceof Map<?, ?> listMap)) {
			return;
		}
		Object groupSku = listMap.get(String.valueOf(currentItemId));
		if (groupSku == null) {
			groupSku = listMap.get(currentItemId);
		}
		if (!(groupSku instanceof Map<?, ?> gm)) {
			return;
		}
		if (!gm.containsKey("store")) {
			return;
		}
		int currentStore = toInt(result.get("store"));
		int baseline = toInt(groupMinBaselineStore);
		result.put("store", Math.min(currentStore, baseline));
	}

	private void post22RateStatus(Map<String, Object> result, long companyId) {
		result.put("rate_status", companyTradeRateItemDisplayRedisReadService.readRateStatus(companyId));
	}

	private void post23SalesStoreSettings(Map<String, Object> result, long companyId) {
		result.put("sales_setting", companyTradeRateItemDisplayRedisReadService.readItemSalesDisplay(companyId));
		result.put("store_setting", companyTradeRateItemDisplayRedisReadService.readItemStoreDisplay(companyId));
	}

	private void post24Tdk(Map<String, Object> result, long companyId, boolean needTdk, HttpServletRequest request) {
		if (!needTdk) {
			return;
		}
		String countryCodeRaw = resolveCountryCodeForTdk(request);
		Map<String, Object> slice = tdkGivenSaveService.getGivenSetInfo("details", companyId, countryCodeRaw);
		Map<String, Object> tdkData = pointsmallFrontTdkGivenRenderService.render(slice, result);
		mergeTdkContentOverrides(result, tdkData);
		result.put("tdk_data", tdkData);
	}

	private void mergeTdkContentOverrides(Map<String, Object> result, Map<String, Object> tdkData) {
		Object rawTc = result.get("tdk_content");
		if (rawTc == null || !StringUtils.hasText(rawTc.toString())) {
			return;
		}
		try {
			Map<String, Object> content = objectMapper.readValue(rawTc.toString(), new TypeReference<Map<String, Object>>() {});
			if (content == null) {
				return;
			}
			Object t = content.get("title");
			if (t != null && StringUtils.hasText(t.toString())) {
				tdkData.put("title", t.toString());
			}
			Object md = content.get("mate_description");
			if (md != null && StringUtils.hasText(md.toString())) {
				tdkData.put("mate_description", md.toString());
			}
			Object mk = content.get("mate_keywords");
			if (mk != null && StringUtils.hasText(mk.toString())) {
				tdkData.put("mate_keywords", mk.toString());
			}
		} catch (Exception ignored) {
		}
	}

	private void post25MedicineAgain(Map<String, Object> result, long companyId) {
		itemsMedicineService.applyMedicineDataToRows(companyId, List.of(result));
	}

	@SuppressWarnings("unchecked")
	private void post26SpecItemsCustom(Map<String, Object> result) {
		Object raw = result.get("spec_items");
		if (!(raw instanceof List<?> specList)) {
			return;
		}
		for (Object sp : specList) {
			if (!(sp instanceof Map<?, ?> spMap)) {
				continue;
			}
			Map<String, Object> row = (Map<String, Object>) spMap;
			Object itemSpec = row.get("item_spec");
			if (!(itemSpec instanceof List<?> isList)) {
				continue;
			}
			List<Map<String, Object>> specRows = new ArrayList<>();
			for (Object o : isList) {
				if (o instanceof Map<?, ?> m) {
					specRows.add((Map<String, Object>) m);
				}
			}
			specRows.sort((a, b) -> Long.compare(toLong(b.get("spec_id")), toLong(a.get("spec_id"))));
			List<String> ids = new ArrayList<>();
			List<String> names = new ArrayList<>();
			for (Map<String, Object> line : specRows) {
				Object vid = line.get("spec_value_id");
				Object vname = line.get("spec_value_name");
				if (vid != null) {
					ids.add(vid.toString());
				}
				if (vname != null) {
					names.add(vname.toString());
				}
			}
			row.put("custom_spec_id", String.join("-", ids));
			row.put("custom_spec_name", String.join("\u3001", names));
		}
	}

	private void post27ItemName(Map<String, Object> result) {
		result.put("itemName", result.get("item_name"));
	}

	private void post28DesignWorks(Map<String, Object> result, long currentItemId) {
		KujialeDesignerWorksByItemIdPort port = kujialeDesignerWorksByItemIdPort.getIfAvailable();
		List<Map<String, Object>> list = List.of();
		if (port != null) {
			try {
				list = port.listByItemId(currentItemId);
			} catch (Exception e) {
				list = List.of();
			}
		}
		result.put("design_works", list);
	}

	private String resolveCountryCodeForTdk(HttpServletRequest request) {
		String q = request.getParameter("country_code");
		if (StringUtils.hasText(q)) {
			String t = q.trim();
			if (!t.isEmpty()) {
				return t;
			}
		}
		String resolved = RequestLangTag.current(langueProperties);
		if (StringUtils.hasText(resolved) && !"zh-CN".equals(resolved)) {
			return resolved;
		}
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (raw instanceof Map<?, ?> ud) {
			Object cc = ud.get("country_code");
			if (cc != null) {
				String s = cc.toString().trim();
				if (!s.isEmpty()) {
					return s;
				}
			}
		}
		return "zh-CN";
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int toInt(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
