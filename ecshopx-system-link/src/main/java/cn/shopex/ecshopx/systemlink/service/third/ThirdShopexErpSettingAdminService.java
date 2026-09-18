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

package cn.shopex.ecshopx.systemlink.service.third;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.service.ome.ShopexErpSettingRedisAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ThirdShopexErpSettingAdminService {

	private final ShopexErpSettingRedisAccessor shopexErpSettingRedisAccessor;
	private final ObjectMapper objectMapper;

	public ThirdShopexErpSettingAdminService(
			ShopexErpSettingRedisAccessor shopexErpSettingRedisAccessor,
			ObjectMapper objectMapper) {
		this.shopexErpSettingRedisAccessor = shopexErpSettingRedisAccessor;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getShopexErpSetting(long companyId) {
		String raw = shopexErpSettingRedisAccessor.getSettingJsonRaw(companyId);
		if (raw == null || !StringUtils.hasText(raw)) {
			Map<String, Object> defaults = new LinkedHashMap<>();
			defaults.put("is_open", Boolean.FALSE);
			defaults.put("node_id", "");
			return defaults;
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			return null;
		}
	}

	public void setShopexErpSetting(long companyId, Map<String, Object> mergedInput) {
		boolean isOpen = mergedInput != null
				&& mergedInput.containsKey("is_open")
				&& isOpenLooseEqTrueString(mergedInput.get("is_open"));

		boolean isOpenapiOpen = mergedInput != null
				&& mergedInput.containsKey("is_openapi_open")
				&& isOpenLooseEqTrueString(mergedInput.get("is_openapi_open"));

		String nodeId = trimToEmpty(mergedInput, "node_id");
		String openapiFlag = trimToEmpty(mergedInput, "openapi_flag");
		String openapiToken = trimToEmpty(mergedInput, "openapi_token");

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("node_id", nodeId);
		data.put("is_open", isOpen);
		data.put("openapi_flag", openapiFlag);
		data.put("openapi_token", openapiToken);
		data.put("is_openapi_open", isOpenapiOpen);

		try {
			String json = objectMapper.writeValueAsString(data);
			shopexErpSettingRedisAccessor.putSettingJson(companyId, json);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("配置序列化失败");
		}
	}

	private static boolean isOpenLooseEqTrueString(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		if (v instanceof String s) {
			return "true".equals(s);
		}
		return false;
	}

	private static String trimToEmpty(Map<String, Object> map, String key) {
		Object v = map == null ? null : map.get(key);
		return v == null ? "" : v.toString().trim();
	}
}
