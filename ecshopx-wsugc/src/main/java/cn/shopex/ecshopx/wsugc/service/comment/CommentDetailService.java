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

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Comment;
import cn.shopex.ecshopx.wsugc.mapper.CommentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommentDetailService {

	private final CommentMapper commentMapper;
	private final CommentOutsideLangReadService commentOutsideLangReadService;
	private final MemberAccountService memberAccountService;

	public CommentDetailService(
			CommentMapper commentMapper,
			CommentOutsideLangReadService commentOutsideLangReadService,
			MemberAccountService memberAccountService) {
		this.commentMapper = commentMapper;
		this.commentOutsideLangReadService = commentOutsideLangReadService;
		this.memberAccountService = memberAccountService;
	}

	public Map<String, Object> buildResponse(
			String commentIdRaw, Map<String, Object> operatorJwt, String requestLangTag) {
		long companyId = readLong(operatorJwt.get("company_id"), 1L);
		Long commentId = parseOptionalLong(commentIdRaw);
		if (commentId == null) {
			return sortedOuter(null);
		}
		LambdaQueryWrapper<Comment> w = new LambdaQueryWrapper<>();
		w.eq(Comment::getCompanyId, companyId).eq(Comment::getCommentId, commentId).last("LIMIT 1");
		Comment row = commentMapper.selectOne(w);
		if (row == null) {
			return sortedOuter(null);
		}
		LinkedHashMap<String, Object> rowMap = commentToRowMap(row);
		commentOutsideLangReadService.applyToRowMap(companyId, requestLangTag, rowMap);
		formatAdminDetail(rowMap);
		return sortedOuter(ksortCopy(rowMap));
	}

	private static Map<String, Object> sortedOuter(Map<String, Object> detailOrNull) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("comment_info", detailOrNull);
		return ksortCopy(out);
	}

	private static LinkedHashMap<String, Object> ksortCopy(Map<String, Object> src) {
		TreeMap<String, Object> sorted = new TreeMap<>(src);
		return new LinkedHashMap<>(sorted);
	}

	private static LinkedHashMap<String, Object> commentToRowMap(Comment e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("comment_id", e.getCommentId());
		m.put("post_id", e.getPostId());
		m.put("user_id", e.getUserId());
		m.put("reply_user_id", e.getReplyUserId());
		m.put("content", e.getContent() != null ? e.getContent() : "");
		m.put("likes", e.getLikes() != null ? e.getLikes() : "0");
		m.put("company_id", e.getCompanyId());
		m.put("created", e.getCreated());
		m.put("status", e.getStatus());
		return m;
	}

	private void formatAdminDetail(Map<String, Object> rowMap) {
		CommentAdminDisplaySupport.enrichAdminRow(rowMap, memberAccountService);
	}

	private static Long parseOptionalLong(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
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
