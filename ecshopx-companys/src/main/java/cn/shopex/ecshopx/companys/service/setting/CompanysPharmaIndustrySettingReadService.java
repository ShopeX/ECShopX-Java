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

package cn.shopex.ecshopx.companys.service.setting;

import cn.shopex.ecshopx.thirdparty.domain.CompanyRelKuaizhen;
import cn.shopex.ecshopx.thirdparty.mapper.CompanyRelKuaizhenMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanysPharmaIndustrySettingReadService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final CompanyRelKuaizhenMapper companyRelKuaizhenMapper;

	public CompanysPharmaIndustrySettingReadService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			CompanyRelKuaizhenMapper companyRelKuaizhenMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.companyRelKuaizhenMapper = companyRelKuaizhenMapper;
	}

	public Map<String, Object> getMedicineSetting(long companyId) {
		String key = "PharmaIndustrySetting:" + companyId;
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return defaultMedicineSetting();
		}
		try {
			Map<String, Object> parsed = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			if (parsed == null) {
				return defaultMedicineSetting();
			}
			Object useThird = parsed.get("use_third_party_system");
			if (!"kuaizhen580".equals(Objects.toString(useThird, ""))) {
				return parsed;
			}
			Map<String, Object> data = new LinkedHashMap<>(parsed);
			Map<String, Object> kuaizhen580Config = new LinkedHashMap<>();
			Object kzRaw = data.get("kuaizhen580_config");
			if (kzRaw instanceof Map<?, ?> m) {
				for (Map.Entry<?, ?> e : m.entrySet()) {
					kuaizhen580Config.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
			data.put("kuaizhen580_config", kuaizhen580Config);
			CompanyRelKuaizhen row =
					companyRelKuaizhenMapper.selectOne(
							new LambdaQueryWrapper<CompanyRelKuaizhen>()
									.eq(CompanyRelKuaizhen::getCompanyId, companyId)
									.last("LIMIT 1"));
			kuaizhen580Config.put(
					"client_id", row == null || row.getClientId() == null ? "" : row.getClientId());
			kuaizhen580Config.put(
					"client_secret",
					row == null || row.getClientSecret() == null ? "" : row.getClientSecret());
			kuaizhen580Config.put(
					"kuaizhen_store_id",
					row == null || row.getKuaizhenStoreId() == null ? 0 : row.getKuaizhenStoreId());
			return data;
		} catch (Exception e) {
			return defaultMedicineSetting();
		}
	}

	private static Map<String, Object> defaultMedicineSetting() {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("is_pharma_industry", 0);
		data.put("use_third_party_system", "");
		Map<String, Object> kz = new LinkedHashMap<>();
		kz.put("client_id", "");
		kz.put("client_secret", "");
		kz.put("kuaizhen_store_id", 0);
		data.put("kuaizhen580_config", kz);
		return data;
	}
}
