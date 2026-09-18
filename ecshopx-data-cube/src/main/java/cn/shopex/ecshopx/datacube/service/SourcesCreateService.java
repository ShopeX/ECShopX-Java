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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SourcesCreateService {

	private final SourcesMapper sourcesMapper;
	private final ObjectMapper objectMapper;

	public SourcesCreateService(SourcesMapper sourcesMapper, ObjectMapper objectMapper) {
		this.sourcesMapper = sourcesMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> addSources(long companyId, String sourceName, Object tagsIdRaw) {
		Sources entity = new Sources();
		entity.setCompanyId(companyId);
		entity.setSourceName(sourceName);
		applyTagsIdColumn(entity, tagsIdRaw);

		int now = (int) (System.currentTimeMillis() / 1000);
		entity.setCreated(now);
		entity.setUpdated(now);

		sourcesMapper.insert(entity);

		Map<String, Object> row = new LinkedHashMap<>();
		Long sid = entity.getSourceId();
		row.put("source_id", sid != null && sid <= Integer.MAX_VALUE ? sid.intValue() : sid);
		row.put("source_name", entity.getSourceName());
		Long cid = entity.getCompanyId();
		row.put("company_id", cid != null && cid <= Integer.MAX_VALUE ? cid.intValue() : cid);
		row.put("tags_id", decodeTagsIdForResponse(entity.getTagsId()));
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		return row;
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
