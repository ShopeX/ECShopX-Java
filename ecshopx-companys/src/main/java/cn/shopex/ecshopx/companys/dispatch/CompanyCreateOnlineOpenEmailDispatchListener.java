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

package cn.shopex.ecshopx.companys.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.companys.service.companycreate.CompanyOnlineOpenNotificationService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CompanyCreateOnlineOpenEmailDispatchListener implements DispatchListener {

	private final CompanyOnlineOpenNotificationService notificationService;

	@Value("${ecshopx.request-field.oem-shuyun:false}")
	private boolean oemShuyun;

	@Value("${common.system-is-saas:false}")
	private boolean systemIsSaas;

	public CompanyCreateOnlineOpenEmailDispatchListener(CompanyOnlineOpenNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (oemShuyun || !systemIsSaas) {
			return;
		}
		String to = stringOrNull(payload.get("notify_email"));
		if (!StringUtils.hasText(to)) {
			return;
		}
		String mobile = stringOrNull(payload.get("sms_mobile"));
		long activeAt = epochSeconds(payload.get("active_at_epoch_seconds"));
		long expiredAt = epochSeconds(payload.get("expired_at_epoch_seconds"));
		notificationService.sendOpeningEmailIfApplicable(to.trim(), mobile, activeAt, expiredAt);
	}

	private static long epochSeconds(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return 0L;
	}

	private static String stringOrNull(Object o) {
		if (o == null) {
			return null;
		}
		String s = o.toString().trim();
		return s.isEmpty() ? null : s;
	}
}
