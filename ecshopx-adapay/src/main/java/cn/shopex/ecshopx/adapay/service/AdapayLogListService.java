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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapayOperationLog;
import cn.shopex.ecshopx.adapay.mapper.AdapayOperationLogMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AdapayLogListService {

	private static final Set<String> SOURCE_TYPES = Set.of("merchant", "distributor", "dealer");
	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
	private static final DateTimeFormatter CREATE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final AdapayOperationLogMapper adapayOperationLogMapper;

	public AdapayLogListService(AdapayOperationLogMapper adapayOperationLogMapper) {
		this.adapayOperationLogMapper = adapayOperationLogMapper;
	}

	public Map<String, Object> getList(long companyId, String logTypeNormalized, int page, int pageSize, String relId) {
		if (!SOURCE_TYPES.contains(logTypeNormalized)) {
			throw new BadRequestException("log source type error");
		}

		LambdaQueryWrapper<AdapayOperationLog> countWrapper = buildWhereWrapper(companyId, logTypeNormalized, relId);
		Long totalObj = adapayOperationLogMapper.selectCount(countWrapper);
		long total = totalObj == null ? 0L : totalObj.longValue();

		if (total == 0L) {
			LinkedHashMap<String, Object> body = new LinkedHashMap<>();
			body.put("total_count", 0L);
			body.put("list", List.of());
			return body;
		}

		LambdaQueryWrapper<AdapayOperationLog> listWrapper = buildWhereWrapper(companyId, logTypeNormalized, relId);
		listWrapper.select(AdapayOperationLog::getContent, AdapayOperationLog::getCreateTime);
		listWrapper.orderByDesc(AdapayOperationLog::getCreateTime);
		Page<AdapayOperationLog> p = new Page<>(page, pageSize, false);
		List<AdapayOperationLog> records = adapayOperationLogMapper.selectPage(p, listWrapper).getRecords();

		List<Map<String, Object>> listOut = new ArrayList<>();
		for (AdapayOperationLog rec : records) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			String content = rec.getContent();
			row.put("content", content == null ? "" : content);
			Integer ct = rec.getCreateTime();
			row.put("create_time", ct);
			if (ct == null) {
				row.put("create_date", "");
			} else {
				row.put(
						"create_date",
						ZonedDateTime.ofInstant(Instant.ofEpochSecond(ct.longValue()), SHANGHAI).format(CREATE_DATE));
			}
			listOut.add(row);
		}

		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("total_count", total);
		body.put("list", listOut);
		return body;
	}

	private LambdaQueryWrapper<AdapayOperationLog> buildWhereWrapper(long companyId, String logTypeNormalized, String relId) {
		LambdaQueryWrapper<AdapayOperationLog> w = new LambdaQueryWrapper<>();
		w.eq(AdapayOperationLog::getCompanyId, companyId);
		w.eq(AdapayOperationLog::getLogType, logTypeNormalized);
		applyRelIdEq(w, relId);
		return w;
	}

	private void applyRelIdEq(LambdaQueryWrapper<AdapayOperationLog> base, String relId) {
		if (relId == null) {
			return;
		}
		String t = relId.trim();
		if (t.isEmpty() || "0".equals(t)) {
			return;
		}
		if (!t.matches("-?\\d+")) {
			throw new BadRequestException("rel_id 格式错误", 400);
		}
		long parsed;
		try {
			parsed = Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new BadRequestException("rel_id 格式错误", 400);
		}
		if (parsed == 0L) {
			return;
		}
		base.eq(AdapayOperationLog::getRelId, parsed);
	}
}
