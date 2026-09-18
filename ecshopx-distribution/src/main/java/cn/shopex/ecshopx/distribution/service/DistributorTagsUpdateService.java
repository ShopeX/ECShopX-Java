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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DistributorTagsUpdateService {

	private final DistributorTagsMapper distributorTagsMapper;
	private final DistributorTagsTagMultiLangWriteService distributorTagsTagMultiLangWriteService;

	public DistributorTagsUpdateService(
			DistributorTagsMapper distributorTagsMapper,
			DistributorTagsTagMultiLangWriteService distributorTagsTagMultiLangWriteService) {
		this.distributorTagsMapper = distributorTagsMapper;
		this.distributorTagsTagMultiLangWriteService = distributorTagsTagMultiLangWriteService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateOneByCompanyAndTagId(
			long tagId, long companyId, Map<String, Object> merged, String requestLang) {
		LambdaQueryWrapper<DistributorTags> w =
				new LambdaQueryWrapper<DistributorTags>()
						.eq(DistributorTags::getTagId, tagId)
						.eq(DistributorTags::getCompanyId, companyId);
		DistributorTags entity = distributorTagsMapper.selectOne(w);
		if (entity == null) {
			throw new ResourceException("未查询到更新数据");
		}
		if (merged.containsKey("tag_name")) {
			entity.setTagName(merged.get("tag_name").toString().trim());
		}
		if (merged.containsKey("tag_color")) {
			entity.setTagColor(merged.get("tag_color").toString().trim());
		}
		if (merged.containsKey("font_color")) {
			entity.setFontColor(merged.get("font_color").toString().trim());
		}
		if (merged.containsKey("description") && merged.get("description") != null) {
			entity.setDescription(merged.get("description").toString());
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
		entity.setUpdated(Long.valueOf((int) (System.currentTimeMillis() / 1000L)));
		LambdaUpdateWrapper<DistributorTags> uw =
				new LambdaUpdateWrapper<DistributorTags>()
						.eq(DistributorTags::getTagId, entity.getTagId())
						.eq(DistributorTags::getCompanyId, companyId);
		if (merged.containsKey("tag_name")) {
			uw.set(DistributorTags::getTagName, entity.getTagName());
		}
		if (merged.containsKey("tag_color")) {
			uw.set(DistributorTags::getTagColor, entity.getTagColor());
		}
		if (merged.containsKey("font_color")) {
			uw.set(DistributorTags::getFontColor, entity.getFontColor());
		}
		if (merged.containsKey("description") && merged.get("description") != null) {
			uw.set(DistributorTags::getDescription, entity.getDescription());
		}
		if (merged.containsKey("front_show")) {
			Object v = merged.get("front_show");
			if (v != null && StringUtils.hasText(v.toString().trim())) {
				uw.set(DistributorTags::getFrontShow, entity.getFrontShow());
			}
		}
		uw.set(DistributorTags::getUpdated, entity.getUpdated());
		try {
			distributorTagsMapper.update(null, uw);
		} catch (DataIntegrityViolationException ex) {
			throw new ResourceException("数据保存失败");
		}
		distributorTagsTagMultiLangWriteService.applyAfterUpdate(tagId, companyId, merged, requestLang);
		DistributorTags fresh = distributorTagsMapper.selectOne(w);
		return DistributorTagsRowMapper.toRow(fresh);
	}
}
