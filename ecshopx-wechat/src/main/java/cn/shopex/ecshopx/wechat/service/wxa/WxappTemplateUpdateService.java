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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.superadmin.domain.WxappTemplate;
import cn.shopex.ecshopx.superadmin.mapper.WxappTemplateMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WxappTemplateUpdateService {

	private final WxappTemplateMapper wxappTemplateMapper;
	private final ObjectMapper objectMapper;
	private final boolean systemIsSaas;

	public WxappTemplateUpdateService(
			WxappTemplateMapper wxappTemplateMapper,
			ObjectMapper objectMapper,
			@Value("${common.system-is-saas:false}") boolean systemIsSaas) {
		this.wxappTemplateMapper = wxappTemplateMapper;
		this.objectMapper = objectMapper;
		this.systemIsSaas = systemIsSaas;
	}

	public Map<String, Object> updateWxappTemplate(Map<String, Object> effectiveBody) {
		if (systemIsSaas) {
			throw new ResourceException("当前系统无权限");
		}
		Object idRaw = effectiveBody == null ? null : effectiveBody.get("id");
		Long id = parseId(idRaw);

		Map<String, Object> data = new LinkedHashMap<>();
		if (effectiveBody != null) {
			data.putAll(effectiveBody);
		}
		data.remove("id");

		WxappTemplate row = wxappTemplateMapper.selectById(id);
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}

		if (data.containsKey("key_name") && isTruthyScalar(data.get("key_name"))) {
			row.setKeyName(toText(data.get("key_name")));
		}
		if (data.containsKey("name") && isTruthyScalar(data.get("name"))) {
			row.setName(toText(data.get("name")));
		}
		if (data.containsKey("tag") && isTruthyScalar(data.get("tag"))) {
			row.setTag(toText(data.get("tag")));
		}
		if (data.containsKey("template_id") && isTruthyScalar(data.get("template_id"))) {
			row.setTemplateId(toInteger(data.get("template_id")));
		}
		if (data.containsKey("template_id_2") && isTruthyScalar(data.get("template_id_2"))) {
			row.setTemplateId2(toInteger(data.get("template_id_2")));
		}
		if (data.containsKey("version") && isTruthyScalar(data.get("version"))) {
			row.setVersion(toText(data.get("version")));
		}
		if (data.containsKey("description") && isTruthyScalar(data.get("description"))) {
			row.setDescription(toText(data.get("description")));
		}
		if (data.containsKey("is_only") && data.get("is_only") != null) {
			row.setIsOnly(parseIsOnly(data.get("is_only")));
		}
		if (data.containsKey("domain")) {
			applyDomainColumn(row, data.get("domain"));
		}
		if (data.containsKey("is_disabled") && data.get("is_disabled") != null) {
			row.setIsDisabled(!isFalsyDisabledToggle(data.get("is_disabled")));
		}
		if (data.containsKey("created") && isTruthyScalar(data.get("created"))) {
			row.setCreated(toInteger(data.get("created")));
		}
		if (data.containsKey("updated") && isTruthyScalar(data.get("updated"))) {
			row.setUpdated(toInteger(data.get("updated")));
		} else {
			row.setUpdated((int) (System.currentTimeMillis() / 1000L));
		}

		wxappTemplateMapper.updateById(row);

		WxappTemplate refreshed = wxappTemplateMapper.selectById(id);
		if (refreshed == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toResponseMap(refreshed);
	}

	private static String toText(Object v) {
		if (v instanceof String s) {
			return s;
		}
		return v == null ? null : String.valueOf(v);
	}

	private static boolean isTruthyScalar(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.FALSE.equals(v)) {
			return false;
		}
		if (v instanceof Number n && n.doubleValue() == 0.0) {
			return false;
		}
		if (v instanceof String s) {
			return !s.isEmpty() && !"0".equals(s);
		}
		if (v instanceof Collection<?> c && c.isEmpty()) {
			return false;
		}
		if (v instanceof Map<?, ?> m && m.isEmpty()) {
			return false;
		}
		return true;
	}

	private static boolean isFalsyDisabledToggle(Object v) {
		if (v == null) {
			return true;
		}
		if (Boolean.FALSE.equals(v)) {
			return true;
		}
		if (v instanceof Boolean b && !b.booleanValue()) {
			return true;
		}
		if (v instanceof Number n && n.doubleValue() == 0.0) {
			return true;
		}
		if (v instanceof String s) {
			if (s.isEmpty()) {
				return true;
			}
			if ("0".equals(s)) {
				return true;
			}
			return "false".equals(s);
		}
		if (v instanceof Collection<?> c && c.isEmpty()) {
			return true;
		}
		return v instanceof Map<?, ?> m && m.isEmpty();
	}

	private static boolean parseIsOnly(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.doubleValue() != 0.0;
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return false;
			}
			String lower = t.toLowerCase(Locale.ROOT);
			if ("1".equals(t) || "true".equals(lower) || "yes".equals(lower) || "on".equals(lower)) {
				return true;
			}
			if ("0".equals(t) || "false".equals(lower) || "no".equals(lower) || "off".equals(lower)) {
				return false;
			}
			return true;
		}
		return false;
	}

	private static int toInteger(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v instanceof String s) {
			try {
				return Integer.parseInt(s.trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("参数错误");
			}
		}
		throw new ResourceException("参数错误");
	}

	private void applyDomainColumn(WxappTemplate row, Object v) {
		if (!isTruthyScalar(v)) {
			return;
		}
		if (v instanceof String s) {
			String trimmed = s.trim();
			try {
				JsonNode node = objectMapper.readTree(trimmed);
				if (node.isNull() || (!node.isObject() && !node.isArray())) {
					throw new ResourceException("参数错误");
				}
				row.setDomain(objectMapper.writeValueAsString(node));
			} catch (JsonProcessingException e) {
				throw new ResourceException("参数错误");
			}
			return;
		}
		if (v instanceof Map<?, ?> || v instanceof Collection<?>) {
			try {
				row.setDomain(objectMapper.writeValueAsString(v));
			} catch (JsonProcessingException e) {
				throw new ResourceException("参数错误");
			}
			return;
		}
		throw new ResourceException("参数错误");
	}

	private static Long parseId(Object idRaw) {
		if (idRaw == null) {
			throw new ResourceException("参数错误");
		}
		if (Boolean.FALSE.equals(idRaw)) {
			throw new ResourceException("参数错误");
		}
		if (idRaw instanceof Number n) {
			if (n.longValue() == 0L) {
				throw new ResourceException("参数错误");
			}
			return n.longValue();
		}
		if (idRaw instanceof String s) {
			if (s.isEmpty() || "0".equals(s)) {
				throw new ResourceException("参数错误");
			}
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("未查询到更新数据");
			}
		}
		if (idRaw instanceof Collection<?> c && c.isEmpty()) {
			throw new ResourceException("参数错误");
		}
		if (idRaw instanceof Map<?, ?> m && m.isEmpty()) {
			throw new ResourceException("参数错误");
		}
		if (idRaw instanceof Boolean || idRaw instanceof Collection<?> || idRaw instanceof Map<?, ?>) {
			throw new ResourceException("参数错误");
		}
		throw new ResourceException("参数错误");
	}

	private Map<String, Object> toResponseMap(WxappTemplate row) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("id", row.getId());
		out.put("key_name", row.getKeyName());
		out.put("name", row.getName());
		out.put("tag", row.getTag());
		out.put("template_id", row.getTemplateId());
		out.put("template_id_2", row.getTemplateId2());
		out.put("version", row.getVersion());
		out.put("is_only", row.getIsOnly());
		out.put("description", row.getDescription());
		out.put("is_disabled", row.getIsDisabled());
		out.put("created", row.getCreated());
		out.put("updated", row.getUpdated());

		Object domainOut;
		String domainCol = row.getDomain();
		if (domainCol == null || domainCol.trim().isEmpty()) {
			domainOut = null;
		} else {
			String trimmed = domainCol.trim();
			try {
				JsonNode root = objectMapper.readTree(trimmed);
				if (root.isNull()) {
					domainOut = null;
				} else if (root.isObject()) {
					LinkedHashMap<String, Object> m =
							objectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
					domainOut = m != null ? m : new LinkedHashMap<String, Object>();
				} else if (root.isArray()) {
					domainOut = objectMapper.convertValue(root, new TypeReference<List<Object>>() {});
				} else {
					domainOut = new LinkedHashMap<String, Object>();
				}
			} catch (Exception e) {
				domainOut = new LinkedHashMap<String, Object>();
			}
		}
		out.put("domain", domainOut);
		return out;
	}
}
