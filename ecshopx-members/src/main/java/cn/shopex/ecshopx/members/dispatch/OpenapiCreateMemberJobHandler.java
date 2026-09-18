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

package cn.shopex.ecshopx.members.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.members.openapi.thirdapi.v2.OpenapiThirdApiV2MemberCreateService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpenapiCreateMemberJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(OpenapiCreateMemberJobHandler.class);

	private final OpenapiThirdApiV2MemberCreateService openapiThirdApiV2MemberCreateService;

	public OpenapiCreateMemberJobHandler(
			OpenapiThirdApiV2MemberCreateService openapiThirdApiV2MemberCreateService) {
		this.openapiThirdApiV2MemberCreateService = openapiThirdApiV2MemberCreateService;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = readLong(payload.get("company_id"), 0L);
		@SuppressWarnings("unchecked")
		Map<String, Object> formData = (Map<String, Object>) payload.get("form_data");
		if (companyId <= 0L || formData == null) {
			log.warn("create member job skipped: invalid payload, companyId={}", companyId);
			return;
		}
		try {
			openapiThirdApiV2MemberCreateService.executeOpenapiCreateDetailFromPhpFormData(
					companyId, formData);
		} catch (Exception e) {
			log.info(
					"Openapi_CreateMemberJob_Error. error: {}, company_id: {}, params: {}",
					e.getMessage(),
					companyId,
					formData,
					e);
		} catch (Throwable t) {
			log.info(
					"Openapi_CreateMemberJob_Error. error: {}, company_id: {}, params: {}",
					t.getMessage(),
					companyId,
					formData,
					t);
		}
	}

	private static long readLong(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
