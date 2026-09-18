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

package cn.shopex.ecshopx.thirdparty.service.dmcrm;

import cn.shopex.ecshopx.common.port.point.PointMemberDmPointMemberInfoReadPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class DmCrmTradeFinishOrderSyncHttpService implements DmCrmTradeFinishOrderSyncPort {

	private static final Logger log = LoggerFactory.getLogger(DmCrmTradeFinishOrderSyncHttpService.class);

	private static final String HOPE_BASE = "https://hope.demogic.com";
	private static final String WORKER = "/cgi-api/order/add_online_store_order";
	private static final String TOKEN_WORKER = "/cgi-api/auth/get_token";
	private static final String SETTING_PREFIX = "DmCrmSetting:";

	private static final ZoneId CN = ZoneId.of("Asia/Shanghai");

	private final ObjectMapper objectMapper;
	private final StringRedisTemplate stringRedisTemplate;
	private final DmCrmApiLogService dmCrmApiLogService;
	private final PointMemberDmPointMemberInfoReadPort pointMemberDmPointMemberInfoReadPort;
	private final RestTemplate restTemplate = new RestTemplate();

	public DmCrmTradeFinishOrderSyncHttpService(
			ObjectMapper objectMapper,
			StringRedisTemplate stringRedisTemplate,
			DmCrmApiLogService dmCrmApiLogService,
			PointMemberDmPointMemberInfoReadPort pointMemberDmPointMemberInfoReadPort) {
		this.objectMapper = objectMapper;
		this.stringRedisTemplate = stringRedisTemplate;
		this.dmCrmApiLogService = dmCrmApiLogService;
		this.pointMemberDmPointMemberInfoReadPort = pointMemberDmPointMemberInfoReadPort;
	}

	@Override
	public void syncOrderForTradeFinish(long companyId, String orderId, Map<String, Object> tradeFinishOrderPayload) {
		long t0 = System.currentTimeMillis();
		Map<String, Object> postBody;
		String url;
		try {
			url = buildRequestUrl(companyId);
			postBody = buildParams(companyId, tradeFinishOrderPayload);
		} catch (Exception e) {
			log.debug("dm crm sync order skip url build: {}", e.toString());
			return;
		}
		String json;
		try {
			json = objectMapper.writeValueAsString(postBody);
		} catch (JsonProcessingException e) {
			log.debug("dm crm sync order skip serialize: {}", e.toString());
			return;
		}
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.add("ruid", Objects.toString(tradeFinishOrderPayload.get("order_id"), orderId));
			HttpEntity<String> entity = new HttpEntity<>(json, headers);
			ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
			int secs = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - t0);
			dmCrmApiLogService.recordApiCall(companyId, WORKER, postBody, resp.getBody(), "request", "success", Math.max(0, secs));
		} catch (RestClientException e) {
			int secs = (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - t0);
			dmCrmApiLogService.recordApiCall(companyId, WORKER, postBody, e.getMessage(), "request", "fail", Math.max(0, secs));
			log.debug("dm crm sync order http error: {}", e.toString());
		}
	}

	private Map<String, Object> buildParams(long companyId, Map<String, Object> p) throws JsonProcessingException {
		long userId = toLong(p.get("user_id"));
		Map<String, Object> memberInfo = pointMemberDmPointMemberInfoReadPort.getMemberInfo(userId, companyId);
		Object orderNoSrc = p.get("refund_bn");
		Object originalOrderNo = p.get("order_id");
		String mobile = Objects.toString(memberInfo.getOrDefault("mobile", ""), "");
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("mobile", mobile);
		root.put(
				"orderNo",
				orderNoSrc != null && StringUtils.hasText(String.valueOf(orderNoSrc))
						? String.valueOf(orderNoSrc)
						: String.valueOf(originalOrderNo));
		root.put("originalOrderNo", String.valueOf(originalOrderNo));
		root.put("goodsAmount", yuanDiv100(p.get("item_fee")));
		root.put("deliveryPaymentAmount", yuanDiv100Int(intOrZero(p.get("freight_fee"))));
		root.put("paymentAmount", yuanDiv100Int(intOrZero(p.get("total_fee"))));
		root.put("orderStatus", 1);
		root.put("channelCode", "c_brand_mall");
		root.put("receiverName", Objects.toString(p.get("receiver_name"), ""));
		root.put("phoneNumber", Objects.toString(p.get("receiver_mobile"), ""));
		root.put("receiverZip", Objects.toString(p.get("receiver_zip"), ""));
		root.put("receiverAddress", Objects.toString(p.get("receiver_address"), ""));
		root.put("remark", Objects.toString(p.get("remark"), ""));
		root.put("orderTime", formatOrderTime(p.get("create_time")));
		root.put("storeName", Objects.toString(p.get("storeName"), ""));
		root.put("storeCode", Objects.toString(p.get("storeCode"), ""));
		root.put("clerkCode", Objects.toString(p.get("clerkCode"), ""));
		root.put("clerkName", Objects.toString(p.get("clerkName"), ""));
		root.put(
				"usedMemberPoints",
				p.containsKey("usedMemberPoints") ? intOrZero(p.get("usedMemberPoints")) : intOrZero(p.get("point_fee")));
		root.put("couponCode", resolveCouponCode(p));

		Object itemsObj = p.get("items");
		List<Map<String, Object>> lineItems = new ArrayList<>();
		if (itemsObj instanceof List<?> list) {
			for (Object o : list) {
				if (o instanceof Map<?, ?> m) {
					@SuppressWarnings("unchecked")
					Map<String, Object> om = new LinkedHashMap<>((Map<String, Object>) m);
					lineItems.add(formatLineItem(om));
				}
			}
		}
		root.put("item", lineItems);
		return root;
	}

	private static String yuanDiv100(Object raw) {
		if (raw == null) {
			return "0.00";
		}
		BigDecimal cents;
		try {
			if (raw instanceof Number n) {
				cents = BigDecimal.valueOf(n.doubleValue());
			} else {
				cents = new BigDecimal(String.valueOf(raw).trim());
			}
		} catch (Exception e) {
			cents = BigDecimal.ZERO;
		}
		return cents.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String yuanDiv100Int(int fenCents) {
		return BigDecimal.valueOf(fenCents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private String resolveCouponCode(Map<String, Object> p) throws JsonProcessingException {
		Object di = p.get("discount_info");
		if (!(di instanceof String s) || !StringUtils.hasText(s)) {
			return "-1";
		}
		JsonNode node = objectMapper.readTree(s.trim());
		if (!node.isArray()) {
			return "-1";
		}
		List<String> codes = new ArrayList<>();
		for (JsonNode n : node) {
			if (!n.has("dm_card_code") || n.get("dm_card_code").isNull()) {
				continue;
			}
			String c = n.get("dm_card_code").asText("");
			if (StringUtils.hasText(c)) {
				codes.add(c);
			}
		}
		return codes.isEmpty() ? "-1" : String.join(",", codes);
	}

	private static Map<String, Object> formatLineItem(Map<String, Object> v) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("goodsCode", Objects.toString(v.get("goods_bn"), ""));
		row.put("goodsTitle", Objects.toString(v.get("item_name"), ""));
		row.put("goodsCategoryName", "");
		row.put("skuCode", Objects.toString(v.get("item_bn"), ""));
		row.put("skuName", Objects.toString(v.get("item_name"), ""));
		row.put("skuNum", Objects.toString(v.get("num"), ""));
		row.put("skuAmount", yuanDiv100(v.get("item_fee")));
		row.put("totalAmount", yuanDiv100Flexible(v.get("item_fee_t")));
		row.put("payAmount", yuanDiv100(v.get("total_fee")));
		row.put("price", yuanDiv100(v.get("price")));
		row.put("imageUrl", Objects.toString(v.get("pic"), ""));
		boolean gift = lineItemIsGift(v.get("is_gift"));
		row.put("goodsType", gift ? 2 : 1);
		return row;
	}

	private static boolean lineItemIsGift(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = String.valueOf(raw).trim();
		return !s.isEmpty() && !"0".equals(s);
	}

	private static String yuanDiv100Flexible(Object raw) {
		if (raw == null) {
			return "0.0000";
		}
		try {
			BigDecimal b =
					raw instanceof BigDecimal bd
							? bd
							: new BigDecimal(String.valueOf(raw).trim());
			return b.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP).toPlainString();
		} catch (Exception e) {
			return "0.0000";
		}
	}

	private static String formatOrderTime(Object createTime) {
		long sec =
				createTime instanceof Number n
						? n.longValue()
						: (long) intOrZero(createTime);
		if (sec <= 0L) {
			return "";
		}
		return Instant.ofEpochSecond(sec).atZone(CN).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private static int intOrZero(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (Exception e) {
			return 0;
		}
	}

	private String buildRequestUrl(long companyId) throws JsonProcessingException {
		JsonNode settings = loadSettings(companyId);
		String appKey = textFromSettings(settings, "app_key");
		String appSecret = textFromSettings(settings, "app_secret");
		String entSign = textFromSettings(settings, "ent_sign");
		if (!StringUtils.hasText(appKey) || !StringUtils.hasText(appSecret) || !StringUtils.hasText(entSign)) {
			throw new IllegalStateException("missing dm crm credentials");
		}
		String token = getOrFetchToken(companyId, appKey, appSecret);
		if (!StringUtils.hasText(token)) {
			throw new IllegalStateException("token unavailable");
		}
		return HOPE_BASE + WORKER + "?token=" + urlEncode(token) + "&entSign=" + urlEncode(entSign);
	}

	private static String urlEncode(String s) {
		return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private JsonNode loadSettings(long companyId) throws JsonProcessingException {
		String key = SETTING_PREFIX + sha1Hex(String.valueOf(companyId));
		String raw = stringRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return objectMapper.readTree("{\"is_open\":false}");
		}
		return objectMapper.readTree(raw);
	}

	private static String textFromSettings(JsonNode settings, String field) {
		if (settings == null || !settings.has(field) || settings.get(field).isNull()) {
			return "";
		}
		JsonNode n = settings.get(field);
		return n.asText("");
	}

	private String getOrFetchToken(long companyId, String appKey, String appSecret) throws JsonProcessingException {
		String tokenKey = "damo_token:" + companyId;
		String cached = stringRedisTemplate.opsForValue().get(tokenKey);
		if (StringUtils.hasText(cached)) {
			JsonNode wrap = objectMapper.readTree(cached);
			String token = wrap.has("token") ? wrap.get("token").asText("") : "";
			long expireTime =
					wrap.has("expireTime") && wrap.get("expireTime").isNumber()
							? wrap.get("expireTime").asLong()
							: 0L;
			long nowMs = System.currentTimeMillis();
			if (StringUtils.hasText(token) && expireTime - nowMs > 60L * 60L * 1000L) {
				return token;
			}
		}
		return fetchNewToken(companyId, appKey, appSecret, tokenKey);
	}

	private String fetchNewToken(long companyId, String appKey, String appSecret, String tokenKey)
			throws JsonProcessingException {
		String url = HOPE_BASE + TOKEN_WORKER;
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("appKey", appKey);
		body.put("appSecret", appSecret);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String json = objectMapper.writeValueAsString(body);
		HttpEntity<String> entity = new HttpEntity<>(json, headers);
		ResponseEntity<String> resp;
		try {
			resp = restTemplate.postForEntity(url, entity, String.class);
		} catch (RestClientException e) {
			return null;
		}
		String rawBody = resp.getBody();
		if (rawBody == null || rawBody.isBlank()) {
			return null;
		}
		JsonNode data = objectMapper.readTree(rawBody);
		JsonNode codeNode = data.get("code");
		boolean ok = codeNode != null && "0".equals(codeNode.asText());
		if (!ok) {
			return null;
		}
		JsonNode result = data.get("result");
		if (result == null || !result.isObject()) {
			return null;
		}
		String token = result.has("token") ? result.get("token").asText("") : "";
		long expireTime =
				result.has("expireTime") && result.get("expireTime").isNumber()
						? result.get("expireTime").asLong()
						: 0L;
		if (!StringUtils.hasText(token) || expireTime <= 0) {
			return null;
		}
		Map<String, Object> toStore = new LinkedHashMap<>();
		toStore.put("token", token);
		toStore.put("expireTime", expireTime);
		long nowMs = System.currentTimeMillis();
		long ttlMillis = expireTime - nowMs;
		long ttlSeconds = Math.max(60L, ttlMillis / 1000L);
		stringRedisTemplate
				.opsForValue()
				.set(tokenKey, objectMapper.writeValueAsString(toStore), ttlSeconds, TimeUnit.SECONDS);
		return token;
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(d.length * 2);
			for (byte b : d) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
