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

package cn.shopex.ecshopx.youshu.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.youshu.domain.YoushuSetting;
import cn.shopex.ecshopx.youshu.mapper.YoushuSettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class YoushuSettingService {

	private final YoushuSettingMapper mapper;

	public YoushuSettingService(YoushuSettingMapper mapper) {
		this.mapper = mapper;
	}

	/** 按公司查询有数配置单行；无记录时返回 {@code null}。 */
	public Map<String, Object> getInfo(long companyId) {
		LambdaQueryWrapper<YoushuSetting> w = Wrappers.lambdaQuery(YoushuSetting.class)
				.eq(YoushuSetting::getCompanyId, companyId)
				.last("LIMIT 1");
		YoushuSetting entity = mapper.selectOne(w);
		if (entity == null) {
			return null;
		}
		return toResponseRow(entity);
	}

	public Map<String, Object> saveData(Map<String, Object> mergedInput, long companyId) {
		Object idRaw = mergedInput.get("id");
		if (isBlankIdValue(idRaw)) {
			YoushuSetting entity = new YoushuSetting();
			applyBusinessFields(entity, mergedInput, companyId);
			LocalDateTime now = LocalDateTime.now();
			entity.setCreatedAt(now);
			entity.setUpdatedAt(now);
			mapper.insert(entity);
			return toResponseRow(entity);
		}
		LambdaQueryWrapper<YoushuSetting> w = Wrappers.lambdaQuery(YoushuSetting.class)
				.eq(YoushuSetting::getCompanyId, companyId)
				.last("LIMIT 1");
		YoushuSetting entity = mapper.selectOne(w);
		if (entity == null) {
			throw new ResourceException("未查询到更新数据");
		}
		applyBusinessFields(entity, mergedInput, companyId);
		entity.setUpdatedAt(LocalDateTime.now());
		mapper.updateById(entity);
		return toResponseRow(entity);
	}

	private static void applyBusinessFields(YoushuSetting entity, Map<String, Object> merged, long companyId) {
		entity.setCompanyId(companyId);
		entity.setMerchantId(trimmedStringOrEmpty(merged.get("merchant_id")));
		entity.setAppId(trimmedStringOrEmpty(merged.get("app_id")));
		entity.setAppSecret(trimmedStringOrEmpty(merged.get("app_secret")));
		entity.setApiUrl(trimmedStringOrEmpty(merged.get("api_url")));
		entity.setSandboxAppId(trimmedStringOrEmpty(merged.get("sandbox_app_id")));
		entity.setSandboxAppSecret(trimmedStringOrEmpty(merged.get("sandbox_app_secret")));
		entity.setSandboxApiUrl(trimmedStringOrEmpty(merged.get("sandbox_api_url")));
		entity.setWeappName(trimmedStringOrEmpty(merged.get("weapp_name")));
		entity.setWeappAppId(trimmedStringOrEmpty(merged.get("weapp_app_id")));
	}

	private static String trimmedStringOrEmpty(Object raw) {
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw).trim();
	}

	/**
	 * 判断请求中的 {@code id} 是否应视为未提供：满足时走新增（insert），否则走更新。
	 * {@code null}、{@code false}、数值零（非 NaN）、空串或仅空白、或字符串 {@code "0"} 视为未提供；
	 * 其余类型视为已提供 id。
	 */
	private static boolean isBlankIdValue(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof Boolean b) {
			return !b;
		}
		if (raw instanceof Number n) {
			double d = n.doubleValue();
			if (Double.isNaN(d)) {
				return false;
			}
			return d == 0.0;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		return false;
	}

	private static Map<String, Object> toResponseRow(YoushuSetting e) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", e.getId());
		row.put("company_id", e.getCompanyId());
		row.put("merchant_id", e.getMerchantId());
		row.put("app_id", e.getAppId());
		row.put("app_secret", e.getAppSecret());
		row.put("api_url", e.getApiUrl());
		row.put("sandbox_app_id", e.getSandboxAppId());
		row.put("sandbox_app_secret", e.getSandboxAppSecret());
		row.put("sandbox_api_url", e.getSandboxApiUrl());
		row.put("weapp_name", e.getWeappName());
		row.put("weapp_app_id", e.getWeappAppId());
		return row;
	}
}
