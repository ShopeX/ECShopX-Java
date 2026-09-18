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

package cn.shopex.ecshopx.members.service.h5;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.integration.h5.H5WxappMemberRegSettingPort;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WxappMemberEditInfoService {

	private final MembersInfoMapper membersInfoMapper;

	private final MemberAccountService memberAccountService;

	private final H5WxappMemberRegSettingPort h5WxappMemberRegSettingPort;

	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;

	private final ObjectMapper objectMapper;

	public Map<String, Object> getMemberEditInfo(long companyId, long userId, String acceptLanguageHeader) {
		MembersInfo row =
				membersInfoMapper.selectOne(
						new LambdaQueryWrapper<MembersInfo>()
								.eq(MembersInfo::getCompanyId, companyId)
								.eq(MembersInfo::getUserId, userId)
								.last("LIMIT 1"));

		Object infoPayload;
		if (row == null) {
			infoPayload = Collections.emptyList();
		} else {
			infoPayload =
					memberAccountService.toMembersInfoForMemberEditInfoBaseMap(row, sensitiveFieldEncryptor);
		}

		LinkedHashMap<String, Map<String, Object>> regSetting =
				new LinkedHashMap<>(h5WxappMemberRegSettingPort.loadWxappMemberRegSettingByKeyName(
						companyId, acceptLanguageHeader));

		if (regSetting.isEmpty()) {
			LinkedHashMap<String, Object> usernameItem = new LinkedHashMap<>();
			usernameItem.put("name", "姓名");
			usernameItem.put("is_open", Boolean.TRUE);
			usernameItem.put("element_type", "input");
			usernameItem.put("is_required", Boolean.TRUE);
			regSetting.put("username", usernameItem);
		}

		if (row != null) {
			@SuppressWarnings("unchecked")
			LinkedHashMap<String, Object> infoMap = (LinkedHashMap<String, Object>) infoPayload;
			for (Map.Entry<String, Object> e : new ArrayList<>(infoMap.entrySet())) {
				String key = e.getKey();
				Object val = e.getValue();
				if (isLooseFalsy(regSetting.get(key))) {
					continue;
				}
				if (isLooseFalsy(val)) {
					continue;
				}
				Object decoded =
						val instanceof List<?> || val instanceof Map<?, ?>
								? val
								: tryJsonDecode(objectMapper, val);
				if ("habbit".equals(key) && decoded instanceof List<?> list) {
					List<String> names = new ArrayList<>();
					for (Object elem : list) {
						if (elem instanceof Map<?, ?> hm) {
							Object ischecked = hm.get("ischecked");
							String ischeckedStr =
									ischecked == null ? "" : String.valueOf(ischecked).trim();
							if ("true".equals(ischeckedStr)) {
								Object nameVal = hm.get("name");
								if (nameVal != null) {
									names.add(String.valueOf(nameVal));
								}
							}
						} else if (elem instanceof String s) {
							names.add(s);
						}
					}
					infoMap.put("habbit", names);
					continue;
				}
			}
		}

		LinkedHashMap<String, Map<String, Object>> regSettingForResponse =
				normalizeRegisterSettingForMemberEditInfo(regSetting);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("info", infoPayload);
		out.put("registerSetting", regSettingForResponse);
		return out;
	}

	private static LinkedHashMap<String, Map<String, Object>> normalizeRegisterSettingForMemberEditInfo(
			LinkedHashMap<String, Map<String, Object>> raw) {
		LinkedHashMap<String, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, Object>> e : raw.entrySet()) {
			out.put(e.getKey(), normalizeRegisterSettingFieldMap(e.getValue()));
		}
		return out;
	}

	private static LinkedHashMap<String, Object> normalizeRegisterSettingFieldMap(Map<String, Object> src) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		if (src == null) {
			m.put("name", "");
			m.put("element_type", "");
			m.put("is_open", Boolean.FALSE);
			m.put("is_required", Boolean.FALSE);
			return m;
		}
		Object nameRaw = src.get("name");
		m.put("name", nameRaw == null ? "" : String.valueOf(nameRaw));
		Object etRaw = src.get("element_type");
		String elementType = etRaw == null ? "" : String.valueOf(etRaw);
		m.put("element_type", elementType);
		m.put("is_open", toRegisterSettingBoolean(src.get("is_open")));
		m.put("is_required", toRegisterSettingBoolean(src.get("is_required")));
		if ("checkbox".equals(elementType)) {
			if (src.containsKey("items") && src.get("items") != null) {
				m.put("items", copyCheckboxItemsForResponse(src.get("items")));
			} else if (src.containsKey("checkbox")) {
				m.put("items", copyCheckboxItemsForResponse(src.get("checkbox")));
			}
		} else if ("select".equals(elementType)) {
			if (src.containsKey("items") && src.get("items") != null) {
				m.put("items", copySelectItemsForResponse(src.get("items")));
			} else if (src.containsKey("select")) {
				m.put("items", copySelectItemsForResponse(src.get("select")));
			}
		}
		return m;
	}

	private static boolean toRegisterSettingBoolean(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		if (v instanceof String s) {
			if (s.isEmpty() || "0".equals(s)) {
				return false;
			}
			return !"false".equalsIgnoreCase(s);
		}
		return false;
	}

	private static List<Map<String, Object>> copyCheckboxItemsForResponse(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object el : list) {
			if (!(el instanceof Map<?, ?> mm)) {
				continue;
			}
			LinkedHashMap<String, Object> item = new LinkedHashMap<>();
			Object nameVal = mm.get("name");
			item.put("name", nameVal == null ? "" : String.valueOf(nameVal));
			item.put("ischecked", toRegisterSettingBoolean(mm.get("ischecked")));
			out.add(item);
		}
		return out;
	}

	private static List<String> copySelectItemsForResponse(Object raw) {
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>(list.size());
			for (Object o : list) {
				out.add(o == null ? "" : String.valueOf(o));
			}
			return out;
		}
		if (raw instanceof Map<?, ?> map) {
			List<String> out = new ArrayList<>();
			for (Object v : map.values()) {
				out.add(v == null ? "" : String.valueOf(v));
			}
			return out;
		}
		return new ArrayList<>();
	}

	private static Object tryJsonDecode(ObjectMapper objectMapper, Object val) {
		if (!(val instanceof String s)) {
			return null;
		}
		if (s.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(s, Object.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private static boolean isLooseFalsy(Object val) {
		if (val == null) {
			return true;
		}
		if (Boolean.FALSE.equals(val)) {
			return true;
		}
		if (val instanceof Number n && n.doubleValue() == 0.0d) {
			return true;
		}
		if (val instanceof String s && (s.isEmpty() || "0".equals(s))) {
			return true;
		}
		if (val instanceof Collection<?> c && c.isEmpty()) {
			return true;
		}
		if (val instanceof Map<?, ?> m && m.isEmpty()) {
			return true;
		}
		if (val instanceof Object[] a && a.length == 0) {
			return true;
		}
		if (val instanceof int[] a && a.length == 0) {
			return true;
		}
		if (val instanceof long[] a && a.length == 0) {
			return true;
		}
		if (val instanceof short[] a && a.length == 0) {
			return true;
		}
		if (val instanceof byte[] a && a.length == 0) {
			return true;
		}
		if (val instanceof char[] a && a.length == 0) {
			return true;
		}
		if (val instanceof float[] a && a.length == 0) {
			return true;
		}
		if (val instanceof double[] a && a.length == 0) {
			return true;
		}
		if (val instanceof boolean[] a && a.length == 0) {
			return true;
		}
		return false;
	}
}
