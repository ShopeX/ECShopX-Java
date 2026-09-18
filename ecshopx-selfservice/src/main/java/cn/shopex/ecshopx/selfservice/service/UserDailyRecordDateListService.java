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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.selfservice.mapper.UserDailyRecordMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserDailyRecordDateListService {

	private final UserDailyRecordPhysicalSettingService userDailyRecordPhysicalSettingService;
	private final UserDailyRecordMapper userDailyRecordMapper;

	public UserDailyRecordDateListService(
			UserDailyRecordPhysicalSettingService userDailyRecordPhysicalSettingService,
			UserDailyRecordMapper userDailyRecordMapper) {
		this.userDailyRecordPhysicalSettingService = userDailyRecordPhysicalSettingService;
		this.userDailyRecordMapper = userDailyRecordMapper;
	}

	public Map<String, Object> getRecordDateList(
			long companyId, String formType, Long userId, int page, int pageSize) {
		long tempId = userDailyRecordPhysicalSettingService.getTempIdForRead(companyId, formType);
		Long companyIdParam = companyId;
		Long tempIdParam = tempId;
		long total = userDailyRecordMapper.countDistinctRecordDates(companyIdParam, tempIdParam, userId);
		Map<String, Object> result = new LinkedHashMap<>();
		if (total == 0L) {
			result.put("list", new ArrayList<>());
			result.put("total_count", 0L);
			return result;
		}
		Long offset;
		Long limit;
		if (pageSize > 0) {
			offset = (long) pageSize * (page - 1);
			limit = (long) pageSize;
		} else {
			offset = null;
			limit = null;
		}
		List<Integer> recordDates =
				userDailyRecordMapper.selectDistinctRecordDatePage(companyIdParam, tempIdParam, userId, offset, limit);
		List<Map<String, Object>> items = new ArrayList<>();
		for (Integer recordDate : recordDates) {
			LinkedHashMap<String, Object> item = new LinkedHashMap<>();
			item.put("record_date", recordDate);
			item.put("record_date_str", formatRecordDateStr(recordDate));
			items.add(item);
		}
		result.put("list", items);
		result.put("total_count", total);
		return result;
	}

	private static String formatRecordDateStr(Integer recordDate) {
		if (recordDate == null) {
			return "";
		}
		String eight = String.format("%08d", recordDate);
		try {
			LocalDate d = LocalDate.parse(eight, DateTimeFormatter.BASIC_ISO_DATE);
			return d.format(DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (DateTimeParseException ex) {
			throw new ResourceException("record_date 数据异常");
		}
	}
}
