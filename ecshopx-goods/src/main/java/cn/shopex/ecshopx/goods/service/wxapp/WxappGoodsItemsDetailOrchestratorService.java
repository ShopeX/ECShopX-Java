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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.companys.service.operatorcart.OperatorCartCompanyProductModelReader;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.items.DistributorItemsDetailMergeService;
import cn.shopex.ecshopx.goods.service.items.ItemLogisticsStoreEnricher;
import cn.shopex.ecshopx.goods.service.items.PlatformItemsDetailCoreService;
import cn.shopex.ecshopx.goods.service.items.SupplierItemsDetailCoreService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.service.SkuValidMarketingActivityService;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappGoodsItemsDetailOrchestratorService {

	private final MemberAccountService memberAccountService;
	private final ItemsRepository itemsRepository;
	private final SupplierItemsRepository supplierItemsRepository;
	private final WxappGoodsItemsDetailPromotionActivityService wxappGoodsItemsDetailPromotionActivityService;
	private final PlatformItemsDetailCoreService platformItemsDetailCoreService;
	private final SupplierItemsDetailCoreService supplierItemsDetailCoreService;
	private final DistributorItemsDetailMergeService distributorItemsDetailMergeService;
	private final OperatorCartCompanyProductModelReader operatorCartCompanyProductModelReader;
	private final WxappGoodsItemsDetailActivityMergeService wxappGoodsItemsDetailActivityMergeService;
	private final WxappGoodsItemsDetailNormalBranchService wxappGoodsItemsDetailNormalBranchService;
	private final WxappGoodsItemsDetailPostEnrichmentService wxappGoodsItemsDetailPostEnrichmentService;
	private final SkuValidMarketingActivityService skuValidMarketingActivityService;
	private final ObjectMapper objectMapper;
	private final ItemLogisticsStoreEnricher itemLogisticsStoreEnricher;
	private final LangueProperties langueProperties;

	public WxappGoodsItemsDetailOrchestratorService(MemberAccountService memberAccountService, ItemsRepository itemsRepository,
			SupplierItemsRepository supplierItemsRepository, WxappGoodsItemsDetailPromotionActivityService wxappGoodsItemsDetailPromotionActivityService,
			PlatformItemsDetailCoreService platformItemsDetailCoreService, SupplierItemsDetailCoreService supplierItemsDetailCoreService,
			DistributorItemsDetailMergeService distributorItemsDetailMergeService,
			OperatorCartCompanyProductModelReader operatorCartCompanyProductModelReader,
			WxappGoodsItemsDetailActivityMergeService wxappGoodsItemsDetailActivityMergeService,
			WxappGoodsItemsDetailNormalBranchService wxappGoodsItemsDetailNormalBranchService,
			WxappGoodsItemsDetailPostEnrichmentService wxappGoodsItemsDetailPostEnrichmentService,
			SkuValidMarketingActivityService skuValidMarketingActivityService, ObjectMapper objectMapper,
			ItemLogisticsStoreEnricher itemLogisticsStoreEnricher,
			LangueProperties langueProperties) {
		this.memberAccountService = memberAccountService;
		this.itemsRepository = itemsRepository;
		this.supplierItemsRepository = supplierItemsRepository;
		this.wxappGoodsItemsDetailPromotionActivityService = wxappGoodsItemsDetailPromotionActivityService;
		this.platformItemsDetailCoreService = platformItemsDetailCoreService;
		this.supplierItemsDetailCoreService = supplierItemsDetailCoreService;
		this.distributorItemsDetailMergeService = distributorItemsDetailMergeService;
		this.operatorCartCompanyProductModelReader = operatorCartCompanyProductModelReader;
		this.wxappGoodsItemsDetailActivityMergeService = wxappGoodsItemsDetailActivityMergeService;
		this.wxappGoodsItemsDetailNormalBranchService = wxappGoodsItemsDetailNormalBranchService;
		this.wxappGoodsItemsDetailPostEnrichmentService = wxappGoodsItemsDetailPostEnrichmentService;
		this.skuValidMarketingActivityService = skuValidMarketingActivityService;
		this.objectMapper = objectMapper;
		this.itemLogisticsStoreEnricher = itemLogisticsStoreEnricher;
		this.langueProperties = langueProperties;
	}

	public Map<String, Object> execute(HttpServletRequest request, long pathItemId, long companyId, long userId, String authorizerAppId, String viewMode,
			Long goodsId, long distributorId, boolean needTdk) {
		boolean preview = "preview".equalsIgnoreCase(viewMode);
		long effectiveItemId;
		if (goodsId != null && goodsId > 0) {
			if (preview) {
				SupplierItems si = supplierItemsRepository.findApprovedDefaultByCompanyAndGoodsId(companyId, goodsId);
				if (si == null || si.getItemId() == null) {
					throw new ResourceException("商品不存在或下架");
				}
				effectiveItemId = si.getItemId();
			} else {
				Items it = itemsRepository.findApprovedDefaultSkuByGoodsIdAndCompany(goodsId, companyId);
				if (it == null || it.getItemId() == null) {
					throw new ResourceException("商品不存在或下架");
				}
				effectiveItemId = it.getItemId();
			}
		} else {
			if (preview) {
				SupplierItems si = supplierItemsRepository.getByItemIdAndCompany(pathItemId, companyId);
				if (si == null || !"approved".equals(si.getAuditStatus())) {
					throw new ResourceException("商品不存在或下架");
				}
				effectiveItemId = si.getItemId();
			} else {
				Items it = itemsRepository.getByItemIdAndCompany(pathItemId, companyId);
				if (it == null || !"approved".equals(it.getAuditStatus())) {
					throw new ResourceException("商品不存在或下架");
				}
				effectiveItemId = it.getItemId();
			}
		}
		if (effectiveItemId < 1L) {
			throw new ResourceException("商品不存在或下架");
		}
		Map<String, Object> promotionActivityData =
				wxappGoodsItemsDetailPromotionActivityService.getCurrentActivityByItemId(companyId, effectiveItemId, distributorId);
		List<Long> limitItemIds = deriveLimitItemIds(promotionActivityData);
		if (!limitItemIds.isEmpty() && !limitItemIds.contains(effectiveItemId)) {
			effectiveItemId = limitItemIds.get(0);
		}
		List<Long> limitItemIdsForDetail = limitItemIds.isEmpty() ? List.of() : List.copyOf(limitItemIds);
		String productModel = operatorCartCompanyProductModelReader.getProductModel(companyId);
		Map<String, Object> result;
		if (distributorId > 0) {
			result = distributorItemsDetailMergeService.merge(companyId, effectiveItemId, distributorId, authorizerAppId, productModel, limitItemIdsForDetail);
			if (isInvalidDetail(result)) {
				return Map.of("item_id", 0L);
			}
		} else if (preview) {
			result = supplierItemsDetailCoreService.build(companyId, effectiveItemId, authorizerAppId, limitItemIdsForDetail);
		} else {
			result = platformItemsDetailCoreService.build(companyId, effectiveItemId, authorizerAppId, limitItemIdsForDetail);
		}
		if (isInvalidDetail(result)) {
			return Map.of("item_id", 0L);
		}
		tryParseIntroJson(result);
		if (isInvalidDetail(result)) {
			return Map.of("item_id", 0L);
		}
		Object groupMinBaselineStore = result.containsKey("item_total_store") && result.get("item_total_store") != null
				? result.get("item_total_store")
				: result.get("store");
		String activityType = promotionActivityData == null ? "" : String.valueOf(promotionActivityData.get("activity_type"));
		boolean activityBranch =
				promotionActivityData != null && ("limited_time_sale".equals(activityType) || "seckill".equals(activityType) || "group".equals(activityType));
		if (activityBranch) {
			wxappGoodsItemsDetailActivityMergeService.applyActivityBranchToDetail(result, promotionActivityData, effectiveItemId, companyId, userId,
					distributorId, skuValidMarketingActivityService);
		} else {
			String acceptLanguage = RequestLangTag.current(langueProperties);
			wxappGoodsItemsDetailNormalBranchService.applyNormalBranchToDetail(result, companyId, userId, distributorId, acceptLanguage);
		}
		long gradeId = resolveGradeId(request, userId, companyId);
		wxappGoodsItemsDetailPostEnrichmentService.runPostSteps16Through28(result, companyId, userId, distributorId, gradeId, needTdk, promotionActivityData,
				effectiveItemId, groupMinBaselineStore, request);
		if (!preview) {
			// 拼团/秒杀跳过配送合成；限时特惠按归属方配送能力合成 logistics_store
			boolean applyDisplayTotal = !ItemLogisticsStoreEnricher.isActivitySkipDisplay(Map.of("activity_type", activityType));
			itemLogisticsStoreEnricher.enrichDetailAndApplyDisplayTotal(
					companyId, distributorId, result, applyDisplayTotal);
		}
		return result;
	}

	private List<Long> deriveLimitItemIds(Map<String, Object> promotionActivityData) {
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

	private void tryParseIntroJson(Map<String, Object> result) {
		Object intro = result.get("intro");
		if (intro instanceof Map || intro instanceof List) {
			return;
		}
		if (intro == null || !StringUtils.hasText(intro.toString())) {
			return;
		}
		try {
			Object parsed = objectMapper.readValue(intro.toString(), Object.class);
			if (parsed instanceof Map || parsed instanceof List) {
				result.put("intro", parsed);
			}
		} catch (Exception ignored) {
		}
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

	private long resolveGradeId(HttpServletRequest request, long userId, long companyId) {
		Object rawClaims = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (rawClaims instanceof Map<?, ?> m) {
			Object g = m.get("grade_id");
			if (g instanceof Number n && n.longValue() > 0L) {
				return n.longValue();
			}
			if (g != null && StringUtils.hasText(g.toString())) {
				try {
					long v = Long.parseLong(g.toString().trim());
					if (v > 0L) {
						return v;
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		if (userId > 0L) {
			Map<String, Object> info = memberAccountService.getMemberInfo(userId, companyId);
			Object g = info.get("grade_id");
			if (g instanceof Number n && n.longValue() > 0L) {
				return n.longValue();
			}
			if (g != null && StringUtils.hasText(g.toString())) {
				try {
					long v = Long.parseLong(g.toString().trim());
					if (v > 0L) {
						return v;
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return 0L;
	}
}
