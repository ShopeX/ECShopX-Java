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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.datacube.domain.Sources;
import cn.shopex.ecshopx.datacube.mapper.SourcesMapper;
import cn.shopex.ecshopx.members.service.MemberRelTagsBatchCreateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CreateMemberSuccessRegisterNumStatsExecutionService {

	private static final Logger log = LoggerFactory.getLogger(CreateMemberSuccessRegisterNumStatsExecutionService.class);

	private static final String REDIS_PREFIX = "datecube_tracklog";

	private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;

	private final StringRedisTemplate datacubeRedis;
	private final SourcesMapper sourcesMapper;
	private final MemberRelTagsBatchCreateService memberRelTagsBatchCreateService;
	private final ObjectMapper objectMapper;

	public CreateMemberSuccessRegisterNumStatsExecutionService(
			@Qualifier("datacubeStringRedisTemplate") StringRedisTemplate datacubeRedis,
			SourcesMapper sourcesMapper,
			MemberRelTagsBatchCreateService memberRelTagsBatchCreateService,
			ObjectMapper objectMapper) {
		this.datacubeRedis = datacubeRedis;
		this.sourcesMapper = sourcesMapper;
		this.memberRelTagsBatchCreateService = memberRelTagsBatchCreateService;
		this.objectMapper = objectMapper;
	}

	public void apply(Map<String, Object> payload) {
		try {
			incrementRegisterStats(payload);
		} catch (RuntimeException e) {
			log.debug("CreateMemberSuccess register stats Redis increment failed", e);
		}
		try {
			applySourceTagsIfNeeded(payload);
		} catch (RuntimeException e) {
			log.debug("CreateMemberSuccess source tags attach failed", e);
		}
	}

	private void incrementRegisterStats(Map<String, Object> payload) {
		String companyStr = String.valueOf(longFrom(payload.get("company_id")));
		String monitorStr = String.valueOf(longFrom(payload.get("monitor_id")));
		String sourceStr = String.valueOf(longFrom(payload.get("source_id")));
		String field = LocalDate.now(ZoneId.systemDefault()).format(YMD);
		String listKey = registerListKey(companyStr, monitorStr, sourceStr);
		String totalKey = registerTotalKey(companyStr, monitorStr);
		datacubeRedis.opsForHash().increment(listKey, field, 1L);
		datacubeRedis.opsForHash().increment(totalKey, field, 1L);
	}

	private void applySourceTagsIfNeeded(Map<String, Object> payload) {
		long sourceId = longFrom(payload.get("source_id"));
		if (sourceId == 0L) {
			return;
		}
		long userId = longFrom(payload.get("user_id"));
		long companyId = longFrom(payload.get("company_id"));
		Sources src = sourcesMapper.selectById(sourceId);
		if (src == null) {
			return;
		}
		List<Long> tagIds = parseTagIdsFromStored(src.getTagsId());
		if (tagIds.isEmpty()) {
			return;
		}
		memberRelTagsBatchCreateService.createRelTagsByUserId(userId, tagIds, companyId);
	}

	private static String registerListKey(String companyId, String monitorId, String sourceId) {
		return REDIS_PREFIX + ":registernum:" + companyId + "|" + monitorId + ":list:" + sourceId;
	}

	private static String registerTotalKey(String companyId, String monitorId) {
		return REDIS_PREFIX + ":registernum:" + companyId + "|" + monitorId + ":total";
	}

	private List<Long> parseTagIdsFromStored(String stored) {
		if (!StringUtils.hasText(stored)) {
			return List.of();
		}
		Object decoded;
		try {
			decoded = objectMapper.readValue(stored.trim(), Object.class);
		} catch (JsonProcessingException e) {
			return List.of();
		}
		return tagIdsFromDecoded(decoded);
	}

	private static List<Long> tagIdsFromDecoded(Object decoded) {
		List<Long> out = new ArrayList<>();
		if (decoded == null) {
			return out;
		}
		if (decoded instanceof Collection<?> c) {
			for (Object el : c) {
				Long id = elementToTagId(el);
				if (id != null) {
					out.add(id);
				}
			}
			return out;
		}
		if (decoded instanceof Number n) {
			out.add(n.longValue());
			return out;
		}
		if (decoded instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return out;
			}
			try {
				out.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
			}
			return out;
		}
		return out;
	}

	private static Long elementToTagId(Object el) {
		if (el == null) {
			return null;
		}
		if (el instanceof Number n) {
			return n.longValue();
		}
		if (el instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return null;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		try {
			return Long.parseLong(String.valueOf(el).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long longFrom(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
