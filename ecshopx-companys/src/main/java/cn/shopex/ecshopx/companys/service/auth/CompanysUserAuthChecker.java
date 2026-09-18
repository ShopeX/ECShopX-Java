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

package cn.shopex.ecshopx.companys.service.auth;

import cn.shopex.ecshopx.common.exception.ResourceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class CompanysUserAuthChecker {

	private final String activationToken;

	public CompanysUserAuthChecker(
			@Value("${ecshopx.companys.activation.token:7e23b4cecd91a0a8500c2fb65341193c}") String activationToken) {
		this.activationToken = activationToken;
	}

	public void checkUserAuth(Map<String, Object> userData) {
		String operatorType = stringVal(userData.get("operator_type"));
		if ("shopadmin".equals(operatorType) || "user".equals(operatorType)) {
			return;
		}
		String source = stringVal(userData.get("source"));
		if ("user".equals(source)) {
			return;
		}
		Object idObj = userData.get("id");
		if (idObj == null || !StringUtils.hasText(String.valueOf(idObj))) {
			throw new ResourceException("用户信息错误，checkUserAuth");
		}
		String id = String.valueOf(idObj);
		Map<String, String> data;
		try {
			data = CompanysActivationCipher.decryptUserPayload(id);
		} catch (Exception ex) {
			throw new ResourceException("登录验证错误");
		}
		String token = data.get("token");
		if (token == null || !activationToken.equals(token)) {
			throw new ResourceException("登录验证错误");
		}
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
