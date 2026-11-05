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

package cn.shopex.ecshopx.comments.service;

import cn.shopex.ecshopx.comments.domain.ShopComments;
import cn.shopex.ecshopx.comments.mapper.ShopCommentsMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ShopCommentUpdateService {

	private final ShopCommentsMapper shopCommentsMapper;
	private final ObjectMapper objectMapper;

	public ShopCommentUpdateService(ShopCommentsMapper shopCommentsMapper, ObjectMapper objectMapper) {
		this.shopCommentsMapper = shopCommentsMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> update(String commentIdRaw, Map<String, Object> merged, long companyId) {
		Long id = parseCommentId(commentIdRaw);
		String companyIdStr = String.valueOf(companyId);
		LambdaQueryWrapper<ShopComments> scope = ownershipQuery(id, companyIdStr);
		ShopComments entity = shopCommentsMapper.selectOne(scope);
		if (entity == null) {
			throw new ResourceException("comment_id=" + commentIdRaw + "的评论不存在");
		}

		boolean touch = false;
		if (merged.containsKey("is_stick") && merged.get("is_stick") != null) {
			entity.setStuck(toLooseBoolean(merged.get("is_stick")));
			touch = true;
		}
		if (merged.containsKey("is_hide") && merged.get("is_hide") != null) {
			entity.setHid(toLooseBoolean(merged.get("is_hide")));
			touch = true;
		}

		ShopComments forResponse;
		if (touch) {
			entity.setUpdated((int) (System.currentTimeMillis() / 1000L));
			LambdaUpdateWrapper<ShopComments> writeScope = new LambdaUpdateWrapper<>();
			writeScope.eq(ShopComments::getCommentId, id).eq(ShopComments::getCompanyId, companyIdStr);
			int rows = shopCommentsMapper.update(entity, writeScope);
			if (rows == 0) {
				throw new ResourceException("comment_id=" + commentIdRaw + "的评论不存在");
			}
			ShopComments reloaded = shopCommentsMapper.selectOne(ownershipQuery(id, companyIdStr));
			if (reloaded == null) {
				throw new ResourceException("comment_id=" + commentIdRaw + "的评论不存在");
			}
			forResponse = reloaded;
		} else {
			forResponse = entity;
		}
		return ShopCommentResponseMaps.buildCommentResponseMap(forResponse, objectMapper);
	}

	private static LambdaQueryWrapper<ShopComments> ownershipQuery(long commentId, String companyId) {
		LambdaQueryWrapper<ShopComments> q = new LambdaQueryWrapper<>();
		q.eq(ShopComments::getCommentId, commentId).eq(ShopComments::getCompanyId, companyId);
		return q;
	}

	private static Long parseCommentId(String commentIdRaw) {
		if (commentIdRaw == null || commentIdRaw.isEmpty()) {
			throw new ResourceException("comment_id=" + commentIdRaw + "的评论不存在");
		}
		try {
			long v = Long.parseLong(commentIdRaw.trim());
			if (v <= 0) {
				throw new ResourceException("comment_id=" + commentIdRaw + "的评论不存在");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException("comment_id=" + commentIdRaw + "的评论不存在");
		}
	}

	private static boolean toLooseBoolean(Object v) {
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		if (v instanceof String s) {
			return "true".equals(s.trim());
		}
		return false;
	}
}
