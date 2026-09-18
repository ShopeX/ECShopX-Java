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

import cn.shopex.ecshopx.selfservice.domain.UserDailyRecord;
import cn.shopex.ecshopx.selfservice.mapper.UserDailyRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class UserDailyRecordPersonalRecordService {

	private final UserDailyRecordPhysicalSettingService userDailyRecordPhysicalSettingService;
	private final UserDailyRecordMapper userDailyRecordMapper;
	private final ObjectMapper objectMapper;

	public UserDailyRecordPersonalRecordService(
			UserDailyRecordPhysicalSettingService userDailyRecordPhysicalSettingService,
			UserDailyRecordMapper userDailyRecordMapper,
			ObjectMapper objectMapper) {
		this.userDailyRecordPhysicalSettingService = userDailyRecordPhysicalSettingService;
		this.userDailyRecordMapper = userDailyRecordMapper;
		this.objectMapper = objectMapper;
	}

	/**
	 * H5 self-service form statistics (same aggregate shape as {@link #getUserPersonalRecord}).
	 */
	public Map<String, Object> statisticalAnalysis(
			long companyId,
			long userId,
			int pageSize,
			int recordDateLte,
			Long shopId,
			String formType) {
		return getUserPersonalRecord(companyId, userId, pageSize, recordDateLte, shopId, formType);
	}

	public Map<String, Object> getUserPersonalRecord(
			long companyId,
			long userId,
			int pageSize,
			int recordDateLte,
			Long shopId,
			String formType) {
		Map<String, Object> result = new LinkedHashMap<>();
		List<Map<String, Object>> empty = List.of();
		result.put("list", empty);
		result.put("keyindex", empty);

		if (userId == 0L) {
			return result;
		}
		long tempId = userDailyRecordPhysicalSettingService.getTempIdForRead(companyId, formType);
		if (tempId == 0L) {
			return result;
		}

		LambdaQueryWrapper<UserDailyRecord> w = new LambdaQueryWrapper<>();
		w.eq(UserDailyRecord::getCompanyId, companyId)
				.eq(UserDailyRecord::getUserId, userId)
				.eq(UserDailyRecord::getTempId, tempId)
				.le(UserDailyRecord::getRecordDate, recordDateLte);
		if (shopId != null && shopId != 0L) {
			w.eq(UserDailyRecord::getShopId, shopId);
		}
		w.orderByDesc(UserDailyRecord::getRecordDate).orderByDesc(UserDailyRecord::getId);

		int limit = Math.max(pageSize, 0);
		Page<UserDailyRecord> page = new Page<>(1, limit);
		userDailyRecordMapper.selectPage(page, w);
		List<UserDailyRecord> records = page.getRecords();
		if (records.isEmpty()) {
			return result;
		}

		LinkedHashMap<String, MutableFieldAgg> aggList = new LinkedHashMap<>();
		LinkedHashMap<String, MutableFieldAgg> aggKeyIndex = new LinkedHashMap<>();

		for (int key = 0; key < records.size(); key++) {
			UserDailyRecord entity = records.get(key);
			String formDataStr = entity.getFormData();
			if (formDataStr == null || formDataStr.isBlank()) {
				continue;
			}
			JsonNode root;
			try {
				root = objectMapper.readTree(formDataStr);
			} catch (JsonProcessingException ex) {
				continue;
			}
			if (!root.isArray()) {
				continue;
			}
			for (JsonNode val : root) {
				if (val == null || !val.isObject()) {
					continue;
				}
				String fieldName = textOrEmpty(val.get("field_name"));
				String fieldTitle = textOrEmpty(val.get("field_title"));
				double fieldVal = parseFloatLoose(val.get("field_value"));

				MutableFieldAgg row = aggList.computeIfAbsent(fieldName, k -> new MutableFieldAgg());
				row.fieldname = fieldTitle;
				row.fieldkey = fieldName;
				row.fieldvalueByIndex.put(key, fieldVal);
				if (key == 0) {
					row.thisweek = fieldVal;
				}
				if (key == 1) {
					row.lastweek = fieldVal;
				}

				if (isLooseTruthyKeyIndex(val.get("key_index"))) {
					MutableFieldAgg krow = aggKeyIndex.computeIfAbsent(fieldName, k -> new MutableFieldAgg());
					krow.fieldname = fieldTitle;
					krow.fieldkey = fieldName;
					krow.fieldvalueByIndex.put(key, fieldVal);
					if (key == 0) {
						krow.thisweek = fieldVal;
					}
					if (key == 1) {
						krow.lastweek = fieldVal;
					}
				}
			}
		}

		List<Map<String, Object>> listOut = new ArrayList<>(aggList.size());
		for (MutableFieldAgg agg : aggList.values()) {
			listOut.add(toResultMap(agg));
		}
		List<Map<String, Object>> keyIndexOut = new ArrayList<>(aggKeyIndex.size());
		for (MutableFieldAgg agg : aggKeyIndex.values()) {
			keyIndexOut.add(toResultMap(agg));
		}

		result.put("list", listOut);
		result.put("keyindex", keyIndexOut);
		return result;
	}

	private static Map<String, Object> toResultMap(MutableFieldAgg agg) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("fieldname", agg.fieldname);
		m.put("fieldkey", agg.fieldkey);
		m.put("fieldvalue", buildFieldValueList(agg.fieldvalueByIndex));
		if (agg.thisweek != null) {
			m.put("thisweek", agg.thisweek);
		}
		if (agg.lastweek != null) {
			m.put("lastweek", agg.lastweek);
		}
		return m;
	}

	private static List<Double> buildFieldValueList(LinkedHashMap<Integer, Double> byIndex) {
		if (byIndex.isEmpty()) {
			return new ArrayList<>();
		}
		int max = byIndex.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
		List<Double> list = new ArrayList<>(max + 1);
		for (int i = 0; i <= max; i++) {
			list.add(byIndex.get(i));
		}
		return list;
	}

	private static String textOrEmpty(JsonNode node) {
		if (node == null || node.isNull()) {
			return "";
		}
		if (node.isTextual()) {
			return node.asText();
		}
		if (node.isNumber()) {
			return node.asText();
		}
		return "";
	}

	private static double parseFloatLoose(JsonNode node) {
		if (node == null || node.isNull()) {
			return 0.0;
		}
		if (node.isNumber()) {
			return node.asDouble();
		}
		if (node.isTextual()) {
			try {
				return Double.parseDouble(node.asText().trim());
			} catch (NumberFormatException ex) {
				return 0.0;
			}
		}
		return 0.0;
	}

	private static boolean isLooseTruthyKeyIndex(JsonNode n) {
		if (n == null || n.isNull()) {
			return false;
		}
		if (n.isBoolean()) {
			return n.booleanValue();
		}
		if (n.isNumber()) {
			return n.asDouble() != 0.0;
		}
		if (n.isTextual()) {
			String s = n.asText().trim();
			if (s.isEmpty()) {
				return false;
			}
			if ("0".equals(s)) {
				return false;
			}
			return true;
		}
		return false;
	}

	private static final class MutableFieldAgg {
		private String fieldname = "";
		private String fieldkey = "";
		private final LinkedHashMap<Integer, Double> fieldvalueByIndex = new LinkedHashMap<>();
		private Double thisweek;
		private Double lastweek;
	}
}
