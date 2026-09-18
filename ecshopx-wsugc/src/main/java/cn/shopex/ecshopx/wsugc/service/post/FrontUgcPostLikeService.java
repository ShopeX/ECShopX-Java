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
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Message;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.domain.PostLike;
import cn.shopex.ecshopx.wsugc.mapper.MessageMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostLikeMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.service.point.UgcPostPointService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FrontUgcPostLikeService {

	private static final Logger log = LoggerFactory.getLogger(FrontUgcPostLikeService.class);

	private final PostMapper postMapper;
	private final PostLikeMapper postLikeMapper;
	private final MessageMapper messageMapper;
	private final UgcPostPointService ugcPostPointService;
	private final UgcPostLikeRedisService ugcPostLikeRedisService;
	private final MemberAccountService memberAccountService;

	public FrontUgcPostLikeService(
			PostMapper postMapper,
			PostLikeMapper postLikeMapper,
			MessageMapper messageMapper,
			UgcPostPointService ugcPostPointService,
			UgcPostLikeRedisService ugcPostLikeRedisService,
			MemberAccountService memberAccountService) {
		this.postMapper = postMapper;
		this.postLikeMapper = postLikeMapper;
		this.messageMapper = messageMapper;
		this.ugcPostPointService = ugcPostPointService;
		this.ugcPostLikeRedisService = ugcPostLikeRedisService;
		this.memberAccountService = memberAccountService;
	}

	public Map<String, Object> like(long userId, long postId) {
		Post post = postMapper.selectById(postId);
		if (post == null) {
			throw new ResourceException("关联数据不存在");
		}
		long postAuthorUserId = post.getUserId() != null ? post.getUserId() : 0L;
		long postCompanyId = post.getCompanyId() != null ? post.getCompanyId() : 0L;
		String postTitle = post.getTitle() != null ? post.getTitle() : "";

		LambdaQueryWrapper<PostLike> pair = new LambdaQueryWrapper<PostLike>()
				.eq(PostLike::getUserId, userId)
				.eq(PostLike::getPostId, postId);
		PostLike existing = postLikeMapper.selectOne(pair);

		String action = "like";
		int now = (int) (System.currentTimeMillis() / 1000L);

		if (existing == null) {
			PostLike entity = new PostLike();
			entity.setUserId(userId);
			entity.setPostId(postId);
			entity.setDisabled(false);
			entity.setCreated(now);
			entity.setUpdated(now);
			int rows = postLikeMapper.insert(entity);
			if (rows <= 0) {
				throw new ResourceException("点赞失败！");
			}
			try {
				ugcPostPointService.addUgcPoint(postId, userId, postCompanyId, 21, "");
			} catch (Exception e) {
				log.debug("addUgcPoint 点赞笔记 送积分失败: {}", e.getMessage(), e);
			}
			try {
				Message m = new Message();
				m.setType("like");
				m.setSubType("likePost");
				m.setSource(1);
				m.setPostId(postId);
				m.setCommentId(0L);
				m.setCompanyId(postCompanyId);
				m.setFromUserId(userId);
				m.setToUserId(postAuthorUserId);
				m.setTitle("赞了您的笔记");
				m.setContent(postTitle);
				m.setFromNickname(wechatNickname(userId, postCompanyId));
				m.setToNickname(wechatNickname(postAuthorUserId, postCompanyId));
				m.setCreated(now);
				m.setUpdated(now);
				m.setHasRead(false);
				messageMapper.insert(m);
			} catch (Exception e) {
				log.debug("发送笔记点赞消息失败: {}", e.getMessage(), e);
			}
		} else if (!Boolean.TRUE.equals(existing.getDisabled())) {
			action = "unlike";
			PostLike fresh = postLikeMapper.selectOne(pair);
			if (fresh == null) {
				throw new ResourceException("未查询到更新数据");
			}
			fresh.setDisabled(true);
			fresh.setUpdated(now);
			int updateRows = postLikeMapper.updateById(fresh);
			if (updateRows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		} else {
			PostLike fresh = postLikeMapper.selectOne(pair);
			if (fresh == null) {
				throw new ResourceException("未查询到更新数据");
			}
			fresh.setDisabled(false);
			fresh.setUpdated(now);
			int updateRows = postLikeMapper.updateById(fresh);
			if (updateRows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		}

		if ("like".equals(action)) {
			ugcPostLikeRedisService.addPostLikesToRedis(userId, postId, postAuthorUserId);
		} else {
			ugcPostLikeRedisService.reducePostLikesToRedis(userId, postId, postAuthorUserId);
		}

		LambdaQueryWrapper<PostLike> active = new LambdaQueryWrapper<PostLike>()
				.eq(PostLike::getPostId, postId)
				.eq(PostLike::getDisabled, false);
		long count = postLikeMapper.selectCount(active);

		int likesVal = (int) Math.min(count, Integer.MAX_VALUE);
		LambdaUpdateWrapper<Post> uw = new LambdaUpdateWrapper<>();
		uw.eq(Post::getPostId, postId).set(Post::getLikes, likesVal);
		int postUpdated = postMapper.update(null, uw);
		if (postUpdated == 0) {
			log.debug("回写 wsugc_post.likes 未影响行: postId={}", postId);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("action", action);
		out.put("likes", count);
		return out;
	}

	private String wechatNickname(long uid, long cid) {
		Map<String, Object> wx = memberAccountService.getWechatUserInfo(Map.of(
				"user_id", uid,
				"company_id", cid));
		Object n = wx.get("nickname");
		return n != null ? n.toString() : "";
	}
}
