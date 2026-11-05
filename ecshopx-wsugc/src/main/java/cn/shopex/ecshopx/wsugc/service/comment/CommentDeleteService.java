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

package cn.shopex.ecshopx.wsugc.service.comment;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Comment;
import cn.shopex.ecshopx.wsugc.mapper.CommentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CommentDeleteService {

	private final CommentMultiLangDeleteService commentMultiLangDeleteService;
	private final CommentMapper commentMapper;

	public CommentDeleteService(
			CommentMultiLangDeleteService commentMultiLangDeleteService, CommentMapper commentMapper) {
		this.commentMultiLangDeleteService = commentMultiLangDeleteService;
		this.commentMapper = commentMapper;
	}

	public Map<String, Object> deleteComments(Map<String, Object> input, long companyId) {
		Object raw = input.get("comment_id");
		if (raw == null) {
			throw new ResourceException("comment_id参数不能为空");
		}

		List<Object> displayIds = new ArrayList<>();
		LinkedHashSet<Long> idsRequested = new LinkedHashSet<>();

		if (raw instanceof Collection<?> col) {
			if (col.isEmpty()) {
				throw new ResourceException("comment_id参数不能为空");
			}
			for (Object el : col) {
				displayIds.add(toDisplayToken(el));
				tryAddParseableLong(idsRequested, el);
			}
		} else {
			displayIds.add(toDisplayToken(raw));
			tryAddParseableLong(idsRequested, raw);
		}

		if (!idsRequested.isEmpty()) {
			LambdaQueryWrapper<Comment> scope = new LambdaQueryWrapper<>();
			scope.in(Comment::getCommentId, idsRequested).eq(Comment::getCompanyId, companyId);
			List<Comment> owned = commentMapper.selectList(scope);
			LinkedHashSet<Long> idsToDelete = new LinkedHashSet<>();
			if (owned != null) {
				for (Comment row : owned) {
					if (row != null && row.getCommentId() != null) {
						idsToDelete.add(row.getCommentId());
					}
				}
			}
			if (!idsToDelete.isEmpty()) {
				List<Long> scopedIds = new ArrayList<>(idsToDelete);
				commentMultiLangDeleteService.deleteMultiLangForComments(companyId, scopedIds);
				commentMapper.deleteBatchIds(scopedIds);
			}
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("comment_id", displayIds);
		out.put("message", "删除成功");
		return out;
	}

	/**
	 * Echo token for JSON: {@link Long} when it parses as long, otherwise trimmed string (原样回显请求 token).
	 */
	private static Object toDisplayToken(Object el) {
		if (el == null) {
			return null;
		}
		Long parsed = tryParseLong(el);
		if (parsed != null) {
			return parsed;
		}
		return el.toString().trim();
	}

	private static Long tryParseLong(Object el) {
		if (el == null) {
			return null;
		}
		if (el instanceof Number n) {
			return n.longValue();
		}
		String s = el.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static void tryAddParseableLong(LinkedHashSet<Long> ids, Object el) {
		Long l = tryParseLong(el);
		if (l != null) {
			ids.add(l);
		}
	}
}
