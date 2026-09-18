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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Follower;
import cn.shopex.ecshopx.wsugc.domain.Message;
import cn.shopex.ecshopx.wsugc.mapper.FollowerMapper;
import cn.shopex.ecshopx.wsugc.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FrontUgcFollowerCreateService {

	private static final Logger log = LoggerFactory.getLogger(FrontUgcFollowerCreateService.class);

	private final FollowerMapper followerMapper;
	private final MessageMapper messageMapper;
	private final MemberAccountService memberAccountService;
	private final UgcFollowerRedisService ugcFollowerRedisService;

	public FrontUgcFollowerCreateService(
			FollowerMapper followerMapper,
			MessageMapper messageMapper,
			MemberAccountService memberAccountService,
			UgcFollowerRedisService ugcFollowerRedisService) {
		this.followerMapper = followerMapper;
		this.messageMapper = messageMapper;
		this.memberAccountService = memberAccountService;
		this.ugcFollowerRedisService = ugcFollowerRedisService;
	}

	public Map<String, Object> create(
			Map<String, Object> merged, long bloggerUserId, long followerUserId, long companyId) {
		LambdaQueryWrapper<Follower> pair = new LambdaQueryWrapper<Follower>()
				.eq(Follower::getUserId, bloggerUserId)
				.eq(Follower::getFollowerUserId, followerUserId);
		Follower existing = followerMapper.selectOne(pair);

		String action;
		int now = (int) (System.currentTimeMillis() / 1000L);

		if (existing == null) {
			action = "follow";
			Follower entity = new Follower();
			entity.setUserId(bloggerUserId);
			entity.setFollowerUserId(followerUserId);
			entity.setCompanyId(companyId);
			entity.setDisabled(false);
			entity.setCreated(now);
			entity.setUpdated(now);
			int inserted = followerMapper.insert(entity);
			boolean insertOk = inserted > 0
					|| (entity.getFollowerId() != null && entity.getFollowerId() > 0);
			if (insertOk) {
				try {
					Message m = new Message();
					m.setType("followerUser");
					m.setSubType("follow");
					m.setSource(1);
					m.setPostId(0L);
					m.setCommentId(0L);
					m.setCompanyId(companyId);
					m.setFromUserId(followerUserId);
					m.setToUserId(bloggerUserId);
					m.setTitle("关注了您");
					m.setContent("关注了您");
					m.setFromNickname(wechatNickname(followerUserId, companyId));
					m.setToNickname(wechatNickname(bloggerUserId, companyId));
					m.setCreated(now);
					m.setUpdated(now);
					m.setHasRead(false);
					messageMapper.insert(m);
				} catch (Exception e) {
					log.debug("发送 关注消息 失败: {}", e.getMessage());
				}
			}
		} else if (!Boolean.TRUE.equals(existing.getDisabled())) {
			action = "unfollow";
			Follower fresh = followerMapper.selectOne(pair);
			if (fresh == null) {
				throw new ResourceException("未查询到更新数据");
			}
			fresh.setDisabled(true);
			fresh.setUpdated(now);
			followerMapper.updateById(fresh);
		} else {
			action = "follow";
			Follower fresh = followerMapper.selectOne(pair);
			if (fresh == null) {
				throw new ResourceException("未查询到更新数据");
			}
			fresh.setDisabled(false);
			fresh.setUpdated(now);
			followerMapper.updateById(fresh);
		}

		if ("follow".equals(action)) {
			ugcFollowerRedisService.addFollowersToRedis(bloggerUserId, followerUserId);
		} else {
			ugcFollowerRedisService.reduceFollowersToRedis(bloggerUserId, followerUserId);
		}

		LambdaQueryWrapper<Follower> activeCount = new LambdaQueryWrapper<Follower>()
				.eq(Follower::getUserId, bloggerUserId)
				.eq(Follower::getDisabled, false);
		long count = followerMapper.selectCount(activeCount);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("action", action);
		out.put("followers", count);
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
