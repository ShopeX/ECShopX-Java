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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.datacube.domain.Sources;
import cn.shopex.ecshopx.datacube.mapper.SourcesMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SourcesUpdateService {

	private final SourcesMapper sourcesMapper;
	private final ObjectMapper objectMapper;

	public SourcesUpdateService(SourcesMapper sourcesMapper, ObjectMapper objectMapper) {
		this.sourcesMapper = sourcesMapper;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateSources(long companyId, long sourceId, String sourceName, Object tagsIdRaw) {
		Sources row = sourcesMapper.selectById(sourceId);
		if (row == null) {
			throw new ResourceException("source_id=" + sourceId + "的来源不存在");
		}
		if (!Objects.equals(companyId, row.getCompanyId())) {
			throw new ResourceException("请确认您的门店信息后再提交.");
		}
		row.setSourceName(sourceName);
		row.setCompanyId(companyId);
		applyTagsIdColumn(row, tagsIdRaw);
		row.setUpdated((int) (System.currentTimeMillis() / 1000));
		int n = sourcesMapper.updateById(row);
		if (n == 0) {
			throw new ResourceException("source_id=" + sourceId + "的来源不存在");
		}
		Map<String, Object> out = new LinkedHashMap<>();
		Long sid = row.getSourceId();
		out.put("source_id", sid != null && sid <= Integer.MAX_VALUE ? sid.intValue() : sid);
		out.put("source_name", row.getSourceName());
		Long cid = row.getCompanyId();
		out.put("company_id", cid != null && cid <= Integer.MAX_VALUE ? cid.intValue() : cid);
		out.put("tags_id", decodeTagsIdForResponse(row.getTagsId()));
		out.put("created", row.getCreated());
		out.put("updated", row.getUpdated());
		return out;
	}

	private void applyTagsIdColumn(Sources entity, Object tagsIdRaw) {
		if (tagsIdRaw == null) {
			entity.setTagsId("");
			return;
		}
		if (tagsIdRaw instanceof Collection<?>) {
			try {
				entity.setTagsId(objectMapper.writeValueAsString(tagsIdRaw));
			} catch (JsonProcessingException e) {
				throw new ResourceException("tags_id 格式无效");
			}
			return;
		}
		if (tagsIdRaw.getClass().isArray()) {
			try {
				entity.setTagsId(objectMapper.writeValueAsString(tagsIdRaw));
			} catch (JsonProcessingException e) {
				throw new ResourceException("tags_id 格式无效");
			}
			return;
		}
		entity.setTagsId(String.valueOf(tagsIdRaw));
	}

	private Object decodeTagsIdForResponse(String stored) {
		if (stored == null) {
			return null;
		}
		String t = stored.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			return objectMapper.readValue(t, Object.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}
}
