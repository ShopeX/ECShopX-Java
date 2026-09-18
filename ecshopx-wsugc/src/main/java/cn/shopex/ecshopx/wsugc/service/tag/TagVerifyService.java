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

package cn.shopex.ecshopx.wsugc.service.tag;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wsugc.domain.Tag;
import cn.shopex.ecshopx.wsugc.mapper.TagMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TagVerifyService {

	private final TagMapper tagMapper;

	public TagVerifyService(TagMapper tagMapper) {
		this.tagMapper = tagMapper;
	}

	@SuppressWarnings("unused")
	public Map<String, Object> verify(
			Map<String, Object> input,
			Map<String, Object> operatorJwtOrEmpty,
			boolean localProfile) {
		if (!input.containsKey("status") || input.get("status") == null) {
			throw new BadRequestException("status参数不能为空");
		}
		int status = parseStatusRequired(input.get("status"));
		// 数值 0 不是有效的审核结果，与未传 status 同等拒绝。
		if (status == 0) {
			throw new BadRequestException("status参数不能为空");
		}

		Object rawTagId = input.get("tag_id");
		List<Long> tagIds = normalizeTagIds(rawTagId);
		if (tagIds.isEmpty()) {
			throw new BadRequestException("tag_id参数不能为空");
		}

		String manual = input.containsKey("refuse_reason") && input.get("refuse_reason") != null
				? input.get("refuse_reason").toString()
				: "";

		int now = (int) (System.currentTimeMillis() / 1000);
		LambdaUpdateWrapper<Tag> uw = new LambdaUpdateWrapper<>();
		uw.in(Tag::getTagId, tagIds)
				.set(Tag::getStatus, status)
				.set(Tag::getManualRefuseReason, manual)
				.set(Tag::getManualVerifyTime, (long) now);
		tagMapper.update(null, uw);

		Object tagIdEcho;
		if (rawTagId instanceof Collection<?>
				|| rawTagId instanceof Object[]
				|| rawTagId instanceof int[]
				|| rawTagId instanceof long[]) {
			tagIdEcho = tagIds;
		} else {
			tagIdEcho = tagIds.get(0);
		}

		LinkedHashMap<String, Object> res = new LinkedHashMap<>();
		res.put("tag_id", tagIdEcho);
		res.put("status", input.get("status"));
		res.put(
				"refuse_reason",
				input.containsKey("refuse_reason") ? input.get("refuse_reason") : null);
		res.put("manual_refuse_reason", manual);
		res.put("message", "审核成功");
		return res;
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

	private static List<Long> normalizeTagIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof Collection<?> c) {
			List<Long> out = new ArrayList<>();
			for (Object o : c) {
				out.add(parseTagIdElement(o));
			}
			return out;
		}
		if (raw instanceof Object[] a) {
			List<Long> out = new ArrayList<>();
			for (Object o : a) {
				out.add(parseTagIdElement(o));
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
		return List.of(parseTagIdElement(raw));
	}

	private static long parseTagIdElement(Object o) {
		if (o == null) {
			throw new BadRequestException("tag_id 格式无效");
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("tag_id 格式无效");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("tag_id 格式无效");
		}
	}
}
