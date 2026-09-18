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

package cn.shopex.ecshopx.kaquan.service.discount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DiscountCardCreateFacadeService {

	private final DiscountCardActionValidationService actionValidationService;
	private final DiscountCardDoParamsService doParamsService;
	private final DiscountNewGiftCardCreateService newGiftCardCreateService;
	private final DiscountStandardCardCreateService standardCardCreateService;

	public DiscountCardCreateFacadeService(DiscountCardActionValidationService actionValidationService,
			DiscountCardDoParamsService doParamsService, DiscountNewGiftCardCreateService newGiftCardCreateService,
			DiscountStandardCardCreateService standardCardCreateService) {
		this.actionValidationService = actionValidationService;
		this.doParamsService = doParamsService;
		this.newGiftCardCreateService = newGiftCardCreateService;
		this.standardCardCreateService = standardCardCreateService;
	}

	public Map<String, Object> create(Map<String, Object> allParams, Map<String, Object> operatorJwt) {
		long companyId = ((Number) operatorJwt.get("company_id")).longValue();
		actionValidationService.validateCommon(allParams);
		actionValidationService.validateFixTermBeginTime(allParams);
		String cardType = DiscountCardParamNormalize.stringVal(allParams.get("card_type"));
		if ("new_gift".equals(cardType)) {
			Map<String, Object> data = new HashMap<>(allParams);
			data.put("company_id", companyId);
			data.put("coupon_type", DiscountCardParamNormalize.normalizeCouponType(allParams.get("coupon_type")));
			data.put("guide_issue_quantity", DiscountCardParamNormalize.parseIntFlexible(allParams.get("guide_issue_quantity"), 0));
			actionValidationService.validateNewGiftDateAndFields(allParams);
			return newGiftCardCreateService.createKaquan(data);
		}
		Map<String, Object> postdata = doParamsService.apply(new HashMap<>(allParams), companyId);
		postdata.put("company_id", companyId);
		postdata.put("coupon_type", DiscountCardParamNormalize.normalizeCouponType(allParams.get("coupon_type")));
		postdata.put("guide_issue_quantity", DiscountCardParamNormalize.parseIntFlexible(allParams.get("guide_issue_quantity"), 0));
		postdata.put("source_id", DiscountCardParamNormalize.longFromObject(operatorJwt.get("distributor_id"), 0L));
		Object opType = operatorJwt.get("operator_type");
		postdata.put("source_type", opType == null ? "admin" : String.valueOf(opType));
		String storeSelf = DiscountCardParamNormalize.stringVal(allParams.get("store_self"));
		if ("true".equals(storeSelf)) {
			postdata.put("use_all_shops", "false");
			postdata.put("distributor_id", new ArrayList<>(List.of("0")));
		} else {
			List<String> reqDist = DiscountCardParamNormalize.normalizeToStringList(allParams.get("distributor_id"));
			if (!reqDist.isEmpty()) {
				postdata.put("distributor_id", new ArrayList<Object>(reqDist));
				postdata.put("use_all_shops", "false");
			}
			@SuppressWarnings("unchecked")
			List<Object> current = (List<Object>) postdata.get("distributor_id");
			boolean noDist = current == null || current.isEmpty();
			if (noDist) {
				List<String> rel = DiscountCardParamNormalize.normalizeToStringList(allParams.get("rel_distributor_ids"));
				if (!rel.isEmpty()) {
					postdata.put("distributor_id", new ArrayList<Object>(rel));
					postdata.put("use_all_shops", "false");
				}
			}
		}
		String authorizerAppid = DiscountCardParamNormalize.stringVal(operatorJwt.get("authorizer_appid"));
		return standardCardCreateService.createKaquan(postdata, authorizerAppid);
	}
}
