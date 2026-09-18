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

package cn.shopex.ecshopx.systemlink.service.omsqueuelog;

import cn.shopex.ecshopx.systemlink.domain.OmsQueueLog;
import cn.shopex.ecshopx.systemlink.mapper.OmsQueueLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OmsQueueLogAdminService {

	private static final DateTimeFormatter UPDATED_RANGE_DATETIME =
			DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

	private final OmsQueueLogMapper omsQueueLogMapper;
	private final ObjectMapper objectMapper;

	public OmsQueueLogAdminService(OmsQueueLogMapper omsQueueLogMapper, ObjectMapper objectMapper) {
		this.omsQueueLogMapper = omsQueueLogMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getLogList(
			long companyId,
			int page,
			int pageSize,
			String apiType,
			String status,
			String content,
			List<String> updated) {
		LambdaQueryWrapper<OmsQueueLog> w = new LambdaQueryWrapper<>();
		w.eq(OmsQueueLog::getCompanyId, companyId);
		if (StringUtils.hasText(apiType)) {
			w.eq(OmsQueueLog::getApiType, apiType);
		}
		if (StringUtils.hasText(status)) {
			w.eq(OmsQueueLog::getStatus, status);
		}
		if (StringUtils.hasText(content)) {
			w.like(OmsQueueLog::getParams, content);
		}
		if (updated != null && !updated.isEmpty()) {
			String startDate = trimOrEmpty(updated, 0);
			String endDate = trimOrEmpty(updated, 1);
			int gteSec = parseUpdatedRangeBoundarySeconds(startDate, false);
			int lteSec = parseUpdatedRangeBoundarySeconds(endDate, true);
			w.ge(OmsQueueLog::getUpdated, gteSec);
			w.le(OmsQueueLog::getUpdated, lteSec);
		}
		w.orderByDesc(OmsQueueLog::getCreated);

		long total = omsQueueLogMapper.selectCount(w);
		List<Map<String, Object>> rows = Collections.emptyList();
		if (total > 0) {
			if (pageSize > 0) {
				Page<OmsQueueLog> p = new Page<>(page, pageSize, false);
				omsQueueLogMapper.selectPage(p, w);
				rows = p.getRecords().stream().map(this::toRowMap).toList();
			} else {
				List<OmsQueueLog> list = omsQueueLogMapper.selectList(w);
				rows = list.stream().map(this::toRowMap).toList();
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", rows);
		return out;
	}

	private static String trimOrEmpty(List<String> updated, int index) {
		if (index >= updated.size()) {
			return "";
		}
		String s = updated.get(index);
		return s == null ? "" : s.trim();
	}

	/**
	 * Lenient boundary: empty or unparseable or int-overflow seconds → 0 (invalid timestamps collapse the filter).
	 */
	private static int parseUpdatedRangeBoundarySeconds(String datePart, boolean endOfDay) {
		if (!StringUtils.hasText(datePart)) {
			return 0;
		}
		String datetime = endOfDay ? datePart + " 23:59:59" : datePart + " 00:00:00";
		try {
			LocalDateTime ldt = LocalDateTime.parse(datetime, UPDATED_RANGE_DATETIME);
			long sec = ldt.atZone(ZoneId.systemDefault()).toEpochSecond();
			if (sec < Integer.MIN_VALUE || sec > Integer.MAX_VALUE) {
				return 0;
			}
			return (int) sec;
		} catch (DateTimeParseException e) {
			return 0;
		}
	}

	private Map<String, Object> toRowMap(OmsQueueLog entity) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", entity.getId());
		row.put("company_id", entity.getCompanyId());
		row.put("api_type", entity.getApiType());
		row.put("worker", entity.getWorker());
		row.put("params", safeJsonToMapOrNull(entity.getParams()));
		row.put("result", safeJsonToMapOrNull(entity.getResult()));
		row.put("status", entity.getStatus());
		row.put("runtime", entity.getRuntime());
		row.put("msg_id", entity.getMsgId());
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		row.put("created_date", formatEpochSecondsOrNull(entity.getCreated()));
		row.put("updated_date", formatEpochSecondsOrNull(entity.getUpdated()));
		return row;
	}

	private Map<String, Object> safeJsonToMapOrNull(String s) {
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return objectMapper.readValue(s, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private static String formatEpochSecondsOrNull(Integer epochSeconds) {
		int sec = epochSeconds == null ? 0 : epochSeconds;
		return Instant.ofEpochSecond(sec)
				.atZone(ZoneId.systemDefault())
				.format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss"));
	}
}
