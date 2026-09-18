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

package cn.shopex.ecshopx.wsugc.service.follower;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Follower;
import cn.shopex.ecshopx.wsugc.domain.Message;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.domain.PostLike;
import cn.shopex.ecshopx.wsugc.mapper.FollowerMapper;
import cn.shopex.ecshopx.wsugc.mapper.MessageMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostLikeMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class FrontUgcFollowerStatService {

	private static final String[] USER_INFO_WHITELIST = {"nickname", "user_id", "headimgurl", "unionid"};

	private final FollowerMapper followerMapper;
	private final PostMapper postMapper;
	private final PostLikeMapper postLikeMapper;
	private final MessageMapper messageMapper;
	private final MemberAccountService memberAccountService;
	private final UgcFollowerRedisService ugcFollowerRedisService;

	public FrontUgcFollowerStatService(
			FollowerMapper followerMapper,
			PostMapper postMapper,
			PostLikeMapper postLikeMapper,
			MessageMapper messageMapper,
			MemberAccountService memberAccountService,
			UgcFollowerRedisService ugcFollowerRedisService) {
		this.followerMapper = followerMapper;
		this.postMapper = postMapper;
		this.postLikeMapper = postLikeMapper;
		this.messageMapper = messageMapper;
		this.memberAccountService = memberAccountService;
		this.ugcFollowerRedisService = ugcFollowerRedisService;
	}

	public Map<String, Object> buildStat(
			HttpServletRequest request, long authUserId, long companyId, long targetUserId) {
		long followers = countFollowers(targetUserId);
		long idols = countIdols(targetUserId);
		long likes = countLikesForUserPosts(targetUserId);
		Object userInfo = buildFilteredUserInfo(targetUserId, companyId);
		long postAllNums = countPublishedPosts(targetUserId, companyId);
		long unreadNums = countUnreadMessages(targetUserId);
		int mutualFollow = computeMutualFollowInline(authUserId, targetUserId);
		int followStatus = resolveFollowStatus(authUserId, targetUserId);
		Object draftPost = buildDraftPost(request, authUserId);

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("followers", followers);
		out.put("idols", idols);
		out.put("likes", likes);
		out.put("userInfo", userInfo);
		out.put("post_all_nums", postAllNums);
		out.put("unread_nums", unreadNums);
		out.put("mutal_follow", mutualFollow);
		out.put("follow_status", followStatus);
		out.put("draft_post", draftPost);
		return out;
	}

	private long countFollowers(long targetUserId) {
		LambdaQueryWrapper<Follower> w = new LambdaQueryWrapper<>();
		w.eq(Follower::getUserId, targetUserId).eq(Follower::getDisabled, false);
		Long c = followerMapper.selectCount(w);
		return c != null ? c : 0L;
	}

	private long countIdols(long targetUserId) {
		LambdaQueryWrapper<Follower> w = new LambdaQueryWrapper<>();
		w.eq(Follower::getFollowerUserId, targetUserId).eq(Follower::getDisabled, false);
		Long c = followerMapper.selectCount(w);
		return c != null ? c : 0L;
	}

	private long countLikesForUserPosts(long targetUserId) {
		LambdaQueryWrapper<Post> pw = new LambdaQueryWrapper<>();
		pw.eq(Post::getUserId, targetUserId)
				.eq(Post::getDisabled, 0)
				.select(Post::getPostId);
		List<Post> posts = postMapper.selectList(pw);
		List<Long> ids = new ArrayList<>();
		for (Post p : posts) {
			if (p.getPostId() != null) {
				ids.add(p.getPostId());
			}
		}
		if (ids.isEmpty()) {
			return 0L;
		}
		LambdaQueryWrapper<PostLike> lw = new LambdaQueryWrapper<>();
		lw.in(PostLike::getPostId, ids).eq(PostLike::getDisabled, false);
		Long c = postLikeMapper.selectCount(lw);
		return c != null ? c : 0L;
	}

	/**
	 * JSON shape: {@link Collections#emptyList()} serializes as an empty array when user info is missing
	 * or none of the whitelist keys are present in the loaded map; otherwise returns a {@link LinkedHashMap}
	 * that serializes as a JSON object with only those whitelisted entries.
	 */
	private Object buildFilteredUserInfo(long targetUserId, long companyId) {
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("user_id", targetUserId);
		filter.put("company_id", companyId);
		Map<String, Object> source = memberAccountService.getWechatUserInfo(filter);
		if (source == null || source.isEmpty()) {
			return Collections.emptyList();
		}
		LinkedHashMap<String, Object> clipped = new LinkedHashMap<>();
		for (String k : USER_INFO_WHITELIST) {
			if (source.containsKey(k)) {
				clipped.put(k, source.get(k));
			}
		}
		if (clipped.isEmpty()) {
			return Collections.emptyList();
		}
		return clipped;
	}

	private long countPublishedPosts(long targetUserId, long companyId) {
		LambdaQueryWrapper<Post> w = new LambdaQueryWrapper<>();
		w.eq(Post::getUserId, targetUserId)
				.eq(Post::getCompanyId, companyId)
				.eq(Post::getDisabled, 0)
				.eq(Post::getIsDraft, 0);
		Long c = postMapper.selectCount(w);
		return c != null ? c : 0L;
	}

	private long countUnreadMessages(long targetUserId) {
		LambdaQueryWrapper<Message> w = new LambdaQueryWrapper<>();
		w.eq(Message::getToUserId, targetUserId).eq(Message::getHasRead, false);
		Long c = messageMapper.selectCount(w);
		return c != null ? c : 0L;
	}

	private int computeMutualFollowInline(long authUserId, long targetUserId) {
		LambdaQueryWrapper<Follower> w1 = new LambdaQueryWrapper<>();
		w1.eq(Follower::getUserId, targetUserId)
				.eq(Follower::getFollowerUserId, authUserId)
				.eq(Follower::getDisabled, false);
		LambdaQueryWrapper<Follower> w2 = new LambdaQueryWrapper<>();
		w2.eq(Follower::getUserId, authUserId)
				.eq(Follower::getFollowerUserId, targetUserId)
				.eq(Follower::getDisabled, false);
		long c1 = followerMapper.selectCount(w1);
		long c2 = followerMapper.selectCount(w2);
		return (c1 == 1L && c2 == 1L) ? 1 : 0;
	}

	public int readFollowStatus(long visitorUserId, long bloggerUserId) {
		return followStatusRedisThenDb(bloggerUserId, visitorUserId);
	}

	private int resolveFollowStatus(long authUserId, long targetUserId) {
		return followStatusRedisThenDb(targetUserId, authUserId);
	}

	private int followStatusRedisThenDb(long bloggerUserId, long followerUserId) {
		String rv = ugcFollowerRedisService.getFollowerHashValue(bloggerUserId, followerUserId);
		if (rv != null) {
			try {
				return Integer.parseInt(rv.trim()) != 0 ? 1 : 0;
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		LambdaQueryWrapper<Follower> w = new LambdaQueryWrapper<>();
		w.eq(Follower::getUserId, bloggerUserId)
				.eq(Follower::getFollowerUserId, followerUserId)
				.eq(Follower::getDisabled, false);
		Long c = followerMapper.selectCount(w);
		return (c != null && c > 0L) ? 1 : 0;
	}

	private Object buildDraftPost(HttpServletRequest request, long authUserId) {
		List<Object> emptyList = Collections.emptyList();
		String raw = request.getParameter("user_id");
		if (raw == null || raw.isEmpty()) {
			return emptyList;
		}
		long parsedQueryUserId = parseLongFlexible(raw.trim(), -1L);
		if (parsedQueryUserId <= 0L
				|| !Objects.equals(Long.valueOf(authUserId), Long.valueOf(parsedQueryUserId))) {
			return emptyList;
		}

		LambdaQueryWrapper<Post> countW = new LambdaQueryWrapper<>();
		countW.eq(Post::getUserId, authUserId).eq(Post::getIsDraft, 1).eq(Post::getDisabled, 0);
		long totalCount = postMapper.selectCount(countW);

		Page<Post> page = new Page<>(1, 1, false);
		LambdaQueryWrapper<Post> listW = new LambdaQueryWrapper<>();
		listW.eq(Post::getUserId, authUserId)
				.eq(Post::getIsDraft, 1)
				.eq(Post::getDisabled, 0)
				.orderByDesc(Post::getPostId)
				.select(Post::getPostId, Post::getTitle, Post::getCover);
		postMapper.selectPage(page, listW);
		List<Post> records = page.getRecords();
		if (records != null && !records.isEmpty()) {
			Post p = records.get(0);
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("post_id", p.getPostId());
			row.put("title", p.getTitle());
			row.put("cover", p.getCover());
			return row;
		}
		LinkedHashMap<String, Object> pkg = new LinkedHashMap<>();
		pkg.put("total_count", totalCount);
		pkg.put("list", Collections.emptyList());
		return pkg;
	}

	private static long parseLongFlexible(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
