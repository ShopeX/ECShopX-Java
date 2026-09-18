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

package cn.shopex.ecshopx.espier.integration.members;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.espier.service.config.ConfigRequestFieldsApplicationService;
import cn.shopex.ecshopx.espier.service.config.ConfigRequestFieldsConstants;
import cn.shopex.ecshopx.members.integration.admin.AdminMemberGetInfoConfigRequestFieldsPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("adminMemberGetInfoConfigRequestFieldsPortImpl")
public class AdminMemberGetInfoConfigRequestFieldsPortImpl implements AdminMemberGetInfoConfigRequestFieldsPort {

	private final ConfigRequestFieldsApplicationService configRequestFieldsApplicationService;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public AdminMemberGetInfoConfigRequestFieldsPortImpl(
			ConfigRequestFieldsApplicationService configRequestFieldsApplicationService,
			ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.configRequestFieldsApplicationService = configRequestFieldsApplicationService;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	@Override
	public void enrichMemberInfo(long companyId, Map<String, Object> memberRow, HttpServletRequest request) {
		enrichMemberInfoList(companyId, List.of(memberRow), request);
	}

	@Override
	public void enrichMemberInfoList(
			long companyId, List<? extends Map<String, Object>> memberRows, HttpServletRequest request) {
		if (memberRows == null || memberRows.isEmpty()) {
			return;
		}
		List<Map<String, Object>> openRows = loadOpenMemberInfoFieldRows(companyId);
		if (openRows.isEmpty()) {
			for (Map<String, Object> memberRow : memberRows) {
				memberRow.put("requestFields", new LinkedHashMap<String, Object>());
				memberRow.put("datapassRequestFields", new LinkedHashMap<String, Object>());
			}
			return;
		}
		Map<String, Map<String, Object>> settingByKey = buildSettingFormatByKey(openRows);
		for (Map<String, Object> memberRow : memberRows) {
			applyOpenRowsToMemberRow(memberRow, openRows, settingByKey);
		}
	}

	private List<Map<String, Object>> loadOpenMemberInfoFieldRows(long companyId) {
		int intCompanyId;
		try {
			intCompanyId = Math.toIntExact(companyId);
		} catch (ArithmeticException e) {
			throw new BadRequestException("参数 company_id 错误");
		}
		String acceptLang = RequestLangTag.current(langueProperties);
		if (acceptLang != null && acceptLang.isBlank()) {
			acceptLang = null;
		}
		Map<String, Object> out = configRequestFieldsApplicationService.listEspierAdminPaginated(
				intCompanyId,
				ConfigRequestFieldsConstants.MODULE_TYPE_MEMBER_INFO,
				-1,
				1,
				0,
				acceptLang);
		Object listObj = out.get("list");
		if (!(listObj instanceof List<?> rawList)) {
			return List.of();
		}
		List<Map<String, Object>> openRows = new ArrayList<>();
		for (Object o : rawList) {
			if (!(o instanceof Map<?, ?> rm)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> row = (Map<String, Object>) (Map<?, ?>) rm;
			if (isTruthyOpen(row.get("is_open"))) {
				openRows.add(row);
			}
		}
		return openRows;
	}

	private void applyOpenRowsToMemberRow(
			Map<String, Object> memberRow,
			List<Map<String, Object>> openRows,
			Map<String, Map<String, Object>> settingByKey) {
		LinkedHashMap<String, Object> requestFields = new LinkedHashMap<>();
		LinkedHashMap<String, List<String>> datapassMobileKeys = new LinkedHashMap<>();
		datapassMobileKeys.put("mobile", new ArrayList<>());
		for (Map<String, Object> item : openRows) {
			String keyName = stringVal(item.get("key_name"));
			if (keyName.isEmpty()) {
				continue;
			}
			int fieldType = intVal(item.get("field_type"), 0);
			Object rawVal = resolveMemberFieldValue(memberRow, keyName);
			requestFields.put(keyName, rawVal);
			if (fieldType == ConfigRequestFieldsConstants.FIELD_TYPE_MOBILE) {
				datapassMobileKeys.get("mobile").add(keyName);
			}
		}
		for (Map<String, Object> item : openRows) {
			String keyName = stringVal(item.get("key_name"));
			if (keyName.isEmpty()) {
				continue;
			}
			int fieldType = intVal(item.get("field_type"), 0);
			if (fieldType != ConfigRequestFieldsConstants.FIELD_TYPE_CHECKBOX) {
				continue;
			}
			Object v = requestFields.get(keyName);
			if (v == null || (v instanceof String s && !StringUtils.hasText(s))) {
				continue;
			}
			applyCheckboxAggregationToMemberRow(memberRow, keyName, v, requestFields);
		}
		for (Map.Entry<String, Object> e : new LinkedHashMap<>(requestFields).entrySet()) {
			String key = e.getKey();
			Map<String, Object> fieldDef = settingByKey.get(key);
			if (fieldDef == null || fieldDef.get("field_type") == null) {
				continue;
			}
			int ft = intVal(fieldDef.get("field_type"), 0);
			if (ft == ConfigRequestFieldsConstants.FIELD_TYPE_RADIO) {
				@SuppressWarnings("unchecked")
				Map<String, Object> select = (Map<String, Object>) fieldDef.get("select");
				if (select != null) {
					Object val = requestFields.get(key);
					String desc = lookupRadioDesc(select, val);
					requestFields.put(key, desc);
				}
			}
		}
		memberRow.put("requestFields", requestFields);
		memberRow.put("datapassRequestFields", datapassMobileKeys);
	}

	private static String lookupRadioDesc(Map<String, Object> select, Object value) {
		if (value == null) {
			return null;
		}
		String vk = String.valueOf(value);
		if (select.containsKey(vk)) {
			Object d = select.get(vk);
			return d == null ? null : String.valueOf(d);
		}
		for (Map.Entry<String, Object> en : select.entrySet()) {
			Object ev = en.getValue();
			if (ev != null && vk.equals(String.valueOf(ev))) {
				return String.valueOf(ev);
			}
		}
		return null;
	}

	private Map<String, Map<String, Object>> buildSettingFormatByKey(List<Map<String, Object>> openRows) {
		LinkedHashMap<String, Map<String, Object>> out = new LinkedHashMap<>();
		for (Map<String, Object> item : openRows) {
			String keyName = stringVal(item.get("key_name"));
			if (keyName.isEmpty()) {
				continue;
			}
			int fieldType = intVal(item.get("field_type"), 0);
			LinkedHashMap<String, Object> data = new LinkedHashMap<>();
			data.put("field_type", fieldType);
			switch (fieldType) {
				case ConfigRequestFieldsConstants.FIELD_TYPE_RADIO -> {
					@SuppressWarnings("unchecked")
					List<Map<String, Object>> radioList =
							item.get("radio_list") instanceof List<?> l
									? (List<Map<String, Object>>) (List<?>) l
									: List.of();
					LinkedHashMap<String, Object> select = new LinkedHashMap<>();
					for (Map<String, Object> datum : radioList) {
						Object value = datum.get("value");
						String valueStr = value != null ? String.valueOf(value) : "";
						select.put(valueStr, stringVal(datum.get("label")));
					}
					data.put("select", select);
				}
				default -> {
				}
			}
			out.put(keyName, data);
		}
		return out;
	}

	private void applyCheckboxAggregationToMemberRow(
			Map<String, Object> memberRow,
			String keyName,
			Object rawCheckbox,
			LinkedHashMap<String, Object> requestFields) {
		List<Map<String, Object>> items = decodeCheckboxItems(rawCheckbox);
		if (items.isEmpty()) {
			requestFields.put(keyName, new ArrayList<>());
			return;
		}
		List<String> checkedNames = new ArrayList<>();
		for (Map<String, Object> checkboxItem : items) {
			Object chk = checkboxItem.get("ischecked");
			boolean on =
					Boolean.TRUE.equals(chk)
							|| "true".equalsIgnoreCase(String.valueOf(chk))
							|| "1".equals(String.valueOf(chk));
			checkboxItem.put("ischecked", on);
			if (on) {
				Object name = checkboxItem.get("name");
				if (name == null) {
					name = checkboxItem.get("label");
				}
				if (name != null && StringUtils.hasText(String.valueOf(name))) {
					checkedNames.add(String.valueOf(name).trim());
				}
			}
		}
		String joined = String.join(",", checkedNames);
		requestFields.put(keyName, items);
		if (memberRow.containsKey(keyName)) {
			memberRow.put(keyName, joined);
			return;
		}
		updateCustomDataInOtherParams(memberRow, keyName, joined);
	}

	private void updateCustomDataInOtherParams(Map<String, Object> memberRow, String keyName, String joined) {
		Object op = memberRow.get("other_params");
		if (op instanceof Map<?, ?> root) {
			LinkedHashMap<String, Object> m = copyStringKeyedMap(root);
			Object cdObj = m.get("custom_data");
			if (!(cdObj instanceof Map<?, ?> cm)) {
				return;
			}
			LinkedHashMap<String, Object> custom = copyStringKeyedMap(cm);
			custom.put(keyName, joined);
			m.put("custom_data", custom);
			memberRow.put("other_params", m);
			return;
		}
		if (!(op instanceof String raw) || !StringUtils.hasText(raw)) {
			return;
		}
		try {
			Object parsed = objectMapper.readValue(raw.trim(), new TypeReference<Object>() {});
			if (!(parsed instanceof Map<?, ?> parsedRoot)) {
				return;
			}
			LinkedHashMap<String, Object> m = copyStringKeyedMap(parsedRoot);
			Object cdObj = m.get("custom_data");
			if (!(cdObj instanceof Map<?, ?> cm)) {
				return;
			}
			LinkedHashMap<String, Object> custom = copyStringKeyedMap(cm);
			custom.put(keyName, joined);
			m.put("custom_data", custom);
			memberRow.put("other_params", objectMapper.writeValueAsString(m));
		} catch (JsonProcessingException ignored) {
			// keep other_params unchanged on malformed JSON
		}
	}

	private static LinkedHashMap<String, Object> copyStringKeyedMap(Map<?, ?> src) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : src.entrySet()) {
			if (e.getKey() != null) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return out;
	}

	private List<Map<String, Object>> decodeCheckboxItems(Object rawCheckbox) {
		try {
			if (rawCheckbox instanceof String s) {
				if (!StringUtils.hasText(s)) {
					return List.of();
				}
				Object v = objectMapper.readValue(s.trim(), new TypeReference<Object>() {});
				return asCheckboxItemList(v);
			}
			return asCheckboxItemList(rawCheckbox);
		} catch (JsonProcessingException e) {
			return List.of();
		}
	}

	private static List<Map<String, Object>> asCheckboxItemList(Object v) {
		if (!(v instanceof List<?> list)) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Map<?, ?> im) {
				@SuppressWarnings("unchecked")
				Map<String, Object> row = (Map<String, Object>) (Map<?, ?>) im;
				out.add(row);
			}
		}
		return out;
	}

	private Object resolveMemberFieldValue(Map<String, Object> memberRow, String keyName) {
		if (memberRow.containsKey(keyName)) {
			return memberRow.get(keyName);
		}
		Object op = memberRow.get("other_params");
		if (op instanceof Map<?, ?> root) {
			Object cd = root.get("custom_data");
			if (cd instanceof Map<?, ?> cm) {
				return cm.get(keyName);
			}
			return null;
		}
		if (op instanceof String raw && StringUtils.hasText(raw)) {
			try {
				Object parsed = objectMapper.readValue(raw.trim(), new TypeReference<Map<String, Object>>() {});
				if (parsed instanceof Map<?, ?> m) {
					Object cd = m.get("custom_data");
					if (cd instanceof Map<?, ?> cm) {
						return cm.get(keyName);
					}
				}
			} catch (JsonProcessingException ignored) {
			}
		}
		return null;
	}

	private static boolean isTruthyOpen(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(v).trim();
		return !s.isEmpty() && !"0".equals(s) && !"false".equalsIgnoreCase(s);
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static int intVal(Object v, int d) {
		if (v == null) {
			return d;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return d;
		}
	}
}
