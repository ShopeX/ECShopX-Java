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

package cn.shopex.ecshopx.aliyunsms.service;

import cn.shopex.ecshopx.aliyunsms.domain.Scene;
import cn.shopex.ecshopx.aliyunsms.domain.Template;
import cn.shopex.ecshopx.aliyunsms.mapper.SceneMapper;
import cn.shopex.ecshopx.aliyunsms.mapper.TemplateMapper;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsAddSmsTemplateJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsTemplateAddService {

	private final SceneMapper sceneMapper;
	private final TemplateMapper templateMapper;
	private final ObjectMapper objectMapper;
	private final AliyunsmsAddSmsTemplateJobDispatchPublisher addSmsTemplateJobDispatchPublisher;

	public AliyunsmsTemplateAddService(
			SceneMapper sceneMapper,
			TemplateMapper templateMapper,
			ObjectMapper objectMapper,
			AliyunsmsAddSmsTemplateJobDispatchPublisher addSmsTemplateJobDispatchPublisher) {
		this.sceneMapper = sceneMapper;
		this.templateMapper = templateMapper;
		this.objectMapper = objectMapper;
		this.addSmsTemplateJobDispatchPublisher = addSmsTemplateJobDispatchPublisher;
	}

	public void addTemplate(
			long companyId,
			String templateName,
			int templateType,
			String remark,
			String templateContent,
			int sceneId,
			String relatedSignName) {
		checkValidTemplateVariables(templateContent, sceneId);
		String templateCode =
				addSmsTemplateJobDispatchPublisher.publish(
						companyId, templateType, templateName, remark, templateContent, sceneId, relatedSignName);
		int now = (int) Instant.now().getEpochSecond();
		Template row = new Template();
		row.setCompanyId(companyId);
		row.setTemplateName(templateName);
		row.setTemplateType(String.valueOf(templateType));
		row.setRemark(remark);
		row.setTemplateContent(templateContent);
		row.setSceneId(sceneId);
		row.setTemplateCode(templateCode);
		row.setRelatedSignName(relatedSignName);
		row.setStatus("0");
		row.setReason("");
		row.setCreated(now);
		row.setUpdated(now);
		templateMapper.insert(row);
	}

	public void checkValidTemplateVariables(String templateContent, int sceneId) {
		Scene row = sceneMapper.selectById((long) sceneId);
		List<String> placeholders = AliyunsmsTemplateVariableSupport.extractPlaceholders(templateContent);
		if (row == null) {
			return;
		}
		if (isPromotionTemplateType(row.getTemplateType())) {
			if (!placeholders.isEmpty()) {
				throw new ResourceException("推广类模板不能包含变量");
			}
			return;
		}
		List<Map<String, Object>> decodedVariables =
				AliyunsmsTemplateVariableSupport.decodeVariables(row.getVariables(), objectMapper);
		if (decodedVariables.isEmpty()) {
			return;
		}
		Set<String> allowedKeys = AliyunsmsTemplateVariableSupport.allowedPlaceholderKeys(decodedVariables);
		if (allowedKeys.isEmpty()) {
			return;
		}
		if (placeholders.size() != new HashSet<>(placeholders).size()) {
			throw new ResourceException("变量不能重复");
		}
		for (String var : placeholders) {
			if (!allowedKeys.contains(var)) {
				throw new ResourceException("${" + var + "} 无效变量");
			}
		}
	}

	private static boolean isPromotionTemplateType(String templateType) {
		if (templateType == null || templateType.isBlank()) {
			return false;
		}
		String t = templateType.trim();
		try {
			return Integer.parseInt(t) == 2;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
