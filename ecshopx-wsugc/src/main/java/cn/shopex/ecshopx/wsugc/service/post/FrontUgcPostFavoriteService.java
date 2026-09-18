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
import cn.shopex.ecshopx.wsugc.domain.PostFavorite;
import cn.shopex.ecshopx.wsugc.mapper.MessageMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostFavoriteMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.service.point.UgcPostPointService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FrontUgcPostFavoriteService {

	private static final Logger log = LoggerFactory.getLogger(FrontUgcPostFavoriteService.class);

	private final PostMapper postMapper;
	private final PostFavoriteMapper postFavoriteMapper;
	private final MessageMapper messageMapper;
	private final UgcPostPointService ugcPostPointService;
	private final UgcPostFavoriteRedisService ugcPostFavoriteRedisService;
	private final MemberAccountService memberAccountService;

	public FrontUgcPostFavoriteService(
			PostMapper postMapper,
			PostFavoriteMapper postFavoriteMapper,
			MessageMapper messageMapper,
			UgcPostPointService ugcPostPointService,
			UgcPostFavoriteRedisService ugcPostFavoriteRedisService,
			MemberAccountService memberAccountService) {
		this.postMapper = postMapper;
		this.postFavoriteMapper = postFavoriteMapper;
		this.messageMapper = messageMapper;
		this.ugcPostPointService = ugcPostPointService;
		this.ugcPostFavoriteRedisService = ugcPostFavoriteRedisService;
		this.memberAccountService = memberAccountService;
	}

	public Map<String, Object> favorite(long userId, long postId) {
		Post post = postMapper.selectById(postId);
		if (post == null) {
			throw new ResourceException("关联数据不存在");
		}
		long postAuthorUserId = post.getUserId() != null ? post.getUserId() : 0L;
		long postCompanyId = post.getCompanyId() != null ? post.getCompanyId() : 0L;
		String postTitle = post.getTitle() != null ? post.getTitle() : "";

		LambdaQueryWrapper<PostFavorite> pair = new LambdaQueryWrapper<PostFavorite>()
				.eq(PostFavorite::getUserId, userId)
				.eq(PostFavorite::getPostId, postId);
		PostFavorite existing = postFavoriteMapper.selectOne(pair);

		String action = "favorite";
		int now = (int) (System.currentTimeMillis() / 1000L);

		if (existing == null) {
			PostFavorite entity = new PostFavorite();
			entity.setUserId(userId);
			entity.setPostId(postId);
			entity.setDisabled(false);
			entity.setCreated(now);
			entity.setUpdated(now);
			int rows = postFavoriteMapper.insert(entity);
			boolean insertOk = rows > 0
					|| (entity.getPostFavoriteId() != null && entity.getPostFavoriteId() > 0);
			if (!insertOk) {
				throw new ResourceException("收藏失败！");
			}
			try {
				ugcPostPointService.addUgcPoint(postId, userId, postCompanyId, 23, "");
			} catch (Exception e) {
				log.debug("addUgcPoint 收藏笔记 送积分失败: {}", e.getMessage(), e);
			}
			try {
				Message m = new Message();
				m.setType("favoritePost");
				m.setSubType("favorite");
				m.setSource(1);
				m.setPostId(postId);
				m.setCommentId(0L);
				m.setCompanyId(postCompanyId);
				m.setFromUserId(userId);
				m.setToUserId(postAuthorUserId);
				m.setTitle("收藏了您的笔记");
				m.setContent(postTitle);
				m.setFromNickname(wechatNickname(userId, postCompanyId));
				m.setToNickname(wechatNickname(postAuthorUserId, postCompanyId));
				m.setCreated(now);
				m.setUpdated(now);
				m.setHasRead(false);
				messageMapper.insert(m);
			} catch (Exception e) {
				log.debug("发送笔记收藏消息失败: {}", e.getMessage(), e);
			}
		} else if (!Boolean.TRUE.equals(existing.getDisabled())) {
			action = "unfavorite";
			PostFavorite fresh = postFavoriteMapper.selectOne(pair);
			if (fresh == null) {
				throw new ResourceException("未查询到更新数据");
			}
			fresh.setDisabled(true);
			fresh.setUpdated(now);
			int updated = postFavoriteMapper.updateById(fresh);
			if (updated <= 0) {
				throw new ResourceException("未查询到更新数据");
			}
		} else {
			PostFavorite fresh = postFavoriteMapper.selectOne(pair);
			if (fresh == null) {
				throw new ResourceException("未查询到更新数据");
			}
			fresh.setDisabled(false);
			fresh.setUpdated(now);
			int updated = postFavoriteMapper.updateById(fresh);
			if (updated <= 0) {
				throw new ResourceException("未查询到更新数据");
			}
		}

		if ("favorite".equals(action)) {
			ugcPostFavoriteRedisService.addPostFavoritesToRedis(userId, postId, postAuthorUserId);
		} else {
			ugcPostFavoriteRedisService.reducePostFavoritesToRedis(userId, postId, postAuthorUserId);
		}

		LambdaQueryWrapper<PostFavorite> active = new LambdaQueryWrapper<PostFavorite>()
				.eq(PostFavorite::getPostId, postId)
				.eq(PostFavorite::getDisabled, false);
		long count = postFavoriteMapper.selectCount(active);

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
