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
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TranscriptDeleteService {

	private final TranscriptsMapper transcriptsMapper;
	private final TranscriptPropertiesMapper transcriptPropertiesMapper;

	public TranscriptDeleteService(
			TranscriptsMapper transcriptsMapper,
			TranscriptPropertiesMapper transcriptPropertiesMapper) {
		this.transcriptsMapper = transcriptsMapper;
		this.transcriptPropertiesMapper = transcriptPropertiesMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> delete(long companyId, long transcriptId) {
		LambdaQueryWrapper<Transcripts> mainWrapper = new LambdaQueryWrapper<>();
		mainWrapper.eq(Transcripts::getCompanyId, companyId).eq(Transcripts::getTranscriptId, transcriptId);
		Transcripts existing = transcriptsMapper.selectOne(mainWrapper);
		if (existing == null) {
			throw new ResourceException("transcriptId为" + transcriptId + "的成绩单不存在");
		}

		LambdaQueryWrapper<TranscriptProperties> childWrapper = new LambdaQueryWrapper<>();
		childWrapper.eq(TranscriptProperties::getCompanyId, companyId)
				.eq(TranscriptProperties::getTranscriptId, transcriptId);
		int childDeleted = transcriptPropertiesMapper.delete(childWrapper);

		transcriptsMapper.delete(mainWrapper);

		boolean status = childDeleted > 0;
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", status);
		return result;
	}
}
