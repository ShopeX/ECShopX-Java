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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.DistributorTags;
import cn.shopex.ecshopx.distribution.mapper.DistributorTagsMapper;
import cn.shopex.ecshopx.distribution.support.DistributorTagsRowMapper;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorTagsCreateService {

	private final DistributorTagsMapper distributorTagsMapper;
	private final DistributorTagsTagMultiLangWriteService distributorTagsTagMultiLangWriteService;

	public DistributorTagsCreateService(
			DistributorTagsMapper distributorTagsMapper,
			DistributorTagsTagMultiLangWriteService distributorTagsTagMultiLangWriteService) {
		this.distributorTagsMapper = distributorTagsMapper;
		this.distributorTagsTagMultiLangWriteService = distributorTagsTagMultiLangWriteService;
	}

	public Map<String, Object> create(Map<String, Object> merged, long companyId, String requestLang) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		DistributorTags entity = new DistributorTags();
		entity.setCompanyId(companyId);
		entity.setTagName(merged.get("tag_name").toString().trim());
		entity.setTagColor(merged.get("tag_color").toString().trim());
		entity.setFontColor(merged.get("font_color").toString().trim());
		if (merged.containsKey("description")) {
			Object d = merged.get("description");
			entity.setDescription(d == null ? null : d.toString());
		}
		if (merged.containsKey("front_show")) {
			Object v = merged.get("front_show");
			if (v != null && StringUtils.hasText(v.toString().trim())) {
				if (v instanceof Number n) {
					entity.setFrontShow(n.intValue());
				} else {
					entity.setFrontShow(Integer.parseInt(v.toString().trim()));
				}
			}
		}
		entity.setCreated((long) now);
		entity.setUpdated((long) now);
		try {
			distributorTagsMapper.insert(entity);
			distributorTagsTagMultiLangWriteService.applyAfterInsert(
					entity.getTagId(), companyId, merged, requestLang);
		} catch (DataIntegrityViolationException ex) {
			throw new ResourceException("数据保存失败");
		}
		return DistributorTagsRowMapper.toRow(entity);
	}
}
