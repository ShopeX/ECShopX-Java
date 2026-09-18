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
import cn.shopex.ecshopx.aliyunsms.mapper.SceneMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AliyunsmsSceneDetailService {

	private final SceneMapper sceneMapper;
	private final ObjectMapper objectMapper;

	public AliyunsmsSceneDetailService(SceneMapper sceneMapper, ObjectMapper objectMapper) {
		this.sceneMapper = sceneMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getDetail(Long id) {
		Map<String, Object> data = null;
		if (id != null) {
			Scene row = sceneMapper.selectById(id);
			if (row != null) {
				data = new LinkedHashMap<>();
				data.put("id", row.getId());
				data.put("company_id", row.getCompanyId());
				data.put("scene_name", row.getSceneName());
				data.put("scene_title", row.getSceneTitle());
				data.put("status", row.getStatus());
				data.put("template_type", row.getTemplateType());
				data.put("default_template", row.getDefaultTemplate());
				data.put("variables", row.getVariables());
				data.put("created", row.getCreated());
				data.put("updated", row.getUpdated());
			}
		}
		if (data == null || data.isEmpty()) {
			Map<String, Object> placeholder = new LinkedHashMap<>();
			placeholder.put("default_template", null);
			placeholder.put("variables", null);
			return placeholder;
		}
		Object varObj = data.get("variables");
		if (varObj instanceof String v) {
			if (!v.isEmpty() && !"0".equals(v)) {
				try {
					String t = v.trim();
					if (t.isEmpty()) {
						data.put("variables", null);
					} else {
						Object parsed = objectMapper.readValue(t, Object.class);
						if (parsed == null) {
							data.put("variables", null);
						} else {
							data.put("variables", parsed);
						}
					}
				} catch (Exception e) {
					data.put("variables", null);
				}
			}
		}
		return data;
	}
}
