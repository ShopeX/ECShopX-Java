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

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service("shuyunMemberUnbindHttp")
@ConditionalOnProperty(
		prefix = "ecshopx.thirdparty.shuyun.member-unbind",
		name = "http-enabled",
		havingValue = "true")
public class ShuyunMemberUnbindHttpService implements ShuyunMemberUnbindPort {

	private static final String PATH = "/lpee-member-interfaces/v1/spi/spmall/member/unbind";

	private final ShuyunSignedGatewayClient shuyunSignedGatewayClient;
	private final ObjectMapper objectMapper;

	public ShuyunMemberUnbindHttpService(
			ShuyunSignedGatewayClient shuyunSignedGatewayClient, ObjectMapper objectMapper) {
		this.shuyunSignedGatewayClient = shuyunSignedGatewayClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public void tryUnbind(long companyId, long userId, String shopId) {
		if (!shuyunSignedGatewayClient.isConfigured()) {
			throw new ResourceException("注销失败");
		}
		try {
			LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
			if (StringUtils.hasText(shopId)) {
				payload.put("shopId", shopId);
			}
			payload.put("platAccount", String.valueOf(userId));
			String json = objectMapper.writeValueAsString(payload);
			JsonNode resp = shuyunSignedGatewayClient.postSignedJson(PATH, json, null);
			if (resp == null) {
				throw new ResourceException("注销失败");
			}
			JsonNode code = resp.get("code");
			if (code == null || code.asInt() != 0) {
				String msg = resp.path("message").asText("");
				throw new ResourceException(StringUtils.hasText(msg) ? msg : "注销失败");
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("shuyun member unbind error companyId={} userId={} msg={}", companyId, userId, e.getMessage());
			throw new ResourceException("注销失败");
		}
	}
}
