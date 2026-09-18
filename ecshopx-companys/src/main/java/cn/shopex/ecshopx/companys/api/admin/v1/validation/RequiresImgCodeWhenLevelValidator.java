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

package cn.shopex.ecshopx.companys.api.admin.v1.validation;

import cn.shopex.ecshopx.companys.api.admin.v1.dto.WxOauthLoginRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RequiresImgCodeWhenLevelValidator
		implements ConstraintValidator<RequiresImgCodeWhenLevel, WxOauthLoginRequest> {

	@Value("${ADMIN_LOGIN_CHECK_LEVEL:}")
	private String adminLoginCheckLevel;

	@Override
	public boolean isValid(WxOauthLoginRequest value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		if (!"img_code".equals(adminLoginCheckLevel)) {
			return true;
		}
		String token = value.getToken();
		String yzm = value.getYzm();
		boolean tokenOk = token != null && !token.trim().isEmpty();
		boolean yzmOk = yzm != null && !yzm.trim().isEmpty();
		return tokenOk && yzmOk;
	}
}
