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

package cn.shopex.ecshopx.shuyun.service.openplatform;

import cn.shopex.ecshopx.shuyun.domain.ShuyunOpenPlatformTrafficAudit;
import cn.shopex.ecshopx.shuyun.mapper.ShuyunOpenPlatformTrafficAuditMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 轻量 traffic_audit 写入（C1 入站）。 */
@Service
public class TrafficAuditWriter {

	private final ShuyunOpenPlatformTrafficAuditMapper auditMapper;

	public TrafficAuditWriter(ShuyunOpenPlatformTrafficAuditMapper auditMapper) {
		this.auditMapper = auditMapper;
	}

	public void writeInboundToken(
			long companyId,
			String correlationId,
			String requestHeadersJson,
			String requestBody,
			String responseBody,
			int httpStatus,
			String outcome,
			String errorMessage) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		ShuyunOpenPlatformTrafficAudit row = new ShuyunOpenPlatformTrafficAudit();
		row.setCompanyId(companyId);
		row.setDirection("inbound");
		row.setCorrelationId(StringUtils.hasText(correlationId) ? correlationId : "in_tk_" + now);
		row.setHttpVerb("POST");
		row.setActionMethod("token");
		row.setHttpStatus(httpStatus);
		row.setOutcome(outcome);
		row.setRequestHeadersJson(requestHeadersJson == null ? "{}" : requestHeadersJson);
		row.setRequestBody(requestBody);
		row.setResponseBody(responseBody);
		row.setErrorMessage(errorMessage);
		row.setCreated(now);
		row.setUpdated(now);
		auditMapper.insert(row);
	}

	public void writeOutbound(
			long companyId,
			String correlationId,
			String httpVerb,
			String actionMethod,
			String requestHeadersJson,
			String requestBody,
			String responseBody,
			int httpStatus,
			String outcome,
			String errorMessage) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		ShuyunOpenPlatformTrafficAudit row = new ShuyunOpenPlatformTrafficAudit();
		row.setCompanyId(companyId);
		row.setDirection("outbound");
		row.setCorrelationId(StringUtils.hasText(correlationId) ? correlationId : "gw_" + now);
		row.setHttpVerb(httpVerb);
		row.setActionMethod(actionMethod);
		row.setHttpStatus(httpStatus);
		row.setOutcome(outcome);
		row.setRequestHeadersJson(requestHeadersJson == null ? "{}" : requestHeadersJson);
		row.setRequestBody(requestBody);
		row.setResponseBody(responseBody);
		row.setErrorMessage(errorMessage);
		row.setCreated(now);
		row.setUpdated(now);
		auditMapper.insert(row);
	}
}
