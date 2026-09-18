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

package cn.shopex.ecshopx.members.service.email;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * 邮箱注册/重置密码的密码策略，对齐 PHP {@code MemberPasswordPolicyService}：
 * 长度 ≥ 8、同时包含字母与数字、命中弱密码黑名单即拒绝。
 */
@Service
public class MemberPasswordPolicyService {

	private static final Pattern LETTER_PATTERN = Pattern.compile("[a-zA-Z]");

	private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");

	private static final List<String> WEAK_PASSWORDS = List.of(
			"12345678",
			"123456789",
			"1234567890",
			"password",
			"password123",
			"qwerty123",
			"admin123",
			"88888888");

	/**
	 * 校验并失败时抛业务异常（文案对齐 PHP 翻译）。
	 */
	public void validateOrFail(String password) {
		String p = password == null ? "" : password;
		if (p.length() < 8) {
			throw new ResourceException("密码至少 8 位");
		}
		if (!LETTER_PATTERN.matcher(p).find() || !DIGIT_PATTERN.matcher(p).find()) {
			throw new ResourceException("密码需同时包含字母与数字");
		}
		String lower = p.toLowerCase();
		for (String weak : WEAK_PASSWORDS) {
			if (lower.equals(weak) || lower.contains(weak)) {
				throw new ResourceException("密码过于简单，请更换");
			}
		}
	}
}
