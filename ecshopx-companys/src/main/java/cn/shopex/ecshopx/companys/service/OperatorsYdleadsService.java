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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.thirdparty.service.prism.PrismIshopexFacade;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OperatorsYdleadsService {

	private final OperatorsQueryService operatorsQueryService;
	private final PrismIshopexFacade prismIshopexFacade;

	public OperatorsYdleadsService(
			OperatorsQueryService operatorsQueryService, PrismIshopexFacade prismIshopexFacade) {
		this.operatorsQueryService = operatorsQueryService;
		this.prismIshopexFacade = prismIshopexFacade;
	}

	public void createYdleadsData(long companyId, Map<String, Object> requestParams) {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("operator_type", "admin");
		Map<String, Object> operatorInfo = operatorsQueryService.getInfo(filter);
		if (operatorInfo == null || operatorInfo.isEmpty()) {
			return;
		}
		LinkedHashMap<String, Object> prismBody = new LinkedHashMap<>();
		prismBody.put("shopexid", operatorInfo.get("passport_uid"));
		prismBody.put("entid", operatorInfo.get("eid"));
		prismBody.put("goods_name", requestParams.get("goods_name"));
		prismBody.put("call_name", requestParams.get("call_name"));
		prismBody.put("sex", requestParams.get("sex"));
		prismBody.put("mobile", requestParams.get("mobile"));
		prismIshopexFacade.opaYdleadsCreate(prismBody);
	}
}
