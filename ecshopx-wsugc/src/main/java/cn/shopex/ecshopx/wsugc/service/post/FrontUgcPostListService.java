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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.domain.PostFavorite;
import cn.shopex.ecshopx.wsugc.domain.PostLike;
import cn.shopex.ecshopx.wsugc.domain.PostTopic;
import cn.shopex.ecshopx.wsugc.domain.Topic;
import cn.shopex.ecshopx.wsugc.mapper.PostFavoriteMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostLikeMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostTopicMapper;
import cn.shopex.ecshopx.wsugc.mapper.TopicMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrontUgcPostListService {

	private final PostMapper postMapper;
	private final PostTopicMapper postTopicMapper;
	private final TopicMapper topicMapper;
	private final PostLikeMapper postLikeMapper;
	private final PostFavoriteMapper postFavoriteMapper;
	private final PostOutsideLangReadService postOutsideLangReadService;
	private final FrontUgcPostDetailEmbedService frontUgcPostDetailEmbedService;

	public FrontUgcPostListService(
			PostMapper postMapper,
			PostTopicMapper postTopicMapper,
			TopicMapper topicMapper,
			PostLikeMapper postLikeMapper,
			PostFavoriteMapper postFavoriteMapper,
			PostOutsideLangReadService postOutsideLangReadService,
			FrontUgcPostDetailEmbedService frontUgcPostDetailEmbedService) {
		this.postMapper = postMapper;
		this.postTopicMapper = postTopicMapper;
		this.topicMapper = topicMapper;
		this.postLikeMapper = postLikeMapper;
		this.postFavoriteMapper = postFavoriteMapper;
		this.postOutsideLangReadService = postOutsideLangReadService;
		this.frontUgcPostDetailEmbedService = frontUgcPostDetailEmbedService;
	}

	public Optional<Map<String, Object>> buildH5List(
			long companyId,
			long visitorUserId,
			String langTag,
			int page,
			int pageSize,
			String source,
			String content,
			List<Long> topicIds,
			String profileUserIdRaw,
			String isDraftRaw,
			String searchType,
			String sort) {
		LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<>();
		w.eq(Post::getCompanyId, companyId).eq(Post::getDisabled, 0);

		applySourceFilter(w, source);

		if (StringUtils.hasText(content)) {
			String trimmedContent = content.trim();
			List<Long> keywordTopicsPostIds = List.of(-1L);
			List<Topic> relatedTopics =
					topicMapper.selectList(
							new LambdaQueryWrapper<Topic>().eq(Topic::getTopicName, trimmedContent));
			if (relatedTopics != null && !relatedTopics.isEmpty()) {
				List<String> topicIdStrs = new ArrayList<>();
				for (Topic t : relatedTopics) {
					if (t.getTopicId() != null) {
						topicIdStrs.add(String.valueOf(t.getTopicId()));
					}
				}
				if (!topicIdStrs.isEmpty()) {
					List<PostTopic> ptRows =
							postTopicMapper.selectList(
									new LambdaQueryWrapper<PostTopic>()
											.in(PostTopic::getTopicId, topicIdStrs)
							);
					LinkedHashSet<Long> pid = new LinkedHashSet<>();
					for (PostTopic pt : ptRows) {
						if (pt.getPostId() != null) {
							pid.add(pt.getPostId());
						}
					}
					if (!pid.isEmpty()) {
						keywordTopicsPostIds = new ArrayList<>(pid);
					}
				}
			}
			applyContentContainsOrGroup(w, trimmedContent, keywordTopicsPostIds);
		}

		List<Long> topicPostIds = null;
		if (topicIds != null && !topicIds.isEmpty()) {
			topicPostIds = resolvePostIdsByTopics(companyId, topicIds);
		}

		long profileUserId = 0L;
		if (StringUtils.hasText(profileUserIdRaw)) {
			try {
				profileUserId = Long.parseLong(profileUserIdRaw.trim());
			} catch (NumberFormatException ignored) {
				profileUserId = 0L;
			}
		}

		boolean searchOverridesAuthor =
				profileUserId > 0L
						&& StringUtils.hasText(searchType)
						&& isLikeOrFavoriteSearchType(searchType);

		if (profileUserId > 0L) {
			if (!searchOverridesAuthor) {
				w.eq(Post::getUserId, profileUserId);
				applyAuthorVisibilityFilters(w, visitorUserId, profileUserId, isDraftRaw);
			} else {
				// 移除 user_id 过滤，仅按收藏/点赞的 post_id 筛选（不限作者）
				applyAuthorVisibilityFilters(w, visitorUserId, profileUserId, isDraftRaw);
				applyLikeOrFavoritePostIdFilter(w, profileUserId, searchType);
			}
		} else {
			w.eq(Post::getEnabled, 1).eq(Post::getIsDraft, 0).eq(Post::getStatus, 1);
		}

		if (!searchOverridesAuthor && topicPostIds != null) {
			w.in(Post::getPostId, topicPostIds);
		}

		applyDefaultAndSortOrder(w, sort);

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
				Post::getLikes,
				Post::getVideo,
				Post::getSource);

		Page<Post> p = new Page<>(page, pageSize);
		postMapper.selectPage(p, w);

		if (p.getRecords() == null || p.getRecords().isEmpty()) {
			return Optional.empty();
		}

		List<Map<String, Object>> list = new ArrayList<>();
		for (Post post : p.getRecords()) {
			LinkedHashMap<String, Object> m =
					new LinkedHashMap<>(PostCreateService.postToSnakeMap(post));
			postOutsideLangReadService.applyToRowMap(companyId, langTag, m);
			m.put("user_id_auth", visitorUserId);
			frontUgcPostDetailEmbedService.applyFrontPostListRowFormat(
					m, post, companyId, langTag, visitorUserId);
			list.add(new LinkedHashMap<>(new TreeMap<>(m)));
		}

		TreeMap<String, Object> body = new TreeMap<>();
		body.put("list", list);
		body.put("total_count", p.getTotal());
		return Optional.of(body);
	}

	private static void applyAuthorVisibilityFilters(
			LambdaQueryWrapper<Post> w,
			long visitorUserId,
			long profileUserId,
			String isDraftRaw) {
		if (visitorUserId == profileUserId && StringUtils.hasText(isDraftRaw)) {
			try {
				w.eq(Post::getIsDraft, Integer.parseInt(isDraftRaw.trim()));
			} catch (NumberFormatException ignored) {
			}
		} else {
			w.eq(Post::getStatus, 1).eq(Post::getEnabled, 1).eq(Post::getIsDraft, 0);
		}
	}

	private static boolean isLikeOrFavoriteSearchType(String searchType) {
		String st = searchType.trim();
		return "like".equals(st) || "favorite".equals(st);
	}

	private static String escapeLike(String s) {
		return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private void applySourceFilter(LambdaQueryWrapper<Post> w, String source) {
		if (!StringUtils.hasText(source)) {
			return;
		}
		String t = source.trim();
		try {
			w.eq(Post::getSource, Integer.parseInt(t));
		} catch (NumberFormatException e) {
			w.apply("source = {0}", t);
		}
	}

	private void applyContentContainsOrGroup(
			LambdaQueryWrapper<Post> w,
			String contentTrimmed,
			List<Long> keywordTopicsPostIds) {
		String pattern = "%" + escapeLike(contentTrimmed) + "%";
		w.and(
				q ->
						q.like(Post::getTitle, pattern)
								.or()
								.like(Post::getContent, pattern)
								.or()
								.in(Post::getPostId, keywordTopicsPostIds));
	}

	private void applyLikeOrFavoritePostIdFilter(
			LambdaQueryWrapper<Post> w, long profileUserId, String searchType) {
		if (!StringUtils.hasText(searchType)) {
			return;
		}
		String st = searchType.trim();
		List<Long> ids;
		if ("like".equals(st)) {
			ids = resolvePostIdsFromLikes(profileUserId);
		} else if ("favorite".equals(st)) {
			ids = resolvePostIdsFromFavorites(profileUserId);
		} else {
			return;
		}
		w.in(Post::getPostId, ids.isEmpty() ? List.of(-1L) : ids);
	}

	private List<Long> resolvePostIdsFromLikes(long profileUserId) {
		List<PostLike> rows =
				postLikeMapper.selectList(
						new LambdaQueryWrapper<PostLike>()
								.eq(PostLike::getDisabled, false)
								.eq(PostLike::getUserId, profileUserId));
		LinkedHashSet<Long> out = new LinkedHashSet<>();
		if (rows != null) {
			for (PostLike r : rows) {
				if (r.getPostId() != null) {
					out.add(r.getPostId());
				}
			}
		}
		return new ArrayList<>(out);
	}

	private List<Long> resolvePostIdsFromFavorites(long profileUserId) {
		List<PostFavorite> rows =
				postFavoriteMapper.selectList(
						new LambdaQueryWrapper<PostFavorite>()
								.eq(PostFavorite::getDisabled, false)
								.eq(PostFavorite::getUserId, profileUserId));
		LinkedHashSet<Long> out = new LinkedHashSet<>();
		if (rows != null) {
			for (PostFavorite r : rows) {
				if (r.getPostId() != null) {
					out.add(r.getPostId());
				}
			}
		}
		return new ArrayList<>(out);
	}

	private void applyDefaultAndSortOrder(LambdaQueryWrapper<Post> w, String sort) {
		if (StringUtils.hasText(sort)) {
			String t = sort.trim();
			if (!"likes desc".equals(t) && !"created desc".equals(t)) {
				throw new ResourceException("排序不合法");
			}
		}
		w.orderByAsc(Post::getPOrder);
		if (StringUtils.hasText(sort)) {
			String t = sort.trim();
			if ("likes desc".equals(t)) {
				w.orderByDesc(Post::getLikes).orderByDesc(Post::getCreated);
			} else {
				w.orderByDesc(Post::getCreated);
			}
		} else {
			w.orderByDesc(Post::getCreated);
		}
		w.orderByAsc(Post::getMobile);
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
}
