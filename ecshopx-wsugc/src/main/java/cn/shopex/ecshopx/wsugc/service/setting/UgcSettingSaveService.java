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

package cn.shopex.ecshopx.wsugc.service.setting;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Setting;
import cn.shopex.ecshopx.wsugc.mapper.SettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UgcSettingSaveService {

	private final SettingMapper settingMapper;
	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public UgcSettingSaveService(
			SettingMapper settingMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper) {
		this.settingMapper = settingMapper;
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> savePointSettings(
			long companyId, String typeRaw, Object settingRaw, boolean jsonRequestBody) {
		String effectiveType = typeRaw != null ? typeRaw : "point";

		LinkedHashMap<String, Object> settingMap = normalizeSettingMap(settingRaw, jsonRequestBody);

		Map<String, Object> lastRow = null;
		int now = (int) (System.currentTimeMillis() / 1000);
		Long companyIdLong = companyId;

		for (Map.Entry<String, Object> e : settingMap.entrySet()) {
			String k = e.getKey();
			String strVal = stringifyValue(e.getValue());

			LambdaQueryWrapper<Setting> w = new LambdaQueryWrapper<Setting>()
					.eq(Setting::getCompanyId, companyIdLong)
					.eq(Setting::getType, effectiveType)
					.eq(Setting::getKeyname, k)
					.last("LIMIT 1");
			Setting existing = settingMapper.selectOne(w);

			if (existing != null && existing.getId() != null) {
				existing.setKeyname(k);
				existing.setValue(strVal);
				existing.setType(effectiveType);
				existing.setCompanyId(companyIdLong);
				existing.setUpdated(now);
				int n = settingMapper.updateById(existing);
				if (n == 0) {
					throw new ResourceException("未查询到更新数据");
				}
				lastRow = toResultRow(existing);
				redis.opsForHash().put("ugc_setting:" + companyId, k, strVal);
			} else {
				Setting row = new Setting();
				row.setCompanyId(companyIdLong);
				row.setType(effectiveType);
				row.setKeyname(k);
				row.setValue(strVal);
				row.setCreated(now);
				row.setUpdated(now);
				settingMapper.insert(row);
				lastRow = toResultRow(row);
				redis.opsForHash().put("ugc_setting:" + companyId, k, strVal);
			}
		}

		String msg = resolveMessage(typeRaw, settingMap);
		Map<String, Object> result = new LinkedHashMap<>();
		if (lastRow != null) {
			result.putAll(lastRow);
		}
		result.put("message", StringUtils.hasText(msg) ? msg : "保存设置成功");
		return result;
	}

	private LinkedHashMap<String, Object> normalizeSettingMap(Object settingRaw, boolean jsonRequestBody) {
		if (settingRaw == null) {
			throw new BadRequestException("setting 不能为空");
		}
		if (settingRaw instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				throw new BadRequestException("setting 不能为空");
			}
			try {
				Map<String, Object> parsed =
						objectMapper.readValue(s, new TypeReference<Map<String, Object>>() {});
				if (parsed == null || parsed.isEmpty()) {
					throw new BadRequestException("setting 不能为空");
				}
				return new LinkedHashMap<>(parsed);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("setting 不是合法的 JSON");
			}
		}
		if (settingRaw instanceof Map<?, ?> rawMap) {
			LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : rawMap.entrySet()) {
				copy.put(String.valueOf(e.getKey()), e.getValue());
			}
			if (copy.isEmpty()) {
				throw new BadRequestException("setting 不能为空");
			}
			return copy;
		}
		throw new BadRequestException("setting 须为 JSON 对象");
	}

	private String stringifyValue(Object v) {
		if (v == null) {
			return "";
		}
		if (v instanceof Map || v instanceof List) {
			try {
				return objectMapper.writeValueAsString(v);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("setting 须为 JSON 对象");
			}
		}
		return String.valueOf(v);
	}

	private static Map<String, Object> toResultRow(Setting s) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", s.getId());
		m.put("type", s.getType());
		m.put("keyname", s.getKeyname());
		m.put("value", s.getValue());
		m.put("company_id", s.getCompanyId());
		return m;
	}

	private String resolveMessage(String typeRaw, Map<String, Object> settingMap) {
		if (typeRaw != null && "official".equals(typeRaw)) {
			return "官方账号设置成功";
		}
		if ("point".equals(typeRaw)) {
			return isEnabledFlag(settingMap.get("point_enable")) ? "积分行为已开启" : "积分行为已关闭";
		}
		if ("video".equals(typeRaw)) {
			return isEnabledFlag(settingMap.get("video_enable")) ? "会员视频上传已开启" : "会员视频上传已关闭";
		}
		if ("contentCheck".equals(typeRaw)) {
			return isEnabledFlag(settingMap.get("contentCheck_enable"))
					? "第三方审核对接已开启"
					: "第三方审核对接已关闭";
		}
		return "";
	}

	private static boolean isEnabledFlag(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() == 1;
		}
		if (v instanceof String s) {
			return "1".equals(s);
		}
		return false;
	}
}
