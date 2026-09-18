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

package cn.shopex.ecshopx.form.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.form.domain.UserTranscripts;
import cn.shopex.ecshopx.form.mapper.UserTranscriptsMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserTranscriptCreateService {

	private final UserTranscriptsMapper userTranscriptsMapper;
	private final ObjectMapper objectMapper;

	public UserTranscriptCreateService(UserTranscriptsMapper userTranscriptsMapper, ObjectMapper objectMapper) {
		this.userTranscriptsMapper = userTranscriptsMapper;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> create(long companyId, Map<String, Object> params) {
		UserTranscripts entity = new UserTranscripts();
		entity.setUserId(toLong(params.get("user_id")));
		entity.setCompanyId(companyId);
		entity.setShopId(shopIdFromParams(params));
		entity.setTranscriptId(toLong(params.get("transcript_id")));
		entity.setTranscriptName(String.valueOf(params.get("transcript_name")));
		entity.setIndicatorDetails(indicatorDetailsForDb(params.get("indicator_details")));

		int nowSec = (int) Instant.now().getEpochSecond();
		entity.setCreated(nowSec);
		entity.setUpdated(nowSec);

		userTranscriptsMapper.insert(entity);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("record_id", entity.getRecordId());
		result.put("user_id", entity.getUserId());
		result.put("company_id", entity.getCompanyId());
		result.put("shop_id", entity.getShopId());
		result.put("transcript_id", entity.getTranscriptId());
		result.put("transcript_name", entity.getTranscriptName());
		result.put("indicator_details", indicatorDetailsForResponse(entity.getIndicatorDetails()));
		return result;
	}

	private String indicatorDetailsForDb(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String s) {
			return s;
		}
		if (raw instanceof Map<?, ?> || raw instanceof List<?>) {
			try {
				return objectMapper.writeValueAsString(raw);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("指标详情格式错误");
			}
		}
		try {
			return objectMapper.writeValueAsString(raw);
		} catch (JsonProcessingException e) {
			throw new BadRequestException("指标详情格式错误");
		}
	}

	private Object indicatorDetailsForResponse(String stored) {
		if (stored == null) {
			return null;
		}
		if (!StringUtils.hasText(stored)) {
			return stored;
		}
		try {
			return objectMapper.readValue(stored, Object.class);
		} catch (JsonProcessingException e) {
			return stored;
		}
	}

	private static Long shopIdFromParams(Map<String, Object> params) {
		if (!params.containsKey("shop_id")) {
			return null;
		}
		Object v = params.get("shop_id");
		if (v == null) {
			return null;
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		return toLong(v);
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
