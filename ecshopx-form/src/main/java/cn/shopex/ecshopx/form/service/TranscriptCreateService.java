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

package cn.shopex.ecshopx.form.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.form.domain.TranscriptProperties;
import cn.shopex.ecshopx.form.domain.Transcripts;
import cn.shopex.ecshopx.form.mapper.TranscriptPropertiesMapper;
import cn.shopex.ecshopx.form.mapper.TranscriptsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TranscriptCreateService {

	private final TranscriptsMapper transcriptsMapper;
	private final TranscriptPropertiesMapper transcriptPropertiesMapper;

	public TranscriptCreateService(
			TranscriptsMapper transcriptsMapper,
			TranscriptPropertiesMapper transcriptPropertiesMapper) {
		this.transcriptsMapper = transcriptsMapper;
		this.transcriptPropertiesMapper = transcriptPropertiesMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> create(long companyId, Map<String, Object> params) {
		Transcripts entity = new Transcripts();
		entity.setCompanyId(companyId);
		entity.setTranscriptName(stringParam(params, "transcript_name"));
		entity.setTemplateName(stringParam(params, "template_name"));
		applyTranscriptStatus(entity, params);
		int nowSec = (int) Instant.now().getEpochSecond();
		entity.setCreated(nowSec);
		entity.setUpdated(nowSec);

		transcriptsMapper.insert(entity);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("transcript_id", entity.getTranscriptId());
		result.put("transcript_name", entity.getTranscriptName());
		result.put("company_id", entity.getCompanyId());
		result.put("template_name", entity.getTemplateName());
		result.put("transcript_status", transcriptStatusForResponse(entity.getTranscriptStatus()));
		result.put("created", entity.getCreated());
		result.put("updated", entity.getUpdated());

		List<Map<String, Object>> evaluateItems = new ArrayList<>();
		Object rawItems = params.get("evaluateItems");
		if (isTruthyEvaluateItems(rawItems) && rawItems instanceof List<?> list) {
			for (Object el : list) {
				if (!(el instanceof Map<?, ?> row)) {
					continue;
				}
				if (!row.containsKey("prop_name") || row.get("prop_name") == null) {
					continue;
				}
				String propName = String.valueOf(row.get("prop_name"));
				LambdaQueryWrapper<TranscriptProperties> dup = new LambdaQueryWrapper<>();
				dup.eq(TranscriptProperties::getCompanyId, companyId)
						.eq(TranscriptProperties::getTranscriptId, entity.getTranscriptId())
						.eq(TranscriptProperties::getPropName, propName);
				if (transcriptPropertiesMapper.selectCount(dup) > 0) {
					throw new ResourceException("考评项目名称不能重复！");
				}
				TranscriptProperties prop = new TranscriptProperties();
				prop.setTranscriptId(entity.getTranscriptId());
				prop.setCompanyId(companyId);
				prop.setPropName(propName);
				String propUnit = "";
				if (row.containsKey("prop_unit") && row.get("prop_unit") != null) {
					propUnit = String.valueOf(row.get("prop_unit"));
				}
				prop.setPropUnit(propUnit);
				transcriptPropertiesMapper.insert(prop);
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("prop_id", prop.getPropId());
				item.put("transcript_id", prop.getTranscriptId());
				item.put("company_id", prop.getCompanyId());
				item.put("prop_name", prop.getPropName());
				item.put("prop_unit", propUnit);
				evaluateItems.add(item);
			}
		}
		result.put("evaluate_items", evaluateItems);
		return result;
	}

	private static String stringParam(Map<String, Object> params, String key) {
		Object v = params.get(key);
		return v != null ? String.valueOf(v) : null;
	}

	private static Object transcriptStatusForResponse(String stored) {
		if (stored == null || stored.isEmpty()) {
			return Boolean.FALSE;
		}
		if ("on".equals(stored)) {
			return Boolean.TRUE;
		}
		return stored;
	}

	private static void applyTranscriptStatus(Transcripts entity, Map<String, Object> params) {
		boolean isset = params.containsKey("transcript_status") && params.get("transcript_status") != null;
		if (!isset) {
			entity.setTranscriptStatus("");
			return;
		}
		Object v = params.get("transcript_status");
		if (v instanceof Boolean b) {
			entity.setTranscriptStatus(b ? "on" : "");
			return;
		}
		if (v instanceof Number n) {
			entity.setTranscriptStatus(n.intValue() != 0 ? "on" : "");
			return;
		}
		entity.setTranscriptStatus(String.valueOf(v));
	}

	private static boolean isTruthyEvaluateItems(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Map<?, ?>) {
			return false;
		}
		if (v instanceof List<?> list) {
			return !list.isEmpty();
		}
		return false;
	}
}
