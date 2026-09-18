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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.wsugc.domain.Message;
import cn.shopex.ecshopx.wsugc.domain.Post;
import cn.shopex.ecshopx.wsugc.mapper.MessageMapper;
import cn.shopex.ecshopx.wsugc.mapper.PostMapper;
import cn.shopex.ecshopx.wsugc.service.point.UgcPostPointService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
public class PostVerifyService {

	private static final Logger log = LoggerFactory.getLogger(PostVerifyService.class);

	private final PostMapper postMapper;
	private final MessageMapper messageMapper;
	private final UgcPostPointService ugcPostPointService;
	private final MemberAccountService memberAccountService;

	public PostVerifyService(
			PostMapper postMapper,
			MessageMapper messageMapper,
			UgcPostPointService ugcPostPointService,
			MemberAccountService memberAccountService) {
		this.postMapper = postMapper;
		this.messageMapper = messageMapper;
		this.ugcPostPointService = ugcPostPointService;
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

		List<Long> postIds = normalizePostIds(input.get("post_id"));
		if (postIds.isEmpty()) {
			throw new BadRequestException("post_id参数不能为空");
		}

		String manual = input.containsKey("refuse_reason") && input.get("refuse_reason") != null
				? input.get("refuse_reason").toString()
				: "";

		int now = (int) (System.currentTimeMillis() / 1000);
		LambdaUpdateWrapper<Post> uw = new LambdaUpdateWrapper<>();
		uw.in(Post::getPostId, postIds)
				.set(Post::getStatus, status)
				.set(Post::getManualRefuseReason, manual)
				.set(Post::getUpdated, now);
		postMapper.update(null, uw);

		Map<String, Object> dataMap = new LinkedHashMap<>();
		dataMap.put("status", status);
		dataMap.put("manual_refuse_reason", manual);
		log.debug("verifyPost: {}", dataMap);

		if (!postIds.isEmpty()) {
			if (status == 4) {
				for (Long id : postIds) {
					Post post = postMapper.selectById(id);
					if (post == null) {
						log.debug("verifyPost selectById miss post_id={}", id);
						continue;
					}
					try {
						ugcPostPointService.addUgcPoint(
								post.getPostId(), post.getUserId(), post.getCompanyId(), 9920, "reduce");
					} catch (Exception e) {
						log.debug("addUgcPoint 拒绝...", e);
					}
					try {
						Message msg = new Message();
						msg.setType("system");
						msg.setSubType("refusePost");
						msg.setSource(2);
						msg.setPostId(id);
						msg.setCommentId(0L);
						msg.setCompanyId(post.getCompanyId());
						msg.setFromUserId(operatorId);
						msg.setFromNickname(fromNickname);
						msg.setToUserId(post.getUserId());
						msg.setToNickname(getNickName(post.getUserId(), post.getCompanyId()));
						msg.setTitle("您的笔记包含违规内容,他人将不可见");
						msg.setContent(manual);
						msg.setCreated(now);
						msg.setUpdated(now);
						msg.setHasRead(false);
						messageMapper.insert(msg);
					} catch (Exception e) {
						log.debug("发送评论消息 失败...", e);
					}
				}
			} else if (status == 1) {
				for (Long id : postIds) {
					Post post = postMapper.selectById(id);
					if (post == null) {
						log.debug("verifyPost selectById miss post_id={}", id);
						continue;
					}
					try {
						ugcPostPointService.addUgcPoint(
								post.getPostId(), post.getUserId(), post.getCompanyId(), 20, "");
					} catch (Exception e) {
						log.debug("addUgcPoint 拒绝...", e);
					}
				}
			}
		}

		LinkedHashMap<String, Object> res = new LinkedHashMap<>();
		res.put("post_id", postIds);
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

	private static List<Long> normalizePostIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<Long> out = new ArrayList<>();
			for (Object o : c) {
				out.add(parsePostIdElement(o));
			}
			return out;
		}
		if (raw instanceof Object[] a) {
			List<Long> out = new ArrayList<>();
			for (Object o : a) {
				out.add(parsePostIdElement(o));
			}
			return out;
		}
		if (raw instanceof int[] a) {
			List<Long> out = new ArrayList<>();
			for (int j : a) {
				out.add((long) j);
			}
			return out;
		}
		if (raw instanceof long[] a) {
			List<Long> out = new ArrayList<>();
			for (long j : a) {
				out.add(j);
			}
			return out;
		}
		return List.of(parsePostIdElement(raw));
	}

	private static long parsePostIdElement(Object o) {
		if (o == null) {
			throw new BadRequestException("post_id 格式无效");
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("post_id 格式无效");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("post_id 格式无效");
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

	private static String readString(Object v, String defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		return v.toString();
	}
}
