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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class FrontUgcCommentListService {

	private final CommentMapper commentMapper;
	private final CommentOutsideLangReadService commentOutsideLangReadService;
	private final MemberAccountService memberAccountService;
	private final FrontUgcCommentLikeService frontUgcCommentLikeService;

	public FrontUgcCommentListService(
			CommentMapper commentMapper,
			CommentOutsideLangReadService commentOutsideLangReadService,
			MemberAccountService memberAccountService,
			FrontUgcCommentLikeService frontUgcCommentLikeService) {
		this.commentMapper = commentMapper;
		this.commentOutsideLangReadService = commentOutsideLangReadService;
		this.memberAccountService = memberAccountService;
		this.frontUgcCommentLikeService = frontUgcCommentLikeService;
	}

	public Map<String, Object> buildList(
			long postId,
			long parentCommentId,
			int pageNo,
			int pageSize,
			long userIdForLike,
			long companyIdForFilter,
			String langTag) {
		LambdaQueryWrapper<Comment> w = new LambdaQueryWrapper<>();
		w.eq(Comment::getPostId, postId)
				.eq(Comment::getStatus, 1)
				.eq(Comment::getEnable, 1)
				.eq(Comment::getDisabled, false)
				.eq(Comment::getCompanyId, companyIdForFilter);
		if (parentCommentId == 0L) {
			w.eq(Comment::getParentCommentId, 0L);
		} else {
			w.eq(Comment::getParentCommentId, parentCommentId);
		}
		// Primary sort by display order ascending; tie-break by created time descending (newest first).
		w.orderByAsc(Comment::getPOrder).orderByDesc(Comment::getCreated);

		Page<Comment> page = new Page<>(pageNo, pageSize);
		commentMapper.selectPage(page, w);

		List<LinkedHashMap<String, Object>> list = new ArrayList<>();
		if (page.getRecords() != null) {
			for (Comment e : page.getRecords()) {
				LinkedHashMap<String, Object> row = commentToMainListRowMap(e);
				long rowCompanyId = e.getCompanyId() != null ? e.getCompanyId() : 0L;
				commentOutsideLangReadService.applyToRowMap(rowCompanyId, langTag, row);
				FrontUgcCommentListDisplaySupport.enrichMainRow(row, memberAccountService);
				long cid = e.getCommentId() != null ? e.getCommentId() : 0L;
				long pid = e.getPostId() != null ? e.getPostId() : postId;
				row.put(
						"like_status",
						userIdForLike > 0L && frontUgcCommentLikeService.isCommentLikedByUser(userIdForLike, pid, cid)
								? 1
								: 0);
				row.put("child", loadChildComments(e.getCommentId(), langTag, userIdForLike, pid));
				list.add(ksortCopy(row));
			}
		}

		TreeMap<String, Object> pager = new TreeMap<>();
		pager.put("count", page.getTotal());
		pager.put("page_no", pageNo);
		pager.put("page_size", pageSize);

		TreeMap<String, Object> body = new TreeMap<>();
		body.put("list", list);
		body.put("pager", pager);
		body.put("total_count", page.getTotal());
		return body;
	}

	public Map<String, Object> buildCommentDetailForMessageEmbed(
			long companyId, long commentId, String langTag) {
		if (commentId <= 0L) {
			return null;
		}
		Comment e =
				commentMapper.selectOne(
						new LambdaQueryWrapper<Comment>()
								.eq(Comment::getCompanyId, companyId)
								.eq(Comment::getCommentId, commentId));
		if (e == null) {
			return null;
		}
		LinkedHashMap<String, Object> row = commentToMainListRowMap(e);
		long rowCompanyId = e.getCompanyId() != null ? e.getCompanyId() : 0L;
		commentOutsideLangReadService.applyToRowMap(rowCompanyId, langTag, row);
		FrontUgcCommentListDisplaySupport.enrichMainRow(row, memberAccountService);
		long pid = e.getPostId() != null ? e.getPostId() : 0L;
		row.put("like_status", 0);
		row.put("child", loadChildComments(e.getCommentId(), langTag, 0L, pid));
		return ksortCopy(row);
	}

	/**
	 * Loads a short preview of direct replies under a top-level comment. The child query matches on
	 * parent id and {@code disabled=false} only; the main list also enforces status, enable flag, and
	 * company scope, so a reply may appear here even when it would not pass the main list filters.
	 */
	private List<Map<String, Object>> loadChildComments(
			Long mainCommentId, String langTag, long userIdForLike, long postId) {
		if (mainCommentId == null || mainCommentId <= 0L) {
			return List.of();
		}
		LambdaQueryWrapper<Comment> cw = new LambdaQueryWrapper<>();
		cw.eq(Comment::getParentCommentId, mainCommentId)
				.eq(Comment::getDisabled, false)
				.orderByAsc(Comment::getPOrder)
				.orderByDesc(Comment::getCreated);
		Page<Comment> cp = new Page<>(1, 2);
		commentMapper.selectPage(cp, cw);
		List<Map<String, Object>> children = new ArrayList<>();
		if (cp.getRecords() != null) {
			for (Comment ch : cp.getRecords()) {
				LinkedHashMap<String, Object> row = commentToChildRowMap(ch);
				long coid = ch.getCompanyId() != null ? ch.getCompanyId() : 0L;
				commentOutsideLangReadService.applyToRowMap(coid, langTag, row);
				FrontUgcCommentListDisplaySupport.enrichChildRow(row, memberAccountService);
				long ccid = ch.getCommentId() != null ? ch.getCommentId() : 0L;
				row.put(
						"like_status",
						userIdForLike > 0L && frontUgcCommentLikeService.isCommentLikedByUser(userIdForLike, postId, ccid)
								? 1
								: 0);
				children.add(ksortCopy(row));
			}
		}
		return children;
	}

	private static LinkedHashMap<String, Object> commentToMainListRowMap(Comment e) {
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

	private static LinkedHashMap<String, Object> commentToChildRowMap(Comment e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("comment_id", e.getCommentId());
		m.put("post_id", e.getPostId());
		m.put("user_id", e.getUserId());
		m.put("reply_user_id", e.getReplyUserId());
		m.put("content", e.getContent() != null ? e.getContent() : "");
		m.put("likes", e.getLikes() != null ? e.getLikes() : "0");
		m.put("company_id", e.getCompanyId());
		m.put("created", e.getCreated());
		return m;
	}

	private static LinkedHashMap<String, Object> ksortCopy(Map<String, Object> src) {
		return new LinkedHashMap<>(new TreeMap<>(src));
	}
}
