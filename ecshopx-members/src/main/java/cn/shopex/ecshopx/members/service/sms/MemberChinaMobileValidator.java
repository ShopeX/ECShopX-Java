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

package cn.shopex.ecshopx.members.service.sms;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

public final class MemberChinaMobileValidator {

	public static final Pattern CHINA_MOBILE = Pattern.compile("^1\\d{10}$");

	private MemberChinaMobileValidator() {}

	public static void requireValidPlainMobile(String phone) {
		String t = phone == null ? "" : phone.trim();
		if (!StringUtils.hasText(t) || !CHINA_MOBILE.matcher(t).matches()) {
			throw new BadRequestException("手机号码错误");
		}
	}
}
