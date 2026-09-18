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

package cn.shopex.ecshopx.espier.service;

import cn.shopex.ecshopx.companys.service.auth.OperatorAuthService;
import cn.shopex.ecshopx.espier.api.admin.v1.dto.OperatorLoginRequest;
import cn.shopex.ecshopx.espier.security.OperatorJwtIssuer;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperatorLoginFacade {

	private final OperatorAuthService operatorAuthService;
	private final OperatorJwtIssuer operatorJwtIssuer;

	public OperatorLoginFacade(OperatorAuthService operatorAuthService, OperatorJwtIssuer operatorJwtIssuer) {
		this.operatorAuthService = operatorAuthService;
		this.operatorJwtIssuer = operatorJwtIssuer;
	}

	public String getLoginCheckLevel() {
		return operatorAuthService.getLoginCheckLevel();
	}

	public String login(OperatorLoginRequest req) {
		Map<String, Object> params = new HashMap<>();
		params.put("username", req.getUsername());
		params.put("password", req.getPassword());
		String logintype = req.getLogintype();
		String normalizedLogintype = (logintype == null || logintype.isBlank()) ? "localadmin" : logintype;
		params.put("logintype", normalizedLogintype);
		if (StringUtils.hasText(req.getCode())) {
			params.put("code", req.getCode().trim());
		} else if ("oauthadmin".equalsIgnoreCase(normalizedLogintype)) {
			// Same credential shape as OperatorsController#login oauth path: OperatorAuthService expects Prism code under "code".
			params.put("code", req.getUsername());
		}
		params.put("product_model", req.getProduct_model());
		params.put("agreement_id", req.getAgreement_id());
		params.put("token", req.getToken());
		params.put("yzm", req.getYzm());
		Map<String, Object> newOperator = operatorAuthService.retrieveByCredentials(params);
		return operatorJwtIssuer.issueToken(newOperator);
	}
}
