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

package cn.shopex.ecshopx.thirdparty.service.shuyun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service("shuyunMemberProfileModifyHttp")
@ConditionalOnProperty(
		prefix = "ecshopx.thirdparty.shuyun.member-modify",
		name = "http-enabled",
		havingValue = "true")
public class ShuyunMemberProfileModifyHttpService implements ShuyunMemberProfileModifyPort {

	private static final String PATH = "/lpee-member-interfaces/v1/spi/spmall/member/modify";

	private final ShuyunSignedGatewayClient shuyunSignedGatewayClient;
	private final ObjectMapper objectMapper;

	public ShuyunMemberProfileModifyHttpService(
			ShuyunSignedGatewayClient shuyunSignedGatewayClient, ObjectMapper objectMapper) {
		this.shuyunSignedGatewayClient = shuyunSignedGatewayClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean modifyMemberProfile(long companyId, long userId, String shopId, Map<String, Object> body) {
		if (!shuyunSignedGatewayClient.isConfigured()) {
			return false;
		}
		try {
			LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
			if (StringUtils.hasText(shopId)) {
				payload.put("shopId", shopId);
			}
			payload.put("platAccount", String.valueOf(userId));
			if (body != null) {
				payload.putAll(body);
			}
			String json = objectMapper.writeValueAsString(payload);
			JsonNode resp = shuyunSignedGatewayClient.postSignedJson(PATH, json, null);
			if (resp == null) {
				log.warn("shuyun member modify empty response companyId={} userId={}", companyId, userId);
				return false;
			}
			JsonNode code = resp.get("code");
			if (code == null || code.asInt() != 0) {
				log.warn(
						"shuyun member modify failed companyId={} userId={} msg={}",
						companyId,
						userId,
						resp.path("message").asText(""));
				return false;
			}
			return true;
		} catch (Exception e) {
			log.warn("shuyun member modify error companyId={} userId={} msg={}", companyId, userId, e.getMessage());
			return false;
		}
	}
}
