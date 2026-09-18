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

package cn.shopex.ecshopx.wsugc.service.topic;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Topic;
import cn.shopex.ecshopx.wsugc.mapper.TopicMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TopicVerifyService {

	private final TopicMapper topicMapper;

	public TopicVerifyService(TopicMapper topicMapper) {
		this.topicMapper = topicMapper;
	}

	@SuppressWarnings("unused")
	public Map<String, Object> verify(
			Map<String, Object> input,
			Map<String, Object> operatorJwtOrEmpty,
			boolean localProfile) {
		if (!input.containsKey("status") || isStatusOuterFalsy(input.get("status"))) {
			throw new ResourceException("status参数不能为空");
		}
		int status = parseStatusRequired(input.get("status"));
		if (status == 0) {
			throw new ResourceException("status参数不能为空");
		}

		if (!input.containsKey("topic_id") || isTopicIdOuterFalsy(input.get("topic_id"))) {
			throw new ResourceException("topic_id参数不能为空");
		}
		Object rawTopicId = input.get("topic_id");
		List<Long> topicIds = normalizeTopicIds(rawTopicId);

		String manual =
				input.containsKey("refuse_reason") && input.get("refuse_reason") != null
						? input.get("refuse_reason").toString()
						: "";

		long nowSec = System.currentTimeMillis() / 1000L;
		LambdaUpdateWrapper<Topic> uw = new LambdaUpdateWrapper<>();
		uw.in(Topic::getTopicId, topicIds)
				.set(Topic::getStatus, status)
				.set(Topic::getManualRefuseReason, manual)
				.set(Topic::getManualVerifyTime, nowSec);
		topicMapper.update(null, uw);

		Object topicIdEcho;
		if (rawTopicId instanceof Collection<?>
				|| rawTopicId instanceof Object[]
				|| rawTopicId instanceof int[]
				|| rawTopicId instanceof long[]) {
			topicIdEcho = topicIds;
		} else {
			topicIdEcho = topicIds.get(0);
		}

		LinkedHashMap<String, Object> res = new LinkedHashMap<>();
		res.put("topic_id", topicIdEcho);
		res.put("status", input.get("status"));
		res.put(
				"refuse_reason",
				input.containsKey("refuse_reason") ? input.get("refuse_reason") : null);
		res.put("manual_refuse_reason", manual);
		res.put("manual_verify_time", nowSec);
		res.put("message", "审核成功");
		return res;
	}

	private static boolean isStatusOuterFalsy(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			double dv = n.doubleValue();
			long lv = n.longValue();
			if (Math.abs(dv - lv) > 1e-10) {
				return false;
			}
			return lv == 0;
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return true;
			}
			return "0".equals(t);
		}
		return false;
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

	private static boolean isTopicIdOuterFalsy(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw == Boolean.FALSE) {
			return true;
		}
		if (raw instanceof String s) {
			return !StringUtils.hasText(s.trim());
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0;
		}
		if (raw instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (raw instanceof Object[] a) {
			return a.length == 0;
		}
		if (raw instanceof int[] a) {
			return a.length == 0;
		}
		if (raw instanceof long[] a) {
			return a.length == 0;
		}
		return false;
	}

	private static List<Long> normalizeTopicIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<Long> out = new ArrayList<>();
			for (Object o : c) {
				out.add(parseTopicIdElement(o));
			}
			return out;
		}
		if (raw instanceof Object[] a) {
			List<Long> out = new ArrayList<>();
			for (Object o : a) {
				out.add(parseTopicIdElement(o));
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
		return List.of(parseTopicIdElement(raw));
	}

	private static long parseTopicIdElement(Object o) {
		if (o == null) {
			throw new BadRequestException("topic_id 格式无效");
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("topic_id 格式无效");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("topic_id 格式无效");
		}
	}
}
