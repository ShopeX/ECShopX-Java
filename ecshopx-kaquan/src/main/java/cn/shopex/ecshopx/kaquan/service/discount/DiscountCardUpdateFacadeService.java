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

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DiscountCardUpdateFacadeService {

	private static final Set<String> NEW_GIFT_WHITELIST = Set.of(
			"quantity",
			"description",
			"receive",
			"grade_ids",
			"vip_grade_ids",
			"lock_time",
			"kq_status",
			"items",
			"distributor_ids",
			"user_tag_ids");

	private final DiscountCardUpdateValidationService updateValidationService;
	private final DiscountCardDoParamsService doParamsService;
	private final DiscountCardStandardUpdatePrecheckService precheckService;
	private final DiscountStandardCardUpdateService standardCardUpdateService;
	private final DiscountNewGiftCardUpdateService newGiftCardUpdateService;

	public DiscountCardUpdateFacadeService(DiscountCardUpdateValidationService updateValidationService,
			DiscountCardDoParamsService doParamsService, DiscountCardStandardUpdatePrecheckService precheckService,
			DiscountStandardCardUpdateService standardCardUpdateService,
			DiscountNewGiftCardUpdateService newGiftCardUpdateService) {
		this.updateValidationService = updateValidationService;
		this.doParamsService = doParamsService;
		this.precheckService = precheckService;
		this.standardCardUpdateService = standardCardUpdateService;
		this.newGiftCardUpdateService = newGiftCardUpdateService;
	}

	public Map<String, Object> update(Map<String, Object> allParams, Map<String, Object> operatorJwt) {
		long companyId = ((Number) operatorJwt.get("company_id")).longValue();
		if ("new_gift".equals(DiscountCardParamNormalize.stringVal(allParams.get("card_type")))) {
			updateValidationService.validateNewGiftCardId(allParams);
			Map<String, Object> data = new HashMap<>();
			for (String k : NEW_GIFT_WHITELIST) {
				if (allParams.containsKey(k)) {
					data.put(k, allParams.get(k));
				}
			}
			if (allParams.containsKey("card_id")) {
				data.put("card_id", allParams.get("card_id"));
			}
			data.put("company_id", companyId);
			return newGiftCardUpdateService.updateKaquan(data);
		}
		updateValidationService.validateStandardPatchFields(allParams);
		Map<String, Object> postdata = doParamsService.apply(new HashMap<>(allParams), companyId);
		String storeSelf = DiscountCardParamNormalize.stringVal(allParams.get("store_self"));
		if ("true".equalsIgnoreCase(storeSelf)) {
			postdata.put("use_all_shops", "false");
			postdata.put("distributor_id", new ArrayList<>(List.of("0")));
		} else {
			List<String> reqDist = DiscountCardParamNormalize.normalizeToStringList(allParams.get("distributor_id"));
			if (!reqDist.isEmpty()) {
				postdata.put("distributor_id", new ArrayList<Object>(reqDist));
				postdata.put("use_all_shops", "false");
			} else {
				List<String> rel = DiscountCardParamNormalize.normalizeToStringList(allParams.get("rel_distributor_ids"));
				if (!rel.isEmpty()) {
					postdata.put("distributor_id", new ArrayList<Object>(rel));
					postdata.put("use_all_shops", "false");
				}
			}
		}
		DiscountCards existing = precheckService.loadCardOrThrow(postdata, companyId);
		precheckService.assertFixTimeRangeNotShrinking(postdata, existing);
		postdata.put("company_id", companyId);
		String authorizerAppid = DiscountCardParamNormalize.stringVal(operatorJwt.get("authorizer_appid"));
		return standardCardUpdateService.updateKaquan(postdata, authorizerAppid, existing);
	}
}
