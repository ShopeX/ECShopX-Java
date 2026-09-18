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
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.service.point.UgcPostPointService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class FrontUgcPostShareService {

	private static final Logger log = LoggerFactory.getLogger(FrontUgcPostShareService.class);

	private final PostMapper postMapper;
	private final UgcPostPointService ugcPostPointService;
	private final StringRedisTemplate redis;

	public FrontUgcPostShareService(
			PostMapper postMapper,
			UgcPostPointService ugcPostPointService,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis) {
		this.postMapper = postMapper;
		this.ugcPostPointService = ugcPostPointService;
		this.redis = redis;
	}

	public int share(long userId, long postId, long companyId) {
		Post row = postMapper.selectById(postId);
		int shareNums = 0;
		if (row != null && row.getShareNums() != null) {
			shareNums = row.getShareNums().intValue();
		}
		shareNums++;

		LambdaUpdateWrapper<Post> w = new LambdaUpdateWrapper<Post>()
				.eq(Post::getPostId, postId)
				.set(Post::getShareNums, shareNums);
		int updated = postMapper.update(null, w);
		if (updated <= 0) {
			throw new ResourceException("未查询到更新数据");
		}

		redis.opsForHash().put("ugc_share", String.valueOf(postId) + "::" + userId, "1");

		try {
			ugcPostPointService.addUgcPoint(postId, userId, companyId, 24, "");
		} catch (Exception e) {
			String paramsSummary = "array ('post_id' => " + postId + ", 'user_id' => " + userId + ", 'company_id' => " + companyId + ")";
			log.debug("addUgcPoint 分享笔记 送积分失败: {}|失败原因: {}", paramsSummary, e.getMessage(), e);
		}

		return shareNums;
	}
}
