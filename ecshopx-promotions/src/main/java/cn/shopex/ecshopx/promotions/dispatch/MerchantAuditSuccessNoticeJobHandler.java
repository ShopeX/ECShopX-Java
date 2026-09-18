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

package cn.shopex.ecshopx.promotions.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.promotions.service.SmsSendTestService;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MerchantAuditSuccessNoticeJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(MerchantAuditSuccessNoticeJobHandler.class);

	private final SmsSendTestService smsSendTestService;

	public MerchantAuditSuccessNoticeJobHandler(SmsSendTestService smsSendTestService) {
		this.smsSendTestService = smsSendTestService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractCompanyId(payload.get("company_id"));
		String mobile = payload.get("mobile") == null ? "" : String.valueOf(payload.get("mobile"));
		try {
			smsSendTestService.sendTemplatedNoticeSms(
					companyId, mobile, "merchant_audit_success_notice", Collections.emptyMap());
		} catch (RuntimeException e) {
			log.debug(
					"merchant_audit_success_notice failed companyId={} mobileTail={} err={}",
					companyId,
					maskTail(mobile),
					e.getMessage());
		}
	}

	private static long extractCompanyId(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw).trim());
	}

	private static String maskTail(String mobile) {
		if (mobile == null || mobile.length() < 4) {
			return "****";
		}
		return "****" + mobile.substring(mobile.length() - 4);
	}
}
