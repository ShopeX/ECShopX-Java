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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.domain.TagsCategory;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.TagsCategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TagsCategoryCreateService {

	private final TagsCategoryMapper tagsCategoryMapper;
	private final MemberTagsMapper memberTagsMapper;

	public TagsCategoryCreateService(
			TagsCategoryMapper tagsCategoryMapper, MemberTagsMapper memberTagsMapper) {
		this.tagsCategoryMapper = tagsCategoryMapper;
		this.memberTagsMapper = memberTagsMapper;
	}

	public Map<String, Object> createTagsCategory(
			long companyId, String categoryName, Long sortOrNull, List<Long> relTagIds) {
		boolean shouldRelink = relTagIds != null && !relTagIds.isEmpty();
		int now = (int) (System.currentTimeMillis() / 1000L);
		TagsCategory entity = new TagsCategory();
		entity.setCategoryName(categoryName);
		entity.setCompanyId(companyId);
		entity.setCreated(now);
		entity.setUpdated(now);
		if (sortOrNull != null) {
			entity.setSort(sortOrNull);
		}
		tagsCategoryMapper.insert(entity);
		if (entity.getCategoryId() == null) {
			throw new ResourceException("创建失败");
		}
		if (shouldRelink) {
			Long newCatId = entity.getCategoryId();
			int catInt;
			try {
				catInt = Math.toIntExact(newCatId);
			} catch (ArithmeticException e) {
				throw new ResourceException("创建失败");
			}
			LambdaUpdateWrapper<MemberTags> uw = new LambdaUpdateWrapper<MemberTags>()
					.in(MemberTags::getTagId, relTagIds)
					.eq(MemberTags::getCompanyId, companyId)
					.set(MemberTags::getCategoryId, catInt);
			try {
				memberTagsMapper.update(null, uw);
			} catch (DataAccessException e) {
				Throwable c = e.getMostSpecificCause();
				String msg = c != null ? c.getMessage() : null;
				throw new ResourceException(StringUtils.hasText(msg) ? msg : "创建失败");
			}
		}
		return toSnakeRow(entity);
	}

	public Map<String, Object> updateTagsCategory(
			long companyId,
			long categoryId,
			String categoryName,
			boolean sortColumnRequested,
			Long sortParsedOrNull,
			boolean relTagIdsExplicit,
			List<Long> relTagIdsNormalized) {
		LambdaQueryWrapper<TagsCategory> q = new LambdaQueryWrapper<>();
		q.eq(TagsCategory::getCompanyId, companyId).eq(TagsCategory::getCategoryId, categoryId);
		TagsCategory existing = tagsCategoryMapper.selectOne(q);
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<TagsCategory> catUw = new LambdaUpdateWrapper<>();
		catUw.eq(TagsCategory::getCompanyId, companyId)
				.eq(TagsCategory::getCategoryId, categoryId)
				.set(TagsCategory::getCategoryName, categoryName)
				.set(TagsCategory::getUpdated, now);
		if (sortColumnRequested && sortParsedOrNull != null) {
			catUw.set(TagsCategory::getSort, sortParsedOrNull);
		}
		tagsCategoryMapper.update(null, catUw);
		if (relTagIdsExplicit
				&& relTagIdsNormalized != null
				&& !relTagIdsNormalized.isEmpty()) {
			int targetCatInt;
			try {
				targetCatInt = Math.toIntExact(categoryId);
			} catch (ArithmeticException e) {
				throw new ResourceException("未查询到更新数据");
			}
			LambdaUpdateWrapper<MemberTags> uw = new LambdaUpdateWrapper<MemberTags>()
					.in(MemberTags::getTagId, relTagIdsNormalized)
					.eq(MemberTags::getCompanyId, companyId)
					.set(MemberTags::getCategoryId, targetCatInt);
			try {
				memberTagsMapper.update(null, uw);
			} catch (DataAccessException e) {
				Throwable c = e.getMostSpecificCause();
				String msg = c != null ? c.getMessage() : null;
				throw new ResourceException(StringUtils.hasText(msg) ? msg : "未查询到更新数据");
			}
		}
		LambdaQueryWrapper<TagsCategory> q2 = new LambdaQueryWrapper<>();
		q2.eq(TagsCategory::getCompanyId, companyId).eq(TagsCategory::getCategoryId, categoryId);
		TagsCategory refreshed = tagsCategoryMapper.selectOne(q2);
		if (refreshed == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toSnakeRow(refreshed);
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteTagsCategory(long companyId, long categoryId) {
		LambdaQueryWrapper<TagsCategory> w =
				new LambdaQueryWrapper<TagsCategory>()
						.eq(TagsCategory::getCompanyId, companyId)
						.eq(TagsCategory::getCategoryId, categoryId);
		tagsCategoryMapper.delete(w);

		int categoryIdInt;
		try {
			categoryIdInt = Math.toIntExact(categoryId);
		} catch (ArithmeticException e) {
			throw new ResourceException("未查询到更新数据");
		}
		LambdaQueryWrapper<MemberTags> mq =
				new LambdaQueryWrapper<MemberTags>()
						.eq(MemberTags::getCompanyId, companyId)
						.eq(MemberTags::getCategoryId, categoryIdInt);
		Long cnt = memberTagsMapper.selectCount(mq);
		if (cnt != null && cnt > 0) {
			throw new ResourceException("删除失败,该分类下已有标签");
		}
	}

	public List<Long> normalizeRelTagIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		LinkedHashSet<Long> out = new LinkedHashSet<>();
		if (raw instanceof Collection<?> coll) {
			for (Object o : coll) {
				appendRelTagId(out, o);
			}
			return new ArrayList<>(out);
		}
		if (raw instanceof Object[] arr) {
			for (Object o : arr) {
				appendRelTagId(out, o);
			}
			return new ArrayList<>(out);
		}
		if (raw instanceof long[] arr) {
			for (long v : arr) {
				if (v > 0) {
					out.add(v);
				}
			}
			return new ArrayList<>(out);
		}
		if (raw instanceof int[] arr) {
			for (int v : arr) {
				if (v > 0) {
					out.add((long) v);
				}
			}
			return new ArrayList<>(out);
		}
		if (raw instanceof Number n) {
			appendRelTagId(out, n);
			return new ArrayList<>(out);
		}
		if (raw instanceof String str) {
			appendRelTagId(out, str);
			return new ArrayList<>(out);
		}
		return List.of();
	}

	private static void appendRelTagId(LinkedHashSet<Long> out, Object o) {
		if (o == null) {
			return;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			if (v > 0) {
				out.add(v);
			}
			return;
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		try {
			long v = Long.parseLong(s);
			if (v > 0) {
				out.add(v);
			}
		} catch (NumberFormatException e) {
			// ignore invalid element
		}
	}

	private static Map<String, Object> toSnakeRow(TagsCategory e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("category_id", String.valueOf(e.getCategoryId()));
		m.put("category_name", e.getCategoryName());
		m.put("sort", e.getSort() == null ? Integer.valueOf(1) : e.getSort().intValue());
		m.put("company_id", String.valueOf(e.getCompanyId()));
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}
}
