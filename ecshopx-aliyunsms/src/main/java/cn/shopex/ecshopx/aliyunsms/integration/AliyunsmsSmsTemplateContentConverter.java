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

package cn.shopex.ecshopx.aliyunsms.integration;

import cn.shopex.ecshopx.aliyunsms.domain.Scene;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AliyunsmsSmsTemplateContentConverter {

	private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(.+?)\\}");

	private static final Map<String, String> TEMPLATE_CONVERSION_RULE;

	static {
		TEMPLATE_CONVERSION_RULE = new LinkedHashMap<>();
		TEMPLATE_CONVERSION_RULE.put("code", "numberCaptcha");
		TEMPLATE_CONVERSION_RULE.put("pay_time", "time");
		TEMPLATE_CONVERSION_RULE.put("pay_money", "money");
		TEMPLATE_CONVERSION_RULE.put("order_id", "other_number2");
		TEMPLATE_CONVERSION_RULE.put("pickup_code", "pick_up_code");
		TEMPLATE_CONVERSION_RULE.put("item_name", "others");
		TEMPLATE_CONVERSION_RULE.put("end_time", "time");
		TEMPLATE_CONVERSION_RULE.put("activity_name", "others");
		TEMPLATE_CONVERSION_RULE.put("review_result", "others");
		TEMPLATE_CONVERSION_RULE.put("password", "other_number2");
		TEMPLATE_CONVERSION_RULE.put("phone", "phone_number2");
		TEMPLATE_CONVERSION_RULE.put("mer_name", "others");
		TEMPLATE_CONVERSION_RULE.put("step", "others");
		TEMPLATE_CONVERSION_RULE.put("dealer", "others");
	}

	private final SceneMapper sceneMapper;
	private final ObjectMapper objectMapper;

	public AliyunsmsSmsTemplateContentConverter(SceneMapper sceneMapper, ObjectMapper objectMapper) {
		this.sceneMapper = sceneMapper;
		this.objectMapper = objectMapper;
	}

	public SmsTemplateConversionResult convert(
			int sceneId, String templateContent, String ruleJsonSerializeFailureMessage) {
		Scene scene = sceneMapper.selectById((long) sceneId);
		if (scene == null
				|| scene.getVariables() == null
				|| scene.getVariables().isBlank()) {
			return new SmsTemplateConversionResult(templateContent, "[]");
		}
		List<Map<String, Object>> varList;
		try {
			varList = objectMapper.readValue(
					scene.getVariables(), new TypeReference<List<Map<String, Object>>>() {});
		} catch (Exception e) {
			return new SmsTemplateConversionResult(templateContent, "[]");
		}
		if (varList == null) {
			return new SmsTemplateConversionResult(templateContent, "[]");
		}
		Map<String, Map<String, Object>> replaceParams = new LinkedHashMap<>();
		for (Map<String, Object> row : varList) {
			if (row == null) {
				continue;
			}
			Object vt = row.get("var_title");
			if (vt != null) {
				replaceParams.put(vt.toString(), row);
			}
			Object vn = row.get("var_name");
			if (vn != null) {
				replaceParams.putIfAbsent(vn.toString(), row);
			}
		}
		List<String> captureOrder = new ArrayList<>();
		Matcher m = PLACEHOLDER.matcher(templateContent);
		while (m.find()) {
			captureOrder.add(m.group(1));
		}
		LinkedHashMap<String, String> patternByV = new LinkedHashMap<>();
		LinkedHashMap<String, String> replacementByV = new LinkedHashMap<>();
		LinkedHashMap<String, String> rule = new LinkedHashMap<>();
		for (String v : captureOrder) {
			Map<String, Object> item = replaceParams.get(v);
			if (item == null) {
				continue;
			}
			Object vn = item.get("var_name");
			String varName = vn != null ? vn.toString() : "";
			patternByV.put(v, "\\$\\{" + Pattern.quote(v) + "\\}");
			replacementByV.put(v, "${" + varName + "}");
			Object ruleField = item.get("rule");
			if (ruleField != null && !String.valueOf(ruleField).isEmpty()) {
				rule.put(varName, ruleField.toString());
			} else {
				rule.put(varName, templateConversionRule(varName));
			}
		}
		String content = templateContent;
		for (String v : patternByV.keySet()) {
			Pattern pat = Pattern.compile(patternByV.get(v));
			content = pat.matcher(content).replaceAll(Matcher.quoteReplacement(replacementByV.get(v)));
		}
		String ruleJson;
		try {
			if (rule.isEmpty()) {
				ruleJson = "[]";
			} else {
				ruleJson = objectMapper.writeValueAsString(rule);
			}
		} catch (Exception e) {
			throw new ResourceException(ruleJsonSerializeFailureMessage);
		}
		return new SmsTemplateConversionResult(content, ruleJson);
	}

	private static String templateConversionRule(String varName) {
		return TEMPLATE_CONVERSION_RULE.getOrDefault(varName, "others");
	}
}
