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

import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.goods.service.popularize.CompanyPopularizeConfigReadService;
import cn.shopex.ecshopx.promotions.service.SkuValidMarketingActivityService;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappGoodsItemsDetailNormalBranchService {

	private final WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;
	private final SkuValidMarketingActivityService skuValidMarketingActivityService;
	private final CompanyPopularizeConfigReadService companyPopularizeConfigReadService;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;

	public WxappGoodsItemsDetailNormalBranchService(WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService,
			SkuValidMarketingActivityService skuValidMarketingActivityService,
			CompanyPopularizeConfigReadService companyPopularizeConfigReadService, CompanyDefaultCurrencyService companyDefaultCurrencyService) {
		this.wxappGoodsItemsListMemberPriceApplyService = wxappGoodsItemsListMemberPriceApplyService;
		this.skuValidMarketingActivityService = skuValidMarketingActivityService;
		this.companyPopularizeConfigReadService = companyPopularizeConfigReadService;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
	}

	public Map<String, Object> applyNormalBranchToDetail(Map<String, Object> result, long companyId, long userId, long distributorId,
			@Nullable String acceptLanguageHeader) {
		result.put("activity_type", "normal");
		applyMemberPriceToDetailAndSpecItems(result, companyId, userId, acceptLanguageHeader);
		long goodsId = toLong(result.get("goods_id"));
		List<Map<String, Object>> pa = goodsId > 0L
				? skuValidMarketingActivityService.getValidMarketingActivityByGoodsId(companyId, goodsId, userId, distributorId)
				: skuValidMarketingActivityService.getValidMarketingActivityByItemId(companyId, toLong(result.get("item_id")), userId, distributorId);
		result.put("promotion_activity", pa);
		Map<String, Object> popularizeSlice = companyPopularizeConfigReadService.readWxappPromoterConfigSlice(companyId);
		applyPromoterPriceFromConfig(result, popularizeSlice);
		CurrencyExchangeRate cur = companyDefaultCurrencyService.getCur(companyId);
		if (cur != null) {
			result.put("cur", currencyToMap(cur));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private void applyMemberPriceToDetailAndSpecItems(Map<String, Object> result, long companyId, long userId,
			@Nullable String acceptLanguageHeader) {
		List<Map<String, Object>> batch = new ArrayList<>();
		batch.add(result);
		List<Map<String, Object>> specRows = new ArrayList<>();
		Object specRaw = result.get("spec_items");
		if (specRaw instanceof List<?> specList) {
			for (Object o : specList) {
				if (o instanceof Map<?, ?> m) {
					specRows.add((Map<String, Object>) m);
				}
			}
		}
		batch.addAll(specRows);
		wxappGoodsItemsListMemberPriceApplyService.applyForRows(companyId, userId, batch, acceptLanguageHeader);
		for (Map<String, Object> sp : specRows) {
			sp.remove("member_grade_name");
		}
	}

	@SuppressWarnings("unchecked")
	private static void applyPromoterPriceFromConfig(Map<String, Object> itemInfo, Map<String, Object> popularizeConfigSlice) {
		Map<String, Object> ratioRoot = popularizeConfigSlice.get("popularize_ratio") instanceof Map<?, ?> m
				? (Map<String, Object>) m
				: Map.of();
		String ratioType = str(ratioRoot.get("type"));
		Object rc = itemInfo.get("rebate_conf");
		JsonNode conf = asJsonNode(rc);
		long priceFen = longVal(itemInfo.get("price"));
		long costFen = longVal(itemInfo.get("cost_price"));
		BigDecimal ratio = BigDecimal.ZERO;
		if (conf != null && conf.has("value") && conf.get("value").isObject()) {
			JsonNode val = conf.get("value");
			JsonNode fl = val.get("first_level");
			if (fl != null && fl.isNumber() && fl.doubleValue() > 0) {
				String ctype = conf.has("type") && conf.get("type").isTextual() ? conf.get("type").asText() : "";
				if ("money".equals(ctype)) {
					BigDecimal yuan = BigDecimal.valueOf(fl.doubleValue());
					itemInfo.put("promoter_price", yuan.multiply(BigDecimal.valueOf(100)).longValue());
					return;
				}
				if ("ratio".equals(ctype) && conf.has("ratio_type")) {
					String rt = conf.get("ratio_type").asText("");
					Map<String, Object> pr = popularizeConfigSlice.get("popularize_ratio") instanceof Map<?, ?> x
							? (Map<String, Object>) x
							: Map.of();
					String popularizeRatioType = str(pr.get("type"));
					if (popularizeRatioType.equals(rt)) {
						ratio = BigDecimal.valueOf(fl.doubleValue());
					}
				}
			}
		}
		if (ratio.compareTo(BigDecimal.ZERO) <= 0) {
			ratio = resolveDefaultRatio(ratioRoot, ratioType);
		}
		BigDecimal promoterPrice;
		if ("profit".equals(ratioType)) {
			BigDecimal margin = BigDecimal.valueOf(priceFen - costFen);
			if (margin.compareTo(BigDecimal.ZERO) < 0) {
				margin = BigDecimal.ZERO;
			}
			promoterPrice = margin.multiply(ratio).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
		} else {
			promoterPrice = BigDecimal.valueOf(priceFen).multiply(ratio).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
		}
		if (promoterPrice.compareTo(BigDecimal.ZERO) < 0) {
			promoterPrice = BigDecimal.ZERO;
		}
		long fen = promoterPrice.longValue();
		itemInfo.put("promoter_price", fen >= 1L ? fen : 0L);
	}

	private static BigDecimal resolveDefaultRatio(Map<String, Object> ratioRoot, String ratioType) {
		if ("profit".equals(ratioType)) {
			return nestedRatio(ratioRoot, "profit", "first_level", "ratio");
		}
		return nestedRatio(ratioRoot, "order_money", "first_level", "ratio");
	}

	private static BigDecimal nestedRatio(Map<String, Object> root, String a, String b, String c) {
		Object oa = root.get(a);
		if (!(oa instanceof Map<?, ?> ma)) {
			return BigDecimal.ZERO;
		}
		Object ob = ma.get(b);
		if (!(ob instanceof Map<?, ?> mb)) {
			return BigDecimal.ZERO;
		}
		Object oc = mb.get(c);
		if (oc instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		if (oc != null && StringUtils.hasText(oc.toString())) {
			try {
				return new BigDecimal(oc.toString().trim());
			} catch (NumberFormatException e) {
				return BigDecimal.ZERO;
			}
		}
		return BigDecimal.ZERO;
	}

	private static JsonNode asJsonNode(Object rc) {
		if (rc instanceof JsonNode j) {
			return j;
		}
		return null;
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static LinkedHashMap<String, Object> currencyToMap(CurrencyExchangeRate c) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("currency", c.getCurrency());
		m.put("title", c.getTitle());
		m.put("symbol", c.getSymbol());
		m.put("rate", c.getRate());
		m.put("is_default", Boolean.TRUE.equals(c.getIsDefault()));
		if (c.getUsePlatform() != null) {
			m.put("use_platform", c.getUsePlatform());
		}
		return m;
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
}
