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
import cn.shopex.ecshopx.wsugc.domain.Message;
import cn.shopex.ecshopx.wsugc.mapper.CommentMapper;
import cn.shopex.ecshopx.wsugc.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommentVerifyService {

	private static final Logger log = LoggerFactory.getLogger(CommentVerifyService.class);

	private final CommentMapper commentMapper;
	private final MessageMapper messageMapper;
	private final MemberAccountService memberAccountService;

	public CommentVerifyService(
			CommentMapper commentMapper,
			MessageMapper messageMapper,
			MemberAccountService memberAccountService) {
		this.commentMapper = commentMapper;
		this.messageMapper = messageMapper;
		this.memberAccountService = memberAccountService;
	}

	public Map<String, Object> verify(
			Map<String, Object> input,
			Map<String, Object> operatorJwtOrEmpty,
			boolean localProfile) {
		long operatorId = 0L;
		if (readLong(operatorJwtOrEmpty.get("operator_id"), 0L) != 0L) {
			operatorId = readLong(operatorJwtOrEmpty.get("operator_id"), 0L);
		} else if (localProfile && input.containsKey("user_id")) {
			operatorId = readLong(input.get("user_id"), 0L);
		}

		String usernameRaw = readString(operatorJwtOrEmpty.get("username"), null);
		String fromNickname = StringUtils.hasText(usernameRaw) ? usernameRaw : "系统管理员";

		if (!input.containsKey("status") || input.get("status") == null) {
			throw new BadRequestException("status参数不能为空");
		}
		int status = parseStatusRequired(input.get("status"));
		if (status == 0) {
			throw new BadRequestException("status参数不能为空");
		}

		List<Object> commentIds = normalizeCommentIdValues(input.get("comment_id"));
		if (commentIds.isEmpty()) {
			throw new BadRequestException("comment_id参数不能为空");
		}

		String manual = input.containsKey("refuse_reason") && input.get("refuse_reason") != null
				? input.get("refuse_reason").toString()
				: "";

		int now = (int) (System.currentTimeMillis() / 1000);
		UpdateWrapper<Comment> uw = new UpdateWrapper<>();
		uw.in("comment_id", commentIds)
				.set("status", status)
				.set("manual_refuse_reason", manual)
				.set("updated", now);
		commentMapper.update(null, uw);

		if (status == 4) {
			for (Object idVal : commentIds) {
				Long commentId = commentIdForSelect(idVal);
				Comment c = commentId != null ? commentMapper.selectById(commentId) : null;
				if (c == null) {
					log.debug("verifyComment selectById miss comment_id={}", idVal);
					continue;
				}
				try {
					Message msg = new Message();
					msg.setType("system");
					msg.setSubType("refuseComment");
					msg.setSource(2);
					msg.setPostId(c.getPostId());
					msg.setCommentId(commentId);
					msg.setCompanyId(c.getCompanyId());
					msg.setFromUserId(operatorId);
					msg.setFromNickname(fromNickname);
					msg.setToUserId(c.getUserId());
					msg.setToNickname(getNickName(c.getUserId(), c.getCompanyId()));
					msg.setTitle("您的评论包含违规内容,他人将不可见");
					msg.setContent(manual);
					msg.setCreated(now);
					msg.setUpdated(now);
					msg.setHasRead(false);
					messageMapper.insert(msg);
				} catch (Exception e) {
					log.debug("发送评论消息 失败...", e);
				}
			}
		}

		LinkedHashMap<String, Object> res = new LinkedHashMap<>();
		res.put("comment_id", new ArrayList<>(commentIds));
		res.put("status", status);
		res.put(
				"refuse_reason",
				input.containsKey("refuse_reason") ? input.get("refuse_reason") : null);
		res.put("manual_refuse_reason", manual);
		res.put("message", "审核成功");
		return res;
	}

	private String getNickName(long userId, long companyId) {
		Map<String, Object> filter = new HashMap<>();
		filter.put("user_id", userId);
		filter.put("company_id", companyId);
		Map<String, Object> info = memberAccountService.getWechatUserInfo(filter);
		Object n = info.get("nickname");
		if (n != null && StringUtils.hasText(n.toString())) {
			return n.toString();
		}
		return "-";
	}

	private static int parseStatusRequired(Object v) {
		if (v instanceof Number n) {
			double dv = n.doubleValue();
			long lv = n.longValue();
			if (Math.abs(dv - lv) > 1e-10) {
				throw new BadRequestException("status参数格式无效");
			}
			if (lv > Integer.MAX_VALUE || lv < Integer.MIN_VALUE) {
				throw new BadRequestException("status参数格式无效");
			}
			return (int) lv;
		}
		if (v instanceof Boolean) {
			throw new BadRequestException("status参数格式无效");
		}
		try {
			String s = v.toString().trim();
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("status参数格式无效");
		}
	}

	/** 可解析为整数的元素规范为 Long；否则保留字符串以参与 IN 并在成功响应中原样返回。 */
	private static List<Object> normalizeCommentIdValues(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<Object> out = new ArrayList<>();
			for (Object o : c) {
				out.add(coerceCommentIdElement(o));
			}
			return out;
		}
		if (raw instanceof Object[] a) {
			List<Object> out = new ArrayList<>();
			for (Object o : a) {
				out.add(coerceCommentIdElement(o));
			}
			return out;
		}
		if (raw instanceof int[] a) {
			List<Object> out = new ArrayList<>();
			for (int j : a) {
				out.add((long) j);
			}
			return out;
		}
		if (raw instanceof long[] a) {
			List<Object> out = new ArrayList<>();
			for (long j : a) {
				out.add(j);
			}
			return out;
		}
		return List.of(coerceCommentIdElement(raw));
	}

	private static Object coerceCommentIdElement(Object o) {
		if (o == null) {
			throw new BadRequestException("comment_id 格式无效");
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof Boolean) {
			throw new BadRequestException("comment_id 格式无效");
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("comment_id 格式无效");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return s;
		}
	}

	private static Long commentIdForSelect(Object idVal) {
		if (idVal instanceof Long l) {
			return l;
		}
		if (idVal instanceof String str) {
			try {
				return Long.parseLong(str);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
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

	private static String readString(Object v, String defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		return v.toString();
	}
}
