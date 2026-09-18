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

package cn.shopex.ecshopx.adapay.service.callback;

import cn.shopex.ecshopx.companys.service.operator.sms.CompanySmsSendPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdapayCallbackSmsNotifier {

	private static final Logger log = LoggerFactory.getLogger(AdapayCallbackSmsNotifier.class);

	private final ObjectMapper objectMapper;
	private final CompanySmsSendPort companySmsSendPort;

	public AdapayCallbackSmsNotifier(ObjectMapper objectMapper, CompanySmsSendPort companySmsSendPort) {
		this.objectMapper = Objects.requireNonNull(objectMapper);
		this.companySmsSendPort = Objects.requireNonNull(companySmsSendPort);
	}

	public void trySend(Map<String, Object> smsParams) {
		try {
			if (smsParams == null || smsParams.isEmpty()) {
				log.debug("AdaPay callback SMS noop: empty params");
				return;
			}
			objectMapper.writeValueAsString(smsParams);
			long companyId = coercePositiveLongOrZero(smsParams.get("company_id"));
			String mobile = coerceTrimmedString(smsParams.get("tel_no"));
			if (!StringUtils.hasText(mobile)) {
				log.debug("AdaPay callback SMS skipped: blank tel_no");
				return;
			}
			String placeholderCode =
					String.format(
							"%06d",
							Math.floorMod(Objects.hash(smsParams.get("mer_name"), mobile), 1_000_000));
			companySmsSendPort.sendVerificationCode(companyId, mobile, placeholderCode);
			throwIfCurrentThreadInterrupted();
		} catch (Exception e) {
			log.warn("AdaPay callback SMS send failed", e);
		}
	}

	private static void throwIfCurrentThreadInterrupted() throws InterruptedException {
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException();
		}
	}

	private static long coercePositiveLongOrZero(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : 0L;
		}
		try {
			long v = Long.parseLong(String.valueOf(o).trim());
			return v > 0L ? v : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String coerceTrimmedString(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
