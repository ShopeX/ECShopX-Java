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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PopularizeCashWithdrawalProcessRequestGate {

	private final CompanysActivationService companysActivationService;

	public PopularizeCashWithdrawalProcessRequestGate(CompanysActivationService companysActivationService) {
		this.companysActivationService = companysActivationService;
	}

	public void validateBeforeProcess(HttpServletRequest request) {
		Object jwt = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(jwt instanceof Map<?, ?> map)) {
			throw new UnauthorizedException("未登录");
		}
		Object cid = map.get("company_id");
		if (cid == null) {
			throw new ForbiddenException("未激活");
		}
		String trimmed = String.valueOf(cid).trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new ForbiddenException("未激活");
		}
		long companyId;
		try {
			companyId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ForbiddenException("未激活");
		}
		companysActivationService.assertShopOperatorCompanyActive(companyId);
	}
}
