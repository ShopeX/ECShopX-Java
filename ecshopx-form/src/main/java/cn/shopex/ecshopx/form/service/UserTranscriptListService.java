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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserTranscriptListService {

	private static final Set<String> ALLOWED_COLUMNS = Set.of(
			"record_id",
			"user_id",
			"company_id",
			"shop_id",
			"transcript_id",
			"transcript_name",
			"indicator_details",
			"created",
			"updated");

	private static final Set<String> NUMERIC_LONG_COLUMNS =
			Set.of("record_id", "user_id", "company_id", "shop_id", "transcript_id");

	private static final Set<String> NUMERIC_INT_COLUMNS = Set.of("created", "updated");

	private static final Set<String> ALLOWED_OPS = Set.of(
			"eq",
			"neq",
			"gt",
			"lt",
			"gte",
			"lte",
			"in",
			"notIn",
			"contains",
			"memberOf",
			"startsWith",
			"endsWith",
			"isNull");

	private final UserTranscriptsMapper userTranscriptsMapper;
	private final ObjectMapper objectMapper;

	public UserTranscriptListService(UserTranscriptsMapper userTranscriptsMapper, ObjectMapper objectMapper) {
		this.userTranscriptsMapper = userTranscriptsMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> list(Map<String, Object> filterParams) {
		Map<String, Object> params = filterParams != null ? filterParams : Map.of();
		QueryWrapper<UserTranscripts> wrapper = new QueryWrapper<>();
		for (Map.Entry<String, Object> e : params.entrySet()) {
			String field = e.getKey();
			Object value = e.getValue();
			int pipe = field.indexOf('|');
			if (pipe >= 0) {
				String col = field.substring(0, pipe);
				String op = field.substring(pipe + 1);
				if (!ALLOWED_COLUMNS.contains(col)) {
					throw new BadRequestException("非法查询字段");
				}
				if (!ALLOWED_OPS.contains(op)) {
					throw new BadRequestException("不支持的操作符");
				}
				applyOp(wrapper, col, op, value);
			} else {
				if (!ALLOWED_COLUMNS.contains(field)) {
					throw new BadRequestException("非法查询字段");
				}
				if (isArrayLike(value)) {
					List<Object> vals = toScalarList(value);
					if (vals.isEmpty()) {
						throw new BadRequestException("IN 条件不能为空");
					}
					wrapper.in(field, coerceListForColumn(field, vals));
				} else {
					wrapper.eq(field, coerceScalarForColumn(field, value));
				}
			}
		}

		int totalCount = userTranscriptsMapper.selectCount(wrapper).intValue();
		List<Map<String, Object>> rows = new ArrayList<>();
		if (totalCount > 0) {
			wrapper.orderBy(true, false, "created");
			wrapper.last("LIMIT 1000");
			for (UserTranscripts entity : userTranscriptsMapper.selectList(wrapper)) {
				rows.add(entityToRow(entity));
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("list", rows);
		return result;
	}

	private void applyOp(QueryWrapper<UserTranscripts> wrapper, String column, String op, Object value) {
		switch (op) {
			case "eq" -> wrapper.eq(column, coerceScalarForColumn(column, value));
			case "neq" -> wrapper.ne(column, coerceScalarForColumn(column, value));
			case "gt" -> wrapper.gt(column, coerceComparableForColumn(column, value));
			case "lt" -> wrapper.lt(column, coerceComparableForColumn(column, value));
			case "gte" -> wrapper.ge(column, coerceComparableForColumn(column, value));
			case "lte" -> wrapper.le(column, coerceComparableForColumn(column, value));
			case "in" -> {
				List<Object> vals = toScalarList(value);
				if (vals.isEmpty()) {
					throw new BadRequestException("IN 条件不能为空");
				}
				wrapper.in(column, coerceListForColumn(column, vals));
			}
			case "notIn" -> {
				List<Object> vals = toScalarList(value);
				if (vals.isEmpty()) {
					throw new BadRequestException("NOT IN 条件不能为空");
				}
				wrapper.notIn(column, coerceListForColumn(column, vals));
			}
			case "contains" ->
					wrapper.apply(column + " LIKE CONCAT('%', {0}, '%')", String.valueOf(value));
			case "startsWith" -> wrapper.apply(column + " LIKE CONCAT({0}, '%')", String.valueOf(value));
			case "endsWith" -> wrapper.apply(column + " LIKE CONCAT('%', {0})", String.valueOf(value));
			case "isNull" -> wrapper.isNull(column);
			case "memberOf" -> applyMemberOf(wrapper, column, value);
			default -> throw new BadRequestException("不支持的操作符");
		}
	}

	private void applyMemberOf(QueryWrapper<UserTranscripts> wrapper, String column, Object value) {
		if (!"indicator_details".equals(column)) {
			throw new BadRequestException("该列不支持 memberOf");
		}
		try {
			String json = objectMapper.writeValueAsString(value);
			wrapper.apply("JSON_CONTAINS(" + column + ", {0})", json);
		} catch (JsonProcessingException ex) {
			throw new BadRequestException("memberOf 参数无法序列化为 JSON");
		}
	}

	private static boolean isArrayLike(Object value) {
		if (value == null) {
			return false;
		}
		if (value instanceof Collection<?>) {
			return true;
		}
		return value instanceof Object[]
				|| value instanceof int[]
				|| value instanceof long[]
				|| value instanceof Integer[]
				|| value instanceof Long[]
				|| value instanceof String[];
	}

	private static List<Object> toScalarList(Object value) {
		if (value instanceof Collection<?> c) {
			return new ArrayList<>(c);
		}
		if (value instanceof Object[] arr) {
			return new ArrayList<>(Arrays.asList(arr));
		}
		if (value instanceof int[] arr) {
			List<Object> out = new ArrayList<>(arr.length);
			for (int x : arr) {
				out.add(x);
			}
			return out;
		}
		if (value instanceof long[] arr) {
			List<Object> out = new ArrayList<>(arr.length);
			for (long x : arr) {
				out.add(x);
			}
			return out;
		}
		if (value instanceof Integer[] arr) {
			return new ArrayList<>(Arrays.asList(arr));
		}
		if (value instanceof Long[] arr) {
			return new ArrayList<>(Arrays.asList(arr));
		}
		if (value instanceof String[] arr) {
			return new ArrayList<>(Arrays.asList(arr));
		}
		throw new BadRequestException("IN/NOT IN 需要数组或集合");
	}

	private List<Object> coerceListForColumn(String column, List<Object> vals) {
		List<Object> out = new ArrayList<>(vals.size());
		for (Object v : vals) {
			out.add(coerceScalarForColumn(column, v));
		}
		return out;
	}

	private Object coerceScalarForColumn(String column, Object value) {
		if (NUMERIC_LONG_COLUMNS.contains(column)) {
			if (value == null) {
				return null;
			}
			if (value instanceof Number n) {
				return n.longValue();
			}
			String s = String.valueOf(value).trim();
			if (!StringUtils.hasText(s)) {
				return null;
			}
			return Long.parseLong(s);
		}
		if (NUMERIC_INT_COLUMNS.contains(column)) {
			if (value == null) {
				return null;
			}
			if (value instanceof Number n) {
				return n.intValue();
			}
			String s = String.valueOf(value).trim();
			if (!StringUtils.hasText(s)) {
				return null;
			}
			return Integer.parseInt(s);
		}
		if (value == null) {
			return null;
		}
		return String.valueOf(value);
	}

	private Object coerceComparableForColumn(String column, Object value) {
		if (NUMERIC_LONG_COLUMNS.contains(column) || NUMERIC_INT_COLUMNS.contains(column)) {
			return coerceScalarForColumn(column, value);
		}
		return coerceScalarForColumn(column, value);
	}

	private Map<String, Object> entityToRow(UserTranscripts entity) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("record_id", entity.getRecordId());
		row.put("user_id", entity.getUserId());
		row.put("company_id", entity.getCompanyId());
		row.put("shop_id", entity.getShopId());
		row.put("transcript_id", entity.getTranscriptId());
		row.put("transcript_name", entity.getTranscriptName());
		row.put("indicator_details", indicatorDetailsForResponse(entity.getIndicatorDetails()));
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		return row;
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
}
