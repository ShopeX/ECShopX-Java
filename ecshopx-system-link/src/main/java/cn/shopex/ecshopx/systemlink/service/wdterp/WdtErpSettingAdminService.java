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

package cn.shopex.ecshopx.systemlink.service.wdterp;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.systemlink.wdterp.WdtErpOpenApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WdtErpSettingAdminService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final WdtErpOpenApiClient wdtErpOpenApiClient;
	private final String shopQueryMethod;

	public WdtErpSettingAdminService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			WdtErpOpenApiClient wdtErpOpenApiClient,
			@Value("${ecshopx.wdterp.methods.shop-query:setting.Shop.queryShop}") String shopQueryMethod) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.wdtErpOpenApiClient = wdtErpOpenApiClient;
		this.shopQueryMethod = shopQueryMethod;
	}

	public Map<String, Object> getSetting(long companyId) {
		String key = "WdtErpSetting:" + sha1HexUtf8(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);

		if (!StringUtils.hasText(raw)) {
			LinkedHashMap<String, Object> data = new LinkedHashMap<>();
			data.put("is_open", Boolean.FALSE);
			return data;
		}
		try {
			LinkedHashMap<String, Object> parsed =
					objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
			if (parsed == null || parsed.isEmpty()) {
				LinkedHashMap<String, Object> data = new LinkedHashMap<>();
				data.put("is_open", Boolean.FALSE);
				return data;
			}
			return parsed;
		} catch (Exception e) {
			LinkedHashMap<String, Object> data = new LinkedHashMap<>();
			data.put("is_open", Boolean.FALSE);
			return data;
		}
	}

	public void setSetting(long companyId, Map<String, Object> mergedInput) {
		boolean isOpen = mergedInput != null
				&& mergedInput.containsKey("is_open")
				&& isOpenLooseEqTrueString(mergedInput.get("is_open"));

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("is_open", Boolean.valueOf(isOpen));
		data.put("sid", trimToEmpty(mergedInput, "sid"));
		data.put("app_key", trimToEmpty(mergedInput, "app_key"));
		data.put("app_secret", trimToEmpty(mergedInput, "app_secret"));
		data.put("shop_no", trimToEmpty(mergedInput, "shop_no"));
		data.put("company_id", Long.valueOf(companyId));

		if (isOpen) {
			String rawShopNoForQuery =
					mergedInput.get("shop_no") == null ? "" : mergedInput.get("shop_no").toString();
			Map<String, Object> parMap = new LinkedHashMap<>();
			parMap.put("platform_id", 127);
			parMap.put("shop_no", rawShopNoForQuery);

			Object raw;
			try {
				raw = wdtErpOpenApiClient.call(
						companyId,
						shopQueryMethod,
						List.of(parMap),
						data.get("sid").toString(),
						data.get("app_key").toString(),
						data.get("app_secret").toString());
			} catch (Exception e) {
				throw new ResourceException(
						"调用旺店通失败:" + (e.getMessage() == null ? "" : e.getMessage()));
			}

			if (raw instanceof Map<?, ?> rm && rm.containsKey("fail_msg")) {
				Object fm = rm.get("fail_msg");
				throw new ResourceException("调用旺店通失败:" + (fm == null ? "" : fm.toString()));
			}
			if (!(raw instanceof Map<?, ?>)) {
				throw new ResourceException("店铺编码不存在");
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> dataNode = (Map<String, Object>) raw;

			Object tc = dataNode.get("total_count");
			if (tc instanceof Number n) {
				if (n.intValue() == 0) {
					throw new ResourceException("店铺编码不存在");
				}
			} else {
				throw new ResourceException("店铺编码不存在");
			}

			Object d = dataNode.get("details");
			if (!(d instanceof List<?>) || ((List<?>) d).isEmpty()) {
				throw new ResourceException("店铺编码不存在");
			}
			Object first = ((List<?>) d).get(0);
			if (!(first instanceof Map<?, ?>)) {
				throw new ResourceException("店铺编码不存在");
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> detailRow = (Map<String, Object>) first;
			if (detailRow.get("shop_id") == null) {
				throw new ResourceException("店铺编码不存在");
			}
			data.put("shop_id", normalizeShopId(detailRow.get("shop_id")));
		}

		String key = "WdtErpSetting:" + sha1HexUtf8(String.valueOf(companyId));
		try {
			companysRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data));
		} catch (JsonProcessingException e) {
			throw new BadRequestException("配置序列化失败");
		}
	}

	private static Object normalizeShopId(Object shopIdObj) {
		if (shopIdObj instanceof Number n) {
			return n.longValue();
		}
		if (shopIdObj instanceof String s) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("调用旺店通失败:invalid shop_id");
			}
		}
		throw new ResourceException("调用旺店通失败:invalid shop_id");
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

	private static String sha1HexUtf8(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(dig);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
