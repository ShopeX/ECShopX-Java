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

package cn.shopex.ecshopx.wsugc.service.post;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.ElementJoiner;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.domain.PostBadge;
import cn.shopex.ecshopx.wsugc.mapper.PostBadgeMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PostSetBadgesService {

	private final PostMapper postMapper;
	private final PostBadgeMapper postBadgeMapper;

	public PostSetBadgesService(PostMapper postMapper, PostBadgeMapper postBadgeMapper) {
		this.postMapper = postMapper;
		this.postBadgeMapper = postBadgeMapper;
	}

	public Map<String, Object> setBadges(
			Map<String, Object> input,
			Map<String, Object> operatorJwtOrEmpty,
			@SuppressWarnings("unused") boolean localProfile) {
		if (!input.containsKey("badges")
				|| input.get("badges") == null
				|| isBadgesEmpty(input.get("badges"))) {
			throw new BadRequestException("badges参数不能为空");
		}
		if (!input.containsKey("post_id") || input.get("post_id") == null) {
			throw new BadRequestException("post_id参数不能为空");
		}
		List<Long> postIds = normalizePostIds(input.get("post_id"));
		if (postIds.isEmpty()) {
			throw new BadRequestException("post_id参数不能为空");
		}

		Object rawBadges = input.get("badges");
		String badgesStr = ElementJoiner.joinComma(rawBadges);
		List<?> badgeList = normalizeBadgesToList(rawBadges);
		long companyId = readLong(operatorJwtOrEmpty.get("company_id"), 1L);
		int now = (int) (System.currentTimeMillis() / 1000);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("post_id", input.get("post_id"));
		out.put("badges", input.get("badges"));

		for (Long postId : postIds) {
			postBadgeMapper.delete(
					new LambdaQueryWrapper<PostBadge>().eq(PostBadge::getPostId, postId));
			for (Object vb : badgeList) {
				if (!isBadgeEntryValid(vb)) {
					continue;
				}
				Long badgeIdLong = tryParseBadgeId(vb);
				if (badgeIdLong == null) {
					continue;
				}
				PostBadge row = new PostBadge();
				row.setPostId(postId);
				row.setBadgeId(badgeIdLong);
				row.setCompanyId(companyId);
				row.setCreated(now);
				postBadgeMapper.insert(row);
			}
			Post post = postMapper.selectById(postId);
			if (post == null) {
				throw new ResourceException("未查询到更新数据");
			}
			post.setBadges(badgesStr);
			post.setUpdated(now);
			postMapper.updateById(post);
		}

		out.put("message", "批量设置角标成功");
		return out;
	}

	private static List<?> normalizeBadgesToList(Object v) {
		if (v instanceof Collection<?> c) {
			return new ArrayList<>(c);
		}
		if (v instanceof Object[] a) {
			List<Object> out = new ArrayList<>(a.length);
			Collections.addAll(out, a);
			return out;
		}
		if (v instanceof int[] a) {
			List<Object> out = new ArrayList<>(a.length);
			for (int j : a) {
				out.add(j);
			}
			return out;
		}
		if (v instanceof long[] a) {
			List<Object> out = new ArrayList<>(a.length);
			for (long j : a) {
				out.add(j);
			}
			return out;
		}
		return List.of(v);
	}

	private static Long tryParseBadgeId(Object vb) {
		if (vb instanceof Number n) {
			return n.longValue();
		}
		if (vb instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static List<Long> normalizePostIds(Object v) {
		if (v == null) {
			return List.of();
		}
		if (v instanceof Number n) {
			return List.of(n.longValue());
		}
		if (v instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return List.of();
			}
			try {
				return List.of(Long.parseLong(s.trim()));
			} catch (NumberFormatException e) {
				throw new BadRequestException("post_id 格式无效");
			}
		}
		if (v instanceof Collection<?> c) {
			List<Long> out = new ArrayList<>();
			for (Object o : c) {
				addPostIdElement(out, o);
			}
			return out;
		}
		if (v instanceof Object[] a) {
			List<Long> out = new ArrayList<>();
			for (Object o : a) {
				addPostIdElement(out, o);
			}
			return out;
		}
		if (v instanceof int[] a) {
			List<Long> out = new ArrayList<>();
			for (int j : a) {
				out.add((long) j);
			}
			return out;
		}
		if (v instanceof long[] a) {
			List<Long> out = new ArrayList<>();
			for (long j : a) {
				out.add(j);
			}
			return out;
		}
		throw new BadRequestException("post_id 格式无效");
	}

	private static void addPostIdElement(List<Long> out, Object o) {
		if (o == null) {
			throw new BadRequestException("post_id 格式无效");
		}
		if (o instanceof Number n) {
			out.add(n.longValue());
			return;
		}
		if (o instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException("post_id 格式无效");
			}
			try {
				out.add(Long.parseLong(s.trim()));
			} catch (NumberFormatException e) {
				throw new BadRequestException("post_id 格式无效");
			}
			return;
		}
		throw new BadRequestException("post_id 格式无效");
	}

	private static boolean isBadgesEmpty(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Object[] a) {
			return a.length == 0;
		}
		if (v instanceof int[] a) {
			return a.length == 0;
		}
		if (v instanceof long[] a) {
			return a.length == 0;
		}
		return false;
	}

	private static boolean isBadgeEntryValid(Object vb) {
		if (vb == null) {
			return false;
		}
		if (vb instanceof Boolean b) {
			return b;
		}
		if (vb instanceof Number n) {
			return n.doubleValue() != 0.0d;
		}
		if (vb instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return false;
			}
			return !"0".equals(s.trim());
		}
		if (vb instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (vb instanceof Object[] a) {
			return a.length > 0;
		}
		if (vb instanceof int[] a) {
			return a.length > 0;
		}
		if (vb instanceof long[] a) {
			return a.length > 0;
		}
		return true;
	}

	private static long readLong(Object v, long defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

}
