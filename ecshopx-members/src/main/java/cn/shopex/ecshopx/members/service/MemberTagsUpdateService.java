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

package cn.shopex.ecshopx.members.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MemberTagsUpdateService {

	private final MemberTagsMapper memberTagsMapper;
	private final MemberTagsSnakeFormatter memberTagsSnakeFormatter;

	public MemberTagsUpdateService(
			MemberTagsMapper memberTagsMapper, MemberTagsSnakeFormatter memberTagsSnakeFormatter) {
		this.memberTagsMapper = memberTagsMapper;
		this.memberTagsSnakeFormatter = memberTagsSnakeFormatter;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateTags(long companyId, long distributorId, Map<String, Object> merged) {
		if (!merged.containsKey("tag_id")) {
			throw new BadRequestException("tagId不能为空");
		}
		Object tagIdRaw = merged.get("tag_id");
		if (tagIdRaw == null || !StringUtils.hasText(String.valueOf(tagIdRaw).trim())) {
			throw new BadRequestException("tagId不能为空");
		}
		long tagId;
		try {
			tagId = Long.parseLong(String.valueOf(tagIdRaw).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("tagId格式错误");
		}

		if (!merged.containsKey("tag_name")) {
			throw new BadRequestException("标签名称不能为空");
		}
		Object tn = merged.get("tag_name");
		if (tn == null || !StringUtils.hasText(String.valueOf(tn).trim())) {
			throw new BadRequestException("标签名称不能为空");
		}
		String tagNameTrim = String.valueOf(tn).trim();

		if (!merged.containsKey("tag_color")) {
			throw new BadRequestException("标签颜色");
		}
		Object tc = merged.get("tag_color");
		if (tc == null || !StringUtils.hasText(String.valueOf(tc).trim())) {
			throw new BadRequestException("标签颜色");
		}
		String tagColorTrim = String.valueOf(tc).trim();

		if (!merged.containsKey("font_color")) {
			throw new BadRequestException("标签字体颜色");
		}
		Object fc = merged.get("font_color");
		if (fc == null || !StringUtils.hasText(String.valueOf(fc).trim())) {
			throw new BadRequestException("标签字体颜色");
		}
		String fontColorTrim = String.valueOf(fc).trim();

		LambdaQueryWrapper<MemberTags> qw = new LambdaQueryWrapper<MemberTags>()
				.eq(MemberTags::getTagId, tagId)
				.eq(MemberTags::getCompanyId, companyId)
				.eq(MemberTags::getDistributorId, distributorId);
		MemberTags entity = memberTagsMapper.selectOne(qw);
		if (entity == null) {
			throw new ResourceException("未查询到更新数据");
		}

		entity.setTagName(tagNameTrim);
		entity.setTagColor(tagColorTrim);
		entity.setFontColor(fontColorTrim);

		if (merged.containsKey("description")) {
			entity.setDescription(resolveNullableDescription(merged.get("description")));
		}
		if (merged.containsKey("category_id")) {
			entity.setCategoryId(parseCategoryId(merged.get("category_id")));
		}

		long nowSec = System.currentTimeMillis() / 1000L;
		entity.setUpdated(nowSec);

		try {
			memberTagsMapper.updateById(entity);
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}

		MemberTags refreshed = memberTagsMapper.selectById(tagId);
		if (refreshed == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return memberTagsSnakeFormatter.toSnakeTagRow(refreshed);
	}

	private static String resolveNullableDescription(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = raw.toString().trim();
		return StringUtils.hasText(s) ? s : null;
	}

	private static int parseCategoryId(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static ResourceException mapDataAccess(DataAccessException e) {
		Throwable c = e.getMostSpecificCause();
		String msg = c != null ? c.getMessage() : null;
		return new ResourceException(StringUtils.hasText(msg) ? msg : "更新失败");
	}
}
