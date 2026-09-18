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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TranscriptGetService {

	private final TranscriptsMapper transcriptsMapper;
	private final TranscriptPropertiesMapper transcriptPropertiesMapper;

	public TranscriptGetService(
			TranscriptsMapper transcriptsMapper,
			TranscriptPropertiesMapper transcriptPropertiesMapper) {
		this.transcriptsMapper = transcriptsMapper;
		this.transcriptPropertiesMapper = transcriptPropertiesMapper;
	}

	public Map<String, Object> getInfo(long companyId, long transcriptId) {
		LambdaQueryWrapper<Transcripts> main = new LambdaQueryWrapper<>();
		main.eq(Transcripts::getCompanyId, companyId).eq(Transcripts::getTranscriptId, transcriptId);
		Transcripts entity = transcriptsMapper.selectOne(main);
		if (entity == null) {
			throw new ResourceException("transcriptId为" + transcriptId + "的成绩单不存在");
		}

		LambdaQueryWrapper<TranscriptProperties> props = new LambdaQueryWrapper<>();
		props.eq(TranscriptProperties::getCompanyId, companyId)
				.eq(TranscriptProperties::getTranscriptId, transcriptId);
		List<TranscriptProperties> propRows = transcriptPropertiesMapper.selectList(props);
		List<Map<String, Object>> indicators = new ArrayList<>();
		for (TranscriptProperties prop : propRows) {
			indicators.add(toPropRowMap(prop));
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("transcript_id", entity.getTranscriptId());
		result.put("transcript_name", entity.getTranscriptName());
		result.put("company_id", entity.getCompanyId());
		result.put("template_name", entity.getTemplateName());
		result.put("transcript_status", entity.getTranscriptStatus());
		result.put("created", entity.getCreated());
		result.put("updated", entity.getUpdated());
		result.put("indicators", indicators);
		return result;
	}

	private static Map<String, Object> toPropRowMap(TranscriptProperties prop) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("prop_id", prop.getPropId());
		item.put("transcript_id", prop.getTranscriptId());
		item.put("company_id", prop.getCompanyId());
		item.put("prop_name", prop.getPropName());
		item.put("prop_unit", prop.getPropUnit() != null ? prop.getPropUnit() : "");
		return item;
	}
}
