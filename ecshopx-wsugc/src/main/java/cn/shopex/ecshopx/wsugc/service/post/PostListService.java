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
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.domain.PostBadge;
import cn.shopex.ecshopx.wsugc.domain.PostTopic;
import cn.shopex.ecshopx.wsugc.mapper.PostBadgeMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostTopicMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PostListService {

	private static final Set<String> SORT_FIELD_WHITELIST =
			Set.of(
					"p_order",
					"created",
					"updated",
					"mobile",
					"post_id",
					"user_id",
					"status",
					"source",
					"likes",
					"is_top",
					"company_id",
					"enabled",
					"disabled",
					"is_draft",
					"operator_id",
					"view_auth",
					"share_nums",
					"title",
					"cover",
					"ai_verify_time",
					"manual_verify_time");

	private final PostMapper postMapper;
	private final PostTopicMapper postTopicMapper;
	private final PostBadgeMapper postBadgeMapper;
	private final PostOutsideLangReadService postOutsideLangReadService;
	private final UgcPostUserIdResolveService ugcPostUserIdResolveService;
	private final PostAdminRowFormatService postAdminRowFormatService;

	public PostListService(
			PostMapper postMapper,
			PostTopicMapper postTopicMapper,
			PostBadgeMapper postBadgeMapper,
			PostOutsideLangReadService postOutsideLangReadService,
			UgcPostUserIdResolveService ugcPostUserIdResolveService,
			PostAdminRowFormatService postAdminRowFormatService) {
		this.postMapper = postMapper;
		this.postTopicMapper = postTopicMapper;
		this.postBadgeMapper = postBadgeMapper;
		this.postOutsideLangReadService = postOutsideLangReadService;
		this.ugcPostUserIdResolveService = ugcPostUserIdResolveService;
		this.postAdminRowFormatService = postAdminRowFormatService;
	}

	public Object buildList(
			long companyId,
			String requestLangTag,
			int page,
			int pageSize,
			String sourceRaw,
			String statusRaw,
			List<Long> topicIds,
			List<Long> badgeIds,
			String nickname,
			String mobile,
			String content,
			String sort) {
		LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<>();
		w.eq(Post::getCompanyId, companyId);

		if (sourceRaw != null && StringUtils.hasText(sourceRaw.trim())) {
			try {
				int s = Integer.parseInt(sourceRaw.trim());
				w.eq(Post::getSource, s);
			} catch (NumberFormatException ignored) {
			}
		}

		if (statusRaw != null && StringUtils.hasText(statusRaw.trim())) {
			try {
				int st = Integer.parseInt(statusRaw.trim());
				w.eq(Post::getStatus, st);
			} catch (NumberFormatException ignored) {
			}
		}

		List<Long> postIdConstraint = null;
		if (topicIds != null && !topicIds.isEmpty()) {
			postIdConstraint = resolvePostIdsByTopics(companyId, topicIds);
		}
		if (badgeIds != null && !badgeIds.isEmpty()) {
			postIdConstraint = resolvePostIdsByBadges(companyId, badgeIds);
		}
		if (postIdConstraint != null) {
			w.in(Post::getPostId, postIdConstraint);
		}

		if (StringUtils.hasText(mobile)) {
			w.in(Post::getUserId, ugcPostUserIdResolveService.userIdsByMobile(mobile.trim()));
		} else if (StringUtils.hasText(nickname)) {
			w.in(Post::getUserId, ugcPostUserIdResolveService.userIdsByNicknameContains(nickname.trim()));
		}

		if (StringUtils.hasText(content)) {
			String trimmed = content.trim();
			List<Long> langIds =
					postOutsideLangReadService.findDataIdsByFieldContains(
							companyId, requestLangTag, "content", trimmed);
			if (!langIds.isEmpty()) {
				w.in(Post::getPostId, langIds);
			} else {
				w.like(Post::getContent, "%" + escapeLike(trimmed) + "%");
			}
		}

		w.eq(Post::getIsDraft, 0).eq(Post::getDisabled, 0);

		applyPostListSort(w, sort);

		w.select(
				Post::getPostId,
				Post::getUserId,
				Post::getCompanyId,
				Post::getTitle,
				Post::getCover,
				Post::getStatus,
				Post::getCreated,
				Post::getBadges,
				Post::getTopics,
				Post::getPOrder,
				Post::getImages,
				Post::getImagePath,
				Post::getImageTag,
				Post::getLikes,
				Post::getSource,
				Post::getIsTop,
				Post::getMobile,
				Post::getAiVerifyTime,
				Post::getManualVerifyTime,
				Post::getShareNums,
				Post::getManualRefuseReason,
				Post::getAiRefuseReason);

		Page<Post> p = new Page<>(page, pageSize);
		postMapper.selectPage(p, w);

		if (p.getRecords() == null || p.getRecords().isEmpty()) {
			return Collections.emptyList();
		}

		List<LinkedHashMap<String, Object>> list = new ArrayList<>();
		for (Post entity : p.getRecords()) {
			LinkedHashMap<String, Object> row =
					new LinkedHashMap<>(PostCreateService.postToSnakeMap(entity));
			postOutsideLangReadService.applyToRowMap(companyId, requestLangTag, row);
			postAdminRowFormatService.formatAdminRow(
					row, entity, companyId, requestLangTag, PostAdminRowFormatService.Mode.POST_LIST);
			list.add(ksortCopy(row));
		}

		TreeMap<String, Object> body = new TreeMap<>();
		body.put("list", list);
		body.put("total_count", p.getTotal());
		return body;
	}

	private List<Long> resolvePostIdsByTopics(long companyId, List<Long> topicIds) {
		List<String> topicIdStrings = new ArrayList<>(topicIds.size());
		for (Long id : topicIds) {
			topicIdStrings.add(String.valueOf(id));
		}
		List<PostTopic> rows =
				postTopicMapper.selectList(
						new LambdaQueryWrapper<PostTopic>()
								.eq(PostTopic::getCompanyId, companyId)
								.in(PostTopic::getTopicId, topicIdStrings));
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		for (PostTopic pt : rows) {
			if (pt.getPostId() != null) {
				ids.add(pt.getPostId());
			}
		}
		return ids.isEmpty() ? List.of(-1L) : new ArrayList<>(ids);
	}

	private List<Long> resolvePostIdsByBadges(long companyId, List<Long> badgeIds) {
		List<PostBadge> rows =
				postBadgeMapper.selectList(
						new LambdaQueryWrapper<PostBadge>()
								.eq(PostBadge::getCompanyId, companyId)
								.in(PostBadge::getBadgeId, badgeIds));
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		for (PostBadge pb : rows) {
			if (pb.getPostId() != null) {
				ids.add(pb.getPostId());
			}
		}
		return ids.isEmpty() ? List.of(-1L) : new ArrayList<>(ids);
	}

	private static String escapeLike(String s) {
		return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static LinkedHashMap<String, Object> ksortCopy(Map<String, Object> src) {
		return new LinkedHashMap<>(new TreeMap<>(src));
	}

	private static void applyPostListSort(LambdaQueryWrapper<Post> w, String sort) {
		if (sort == null || sort.trim().isEmpty()) {
			w.orderByAsc(Post::getPOrder).orderByDesc(Post::getCreated).orderByAsc(Post::getMobile);
			return;
		}
		String trimmed = sort.trim();
		int sp = trimmed.indexOf(' ');
		if (sp <= 0 || sp >= trimmed.length() - 1) {
			throw new BadRequestException("sort 参数格式无效");
		}
		String fieldRaw = trimmed.substring(0, sp).trim();
		String direction = trimmed.substring(sp + 1).trim();
		if (!StringUtils.hasText(fieldRaw) || !StringUtils.hasText(direction)) {
			throw new BadRequestException("sort 参数格式无效");
		}
		String field = fieldRaw.toLowerCase(Locale.ROOT);
		String dirLower = direction.toLowerCase(Locale.ROOT);
		boolean userAsc;
		if ("asc".equals(dirLower)) {
			userAsc = true;
		} else if ("desc".equals(dirLower)) {
			userAsc = false;
		} else {
			throw new BadRequestException("sort 参数格式无效");
		}
		if (!SORT_FIELD_WHITELIST.contains(field)) {
			throw new BadRequestException("sort 参数格式无效");
		}

		LinkedHashMap<String, Boolean> tmp = new LinkedHashMap<>();
		tmp.put("p_order", true);
		tmp.put(field, userAsc);
		if (!tmp.containsKey("created")) {
			tmp.put("created", false);
		}
		tmp.put("mobile", true);

		for (Map.Entry<String, Boolean> e : tmp.entrySet()) {
			applyPostOrderByField(w, e.getKey(), e.getValue());
		}
	}

	private static void applyPostOrderByField(LambdaQueryWrapper<Post> w, String field, boolean asc) {
		switch (field) {
			case "p_order" -> {
				if (asc) {
					w.orderByAsc(Post::getPOrder);
				} else {
					w.orderByDesc(Post::getPOrder);
				}
			}
			case "created" -> {
				if (asc) {
					w.orderByAsc(Post::getCreated);
				} else {
					w.orderByDesc(Post::getCreated);
				}
			}
			case "updated" -> {
				if (asc) {
					w.orderByAsc(Post::getUpdated);
				} else {
					w.orderByDesc(Post::getUpdated);
				}
			}
			case "mobile" -> {
				if (asc) {
					w.orderByAsc(Post::getMobile);
				} else {
					w.orderByDesc(Post::getMobile);
				}
			}
			case "post_id" -> {
				if (asc) {
					w.orderByAsc(Post::getPostId);
				} else {
					w.orderByDesc(Post::getPostId);
				}
			}
			case "user_id" -> {
				if (asc) {
					w.orderByAsc(Post::getUserId);
				} else {
					w.orderByDesc(Post::getUserId);
				}
			}
			case "status" -> {
				if (asc) {
					w.orderByAsc(Post::getStatus);
				} else {
					w.orderByDesc(Post::getStatus);
				}
			}
			case "source" -> {
				if (asc) {
					w.orderByAsc(Post::getSource);
				} else {
					w.orderByDesc(Post::getSource);
				}
			}
			case "likes" -> {
				if (asc) {
					w.orderByAsc(Post::getLikes);
				} else {
					w.orderByDesc(Post::getLikes);
				}
			}
			case "is_top" -> {
				if (asc) {
					w.orderByAsc(Post::getIsTop);
				} else {
					w.orderByDesc(Post::getIsTop);
				}
			}
			case "company_id" -> {
				if (asc) {
					w.orderByAsc(Post::getCompanyId);
				} else {
					w.orderByDesc(Post::getCompanyId);
				}
			}
			case "enabled" -> {
				if (asc) {
					w.orderByAsc(Post::getEnabled);
				} else {
					w.orderByDesc(Post::getEnabled);
				}
			}
			case "disabled" -> {
				if (asc) {
					w.orderByAsc(Post::getDisabled);
				} else {
					w.orderByDesc(Post::getDisabled);
				}
			}
			case "is_draft" -> {
				if (asc) {
					w.orderByAsc(Post::getIsDraft);
				} else {
					w.orderByDesc(Post::getIsDraft);
				}
			}
			case "operator_id" -> {
				if (asc) {
					w.orderByAsc(Post::getOperatorId);
				} else {
					w.orderByDesc(Post::getOperatorId);
				}
			}
			case "view_auth" -> {
				if (asc) {
					w.orderByAsc(Post::getViewAuth);
				} else {
					w.orderByDesc(Post::getViewAuth);
				}
			}
			case "share_nums" -> {
				if (asc) {
					w.orderByAsc(Post::getShareNums);
				} else {
					w.orderByDesc(Post::getShareNums);
				}
			}
			case "title" -> {
				if (asc) {
					w.orderByAsc(Post::getTitle);
				} else {
					w.orderByDesc(Post::getTitle);
				}
			}
			case "cover" -> {
				if (asc) {
					w.orderByAsc(Post::getCover);
				} else {
					w.orderByDesc(Post::getCover);
				}
			}
			case "ai_verify_time" -> {
				if (asc) {
					w.orderByAsc(Post::getAiVerifyTime);
				} else {
					w.orderByDesc(Post::getAiVerifyTime);
				}
			}
			case "manual_verify_time" -> {
				if (asc) {
					w.orderByAsc(Post::getManualVerifyTime);
				} else {
					w.orderByDesc(Post::getManualVerifyTime);
				}
			}
			default -> throw new BadRequestException("sort 参数格式无效");
		}
	}
}
