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

package cn.shopex.ecshopx.goods.service.salesperson;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartCompanyProductModelReader;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.DistributorItemsDetailMergeService;
import cn.shopex.ecshopx.goods.service.items.PlatformItemsDetailCoreService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsDetailPromotionActivityService;
import cn.shopex.ecshopx.goods.service.wxapp.WxappGoodsItemsListMemberPriceApplyService;
import cn.shopex.ecshopx.promotions.service.GoodsItemsListPromotionEnrichmentService;
import cn.shopex.ecshopx.promotions.service.SalespersonItemsShelvesActivityNameQueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SalespersonItemsDetailServiceImpl implements SalespersonItemsDetailService {

	private static final Map<String, Object> EMPTY_ITEM = Map.of("item_id", 0L);

	private final ItemsRepository itemsRepository;
	private final WxappGoodsItemsDetailPromotionActivityService wxappGoodsItemsDetailPromotionActivityService;
	private final PlatformItemsDetailCoreService platformItemsDetailCoreService;
	private final DistributorItemsDetailMergeService distributorItemsDetailMergeService;
	private final OperatorCartCompanyProductModelReader operatorCartCompanyProductModelReader;
	private final WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService;
	private final SalespersonItemsShelvesActivityNameQueryService salespersonItemsShelvesActivityNameQueryService;
	private final LangueProperties langueProperties;

	public SalespersonItemsDetailServiceImpl(ItemsRepository itemsRepository,
			WxappGoodsItemsDetailPromotionActivityService wxappGoodsItemsDetailPromotionActivityService,
			PlatformItemsDetailCoreService platformItemsDetailCoreService,
			DistributorItemsDetailMergeService distributorItemsDetailMergeService,
			OperatorCartCompanyProductModelReader operatorCartCompanyProductModelReader,
			WxappGoodsItemsListMemberPriceApplyService wxappGoodsItemsListMemberPriceApplyService,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			GoodsItemsListPromotionEnrichmentService goodsItemsListPromotionEnrichmentService,
			SalespersonItemsShelvesActivityNameQueryService salespersonItemsShelvesActivityNameQueryService,
			LangueProperties langueProperties) {
		this.itemsRepository = itemsRepository;
		this.wxappGoodsItemsDetailPromotionActivityService = wxappGoodsItemsDetailPromotionActivityService;
		this.platformItemsDetailCoreService = platformItemsDetailCoreService;
		this.distributorItemsDetailMergeService = distributorItemsDetailMergeService;
		this.operatorCartCompanyProductModelReader = operatorCartCompanyProductModelReader;
		this.wxappGoodsItemsListMemberPriceApplyService = wxappGoodsItemsListMemberPriceApplyService;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.goodsItemsListPromotionEnrichmentService = goodsItemsListPromotionEnrichmentService;
		this.salespersonItemsShelvesActivityNameQueryService = salespersonItemsShelvesActivityNameQueryService;
		this.langueProperties = langueProperties;
	}

	@Override
	public Map<String, Object> execute(long companyId, long userId, long goodsId, long itemId, long distributorId, String woaAppid,
			HttpServletRequest request) {
		long effectiveItemId;
		if (goodsId > 0L) {
			Items def = itemsRepository.findApprovedDefaultSkuByGoodsIdAndCompany(goodsId, companyId);
			if (def != null && def.getItemId() != null) {
				effectiveItemId = def.getItemId();
			} else {
				effectiveItemId = itemId;
			}
		} else {
			Items it = itemsRepository.getByItemIdAndCompany(itemId, companyId);
			if (it == null || !"approved".equals(it.getAuditStatus())) {
				return EMPTY_ITEM;
			}
			effectiveItemId = it.getItemId();
		}

		if (effectiveItemId < 1L) {
			return EMPTY_ITEM;
		}

		long workingItemId = effectiveItemId;
		Map<String, Object> promotionActivityData =
				wxappGoodsItemsDetailPromotionActivityService.getCurrentActivityByItemId(companyId, workingItemId, distributorId);
		List<Long> limitItemIds = deriveLimitItemIds(promotionActivityData);
		if (!limitItemIds.isEmpty() && !limitItemIds.contains(workingItemId)) {
			workingItemId = limitItemIds.get(0);
		}
		List<Long> limitItemIdsForDetail = limitItemIds.isEmpty() ? List.of() : List.copyOf(limitItemIds);

		String authorizerAppId = woaAppid != null ? woaAppid : "";
		Map<String, Object> result;
		if (distributorId > 0L) {
			String productModel = operatorCartCompanyProductModelReader.getProductModel(companyId);
			result = distributorItemsDetailMergeService.merge(companyId, workingItemId, distributorId, authorizerAppId, productModel, limitItemIdsForDetail);
		} else {
			result = platformItemsDetailCoreService.build(companyId, workingItemId, authorizerAppId, limitItemIdsForDetail);
		}

		if (isInvalidDetail(result)) {
			return EMPTY_ITEM;
		}

		String acceptLanguage = RequestLangTag.current(langueProperties);
		wxappGoodsItemsListMemberPriceApplyService.applyForRows(companyId, userId, List.of(result), acceptLanguage);

		long promoterFen = toLong(result.get("promoter_price"));
		result.put("promoter_price", promoterFen >= 1L ? promoterFen : 0L);

		putCurForDetail(result, companyId);

		Object storeVal = result.get("item_total_store");
		if (storeVal == null) {
			storeVal = result.get("store");
		}
		result.put("store", storeVal);

		List<Map<String, Object>> wrap = new ArrayList<>();
		wrap.add(result);
		goodsItemsListPromotionEnrichmentService.enrich(wrap);
		result = wrap.get(0);

		applyPromotionActivityName(result, companyId);

		return result;
	}

	private void applyPromotionActivityName(Map<String, Object> result, long companyId) {
		Object paObj = result.get("promotion_activity");
		if (!(paObj instanceof List<?> paList) || paList.isEmpty()) {
			return;
		}
		Object first = paList.get(0);
		if (!(first instanceof Map<?, ?> rawFirst)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> firstMap = (Map<String, Object>) rawFirst;
		long promotionId = toLong(firstMap.get("promotion_id"));
		String tagType = firstMap.get("tag_type") == null ? "" : String.valueOf(firstMap.get("tag_type"));
		long shelfItemId = toLong(result.get("item_id"));
		String name = salespersonItemsShelvesActivityNameQueryService.resolveActivityName(companyId, shelfItemId, promotionId, tagType);
		firstMap.put("activity_name", StringUtils.hasText(name) ? name : "未知活动");
	}

	private void putCurForDetail(Map<String, Object> result, long companyId) {
		CurrencyExchangeRate row = companyDefaultCurrencyService.getCur(companyId);
		if (row == null) {
			return;
		}
		LinkedHashMap<String, Object> curMap = new LinkedHashMap<>();
		if (row.getId() != null) {
			curMap.put("id", String.valueOf(row.getId()));
		}
		curMap.put("company_id", String.valueOf(companyId));
		curMap.put("currency", row.getCurrency());
		curMap.put("title", row.getTitle());
		curMap.put("symbol", row.getSymbol());
		curMap.put("rate", row.getRate());
		curMap.put("is_default", Boolean.TRUE.equals(row.getIsDefault()));
		if (row.getUsePlatform() != null) {
			curMap.put("use_platform", row.getUsePlatform());
		}
		result.put("cur", curMap);
	}

	private static List<Long> deriveLimitItemIds(Map<String, Object> promotionActivityData) {
		if (promotionActivityData == null) {
			return List.of();
		}
		if ("limited_buy".equals(String.valueOf(promotionActivityData.get("activity_type")))) {
			return List.of();
		}
		Object listObj = promotionActivityData.get("list");
		if (!(listObj instanceof Map<?, ?> lm)) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (Object v : lm.values()) {
			if (v instanceof Map<?, ?> row) {
				Object iid = row.get("item_id");
				if (iid instanceof Number n) {
					out.add(n.longValue());
				}
			}
		}
		return out;
	}

	private static boolean isInvalidDetail(Map<String, Object> r) {
		if (r == null || r.isEmpty()) {
			return true;
		}
		Object id = r.get("item_id");
		if (id == null) {
			return true;
		}
		long v;
		if (id instanceof Number n) {
			v = n.longValue();
		} else {
			try {
				v = Long.parseLong(id.toString().trim());
			} catch (NumberFormatException e) {
				return true;
			}
		}
		return v < 1L;
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
