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

package cn.shopex.ecshopx.thirdparty.service.marketingcenter;

import cn.shopex.ecshopx.openapi.domain.OpenapiDeveloper;
import cn.shopex.ecshopx.openapi.mapper.OpenapiDeveloperMapper;
import cn.shopex.ecshopx.thirdparty.config.MarketingCenterTasksCompleteProperties;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Component
public class MarketingCenterOpenApiSignedFormClient {

	private static final Logger log = LoggerFactory.getLogger(MarketingCenterOpenApiSignedFormClient.class);

	private static final DateTimeFormatter TIMESTAMP_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final OpenapiDeveloperMapper openapiDeveloperMapper;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate;

	public MarketingCenterOpenApiSignedFormClient(
			OpenapiDeveloperMapper openapiDeveloperMapper,
			ObjectMapper objectMapper,
			MarketingCenterTasksCompleteProperties properties) {
		this.openapiDeveloperMapper = openapiDeveloperMapper;
		this.objectMapper = objectMapper;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(properties.getConnectTimeoutMs());
		factory.setReadTimeout(properties.getReadTimeoutMs());
		this.restTemplate = new RestTemplate(factory);
	}

	/**
	 * Signed POST to marketing center OpenAPI and parse JSON {@code data} object; failures return empty map.
	 */
	public Map<String, Object> postReturningParsedData(long companyId, String openApiMethod, Map<String, Object> dataPayload) {
		return postReturningParsedData(companyId, openApiMethod, dataPayload, false);
	}

	/**
	 * @param nestedIndexedDataFormEncoding when {@code true}, {@code data} map values that are nested maps are
	 *     encoded as {@code data[key][sub]=...} (used only for {@code basics.item.proccess} indexed SKU rows).
	 *     Standard OpenAPI form encoding for other callers leaves nested maps as scalar {@link String#valueOf}.
	 */
	private Map<String, Object> postReturningParsedData(
			long companyId, String openApiMethod, Map<String, Object> dataPayload, boolean nestedIndexedDataFormEncoding) {
		try {
			OpenapiDeveloper dev = openapiDeveloperMapper.selectOne(new LambdaQueryWrapper<OpenapiDeveloper>()
					.eq(OpenapiDeveloper::getCompanyId, companyId)
					.last("LIMIT 1"));
			if (dev == null) {
				log.debug("MarketingCenter:call-----参数配置错误 companyId={}", companyId);
				return new LinkedHashMap<>();
			}
			String base = dev.getExternalBaseUri();
			if (StringUtils.hasText(base)) {
				base = base.trim();
				while (base.endsWith("/")) {
					base = base.substring(0, base.length() - 1);
				}
			}
			if (!StringUtils.hasText(base)) {
				return new LinkedHashMap<>();
			}
			String appKey = dev.getExternalAppKey();
			String appSecret = dev.getExternalAppSecret();
			if (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret)) {
				log.debug("MarketingCenter:call-----参数配置错误 companyId={} (missing external app key/secret)", companyId);
				return new LinkedHashMap<>();
			}

			LinkedHashMap<String, Object> dataCopy = new LinkedHashMap<>(dataPayload);
			LinkedHashMap<String, Object> unsigned = new LinkedHashMap<>();
			unsigned.put("data", dataCopy);
			unsigned.put("timestamp", TIMESTAMP_FMT.format(ZonedDateTime.now()));
			unsigned.put("app_key", appKey);
			unsigned.put("version", "1.0");
			unsigned.put("method", openApiMethod);

			Map<String, Object> strvalled = strvalRecursiveMap(unsigned);
			String assembleString = assembleForSign(strvalled);
			String sign = md5HexUpper(appSecret + assembleString + appSecret);
			strvalled.put("sign", sign);

			String url = base + "/api/openapi";
			MultiValueMap<String, String> form =
					nestedIndexedDataFormEncoding ? toFormBodyWithNestedIndexedDataMaps(strvalled) : toFormBody(strvalled);

			if (log.isDebugEnabled()) {
				log.debug("MarketingCenter method={} input===> {}", openApiMethod, maskFormForLog(form));
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
			ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
			HttpStatusCode st = resp.getStatusCode();
			if (!st.is2xxSuccessful()) {
				return new LinkedHashMap<>();
			}
			String body = resp.getBody();
			if (log.isDebugEnabled()) {
				String snippet = body == null ? "" : body.length() > 2000 ? body.substring(0, 2000) + "..." : body;
				log.debug("MarketingCenter method={} result===> {}", openApiMethod, snippet);
			}
			if (!StringUtils.hasText(body)) {
				return new LinkedHashMap<>();
			}
			JsonNode root = objectMapper.readTree(body);
			JsonNode dataNode = root.get("data");
			if (dataNode == null || !dataNode.isObject()) {
				return new LinkedHashMap<>();
			}
			return jsonObjectNodeToMap(dataNode);
		} catch (Exception e) {
			log.debug("MarketingCenter postReturningParsedData failed method={} companyId={}: {}", openApiMethod, companyId, e.toString());
			return new LinkedHashMap<>();
		}
	}

	/**
	 * Signed POST and return the full JSON root object as a map (preserves {@code errcode}, {@code data}, etc.).
	 * On any failure returns an empty {@link LinkedHashMap} (truthy-empty for downstream guards).
	 */
	public Map<String, Object> postReturningFullRootMap(long companyId, String openApiMethod, Map<String, Object> dataPayload) {
		try {
			OpenapiDeveloper dev = openapiDeveloperMapper.selectOne(new LambdaQueryWrapper<OpenapiDeveloper>()
					.eq(OpenapiDeveloper::getCompanyId, companyId)
					.last("LIMIT 1"));
			if (dev == null) {
				log.debug("MarketingCenter:call-----参数配置错误 companyId={}", companyId);
				return new LinkedHashMap<>();
			}
			String base = dev.getExternalBaseUri();
			if (StringUtils.hasText(base)) {
				base = base.trim();
				while (base.endsWith("/")) {
					base = base.substring(0, base.length() - 1);
				}
			}
			if (!StringUtils.hasText(base)) {
				return new LinkedHashMap<>();
			}
			String appKey = dev.getExternalAppKey();
			String appSecret = dev.getExternalAppSecret();
			if (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret)) {
				log.debug("MarketingCenter:call-----参数配置错误 companyId={} (missing external app key/secret)", companyId);
				return new LinkedHashMap<>();
			}

			LinkedHashMap<String, Object> dataCopy = new LinkedHashMap<>(dataPayload);
			LinkedHashMap<String, Object> unsigned = new LinkedHashMap<>();
			unsigned.put("data", dataCopy);
			unsigned.put("timestamp", TIMESTAMP_FMT.format(ZonedDateTime.now()));
			unsigned.put("app_key", appKey);
			unsigned.put("version", "1.0");
			unsigned.put("method", openApiMethod);

			Map<String, Object> strvalled = strvalRecursiveMap(unsigned);
			String assembleString = assembleForSign(strvalled);
			String sign = md5HexUpper(appSecret + assembleString + appSecret);
			strvalled.put("sign", sign);

			String url = base + "/api/openapi";
			MultiValueMap<String, String> form = toFormBody(strvalled);

			if (log.isDebugEnabled()) {
				log.debug("MarketingCenter method={} input===> {}", openApiMethod, maskFormForLog(form));
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
			ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
			HttpStatusCode st = resp.getStatusCode();
			if (!st.is2xxSuccessful()) {
				return new LinkedHashMap<>();
			}
			String body = resp.getBody();
			if (log.isDebugEnabled()) {
				String snippet = body == null ? "" : body.length() > 2000 ? body.substring(0, 2000) + "..." : body;
				log.debug("MarketingCenter method={} result===> {}", openApiMethod, snippet);
			}
			if (!StringUtils.hasText(body)) {
				return new LinkedHashMap<>();
			}
			JsonNode root = objectMapper.readTree(body);
			if (root == null || !root.isObject()) {
				return new LinkedHashMap<>();
			}
			return jsonObjectNodeToMap(root);
		} catch (Exception e) {
			log.debug("MarketingCenter postReturningFullRootMap failed method={} companyId={}: {}", openApiMethod, companyId,
					e.toString());
			return new LinkedHashMap<>();
		}
	}

	/**
	 * Same as {@link #postReturningParsedData} but signs and posts {@code data} as a JSON array string (indexed list
	 * payload), matching marketing center OpenAPI when {@code data} is a list root.
	 */
	public Map<String, Object> postReturningParsedDataWithJsonArrayData(long companyId, String openApiMethod,
			List<Map<String, Object>> dataArrayRoot) {
		try {
			OpenapiDeveloper dev = openapiDeveloperMapper.selectOne(new LambdaQueryWrapper<OpenapiDeveloper>()
					.eq(OpenapiDeveloper::getCompanyId, companyId)
					.last("LIMIT 1"));
			if (dev == null) {
				log.debug("MarketingCenter:call-----参数配置错误 companyId={}", companyId);
				return new LinkedHashMap<>();
			}
			String base = dev.getExternalBaseUri();
			if (StringUtils.hasText(base)) {
				base = base.trim();
				while (base.endsWith("/")) {
					base = base.substring(0, base.length() - 1);
				}
			}
			if (!StringUtils.hasText(base)) {
				return new LinkedHashMap<>();
			}
			String appKey = dev.getExternalAppKey();
			String appSecret = dev.getExternalAppSecret();
			if (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret)) {
				log.debug("MarketingCenter:call-----参数配置错误 companyId={} (missing external app key/secret)", companyId);
				return new LinkedHashMap<>();
			}

			String dataJson = objectMapper.writeValueAsString(dataArrayRoot == null ? List.of() : dataArrayRoot);
			LinkedHashMap<String, Object> unsigned = new LinkedHashMap<>();
			unsigned.put("data", dataJson);
			unsigned.put("timestamp", TIMESTAMP_FMT.format(ZonedDateTime.now()));
			unsigned.put("app_key", appKey);
			unsigned.put("version", "1.0");
			unsigned.put("method", openApiMethod);

			Map<String, Object> strvalled = strvalRecursiveMap(unsigned);
			String assembleString = assembleForSign(strvalled);
			String sign = md5HexUpper(appSecret + assembleString + appSecret);
			strvalled.put("sign", sign);

			String url = base + "/api/openapi";
			MultiValueMap<String, String> form = toFormBody(strvalled);

			if (log.isDebugEnabled()) {
				log.debug("MarketingCenter method={} input===> {}", openApiMethod, maskFormForLog(form));
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
			ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
			HttpStatusCode st = resp.getStatusCode();
			if (!st.is2xxSuccessful()) {
				return new LinkedHashMap<>();
			}
			String body = resp.getBody();
			if (log.isDebugEnabled()) {
				String snippet = body == null ? "" : body.length() > 2000 ? body.substring(0, 2000) + "..." : body;
				log.debug("MarketingCenter method={} result===> {}", openApiMethod, snippet);
			}
			if (!StringUtils.hasText(body)) {
				return new LinkedHashMap<>();
			}
			JsonNode root = objectMapper.readTree(body);
			JsonNode dataNode = root.get("data");
			if (dataNode == null || !dataNode.isObject()) {
				return new LinkedHashMap<>();
			}
			return jsonObjectNodeToMap(dataNode);
		} catch (Exception e) {
			log.debug("MarketingCenter postReturningParsedDataWithJsonArrayData failed method={} companyId={}: {}",
					openApiMethod, companyId, e.toString());
			return new LinkedHashMap<>();
		}
	}

	public Map<String, Object> basicsSalespersonUseridToOpenUserid(long companyId, String workUserid) {
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("work_userid", workUserid);
		return postReturningParsedData(companyId, "basics.salesperson.useridtopenuserid", payload);
	}

	public Map<String, Object> basicsAftersalesProccess(long companyId, Map<String, Object> params) {
		return postReturningParsedData(companyId, "basics.aftersales.proccess", params);
	}

	public Map<String, Object> basicsOrderProccess(long companyId, Map<String, Object> params) {
		return postReturningParsedData(companyId, "basics.order.proccess", params);
	}

	public Map<String, Object> basicsItemProccess(long companyId, Map<String, Object> params) {
		return postReturningParsedData(companyId, "basics.item.proccess", params, true);
	}

	/**
	 * Signs and posts {@code params} to OpenAPI method {@code basics.distribution.proccess}; returns the parsed
	 * {@code data} object as a map, or an empty map on failure.
	 */
	public Map<String, Object> basicsDistributionProccess(long companyId, Map<String, Object> params) {
		return postReturningParsedData(companyId, "basics.distribution.proccess", params);
	}

	/**
	 * Signed POST for distribution edit sync; parses JSON {@code data} object as a map, or an empty map on failure.
	 */
	public Map<String, Object> basicsDistributionEditProccess(long companyId, Map<String, Object> params) {
		return postReturningParsedData(companyId, "basics.distribution.edit.proccess", params);
	}

	public Map<String, Object> basicsOrderPay(long companyId, Map<String, Object> params) {
		return postReturningParsedData(companyId, "basics.order.pay", params);
	}

	private static Map<String, Object> jsonObjectNodeToMap(JsonNode node) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		node.fields().forEachRemaining(e -> {
			String k = e.getKey();
			JsonNode v = e.getValue();
			if (v == null || v.isNull()) {
				out.put(k, null);
			} else if (v.isObject()) {
				out.put(k, jsonObjectNodeToMap(v));
			} else if (v.isArray()) {
				out.put(k, jsonArrayNodeToList(v));
			} else if (v.isBoolean()) {
				out.put(k, v.booleanValue());
			} else if (v.isIntegralNumber()) {
				out.put(k, v.longValue());
			} else if (v.isNumber()) {
				out.put(k, v.doubleValue());
			} else {
				out.put(k, v.asText());
			}
		});
		return out;
	}

	private static List<Object> jsonArrayNodeToList(JsonNode arr) {
		List<Object> list = new ArrayList<>(arr.size());
		for (JsonNode v : arr) {
			if (v == null || v.isNull()) {
				list.add(null);
			} else if (v.isObject()) {
				list.add(jsonObjectNodeToMap(v));
			} else if (v.isArray()) {
				list.add(jsonArrayNodeToList(v));
			} else if (v.isBoolean()) {
				list.add(v.booleanValue());
			} else if (v.isIntegralNumber()) {
				list.add(v.longValue());
			} else if (v.isNumber()) {
				list.add(v.doubleValue());
			} else {
				list.add(v.asText());
			}
		}
		return list;
	}

	public void post(long companyId, String openApiMethod, Map<String, Object> dataPayload) throws Exception {
		OpenapiDeveloper dev = openapiDeveloperMapper.selectOne(new LambdaQueryWrapper<OpenapiDeveloper>()
				.eq(OpenapiDeveloper::getCompanyId, companyId)
				.last("LIMIT 1"));
		if (dev == null) {
			log.debug("MarketingCenter:call-----参数配置错误 companyId={}", companyId);
			return;
		}
		String base = dev.getExternalBaseUri();
		if (StringUtils.hasText(base)) {
			base = base.trim();
			while (base.endsWith("/")) {
				base = base.substring(0, base.length() - 1);
			}
		}
		if (!StringUtils.hasText(base)) {
			return;
		}
		String appKey = dev.getExternalAppKey();
		String appSecret = dev.getExternalAppSecret();
		if (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret)) {
			log.debug("MarketingCenter:call-----参数配置错误 companyId={} (missing external app key/secret)", companyId);
			return;
		}

		LinkedHashMap<String, Object> dataCopy = new LinkedHashMap<>(dataPayload);
		LinkedHashMap<String, Object> unsigned = new LinkedHashMap<>();
		unsigned.put("data", dataCopy);
		unsigned.put("timestamp", TIMESTAMP_FMT.format(ZonedDateTime.now()));
		unsigned.put("app_key", appKey);
		unsigned.put("version", "1.0");
		unsigned.put("method", openApiMethod);

		Map<String, Object> strvalled = strvalRecursiveMap(unsigned);
		String assembleString = assembleForSign(strvalled);
		String sign = md5HexUpper(appSecret + assembleString + appSecret);
		strvalled.put("sign", sign);

		String url = base + "/api/openapi";
		MultiValueMap<String, String> form = toFormBody(strvalled);

		if (log.isDebugEnabled()) {
			log.debug("MarketingCenter method={} input===> {}", openApiMethod, maskFormForLog(form));
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
		ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
		String body = resp.getBody();
		if (log.isDebugEnabled()) {
			String snippet = body == null ? "" : body.length() > 2000 ? body.substring(0, 2000) + "..." : body;
			log.debug("MarketingCenter method={} result===> {}", openApiMethod, snippet);
		}
	}

	private static String maskFormForLog(MultiValueMap<String, String> form) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, java.util.List<String>> e : form.entrySet()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(e.getKey()).append("=");
			if ("sign".equals(e.getKey())) {
				sb.append("***");
			} else {
				sb.append(e.getValue());
			}
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private MultiValueMap<String, String> toFormBody(Map<String, Object> strvalledTop) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		Object dataObj = strvalledTop.get("data");
		if (dataObj instanceof Map<?, ?> rawData) {
			for (Map.Entry<?, ?> e : rawData.entrySet()) {
				String k = String.valueOf(e.getKey());
				Object val = e.getValue();
				if (val instanceof Collection<?> coll) {
					for (Object item : coll) {
						form.add("data[" + k + "][]", item == null ? "" : String.valueOf(item));
					}
				} else {
					form.add("data[" + k + "]", val == null ? "" : String.valueOf(val));
				}
			}
		} else if (dataObj instanceof String s) {
			form.add("data", s);
		}
		for (Map.Entry<String, Object> e : strvalledTop.entrySet()) {
			if ("data".equals(e.getKey())) {
				continue;
			}
			form.add(e.getKey(), e.getValue() == null ? "" : String.valueOf(e.getValue()));
		}
		return form;
	}

	/**
	 * Form body for {@code basics.item.proccess} only: expands {@code data} entries whose values are maps into
	 * bracketed field names (e.g. {@code data[0][item_bn]}). Other OpenAPI methods use {@link #toFormBody}.
	 */
	private MultiValueMap<String, String> toFormBodyWithNestedIndexedDataMaps(Map<String, Object> strvalledTop) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		Object dataObj = strvalledTop.get("data");
		if (dataObj instanceof Map<?, ?> rawData) {
			for (Map.Entry<?, ?> e : rawData.entrySet()) {
				String k = String.valueOf(e.getKey());
				Object val = e.getValue();
				if (val instanceof Collection<?> coll) {
					for (Object item : coll) {
						form.add("data[" + k + "][]", item == null ? "" : String.valueOf(item));
					}
				} else if (val instanceof Map<?, ?> nested) {
					addNestedDataFormFields(form, "data[" + k + "]", nested);
				} else {
					form.add("data[" + k + "]", val == null ? "" : String.valueOf(val));
				}
			}
		} else if (dataObj instanceof String s) {
			form.add("data", s);
		}
		for (Map.Entry<String, Object> e : strvalledTop.entrySet()) {
			if ("data".equals(e.getKey())) {
				continue;
			}
			form.add(e.getKey(), e.getValue() == null ? "" : String.valueOf(e.getValue()));
		}
		return form;
	}

	private static void addNestedDataFormFields(
			MultiValueMap<String, String> form, String prefix, Map<?, ?> nested) {
		for (Map.Entry<?, ?> se : nested.entrySet()) {
			String subk = String.valueOf(se.getKey());
			Object subv = se.getValue();
			String field = prefix + "[" + subk + "]";
			if (subv instanceof Map<?, ?> deeper) {
				addNestedDataFormFields(form, field, deeper);
			} else if (subv instanceof Collection<?> coll) {
				for (Object item : coll) {
					form.add(field + "[]", item == null ? "" : String.valueOf(item));
				}
			} else {
				form.add(field, subv == null ? "" : String.valueOf(subv));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> strvalRecursiveMap(Map<String, Object> src) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : src.entrySet()) {
			out.put(e.getKey(), strvalValue(e.getValue()));
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static Object strvalValue(Object v) {
		if (v == null) {
			return "";
		}
		if (v instanceof Map<?, ?> m) {
			LinkedHashMap<String, Object> inner = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				inner.put(String.valueOf(e.getKey()), strvalValue(e.getValue()));
			}
			return inner;
		}
		if (v instanceof Collection<?> coll) {
			List<Object> inner = new ArrayList<>(coll.size());
			for (Object o : coll) {
				inner.add(strvalValue(o));
			}
			return inner;
		}
		if (v instanceof Boolean b) {
			return b ? "1" : "0";
		}
		if (v instanceof Number n) {
			return numberToStrvalString(n);
		}
		return String.valueOf(v);
	}

	private static String numberToStrvalString(Number n) {
		if (n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte) {
			return String.valueOf(n.longValue());
		}
		if (n instanceof java.math.BigInteger bi) {
			return bi.toString();
		}
		double d = n.doubleValue();
		if (Double.isNaN(d) || Double.isInfinite(d)) {
			return "";
		}
		if (d == Math.rint(d) && d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
			return String.valueOf((long) d);
		}
		return String.valueOf(n);
	}

	private String assembleForSign(Map<String, Object> strvalMap) throws JsonProcessingException {
		TreeMap<String, Object> sorted = new TreeMap<>(Comparator.naturalOrder());
		sorted.putAll(strvalMap);
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Object> e : sorted.entrySet()) {
			Object v = e.getValue();
			if (v == null) {
				continue;
			}
			sb.append(e.getKey());
			if (v instanceof Map<?, ?> m) {
				sb.append(objectMapper.writeValueAsString(castToStringObjectMap(m)));
			} else if (v instanceof List<?> list) {
				sb.append(objectMapper.writeValueAsString(list));
			} else {
				sb.append(v);
			}
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> castToStringObjectMap(Map<?, ?> m) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	private static String md5HexUpper(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(dig.length * 2);
			for (byte b : dig) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString().toUpperCase();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
