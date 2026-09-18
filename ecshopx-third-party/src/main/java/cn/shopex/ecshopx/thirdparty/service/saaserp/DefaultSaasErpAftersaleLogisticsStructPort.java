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

package cn.shopex.ecshopx.thirdparty.service.saaserp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DefaultSaasErpAftersaleLogisticsStructPort implements SaasErpAftersaleLogisticsStructPort {

	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

	private final ObjectMapper objectMapper;

	public DefaultSaasErpAftersaleLogisticsStructPort(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public Map<String, Object> buildAfterLogisticsOrNull(long companyId, Map<String, Object> entities) {
		if (companyId <= 0L || entities == null || entities.isEmpty()) {
			return null;
		}
		Map<String, Object> sendback = resolveSendbackMap(entities.get("sendback_data"));
		if (sendback == null || sendback.isEmpty()) {
			return null;
		}
		Object corpCode = sendback.get("corp_code");
		Object logiNo = sendback.get("logi_no");
		if (!StringUtils.hasText(stringOf(corpCode)) || !StringUtils.hasText(stringOf(logiNo))) {
			return null;
		}
		Object bnRaw = entities.get("aftersales_bn");
		Object tidRaw = entities.get("order_id");
		if (bnRaw == null || tidRaw == null) {
			return null;
		}
		try {
			Map<String, Object> logisticsInfo = new LinkedHashMap<>();
			logisticsInfo.put("logistics_company", stringOf(corpCode));
			logisticsInfo.put("logistics_no", stringOf(logiNo));
			String logisticsJson = objectMapper.writeValueAsString(logisticsInfo);
			Map<String, Object> afterData = new LinkedHashMap<>();
			afterData.put("aftersale_id", bnRaw);
			afterData.put("tid", tidRaw);
			afterData.put("logistics_info", logisticsJson);
			return afterData;
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> resolveSendbackMap(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return null;
			}
			try {
				return objectMapper.readValue(s, MAP_TYPE);
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	private static String stringOf(Object o) {
		if (o == null) {
			return "";
		}
		return o.toString().trim();
	}
}
