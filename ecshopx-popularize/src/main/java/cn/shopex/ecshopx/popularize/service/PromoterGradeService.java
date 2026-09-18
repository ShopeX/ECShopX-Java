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

package cn.shopex.ecshopx.popularize.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromoterGradeService {

	private static final String REDIS_IS_OPEN_PROMOTER_GRADE_PREFIX = "isOpenPromoterGrade:";
	private static final String REDIS_PROMOTER_GRADE_CONFIG_PREFIX = "promoterGradeConfig:";

	/**
	 * 单档默认元数据：key 为请求/Redis JSON 中的 grade 键名；defaultName 为写入 config.grade.*.name 的默认展示名；gradeLevel 为 grade_level。
	 * 顺序：first_grade(1) → second_grade(2) → third_grade(3)。
	 */
	private record DefaultGradeRow(String key, String defaultName, int gradeLevel) {}

	private static final List<DefaultGradeRow> DEFAULT_GRADE_ROWS = List.of(
			new DefaultGradeRow("first_grade", "等级一", 1),
			new DefaultGradeRow("second_grade", "等级二", 2),
			new DefaultGradeRow("third_grade", "等级三", 3));

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public PromoterGradeService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getPromoterGradeConfig(long companyId) {
		Map<String, Object> config;
		String rawConfig =
				stringRedisTemplate.opsForValue().get(REDIS_PROMOTER_GRADE_CONFIG_PREFIX + companyId);
		if (rawConfig == null || rawConfig.isBlank()) {
			config = new LinkedHashMap<>();
		} else {
			try {
				Object parsed =
						objectMapper.readValue(rawConfig, new TypeReference<Map<String, Object>>() {});
				if (!(parsed instanceof Map<?, ?>)) {
					config = new LinkedHashMap<>();
				} else {
					@SuppressWarnings("unchecked")
					Map<String, Object> asMap = (Map<String, Object>) parsed;
					config = new LinkedHashMap<>(asMap);
				}
			} catch (JsonProcessingException e) {
				config = new LinkedHashMap<>();
			}
		}

		String rawOpen =
				stringRedisTemplate.opsForValue().get(REDIS_IS_OPEN_PROMOTER_GRADE_PREFIX + companyId);
		String t = rawOpen == null ? "" : rawOpen.trim();
		String flag = "true".equals(t) ? "true" : "false";
		config.put("isOpenPromoterGrade", flag);
		return config;
	}

	public boolean readIsOpenPromoterGrade(long companyId) {
		String v =
				stringRedisTemplate.opsForValue().get(REDIS_IS_OPEN_PROMOTER_GRADE_PREFIX + companyId);
		if (v == null || v.isBlank()) {
			return false;
		}
		String t = v.trim();
		if ("true".equalsIgnoreCase(t)) {
			return true;
		}
		if ("false".equalsIgnoreCase(t)) {
			return false;
		}
		return isOpenPromoterGradeInputTruthy(t);
	}

	public String readPromoterGradeDisplayName(long companyId, Integer gradeLevel) {
		int lvl = gradeLevel == null ? 1 : gradeLevel;
		if (lvl < 1 || lvl > 3) {
			lvl = 1;
		}
		DefaultGradeRow row = DEFAULT_GRADE_ROWS.get(lvl - 1);
		String json =
				stringRedisTemplate.opsForValue().get(REDIS_PROMOTER_GRADE_CONFIG_PREFIX + companyId);
		if (json == null || json.isBlank()) {
			return row.defaultName();
		}
		try {
			Map<String, Object> root =
					objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
			Object gradeObj = root.get("grade");
			if (!(gradeObj instanceof Map<?, ?>)) {
				return row.defaultName();
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> grade = (Map<String, Object>) gradeObj;
			Object nested = grade.get(row.key());
			if (!(nested instanceof Map<?, ?>)) {
				return row.defaultName();
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> nm = (Map<String, Object>) nested;
			Object cn = nm.get("custom_name");
			if (cn != null && StringUtils.hasText(String.valueOf(cn).trim())) {
				return String.valueOf(cn).trim();
			}
			return row.defaultName();
		} catch (JsonProcessingException e) {
			return row.defaultName();
		}
	}

	public void setPromoterGradeConfig(long companyId, Map<String, Object> body) {
		Object isOpenRaw =
				body.containsKey("isOpenPromoterGrade") ? body.get("isOpenPromoterGrade") : null;
		stringRedisTemplate
				.opsForValue()
				.set(
						REDIS_IS_OPEN_PROMOTER_GRADE_PREFIX + companyId,
						normalizeOpenFlagForRedis(isOpenRaw));

		Object upgradeObj = body.get("upgrade");
		if (!(upgradeObj instanceof Map<?, ?>)) {
			throw new BadRequestException("请求参数错误");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> upgrade = (Map<String, Object>) upgradeObj;
		Object filterObj = upgrade.get("filter");
		if (!(filterObj instanceof Map<?, ?>)) {
			throw new BadRequestException("请求参数错误");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> filter = (Map<String, Object>) filterObj;
		Object gradeObj = body.get("grade");
		if (!(gradeObj instanceof Map<?, ?>)) {
			throw new BadRequestException("请求参数错误");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> grade = (Map<String, Object>) gradeObj;

		Object rawChildrenNum = filter.get("children_num");
		Object rawSales = filter.get("children_sales_amount");
		Object rawGradeMember = filter.get("grade_member");
		if (!looseBooleanFromDynamicValue(rawChildrenNum)
				&& !looseBooleanFromDynamicValue(rawSales)
				&& !looseBooleanFromDynamicValue(rawGradeMember)) {
			throw new ResourceException("请最少选择一个升级条件");
		}

		Map<String, Object> config = new LinkedHashMap<>();
		Map<String, Object> upgradeOut = new LinkedHashMap<>();
		upgradeOut.put("stat_cycle", upgrade.get("stat_cycle"));
		Map<String, Object> filterOut = new LinkedHashMap<>();
		filterOut.put("children_num", looseTrueStringEquals(filter.get("children_num")));
		filterOut.put(
				"children_sales_amount", looseTrueStringEquals(filter.get("children_sales_amount")));
		filterOut.put("grade_member", looseTrueStringEquals(filter.get("grade_member")));
		upgradeOut.put("filter", filterOut);
		config.put("upgrade", upgradeOut);

		Map<String, Object> gradeOut = new LinkedHashMap<>();
		for (DefaultGradeRow row : DEFAULT_GRADE_ROWS) {
			String k = row.key();
			Object raw = grade.get(k);
			if (!(raw instanceof Map<?, ?>)) {
				throw new BadRequestException("请填写等级名称");
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> gradeNested = (Map<String, Object>) raw;

			if (!mapHasKeyWithNonNullValue(gradeNested, "custom_name")) {
				throw new BadRequestException("请填写等级名称");
			}

			if (row.gradeLevel() > 1 && Boolean.TRUE.equals(filterOut.get("children_num"))) {
				if (!mapHasKeyWithNonNullValue(gradeNested, "children_num")) {
					throw new ResourceException("请填写升级直属下线数量");
				}
			}
			if (row.gradeLevel() > 1 && Boolean.TRUE.equals(filterOut.get("children_sales_amount"))) {
				if (!mapHasKeyWithNonNullValue(gradeNested, "children_sales_amount")) {
					throw new ResourceException("请填写升级销售总额");
				}
			}

			Map<String, Object> perGrade = new LinkedHashMap<>();
			perGrade.put("name", row.defaultName());
			perGrade.put("grade_level", row.gradeLevel());
			perGrade.put("custom_name", gradeNested.get("custom_name"));
			perGrade.put(
					"children_num",
					gradeNested.containsKey("children_num")
							? safeNumericLong(gradeNested.get("children_num"))
							: 0L);
			perGrade.put(
					"children_sales_amount",
					gradeNested.containsKey("children_sales_amount")
							? safeNumericLong(gradeNested.get("children_sales_amount"))
							: 0L);
			perGrade.put(
					"grade_member",
					gradeNested.containsKey("grade_member") ? gradeNested.get("grade_member") : "");
			perGrade.put(
					"first_ratio",
					gradeNested.containsKey("first_ratio")
							? safeNumericLong(gradeNested.get("first_ratio"))
							: 0L);
			perGrade.put(
					"second_ratio",
					gradeNested.containsKey("second_ratio")
							? safeNumericLong(gradeNested.get("second_ratio"))
							: 0L);
			gradeOut.put(k, perGrade);
		}
		config.put("grade", gradeOut);

		String json;
		try {
			json = objectMapper.writeValueAsString(config);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
		stringRedisTemplate.opsForValue().set(REDIS_PROMOTER_GRADE_CONFIG_PREFIX + companyId, json);
	}

	private static boolean isOpenPromoterGradeInputTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			return false;
		}
		if ("1".equals(s)) {
			return true;
		}
		return "true".equalsIgnoreCase(s);
	}

	private static String normalizeOpenFlagForRedis(Object raw) {
		return isOpenPromoterGradeInputTruthy(raw) ? "true" : "false";
	}

	/** 等价动态语言中对 filter 三字段做 (bool) 再取反的语义。 */
	private static boolean looseBooleanFromDynamicValue(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			double d = n.doubleValue();
			return d != 0.0d && !Double.isNaN(d);
		}
		if (v instanceof String s) {
			if (s.isEmpty()) {
				return false;
			}
			if ("0".equals(s)) {
				return false;
			}
			return true;
		}
		if (v instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		if (v instanceof Iterable<?> it) {
			return it.iterator().hasNext();
		}
		return true;
	}

	/** 等价松散比较 x == 'true'，用于写入 config.upgrade.filter 的布尔字段。 */
	private static boolean looseTrueStringEquals(Object x) {
		if (Boolean.TRUE.equals(x)) {
			return true;
		}
		return "true".equals(x);
	}

	/** 键存在且值不为 null；值为空字符串时仍视为已设置。 */
	private static boolean mapHasKeyWithNonNullValue(Map<String, Object> m, String key) {
		return m != null && m.containsKey(key) && m.get(key) != null;
	}

	private static long safeNumericLong(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				try {
					return (long) Double.parseDouble(t);
				} catch (NumberFormatException e2) {
					return 0L;
				}
			}
		}
		return 0L;
	}
}
