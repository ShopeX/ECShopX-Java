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

import cn.shopex.ecshopx.shuyun.auth.InboundSignedCallbackPreparer;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenGatewayClient;
import cn.shopex.ecshopx.shuyun.gateway.ShuyunOpenPlatformGatewayActions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** D11：线下权益 report / result.detail / result push v2。 */
@Service
public class OfflineBenefitReportService {

	private static final Logger log = LoggerFactory.getLogger(OfflineBenefitReportService.class);

	private final OpenPlatformConfigService openPlatformConfigService;
	private final ShuyunOpenGatewayClient gatewayClient;

	public OfflineBenefitReportService(
			OpenPlatformConfigService openPlatformConfigService, ShuyunOpenGatewayClient gatewayClient) {
		this.openPlatformConfigService = openPlatformConfigService;
		this.gatewayClient = gatewayClient;
	}

	public boolean pushSendReportV2(
			long companyId, String platform, String benefitId, String requestId, int total, int success, int failure) {
		if (!eligible(companyId) || !StringUtils.hasText(platform)) {
			return false;
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("benefitId", benefitId);
		body.put("requestId", requestId);
		body.put("total", total);
		body.put("success", success);
		body.put("failure", failure);
		return post(companyId, ShuyunOpenPlatformGatewayActions.OFFLINE_BENEFIT_SEND_REPORT_PUSH_V2, body, platform);
	}

	public boolean pushSendResultDetailV2(long companyId, String platform, List<Map<String, Object>> rows) {
		if (!eligible(companyId) || !StringUtils.hasText(platform)) {
			return false;
		}
		if (rows == null || rows.isEmpty()) {
			return true;
		}
		return post(
				companyId,
				ShuyunOpenPlatformGatewayActions.OFFLINE_BENEFIT_SEND_RESULT_DETAIL_PUSH_V2,
				rows,
				platform);
	}

	public boolean pushResultV2(long companyId, String platform, List<Map<String, Object>> rows) {
		if (!eligible(companyId) || !StringUtils.hasText(platform)) {
			return false;
		}
		if (rows == null || rows.isEmpty()) {
			return true;
		}
		return post(companyId, ShuyunOpenPlatformGatewayActions.OFFLINE_BENEFIT_RESULT_PUSH_V2, rows, platform);
	}

	private boolean eligible(long companyId) {
		CompanyShuyunOpenPlatformConfig cfg = openPlatformConfigService.findByCompanyId(companyId);
		return InboundSignedCallbackPreparer.isEligible(cfg);
	}

	private boolean post(long companyId, String action, Object body, String platform) {
		try {
			gatewayClient.postJson(companyId, action, body, platform.trim().toLowerCase());
			return true;
		} catch (Exception e) {
			log.error("Shuyun offline benefit push failed action={} companyId={} err={}", action, companyId, e.getMessage());
			return false;
		}
	}
}
