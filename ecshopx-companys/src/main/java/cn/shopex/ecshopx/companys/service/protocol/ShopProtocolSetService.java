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

package cn.shopex.ecshopx.companys.service.protocol;

import cn.shopex.ecshopx.companys.domain.ProtocolUpdateLog;
import cn.shopex.ecshopx.companys.mapper.ProtocolUpdateLogMapper;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShopProtocolSetService {

	private static final Map<String, String> PROTOCOL_TITLE_DEFAULT = Map.of(
			"member_register", "注册协议",
			"privacy", "隐私政策",
			"member_logout", "注销协议",
			"member_logout_config", "订单完成之前，无法注销会员。如有疑问，请联系客服");

	private static final String TYPE_MEMBER_LOGOUT = "member_logout";
	private static final String TYPE_MEMBER_LOGOUT_CONFIG = "member_logout_config";

	private static final List<String> SALESMAN_PROTOCOL_TYPES =
			List.of("salesman_service", "salesman_privacy");

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper redisJsonMapper;
	private final ProtocolUpdateLogMapper protocolUpdateLogMapper;

	@Autowired
	public ShopProtocolSetService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			ProtocolUpdateLogMapper protocolUpdateLogMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.redisJsonMapper = objectMapper.copy();
		this.redisJsonMapper.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
		this.protocolUpdateLogMapper = protocolUpdateLogMapper;
	}

	public void set(long companyId, Map<String, Object> mergedRequest) {
		Object cc = mergedRequest.get("country_code");
		String rawCountry =
				(cc == null || String.valueOf(cc).trim().isEmpty()) ? "zh-CN" : String.valueOf(cc).trim();
		String hashKey = resolveProtocolHashKey(companyId, rawCountry);

		List<Map<String, Object>> protocols = parseAndValidateData(mergedRequest.get("data"));
		applyMemberLogoutDateRules(protocols);

		for (Map<String, Object> protocol : protocols) {
			String baseType = String.valueOf(protocol.get("type")).trim();
			Object saveTypeRaw = protocol.get("save_type");
			boolean isDraft = saveTypeRaw != null
					&& String.valueOf(saveTypeRaw).trim().length() > 0
					&& "draft".equals(String.valueOf(saveTypeRaw).trim());

			LinkedHashMap<String, Object> updateData = buildUpdateData(protocol, baseType);

			if (isDraft) {
				String protocolType = baseType + "_draft";
				persistHashField(companyId, hashKey, protocolType, updateData);
			} else {
				companysRedisTemplate.opsForHash().put(hashKey, baseType + "_draft", "[]");
				persistHashField(companyId, hashKey, baseType, updateData);
			}
		}
	}

	public Map<String, Object> get(long companyId, String type, String countryCode) {
		String hashKey = resolveProtocolHashKey(companyId, countryCode);
		LinkedHashMap<String, String> raw = new LinkedHashMap<>();
		boolean singleType = type != null && !type.trim().isEmpty();
		if (singleType) {
			String field = type.trim();
			Object v = companysRedisTemplate.opsForHash().get(hashKey, field);
			String val = v == null ? null : String.valueOf(v);
			raw.put(field, val);
		} else {
			Map<Object, Object> entries = companysRedisTemplate.opsForHash().entries(hashKey);
			for (Map.Entry<Object, Object> e : entries.entrySet()) {
				String k = e.getKey() == null ? "" : String.valueOf(e.getKey());
				Object ev = e.getValue();
				raw.put(k, ev == null ? null : String.valueOf(ev));
			}
		}
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		for (Map.Entry<String, String> e : raw.entrySet()) {
			String infoType = e.getKey();
			String infoRaw = e.getValue();
			LinkedHashMap<String, Object> info = parseMainProtocolJson(infoRaw);
			applyDefaultProtocolTitle(infoType, info);
			attachDraftSubstructure(hashKey, infoType, info);
			result.put(infoType, info);
		}
		return result;
	}

	public void updateMemberRegisterFromAdmin(long companyId, String rawCountryCode, String contentHtml) {
		String trimmed = rawCountryCode == null ? null : rawCountryCode.trim();
		String effectiveCountry = effectiveCountryCodeForProtocolRead(trimmed);
		String hashKey = resolveProtocolHashKey(companyId, effectiveCountry);
		Map<String, Object> typeBlock = this.get(companyId, "member_register", effectiveCountry);
		Object innerObj = typeBlock.get("member_register");
		Map<?, ?> inner = innerObj instanceof Map<?, ?> m ? m : Collections.emptyMap();
		String title = toResponseString(inner.get("title"));
		String updateDate = toResponseString(inner.get("update_date"));
		String takeEffect = toResponseString(inner.get("take_effect_date"));
		LinkedHashMap<String, Object> protocolRow = new LinkedHashMap<>();
		protocolRow.put("type", "member_register");
		protocolRow.put("title", title);
		protocolRow.put("content", contentHtml == null ? "" : contentHtml);
		protocolRow.put("update_date", updateDate);
		protocolRow.put("take_effect_date", takeEffect);
		LinkedHashMap<String, Object> paramsForRedis = buildUpdateData(protocolRow, "member_register");
		persistHashField(companyId, hashKey, "member_register", paramsForRedis);
	}

	public LinkedHashMap<String, LinkedHashMap<String, String>> protocolsaleman(
			long companyId, String countryCode) {
		LinkedHashMap<String, LinkedHashMap<String, String>> outer = new LinkedHashMap<>();
		for (String type : SALESMAN_PROTOCOL_TYPES) {
			Map<String, Object> data = this.get(companyId, type, countryCode);
			LinkedHashMap<String, String> row = new LinkedHashMap<>(5);
			row.put("type", "");
			row.put("title", "");
			row.put("content", "");
			row.put("update_date", "");
			row.put("take_effect_date", "");
			Object rawRow = data.get(type);
			if (rawRow instanceof Map<?, ?> src) {
				row.put("type", toResponseString(src.get("type")));
				row.put("title", toResponseString(src.get("title")));
				row.put("content", toResponseString(src.get("content")));
				row.put("update_date", toResponseString(src.get("update_date")));
				row.put("take_effect_date", toResponseString(src.get("take_effect_date")));
			}
			outer.put(type, row);
		}
		return outer;
	}

	private static String toResponseString(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	public long getUpdateTime(long companyId, String countryCode) {
		Map<String, Object> privacyData = this.get(companyId, "privacy", countryCode);
		Map<String, Object> memberData = this.get(companyId, "member_register", countryCode);
		long privacyTs = extractUpdateTimeForMax(privacyData, "privacy");
		long memberTs = extractUpdateTimeForMax(memberData, "member_register");
		long rawMax = Math.max(privacyTs, memberTs);
		if (rawMax == 0L && privacyTs == 0L && memberTs == 0L) {
			return 0L;
		}
		return rawMax;
	}

	private static long extractUpdateTimeForMax(Map<String, Object> typeResult, String typeKey) {
		if (typeResult == null) {
			return 0L;
		}
		Object row = typeResult.get(typeKey);
		if (!(row instanceof Map<?, ?> m)) {
			return 0L;
		}
		Object v = m.get("update_time");
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
				return 0L;
			}
		}
		return 0L;
	}

	private String resolveProtocolHashKey(long companyId, String rawCountryCode) {
		String effective = effectiveCountryCodeForProtocolRead(rawCountryCode);
		String lang = effective.toLowerCase(Locale.ROOT).replace("-", "");
		String cacheName = "zhcn".equals(lang) ? "companyProtocol" : ("companyProtocol_" + lang);
		return "hash:" + cacheName + ":" + sha1HexUtf8(String.valueOf(companyId));
	}

	/**
	 * Optional {@code country_code} behaves like a defaulted request input: absent means {@code zh-CN};
	 * only {@code ""} and {@code "0"} collapse to the default; other values (including whitespace-only)
	 * are kept for Redis key language normalization.
	 */
	private static String effectiveCountryCodeForProtocolRead(String rawCountryCode) {
		if (rawCountryCode == null) {
			return "zh-CN";
		}
		if (isEmptyOrZeroCountryCodeInput(rawCountryCode)) {
			return "zh-CN";
		}
		return rawCountryCode;
	}

	private static boolean isEmptyOrZeroCountryCodeInput(String s) {
		return s.isEmpty() || "0".equals(s);
	}

	private LinkedHashMap<String, Object> parseMainProtocolJson(String infoRaw) {
		if (infoRaw == null || infoRaw.trim().isEmpty()) {
			return new LinkedHashMap<>();
		}
		try {
			JsonNode node = redisJsonMapper.readTree(infoRaw);
			if (node.isObject()) {
				return redisJsonMapper.convertValue(node, new TypeReference<LinkedHashMap<String, Object>>() {});
			}
		} catch (JsonProcessingException ignored) {
		}
		return new LinkedHashMap<>();
	}

	private void applyDefaultProtocolTitle(String infoType, LinkedHashMap<String, Object> info) {
		Object titleObj = info.get("title");
		if (titleObj == null || String.valueOf(titleObj).trim().isEmpty()) {
			info.put("title", PROTOCOL_TITLE_DEFAULT.getOrDefault(infoType, ""));
		}
	}

	private void attachDraftSubstructure(String hashKey, String infoType, LinkedHashMap<String, Object> info) {
		String draftKey = infoType + "_draft";
		Object draftField = companysRedisTemplate.opsForHash().get(hashKey, draftKey);
		String draftStr = draftField == null ? null : String.valueOf(draftField);
		if (draftStr == null || draftStr.trim().isEmpty()) {
			info.put("draft", Collections.emptyList());
			return;
		}
		try {
			JsonNode node = redisJsonMapper.readTree(draftStr);
			if (node.isArray()) {
				info.put("draft", redisJsonMapper.convertValue(node, new TypeReference<List<Object>>() {}));
			} else if (node.isObject()) {
				info.put("draft", redisJsonMapper.convertValue(node, new TypeReference<Map<String, Object>>() {}));
			} else {
				info.put("draft", Collections.emptyList());
			}
		} catch (JsonProcessingException e) {
			info.put("draft", Collections.emptyList());
		}
	}

	private List<Map<String, Object>> parseAndValidateData(Object rawData) {
		if (rawData == null || !(rawData instanceof List<?> list) || list.isEmpty()) {
			throw new BadRequestException("协议格式有误！");
		}
		List<Map<String, Object>> protocols = new ArrayList<>();
		for (Object elem : list) {
			Map<String, Object> row = parseDataElement(elem);
			if (!row.containsKey("type")
					|| row.get("type") == null
					|| String.valueOf(row.get("type")).trim().isEmpty()) {
				throw new BadRequestException("协议类型必填！");
			}
			protocols.add(row);
		}
		return protocols;
	}

	private Map<String, Object> parseDataElement(Object elem) {
		if (elem instanceof String s) {
			try {
				JsonNode node = redisJsonMapper.readTree(s);
				if (!node.isObject()) {
					throw new BadRequestException("协议格式有误！");
				}
				return redisJsonMapper.convertValue(node, new TypeReference<LinkedHashMap<String, Object>>() {});
			} catch (JsonProcessingException e) {
				throw new BadRequestException("协议格式有误！");
			}
		}
		if (elem instanceof Map<?, ?> m) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				if (e.getKey() != null) {
					row.put(String.valueOf(e.getKey()), e.getValue());
				}
			}
			return row;
		}
		throw new BadRequestException("协议格式有误！");
	}

	private void applyMemberLogoutDateRules(List<Map<String, Object>> protocols) {
		ZoneId z = ZoneId.systemDefault();
		String todayYmd = LocalDate.now(z).format(DateTimeFormatter.ISO_LOCAL_DATE);
		long todayEpoch = LocalDate.now(z).atStartOfDay(z).toEpochSecond();

		for (Map<String, Object> row : protocols) {
			if (!TYPE_MEMBER_LOGOUT.equals(String.valueOf(row.get("type")).trim())) {
				continue;
			}
			if (!row.containsKey("update_date") && !row.containsKey("take_effect_date")) {
				continue;
			}

			String updateStr = normalizeLogoutDateField(row.get("update_date"), todayYmd);
			String takeStr = normalizeLogoutDateField(row.get("take_effect_date"), todayYmd);
			row.put("update_date", updateStr);
			row.put("take_effect_date", takeStr);

			long updateEpoch;
			long takeEffectEpoch;
			try {
				updateEpoch = LocalDate.parse(updateStr, DateTimeFormatter.ISO_LOCAL_DATE)
						.atStartOfDay(z)
						.toEpochSecond();
				takeEffectEpoch = LocalDate.parse(takeStr, DateTimeFormatter.ISO_LOCAL_DATE)
						.atStartOfDay(z)
						.toEpochSecond();
			} catch (DateTimeParseException e) {
				throw new BadRequestException("协议格式有误！");
			}

			if (takeEffectEpoch < updateEpoch) {
				throw new ResourceException("更新日期不能大于生效日期");
			}
			if (todayEpoch > updateEpoch) {
				throw new ResourceException("更新日期不能小于当前日期");
			}
			if (todayEpoch > takeEffectEpoch) {
				throw new ResourceException("生效日期不能小于当前日期");
			}
		}
	}

	private static String normalizeLogoutDateField(Object val, String todayYmd) {
		if (val == null) {
			return todayYmd;
		}
		String s = String.valueOf(val).trim();
		if (s.isEmpty() || "0".equals(s)) {
			return todayYmd;
		}
		return s;
	}

	private LinkedHashMap<String, Object> buildUpdateData(Map<String, Object> protocol, String baseType) {
		LinkedHashMap<String, Object> updateData = new LinkedHashMap<>();
		updateData.put("type", baseType);
		updateData.put(
				"title",
				protocol.get("title") == null ? "" : String.valueOf(protocol.get("title")));

		if (TYPE_MEMBER_LOGOUT_CONFIG.equals(baseType)) {
			updateData.put(
					"new_rights",
					protocol.get("new_rights") == null ? "0" : String.valueOf(protocol.get("new_rights")));
			return updateData;
		}

		updateData.put(
				"content",
				protocol.get("content") == null ? "" : String.valueOf(protocol.get("content")));
		updateData.put(
				"update_date",
				protocol.get("update_date") == null ? "" : String.valueOf(protocol.get("update_date")));
		updateData.put(
				"take_effect_date",
				protocol.get("take_effect_date") == null
						? ""
						: String.valueOf(protocol.get("take_effect_date")));
		updateData.put("update_time", (int) Instant.now().getEpochSecond());
		return updateData;
	}

	private void persistHashField(long companyId, String hashKey, String typeKey, Map<String, Object> paramsForRedis) {
		boolean needDigest = !typeKey.contains("_draft") && !TYPE_MEMBER_LOGOUT_CONFIG.equals(typeKey);
		if (needDigest) {
			Object content = paramsForRedis.get("content");
			String contentStr = content == null ? "" : String.valueOf(content);
			paramsForRedis.put("digest", md5Utf8(contentStr));
		}

		String json;
		try {
			json = redisJsonMapper.writeValueAsString(paramsForRedis);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
		companysRedisTemplate.opsForHash().put(hashKey, typeKey, json);

		if (!needDigest) {
			return;
		}
		String digest = (String) paramsForRedis.get("digest");
		LambdaQueryWrapper<ProtocolUpdateLog> w = new LambdaQueryWrapper<>();
		w.eq(ProtocolUpdateLog::getCompanyId, companyId)
				.eq(ProtocolUpdateLog::getType, typeKey)
				.orderByDesc(ProtocolUpdateLog::getCreated)
				.last("LIMIT 1");
		ProtocolUpdateLog latest = protocolUpdateLogMapper.selectOne(w);
		if (latest != null && latest.getDigest() != null && digest.equals(latest.getDigest())) {
			return;
		}
		int now = (int) Instant.now().getEpochSecond();
		ProtocolUpdateLog e = new ProtocolUpdateLog();
		e.setCompanyId(companyId);
		e.setType(typeKey);
		e.setContent(
				String.valueOf(paramsForRedis.get("content") != null ? paramsForRedis.get("content") : ""));
		e.setDigest(digest);
		e.setCreated(now);
		e.setUpdated(now);
		protocolUpdateLogMapper.insert(e);
	}

	private static String md5Utf8(String content) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static String sha1HexUtf8(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
