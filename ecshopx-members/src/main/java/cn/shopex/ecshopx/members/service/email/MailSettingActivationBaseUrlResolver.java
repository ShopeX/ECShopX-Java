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

import cn.shopex.ecshopx.companys.service.setting.MailSettingRedisService;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 读取 {@code mailSetting:{companyId}} 中租户级「邮箱激活」根地址，与 PHP
 * {@code MailSettingActivationBaseUrlResolver} 对齐（去 CR/LF、trim、截断 512）。
 */
@Service
public class MailSettingActivationBaseUrlResolver {

	private final MailSettingRedisService mailSettingRedisService;

	public MailSettingActivationBaseUrlResolver(MailSettingRedisService mailSettingRedisService) {
		this.mailSettingRedisService = mailSettingRedisService;
	}

	public String getH5ActivationBaseUrl(long companyId) {
		Map<String, Object> config = mailSettingRedisService.readMailSetting(companyId);
		Object value = config.get(MailSettingRedisService.EMAIL_ACTIVATION_H5_DOMAIN);
		return sanitizeStoredDomain(value == null ? "" : String.valueOf(value));
	}

	public static String sanitizeStoredDomain(String value) {
		String s = (value == null ? "" : value).trim().replace("\r", "").replace("\n", "");
		if (s.length() > 512) {
			s = s.substring(0, 512);
		}
		return s;
	}
}
