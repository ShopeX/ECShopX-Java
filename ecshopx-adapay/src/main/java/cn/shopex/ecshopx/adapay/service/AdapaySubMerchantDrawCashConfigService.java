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

import cn.shopex.ecshopx.adapay.domain.AdapayCorpMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.mapper.AdapayCorpMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdapaySubMerchantDrawCashConfigService {

	private static final String KEY_PREFIX_LIMIT = "draw_limit";
	private static final String KEY_PREFIX_LIST = "draw_limit_list";
	private static final String KEY_PREFIX_AUTO = "draw_limit_config";

	private static final DateTimeFormatter MONTH_LITERAL = DateTimeFormatter.ofPattern("uuuu-MM", Locale.ROOT);

	private static final DateTimeFormatter MONTH_DAY_TIME = new DateTimeFormatterBuilder()
			.append(MONTH_LITERAL)
			.appendLiteral('-')
			.appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
			.appendLiteral(' ')
			.appendPattern("H:mm")
			.toFormatter(Locale.ROOT)
			.withResolverStyle(ResolverStyle.LENIENT);

	private static final Pattern AUTO_DAY_PATTERN = Pattern.compile("^(0?[1-9]|[12][0-9]|3[01])$");

	private static final DateTimeFormatter AUTO_TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final AdapayMemberMapper adapayMemberMapper;
	private final AdapayCorpMemberMapper adapayCorpMemberMapper;

	public AdapaySubMerchantDrawCashConfigService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper,
			AdapayMemberMapper adapayMemberMapper,
			AdapayCorpMemberMapper adapayCorpMemberMapper) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
		this.adapayMemberMapper = adapayMemberMapper;
		this.adapayCorpMemberMapper = adapayCorpMemberMapper;
	}

	public Map<String, Object> getDrawCashConfig(long companyId) {
		Map<String, Object> result = new LinkedHashMap<>();

		BigDecimal cents = getDrawLimitCents(companyId);
		boolean nonzero = cents.compareTo(BigDecimal.ZERO) != 0;
		if (!nonzero) {
			result.put("draw_limit", 0);
		} else {
			result.put(
					"draw_limit",
					cents.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).toPlainString());
		}

		Map<String, Object> cfg = getAutoCashConfig(companyId);
		if (cfg.isEmpty()) {
			result.put("auto_config", Collections.emptyList());
		} else {
			result.put("auto_config", new LinkedHashMap<>(cfg));
		}

		Map<String, BigDecimal> centsByMemberKey = getDrawLimitListCentsByMemberId(companyId);
		if (centsByMemberKey.isEmpty()) {
			result.put("draw_limit_list", Collections.emptyList());
		} else {
			List<Long> memberIds = new ArrayList<>();
			for (String k : centsByMemberKey.keySet()) {
				if (k == null) {
					continue;
				}
				try {
					memberIds.add(Long.parseLong(k.trim()));
				} catch (NumberFormatException ignored) {
					// skip invalid redis keys
				}
			}
			if (memberIds.isEmpty()) {
				result.put("draw_limit_list", Collections.emptyList());
			} else {
				List<AdapayMember> members = adapayMemberMapper.selectList(
						new LambdaQueryWrapper<AdapayMember>().in(AdapayMember::getId, memberIds));
				List<AdapayCorpMember> corps = adapayCorpMemberMapper.selectList(
						new LambdaQueryWrapper<AdapayCorpMember>()
								.in(AdapayCorpMember::getMemberId, memberIds));
				Map<Long, AdapayCorpMember> corpByMemberId = new LinkedHashMap<>();
				for (AdapayCorpMember c : corps) {
					corpByMemberId.put(c.getMemberId(), c);
				}
				List<Map<String, Object>> list = new ArrayList<>();
				for (AdapayMember m : members) {
					BigDecimal memberCents = centsByMemberKey.get(String.valueOf(m.getId()));
					if (memberCents == null) {
						continue;
					}
					String drawYuan =
							memberCents.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).toPlainString();
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("id", m.getId());
					row.put("member_id", m.getId());
					row.put("user_name", nullToEmpty(m.getUserName()));
					row.put("merchant_name", nullToEmpty(m.getUserName()));
					row.put("location", nullToEmpty(m.getLocation()));
					row.put("contact_name", nullToEmpty(m.getUserName()));
					row.put("draw_limit", drawYuan);
					if ("corp".equals(m.getMemberType())) {
						AdapayCorpMember c = corpByMemberId.get(m.getId());
						if (c != null) {
							row.put("merchant_name", nullToEmpty(c.getName()));
							row.put("contact_name", nullToEmpty(c.getLegalPerson()));
						}
					}
					list.add(row);
				}
				result.put("draw_limit_list", list);
			}
		}

		List<Map<String, String>> cashTypeOptions = List.of(
				Map.of("label", "T+1取现", "value", "T1"),
				Map.of("label", "D+1取现", "value", "D1"),
				Map.of("label", "即时取现", "value", "D0"));
		result.put("cash_type_options", cashTypeOptions);

		return result;
	}

	/**
	 * 将自动提现配置写回 Redis，与 {@link #getAutoCashConfig} 同键、同 JSON 结构。
	 */
	public void putAutoCashConfig(long companyId, Map<String, Object> config) {
		setAutoCashConfig(companyId, config);
	}

	/**
	 * 在开启自动提现场景下，计算与后台保存时一致的下一可执行点 epoch 秒；与
	 * {@link #setDrawCashConfig} 的 day / month 分支使用相同的时区、解析与候选时刻策略。
	 */
	public long computeNextAutoDrawEpochSecondForEnabled(
			String autoType, String autoDay, String autoTime, Clock clock) {
		String autoTimeTrimmed = autoTime == null ? "" : autoTime.trim();
		if (autoTimeTrimmed.isEmpty()) {
			throw new ResourceException("自动提现时间错误");
		}
		LocalTime parsedAutoTime;
		try {
			parsedAutoTime = LocalTime.parse(autoTimeTrimmed, AUTO_TIME_FORMAT);
		} catch (DateTimeParseException ex) {
			throw new ResourceException("自动提现时间错误");
		}
		ZoneId zone = ZoneId.systemDefault();
		ZonedDateTime nowZ = ZonedDateTime.ofInstant(clock.instant(), zone);
		if ("day".equals(autoType) || "month".equals(autoType)) {
			return computeNextAutoDrawEpochSecondCore(
					autoType, autoDay, parsedAutoTime, nowZ);
		}
		throw new ResourceException("自动提现类型错误");
	}

	/**
	 * 对应 PHP 侧 getNextTime：在入参 map 上原地更新 {@code next_time}；G5 时置 {@code auto_draw_cash}
	 * 为 N 并返回 false；空 config 时返回 false（不修改）。
	 */
	public boolean tryAdvanceNextAutoDrawTime(Map<String, Object> config, Clock clock) {
		if (config == null || config.isEmpty()) {
			return false;
		}
		Object autoTypeObj = config.get("auto_type");
		String autoType = autoTypeObj == null ? "" : String.valueOf(autoTypeObj).trim();
		if (!"day".equals(autoType) && !"month".equals(autoType)) {
			config.put("auto_draw_cash", "N");
			return false;
		}
		Object at = config.get("auto_time");
		String autoTime = at == null ? "" : String.valueOf(at).trim();
		if (autoTime.isEmpty()) {
			return false;
		}
		LocalTime parsedAutoTime;
		try {
			parsedAutoTime = LocalTime.parse(autoTime, AUTO_TIME_FORMAT);
		} catch (DateTimeParseException ex) {
			return false;
		}
		Object adObj = config.get("auto_day");
		String autoDay = adObj == null ? "" : String.valueOf(adObj);
		ZoneId zone = ZoneId.systemDefault();
		ZonedDateTime nowZ = ZonedDateTime.ofInstant(clock.instant(), zone);
		try {
			long nextEpoch = computeNextAutoDrawEpochSecondCore(
					autoType, autoDay, parsedAutoTime, nowZ);
			config.put("next_time", nextEpoch);
			return true;
		} catch (ResourceException ex) {
			return false;
		}
	}

	private static long computeNextAutoDrawEpochSecondCore(
			String autoType, String autoDay, LocalTime parsedAutoTime, ZonedDateTime nowZ) {
		ZoneId zone = nowZ.getZone();
		if ("day".equals(autoType)) {
			LocalDate today = nowZ.toLocalDate();
			ZonedDateTime candidate = ZonedDateTime.of(today, parsedAutoTime, zone);
			if (!candidate.isAfter(nowZ)) {
				candidate = ZonedDateTime.of(today.plusDays(1), parsedAutoTime, zone);
			}
			return candidate.toEpochSecond();
		}
		if (!"month".equals(autoType)) {
			throw new ResourceException("自动提现类型错误");
		}
		if (autoDay == null || autoDay.trim().isEmpty()) {
			throw new ResourceException("自动提现日期错误");
		}
		String ad = autoDay.trim();
		if (!AUTO_DAY_PATTERN.matcher(ad).matches()) {
			throw new ResourceException("自动提现日期错误");
		}
		String timeStr = AUTO_TIME_FORMAT.format(parsedAutoTime);
		String ym0 = MONTH_LITERAL.format(nowZ);
		String literal0 = ym0 + "-" + ad + " " + timeStr;
		ZonedDateTime candidate;
		try {
			LocalDateTime ldt0 = LocalDateTime.parse(literal0, MONTH_DAY_TIME);
			candidate = ldt0.atZone(zone);
		} catch (DateTimeException ex) {
			throw new ResourceException("自动提现日期错误");
		}
		if (!candidate.isAfter(nowZ)) {
			String ym1 = MONTH_LITERAL.format(nowZ.plusMonths(1));
			String literal1 = ym1 + "-" + ad + " " + timeStr;
			try {
				LocalDateTime ldt1 = LocalDateTime.parse(literal1, MONTH_DAY_TIME);
				candidate = ldt1.atZone(zone);
			} catch (DateTimeException ex) {
				throw new ResourceException("自动提现日期错误");
			}
		}
		return candidate.toEpochSecond();
	}

	public void setDrawCashConfig(long companyId, Map<String, Object> body) {
		String drawLimitRaw = adaptDrawLimitInputToString(body.get("draw_limit"));

		Object drawLimitListInput = body.get("draw_limit_list");
		if (drawLimitListInput == null) {
			drawLimitListInput = "";
		}

		String autoDrawCash = inputString(body, "auto_draw_cash", "N");
		String autoType = inputString(body, "auto_type", "");
		String autoDay = inputString(body, "auto_day", "");
		String autoTime = inputString(body, "auto_time", "");
		String minCash = inputString(body, "min_cash", "");
		String cashType = inputString(body, "cash_type", "");

		long nextTime = -1L;
		if ("Y".equals(autoDrawCash)) {
			nextTime =
					computeNextAutoDrawEpochSecondForEnabled(
							autoType, autoDay, autoTime, Clock.systemDefaultZone());
		}

		setDrawLimit(companyId, drawLimitRaw);

		Map<String, String> limitDataCentStr;
		if (drawLimitListBodyTruthy(drawLimitListInput)) {
			if (!(drawLimitListInput instanceof String str)) {
				throw new ResourceException("指定商户暂冻金额参数错误");
			}
			JsonNode root;
			try {
				root = objectMapper.readTree(str);
			} catch (JsonProcessingException ex) {
				throw new ResourceException("指定商户暂冻金额参数错误");
			}
			if (root == null || !root.isArray()) {
				throw new ResourceException("指定商户暂冻金额参数错误");
			}
			limitDataCentStr = new LinkedHashMap<>();
			for (JsonNode item : root) {
				if (item == null || !item.isObject()) {
					throw new ResourceException("指定商户暂冻金额参数错误");
				}
				JsonNode idNode = item.get("id");
				JsonNode drawNode = item.get("draw_limit");
				if (idNode == null || idNode.isNull()) {
					throw new ResourceException("暂冻金额设置错误!");
				}
				String idText = idNode.asText();
				if (idText == null || idText.trim().isEmpty()) {
					throw new ResourceException("暂冻金额设置错误!");
				}
				if (drawNode == null || drawNode.isNull()) {
					throw new ResourceException("暂冻金额设置错误!");
				}
				if (drawNode.isBoolean() && !drawNode.booleanValue()) {
					throw new ResourceException("暂冻金额设置错误!");
				}
				String drawStr = drawNode.asText();
				if (drawStr == null || drawStr.trim().isEmpty()) {
					throw new ResourceException("暂冻金额设置错误!");
				}
				BigDecimal drawAmount;
				try {
					drawAmount = new BigDecimal(drawStr.trim());
				} catch (NumberFormatException ex) {
					throw new ResourceException("指定商户暂冻金额参数错误");
				}
				if (BigDecimal.ZERO.compareTo(drawAmount) == 0) {
					throw new ResourceException("暂冻金额设置错误!");
				}
				String idKey = idText.trim();
				limitDataCentStr.put(idKey, drawAmount.multiply(new BigDecimal("100")).toPlainString());
			}
		} else {
			limitDataCentStr = Collections.emptyMap();
		}
		setDrawLimitList(companyId, limitDataCentStr);

		Map<String, Object> autoCashConfig = new LinkedHashMap<>();
		autoCashConfig.put("auto_draw_cash", autoDrawCash);
		autoCashConfig.put("auto_type", autoType);
		autoCashConfig.put("auto_day", autoDay);
		autoCashConfig.put("auto_time", autoTime);
		autoCashConfig.put("min_cash", minCash);
		autoCashConfig.put("cash_type", cashType);
		autoCashConfig.put("next_time", nextTime);
		setAutoCashConfig(companyId, autoCashConfig);
	}

	/**
	 * 供废弃单键接口 POST /sub_approve/draw_limit 使用：将原始 input（含 null）适配为字符串后写入 Redis。
	 */
	public void setDrawLimit(long companyId, Object drawLimitInput) {
		String drawLimitRaw = adaptDrawLimitInputToString(drawLimitInput);
		setDrawLimit(companyId, drawLimitRaw);
	}

	/**
	 * 读取 Redis 中全公司暂冻额度（分，整数或小数字符串）。
	 */
	public BigDecimal getDrawLimitCents(long companyId) {
		String key = KEY_PREFIX_LIMIT + sha1Hex(String.valueOf(companyId));
		String raw = sharedStringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return BigDecimal.ZERO;
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (root == null || !root.isObject()) {
				return BigDecimal.ZERO;
			}
			JsonNode dl = root.get("draw_limit");
			if (dl == null || dl.isNull()) {
				return BigDecimal.ZERO;
			}
			String s = dl.asText();
			if (!StringUtils.hasText(s)) {
				return BigDecimal.ZERO;
			}
			return new BigDecimal(s.trim());
		} catch (Exception ex) {
			return BigDecimal.ZERO;
		}
	}

	/**
	 * 读取 Redis 中全公司暂冻额度原始 JSON：无键、空值或无效 JSON 时返回空列表；否则返回解析后的对象、数组或标量。
	 */
	public Object getDrawLimit(long companyId) {
		String key = KEY_PREFIX_LIMIT + sha1Hex(String.valueOf(companyId));
		String raw = sharedStringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return Collections.emptyList();
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (JsonProcessingException ex) {
			return Collections.emptyList();
		}
		if (root == null || root.isNull()) {
			return Collections.emptyList();
		}
		if (root.isObject()) {
			return objectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
		}
		if (root.isArray()) {
			return objectMapper.convertValue(root, new TypeReference<ArrayList<Object>>() {});
		}
		return objectMapper.convertValue(root, Object.class);
	}

	/**
	 * 读取 Redis 中自动提现等配置；无键或解析失败时返回空 map。
	 */
	public Map<String, Object> getAutoCashConfig(long companyId) {
		String key = KEY_PREFIX_AUTO + sha1Hex(String.valueOf(companyId));
		String raw = sharedStringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return Collections.emptyMap();
		}
		try {
			Map<String, Object> parsed =
					objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			return parsed == null ? Collections.emptyMap() : parsed;
		} catch (JsonProcessingException ex) {
			return Collections.emptyMap();
		}
	}

	/**
	 * 读取 Redis 中按子商户 member 主键覆盖的暂冻额度（分）；JSON 数组视为空 map。
	 */
	public Map<String, BigDecimal> getDrawLimitListCentsByMemberId(long companyId) {
		String key = KEY_PREFIX_LIST + sha1Hex(String.valueOf(companyId));
		String raw = sharedStringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return Collections.emptyMap();
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (root == null || root.isNull() || !root.isObject()) {
				return Collections.emptyMap();
			}
			Map<String, BigDecimal> out = new LinkedHashMap<>();
			var it = root.fields();
			while (it.hasNext()) {
				var e = it.next();
				JsonNode v = e.getValue();
				if (v == null || v.isNull()) {
					continue;
				}
				try {
					if (v.isNumber()) {
						out.put(e.getKey(), new BigDecimal(v.asText()));
					} else {
						String t = v.asText();
						if (StringUtils.hasText(t)) {
							out.put(e.getKey(), new BigDecimal(t.trim()));
						}
					}
				} catch (NumberFormatException ignored) {
					// skip malformed entry
				}
			}
			return out;
		} catch (JsonProcessingException ex) {
			return Collections.emptyMap();
		}
	}

	private static String inputString(Map<String, Object> body, String key, String defaultVal) {
		Object val = body.get(key);
		if (val == null) {
			return defaultVal;
		}
		if (val instanceof String s) {
			return s;
		}
		if (val instanceof Number || val instanceof Boolean) {
			return String.valueOf(val);
		}
		return String.valueOf(val);
	}

	private static boolean drawLimitListBodyTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof BigDecimal bd) {
			return bd.compareTo(BigDecimal.ZERO) != 0;
		}
		if (v instanceof Byte || v instanceof Short || v instanceof Integer || v instanceof Long) {
			return ((Number) v).longValue() != 0L;
		}
		if (v instanceof Float f) {
			return Float.compare(f, 0.0f) != 0;
		}
		if (v instanceof Double d) {
			return Double.compare(d, 0.0d) != 0;
		}
		if (v instanceof Number n) {
			try {
				return new BigDecimal(n.toString()).compareTo(BigDecimal.ZERO) != 0;
			} catch (NumberFormatException e) {
				return true;
			}
		}
		if (v instanceof CharSequence cs) {
			int len = cs.length();
			if (len == 0) {
				return false;
			}
			return len != 1 || cs.charAt(0) != '0';
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		if (v instanceof boolean[] arr) {
			return arr.length > 0;
		}
		if (v instanceof byte[] arr) {
			return arr.length > 0;
		}
		if (v instanceof short[] arr) {
			return arr.length > 0;
		}
		if (v instanceof char[] arr) {
			return arr.length > 0;
		}
		if (v instanceof int[] arr) {
			return arr.length > 0;
		}
		if (v instanceof long[] arr) {
			return arr.length > 0;
		}
		if (v instanceof float[] arr) {
			return arr.length > 0;
		}
		if (v instanceof double[] arr) {
			return arr.length > 0;
		}
		if (v instanceof Object[] arr) {
			return arr.length > 0;
		}
		return true;
	}

	private static String adaptDrawLimitInputToString(Object v) {
		if (v == null) {
			return "";
		}
		if (v instanceof String s) {
			return s;
		}
		if (v instanceof BigDecimal bd) {
			return bd.toPlainString();
		}
		if (v instanceof Number || v instanceof Boolean) {
			return String.valueOf(v);
		}
		throw new ResourceException("暂冻金额参数错误");
	}

	private void setDrawLimit(long companyId, String drawLimitRaw) {
		String scaledOperand = drawLimitRaw.isEmpty() ? "0" : drawLimitRaw;
		BigDecimal yuan;
		try {
			yuan = new BigDecimal(scaledOperand);
		} catch (NumberFormatException ex) {
			throw new ResourceException("暂冻金额参数错误");
		}
		String cents = yuan.multiply(new BigDecimal("100")).toPlainString();
		Map<String, String> payload = new HashMap<>();
		payload.put("draw_limit", cents);
		String key = KEY_PREFIX_LIMIT + sha1Hex(String.valueOf(companyId));
		try {
			String json = objectMapper.writeValueAsString(payload);
			sharedStringRedisTemplate.opsForValue().set(key, json);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	private void setDrawLimitList(long companyId, Map<String, String> limitDataCentStr) {
		String key = KEY_PREFIX_LIST + sha1Hex(String.valueOf(companyId));
		String json;
		try {
			if (limitDataCentStr == null || limitDataCentStr.isEmpty()) {
				json = objectMapper.writeValueAsString(List.of());
			} else {
				json = objectMapper.writeValueAsString(limitDataCentStr);
			}
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
		sharedStringRedisTemplate.opsForValue().set(key, json);
	}

	private void setAutoCashConfig(long companyId, Map<String, Object> config) {
		String key = KEY_PREFIX_AUTO + sha1Hex(String.valueOf(companyId));
		try {
			String json = objectMapper.writeValueAsString(config);
			sharedStringRedisTemplate.opsForValue().set(key, json);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
