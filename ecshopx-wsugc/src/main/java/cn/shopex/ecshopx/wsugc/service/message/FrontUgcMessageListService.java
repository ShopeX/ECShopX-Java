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

package cn.shopex.ecshopx.wsugc.service.message;

import cn.shopex.ecshopx.wsugc.domain.Message;
import cn.shopex.ecshopx.wsugc.mapper.MessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrontUgcMessageListService {

	private final MessageMapper messageMapper;
	private final FrontUgcMessageDetailService frontUgcMessageDetailService;

	public FrontUgcMessageListService(
			MessageMapper messageMapper, FrontUgcMessageDetailService frontUgcMessageDetailService) {
		this.messageMapper = messageMapper;
		this.frontUgcMessageDetailService = frontUgcMessageDetailService;
	}

	public Object buildMessageList(
			long companyId,
			long toUserId,
			String typeFilter,
			int page,
			int pageSize,
			String sortRaw,
			String langTag) {
		LambdaQueryWrapper<Message> w = baseWrapper(companyId, toUserId, typeFilter);
		applySort(w, sortRaw);

		Long totalLong = messageMapper.selectCount(w);
		long total = totalLong == null ? 0L : totalLong;
		if (total == 0L) {
			return Collections.emptyList();
		}

		Page<Message> p = new Page<>(page, pageSize, false);
		messageMapper.selectPage(p, w);
		List<Message> records = p.getRecords() != null ? p.getRecords() : List.of();
		if (records.isEmpty()) {
			return Collections.emptyList();
		}

		String listFilterType = typeFilter;
		List<Map<String, Object>> list = new ArrayList<>();
		for (Message entity : records) {
			Map<String, Object> row =
					frontUgcMessageDetailService.formatListRow(entity, listFilterType, langTag);
			list.add(new LinkedHashMap<>(new TreeMap<>(row)));
		}

		Map<String, Object> top = new LinkedHashMap<>();
		top.put("list", list);
		top.put("total_count", total);
		return new LinkedHashMap<>(new TreeMap<>(top));
	}

	private static LambdaQueryWrapper<Message> baseWrapper(
			long companyId, long toUserId, String typeFilter) {
		LambdaQueryWrapper<Message> w = new LambdaQueryWrapper<>();
		w.eq(Message::getCompanyId, companyId).eq(Message::getToUserId, toUserId);
		if (typeFilter != null) {
			w.eq(Message::getType, typeFilter);
		}
		return w;
	}

	private static void applySort(LambdaQueryWrapper<Message> w, String sortRaw) {
		if (sortRaw == null || !StringUtils.hasText(sortRaw.trim())) {
			w.orderByDesc(Message::getCreated);
			return;
		}
		String s = sortRaw.trim();
		String[] tokens = s.split(" ", -1);
		String fieldToken = tokens.length > 0 ? tokens[0] : "";
		String dirToken = tokens.length > 1 ? tokens[1] : null;

		if (!StringUtils.hasText(fieldToken)) {
			w.orderByDesc(Message::getCreated);
			return;
		}

		String field = whitelistField(fieldToken.trim());
		String dir = resolveSortDir(dirToken);

		if (field == null) {
			w.orderByDesc(Message::getCreated);
			return;
		}
		if ("created".equals(field)) {
			w.orderByDesc(Message::getCreated);
			return;
		}
		if (dir == null) {
			orderByWhitelisted(w, field, true);
		} else if ("asc".equals(dir)) {
			orderByWhitelisted(w, field, true);
		} else {
			orderByWhitelisted(w, field, false);
		}
		w.orderByDesc(Message::getCreated);
	}

	private static String resolveSortDir(String dirToken) {
		if (dirToken == null) {
			return null;
		}
		String t = dirToken.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		if ("asc".equalsIgnoreCase(t)) {
			return "asc";
		}
		if ("desc".equalsIgnoreCase(t)) {
			return "desc";
		}
		return null;
	}

	private static String whitelistField(String token) {
		return switch (token) {
			case "created", "updated", "message_id" -> token;
			default -> null;
		};
	}

	private static void orderByWhitelisted(LambdaQueryWrapper<Message> w, String field, boolean asc) {
		switch (field) {
			case "message_id" -> {
				if (asc) {
					w.orderByAsc(Message::getMessageId);
				} else {
					w.orderByDesc(Message::getMessageId);
				}
			}
			case "updated" -> {
				if (asc) {
					w.orderByAsc(Message::getUpdated);
				} else {
					w.orderByDesc(Message::getUpdated);
				}
			}
			case "created" -> {
				if (asc) {
					w.orderByAsc(Message::getCreated);
				} else {
					w.orderByDesc(Message::getCreated);
				}
			}
			default -> {
				// unreachable for whitelisted names
			}
		}
	}
}
