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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.adapay.service.AdapaySubMerchantDrawCashConfigService;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class BsPaySubMerchantDrawCashConfigService {

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

	private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;
	private final AdapaySubMerchantDrawCashConfigService adapaySubMerchantDrawCashConfigService;

	public BsPaySubMerchantDrawCashConfigService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper,
			AdapaySubMerchantDrawCashConfigService adapaySubMerchantDrawCashConfigService) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
		this.adapaySubMerchantDrawCashConfigService = adapaySubMerchantDrawCashConfigService;
	}

	public Map<String, Object> getDrawCashConfig(long companyId) {
		return adapaySubMerchantDrawCashConfigService.getDrawCashConfig(companyId);
	}

	public Object getDrawLimit(long companyId) {
		return adapaySubMerchantDrawCashConfigService.getDrawLimit(companyId);
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

			ZoneId zone = SHANGHAI;

			if ("day".equals(autoType)) {
				ZonedDateTime now = ZonedDateTime.now(zone);
				LocalDate today = now.toLocalDate();
				ZonedDateTime candidate = ZonedDateTime.of(today, parsedAutoTime, zone);
				if (!candidate.isAfter(now)) {
					candidate = ZonedDateTime.of(today.plusDays(1), parsedAutoTime, zone);
				}
				nextTime = candidate.toEpochSecond();
			} else if ("month".equals(autoType)) {
				if (autoDay == null || autoDay.trim().isEmpty()) {
					throw new ResourceException("自动提现日期错误");
				}
				String ad = autoDay.trim();
				if (!AUTO_DAY_PATTERN.matcher(ad).matches()) {
					throw new ResourceException("自动提现日期错误");
				}
				String timeStr = AUTO_TIME_FORMAT.format(parsedAutoTime);
				ZonedDateTime now = ZonedDateTime.now(zone);

				String ym0 = MONTH_LITERAL.format(now);
				String literal0 = ym0 + "-" + ad + " " + timeStr;
				ZonedDateTime candidate;
				try {
					LocalDateTime ldt0 = LocalDateTime.parse(literal0, MONTH_DAY_TIME);
					candidate = ldt0.atZone(zone);
				} catch (DateTimeException ex) {
					throw new ResourceException("自动提现日期错误");
				}

				if (!candidate.isAfter(now)) {
					String ym1 = MONTH_LITERAL.format(now.plusMonths(1));
					String literal1 = ym1 + "-" + ad + " " + timeStr;
					try {
						LocalDateTime ldt1 = LocalDateTime.parse(literal1, MONTH_DAY_TIME);
						candidate = ldt1.atZone(zone);
					} catch (DateTimeException ex) {
						throw new ResourceException("自动提现日期错误");
					}
				}
				nextTime = candidate.toEpochSecond();
			} else {
				throw new ResourceException("自动提现类型错误");
			}
		}

		setDrawLimit(companyId, drawLimitRaw);

		Map<String, String> limitDataCentStr;
		if (drawLimitListBodyTruthy(drawLimitListInput)) {
			if (!(drawLimitListInput instanceof String str)) {
				throw new BadRequestException("指定商户暂冻金额参数错误");
			}
			JsonNode root;
			try {
				root = objectMapper.readTree(str);
			} catch (JsonProcessingException ex) {
				throw new BadRequestException("指定商户暂冻金额参数错误");
			}
			if (root == null || !root.isArray()) {
				throw new BadRequestException("指定商户暂冻金额参数错误");
			}
			limitDataCentStr = new LinkedHashMap<>();
			for (JsonNode item : root) {
				if (item == null || !item.isObject()) {
					throw new BadRequestException("指定商户暂冻金额参数错误");
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
					throw new BadRequestException("指定商户暂冻金额参数错误");
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

		LinkedHashMap<String, Object> autoCashConfig = new LinkedHashMap<>();
		autoCashConfig.put("auto_draw_cash", autoDrawCash);
		autoCashConfig.put("auto_type", autoType);
		autoCashConfig.put("auto_day", autoDay);
		autoCashConfig.put("auto_time", autoTime);
		autoCashConfig.put("min_cash", minCash);
		autoCashConfig.put("cash_type", cashType);
		autoCashConfig.put("next_time", Long.valueOf(nextTime));
		setAutoCashConfig(companyId, autoCashConfig);
	}

	public void setDrawLimit(long companyId, Object drawLimitInput) {
		String drawLimitRaw = adaptDrawLimitInputToString(drawLimitInput);
		setDrawLimit(companyId, drawLimitRaw);
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
		String operand = drawLimitRaw.isEmpty() ? "0" : drawLimitRaw;
		BigDecimal yuan;
		try {
			yuan = new BigDecimal(operand);
		} catch (NumberFormatException ex) {
			throw new ResourceException("暂冻金额参数错误");
		}
		String cents = yuan.multiply(new BigDecimal("100")).toPlainString();
		Map<String, String> payload = Map.of("draw_limit", cents);
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
			ObjectMapper redisWriter = objectMapper.copy();
			redisWriter.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
			String json = redisWriter.writeValueAsString(config);
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
}
