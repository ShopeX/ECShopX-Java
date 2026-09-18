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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Comment;
import cn.shopex.ecshopx.wsugc.mapper.CommentMapper;
import cn.shopex.ecshopx.wsugc.service.post.UgcPostUserIdResolveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommentListService {

	private final CommentMapper commentMapper;
	private final CommentOutsideLangReadService commentOutsideLangReadService;
	private final UgcPostUserIdResolveService ugcPostUserIdResolveService;
	private final MemberAccountService memberAccountService;

	public CommentListService(
			CommentMapper commentMapper,
			CommentOutsideLangReadService commentOutsideLangReadService,
			UgcPostUserIdResolveService ugcPostUserIdResolveService,
			MemberAccountService memberAccountService) {
		this.commentMapper = commentMapper;
		this.commentOutsideLangReadService = commentOutsideLangReadService;
		this.ugcPostUserIdResolveService = ugcPostUserIdResolveService;
		this.memberAccountService = memberAccountService;
	}

	public Map<String, Object> buildList(
			long companyId,
			String langTag,
			int page,
			int pageSize,
			String statusRaw,
			List<Long> postIds,
			String nickname,
			String mobile,
			String content,
			String sort) {
		LambdaQueryWrapper<Comment> w = new LambdaQueryWrapper<>();
		w.eq(Comment::getCompanyId, companyId);

		if (statusRaw != null && !statusRaw.trim().isEmpty()) {
			try {
				int st = Integer.parseInt(statusRaw.trim());
				w.eq(Comment::getStatus, st);
			} catch (NumberFormatException ignored) {
				// Non-numeric status: do not add a status predicate to the query.
			}
		}

		if (postIds != null && !postIds.isEmpty()) {
			w.in(Comment::getPostId, postIds);
		}

		if (StringUtils.hasText(mobile)) {
			w.in(Comment::getUserId, ugcPostUserIdResolveService.userIdsByMobile(mobile.trim()));
		} else if (StringUtils.hasText(nickname)) {
			w.in(Comment::getUserId, ugcPostUserIdResolveService.userIdsByNicknameContains(nickname.trim()));
		}

		if (StringUtils.hasText(content)) {
			String trimmed = content.trim();
			List<Long> langIds =
					commentOutsideLangReadService.findDataIdsByFieldContains(
							companyId, langTag, "content", trimmed);
			if (!langIds.isEmpty()) {
				w.in(Comment::getCommentId, langIds);
			} else {
				w.like(Comment::getContent, "%" + escapeLike(trimmed) + "%");
			}
		}

		applySort(w, sort);

		Page<Comment> p = new Page<>(page, pageSize);
		commentMapper.selectPage(p, w);

		List<LinkedHashMap<String, Object>> list = new ArrayList<>();
		if (p.getRecords() != null) {
			for (Comment e : p.getRecords()) {
				LinkedHashMap<String, Object> row = commentToFullRowMap(e);
				commentOutsideLangReadService.applyToRowMap(companyId, langTag, row);
				CommentAdminDisplaySupport.enrichAdminRow(row, memberAccountService);
				list.add(ksortCopy(row));
			}
		}

		TreeMap<String, Object> pager = new TreeMap<>();
		pager.put("count", p.getTotal());
		pager.put("page_no", page);
		pager.put("page_size", pageSize);

		TreeMap<String, Object> body = new TreeMap<>();
		body.put("list", list);
		body.put("pager", pager);
		body.put("total_count", p.getTotal());
		return body;
	}

	private static LinkedHashMap<String, Object> commentToFullRowMap(Comment e) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("comment_id", e.getCommentId());
		m.put("post_id", e.getPostId());
		m.put("user_id", e.getUserId());
		m.put("parent_comment_id", e.getParentCommentId());
		m.put("reply_comment_id", e.getReplyCommentId());
		m.put("reply_user_id", e.getReplyUserId());
		m.put("content", e.getContent() != null ? e.getContent() : "");
		m.put("likes", e.getLikes() != null ? e.getLikes() : "0");
		m.put("ip", e.getIp());
		m.put("province", e.getProvince());
		m.put("city", e.getCity());
		m.put("district", e.getDistrict());
		m.put("p_order", e.getPOrder());
		m.put("status", e.getStatus());
		m.put("enable", e.getEnable());
		m.put("disabled", e.getDisabled());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("ai_verify_time", e.getAiVerifyTime());
		m.put("manual_verify_time", e.getManualVerifyTime());
		m.put("ai_refuse_reason", e.getAiRefuseReason());
		m.put("manual_refuse_reason", e.getManualRefuseReason());
		m.put("company_id", e.getCompanyId());
		return m;
	}

	private static LinkedHashMap<String, Object> ksortCopy(Map<String, Object> src) {
		return new LinkedHashMap<>(new TreeMap<>(src));
	}

	private static String escapeLike(String s) {
		return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static void applySort(LambdaQueryWrapper<Comment> wrapper, String sort) {
		if (sort == null || sort.trim().isEmpty()) {
			wrapper.orderByAsc(Comment::getPOrder).orderByDesc(Comment::getCreated);
			return;
		}
		String trimmed = sort.trim();
		int sp = trimmed.indexOf(' ');
		if (sp <= 0 || sp >= trimmed.length() - 1) {
			throw new BadRequestException("sort 参数格式无效");
		}
		String fieldRaw = trimmed.substring(0, sp).trim();
		String direction = trimmed.substring(sp + 1).trim();
		if (!StringUtils.hasText(fieldRaw) || !StringUtils.hasText(direction)) {
			throw new BadRequestException("sort 参数格式无效");
		}
		String field = fieldRaw.toLowerCase(Locale.ROOT);
		String dirLower = direction.toLowerCase(Locale.ROOT);
		boolean asc;
		if ("asc".equals(dirLower)) {
			asc = true;
		} else if ("desc".equals(dirLower)) {
			asc = false;
		} else {
			throw new BadRequestException("sort 参数格式无效");
		}
		switch (field) {
			case "p_order" -> {
				if (asc) {
					wrapper.orderByAsc(Comment::getPOrder);
				} else {
					wrapper.orderByDesc(Comment::getPOrder);
				}
			}
			case "created" -> {
				if (asc) {
					wrapper.orderByAsc(Comment::getCreated);
				} else {
					wrapper.orderByDesc(Comment::getCreated);
				}
			}
			case "updated" -> {
				if (asc) {
					wrapper.orderByAsc(Comment::getUpdated);
				} else {
					wrapper.orderByDesc(Comment::getUpdated);
				}
			}
			case "comment_id" -> {
				if (asc) {
					wrapper.orderByAsc(Comment::getCommentId);
				} else {
					wrapper.orderByDesc(Comment::getCommentId);
				}
			}
			case "post_id" -> {
				if (asc) {
					wrapper.orderByAsc(Comment::getPostId);
				} else {
					wrapper.orderByDesc(Comment::getPostId);
				}
			}
			case "user_id" -> {
				if (asc) {
					wrapper.orderByAsc(Comment::getUserId);
				} else {
					wrapper.orderByDesc(Comment::getUserId);
				}
			}
			case "parent_comment_id" -> {
				if (asc) {
					wrapper.orderByAsc(Comment::getParentCommentId);
				} else {
					wrapper.orderByDesc(Comment::getParentCommentId);
				}
			}
			case "reply_comment_id" -> {
				if (asc) {
					wrapper.orderByAsc(Comment::getReplyCommentId);
				} else {
					wrapper.orderByDesc(Comment::getReplyCommentId);
				}
			}
			case "reply_user_id" -> {
				if (asc) {
					wrapper.orderByAsc(Comment::getReplyUserId);
				} else {
					wrapper.orderByDesc(Comment::getReplyUserId);
				}
			}
			case "status" -> {
				if (asc) {
					wrapper.orderByAsc(Comment::getStatus);
				} else {
					wrapper.orderByDesc(Comment::getStatus);
				}
			}
			case "enable" -> {
				if (asc) {
					wrapper.orderByAsc(Comment::getEnable);
				} else {
					wrapper.orderByDesc(Comment::getEnable);
				}
			}
			case "likes" -> {
				if (asc) {
					wrapper.orderByAsc(Comment::getLikes);
				} else {
					wrapper.orderByDesc(Comment::getLikes);
				}
			}
			default -> throw new BadRequestException("sort 参数格式无效");
		}
	}
}
