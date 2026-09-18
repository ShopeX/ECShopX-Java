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

package cn.shopex.ecshopx.companys.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV1OperatorResetPasswordService {

	private final OperatorsQueryService operatorsQueryService;
	private final CompanysActivationService companysActivationService;

	public OpenapiThirdApiV1OperatorResetPasswordService(
			OperatorsQueryService operatorsQueryService,
			CompanysActivationService companysActivationService) {
		this.operatorsQueryService = operatorsQueryService;
		this.companysActivationService = companysActivationService;
	}

	public void invalidateAdminSession(String shopexId) {
		Map<String, Object> filter = Map.of("mobile", shopexId, "operator_type", "admin");
		Map<String, Object> operator = operatorsQueryService.getInfo(filter);
		if (operator == null || operator.isEmpty()) {
			throw new ResourceException("账号信息不存在");
		}

		Long operatorId = toLong(operator.get("operator_id"));
		if (operatorId == null) {
			throw new ResourceException("账号信息不存在");
		}
		String operatorType = String.valueOf(operator.get("operator_type"));
		companysActivationService.setBlackTokenCache(operatorId, operatorType);
	}

	private static Long toLong(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		try {
			return Long.parseLong(value.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
