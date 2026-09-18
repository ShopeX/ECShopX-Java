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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Map;
import org.springframework.stereotype.Service;
@Service
public class MarketingActivityCreateService {

	private final MarketingActivityCreateRulesService marketingActivityCreateRulesService;
	private final MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService;
	private final MarketingActivityCreatePersistenceService marketingActivityCreatePersistenceService;

	public MarketingActivityCreateService(
			MarketingActivityCreateRulesService marketingActivityCreateRulesService,
			MarketingActivityCrossPromotionGuardService marketingActivityCrossPromotionGuardService,
			MarketingActivityCreatePersistenceService marketingActivityCreatePersistenceService) {
		this.marketingActivityCreateRulesService = marketingActivityCreateRulesService;
		this.marketingActivityCrossPromotionGuardService = marketingActivityCrossPromotionGuardService;
		this.marketingActivityCreatePersistenceService = marketingActivityCreatePersistenceService;
	}

	public Map<String, Object> createMarketingActivity(Map<String, Object> params, String requestLangTag) {
		normalizeUseBoundForCreate(params);
		if (params.get("company_id") == null) {
			throw new BadRequestException("企业id必填");
		}
		marketingActivityCreateRulesService.validateAddPromotionData(params);
		marketingActivityCrossPromotionGuardService.checkActivityValidByMarketing(params);
		String resolvedLangTag =
				(requestLangTag != null && !requestLangTag.isBlank()) ? requestLangTag : "zh-CN";
		return marketingActivityCreatePersistenceService.createInTransaction(params, resolvedLangTag);
	}

	private void normalizeUseBoundForCreate(Map<String, Object> params) {
		Object raw = params.get("use_bound");
		String ubStr = raw == null ? "" : String.valueOf(raw).trim();
		if ("all".equalsIgnoreCase(ubStr) || "0".equals(ubStr) || (raw instanceof Number n && n.intValue() == 0)) {
			params.put("use_bound", 0);
			return;
		}
		if ("goods".equalsIgnoreCase(ubStr) || "1".equals(ubStr) || (raw instanceof Number n && n.intValue() == 1)) {
			params.put("use_bound", 1);
			return;
		}
		if ("category".equalsIgnoreCase(ubStr) || "2".equals(ubStr) || (raw instanceof Number n && n.intValue() == 2)) {
			Object ic = params.get("item_category");
			if (ic == null || (ic instanceof java.util.Collection<?> c && c.isEmpty())) {
				throw new BadRequestException("请选择主分类");
			}
			params.put("use_bound", 2);
			return;
		}
		if ("tag".equalsIgnoreCase(ubStr) || "3".equals(ubStr) || (raw instanceof Number n && n.intValue() == 3)) {
			if (!params.containsKey("tag_ids")
					|| !(params.get("tag_ids") instanceof java.util.Collection<?> c)
					|| c.isEmpty()) {
				throw new BadRequestException("请选择标签");
			}
			params.put("use_bound", 3);
			return;
		}
		if ("brand".equalsIgnoreCase(ubStr) || "4".equals(ubStr) || (raw instanceof Number n && n.intValue() == 4)) {
			if (!params.containsKey("brand_ids")
					|| !(params.get("brand_ids") instanceof java.util.Collection<?> c)
					|| c.isEmpty()) {
				throw new BadRequestException("请选择品牌");
			}
			params.put("use_bound", 4);
		}
	}
}
