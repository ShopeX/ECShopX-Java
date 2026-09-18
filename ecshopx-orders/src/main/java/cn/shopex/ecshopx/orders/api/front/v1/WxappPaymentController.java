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

package cn.shopex.ecshopx.orders.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappPaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontNoAuth
@RestController("ordersFrontV1WxappPayment")
@RequestMapping("/api/v1/h5app/wxapp")
public class WxappPaymentController {

	private static final Logger log = LoggerFactory.getLogger(WxappPaymentController.class);

	private final WxappPaymentService wxappPaymentService;
	private final ObjectMapper objectMapper;

	public WxappPaymentController(WxappPaymentService wxappPaymentService, ObjectMapper objectMapper) {
		this.wxappPaymentService = wxappPaymentService;
		this.objectMapper = objectMapper;
	}

	@FrontAuth
	@GetMapping(value = "/payment/config", name = "获取支付参数")
	public ResponseEntity<ApiResult<Map<String, Object>>> doPayment(HttpServletRequest request) {
		Map<String, Object> query = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		Map<String, Object> result = wxappPaymentService.doPayment(request, query);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@FrontAuth
	@PostMapping(value = "/payment_deposit", name = "预存款支付")
	public ResponseEntity<Void> depositPayment() {
		return ResponseEntity.ok().build();
	}

	@PostMapping(
			value = "/payment",
			name = "发起支付",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> payment(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		LinkedHashMap<String, Object> auth = resolveH5Auth(request, merged);

		try {
			String json = objectMapper.writeValueAsString(merged);
			log.info("payment params: {}", json);
		} catch (JsonProcessingException e) {
			log.warn("payment params serialization: {}", e.toString());
		}

		Map<String, Object> payResult = wxappPaymentService.payment(request, merged, auth);
		splitLogisticsItems(payResult);

		try {
			String outJson = objectMapper.writeValueAsString(payResult);
			log.info("payment payResult: {}", outJson);
		} catch (JsonProcessingException e) {
			log.warn("payment payResult serialization: {}", e.toString());
		}

		return ResponseEntity.ok(ApiResult.ok(payResult));
	}

	private LinkedHashMap<String, Object> resolveH5Auth(HttpServletRequest request, Map<String, Object> merged) {
		LinkedHashMap<String, Object> authMap;
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (!(rawAuth instanceof Map<?, ?>)) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (rawAuth instanceof Map<?, ?> authRaw) {
			authMap = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : authRaw.entrySet()) {
				authMap.put(String.valueOf(e.getKey()), e.getValue());
			}
		} else {
			authMap = new LinkedHashMap<>();
		}
		long companyId = parsePositiveLongOrZero(authMap.get("company_id"));
		if (companyId <= 0L) {
			companyId = parsePositiveLongOrZero(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
			if (companyId > 0L) {
				authMap.put("company_id", companyId);
			}
		}
		if (parsePositiveLongOrZero(authMap.get("company_id")) <= 0L) {
			companyId = parsePositiveLongOrZero(merged.get("company_id"));
			if (companyId > 0L) {
				authMap.put("company_id", companyId);
			}
		}
		if (parsePositiveLongOrZero(authMap.get("company_id")) <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (!authMap.containsKey("user_id") || authMap.get("user_id") == null) {
			authMap.put("user_id", 0L);
		}
		return authMap;
	}

	@SuppressWarnings("unchecked")
	private static void splitLogisticsItems(Map<String, Object> payResult) {
		Object oi = payResult.get("order_info");
		if (!(oi instanceof Map<?, ?>)) {
			return;
		}
		Map<String, Object> orderInfo = (Map<String, Object>) oi;
		Object itemsRaw = orderInfo.get("items");
		if (!(itemsRaw instanceof List<?> items)) {
			return;
		}
		List<Map<String, Object>> logistics = new ArrayList<>();
		List<Map<String, Object>> normal = new ArrayList<>();
		for (Object el : items) {
			if (!(el instanceof Map<?, ?> im)) {
				continue;
			}
			Map<String, Object> item = (Map<String, Object>) im;
			if (isLogisticsItem(item)) {
				logistics.add(item);
			} else {
				normal.add(item);
			}
		}
		orderInfo.put("items", normal);
		orderInfo.put("logistics_items", logistics);
		payResult.put("order_info", orderInfo);
	}

	private static boolean isLogisticsItem(Map<String, Object> item) {
		Object flag = item.get("is_logistics");
		if (Boolean.TRUE.equals(flag)) {
			return true;
		}
		return flag instanceof Number n && n.intValue() == 1;
	}

	private static long parsePositiveLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@PostMapping(
			value = "/payment/query",
			name = "支付结果查询",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> query(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		if (merged.containsKey("trade_id") && isFalsyTradeId(merged.get("trade_id"))) {
			throw new BadRequestException("支付单号不存在", 400);
		}
		String tradeId;
		if (!merged.containsKey("trade_id")) {
			tradeId = null;
		} else {
			tradeId = stringifyTradeIdForQuery(merged.get("trade_id"));
		}
		Map<String, Object> data = wxappPaymentService.query(tradeId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static boolean isFalsyTradeId(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (v instanceof CharSequence cs) {
			if (!StringUtils.hasText(cs.toString())) {
				return true;
			}
			String t = cs.toString().trim();
			return !StringUtils.hasText(t) || "0".equals(t);
		}
		if (v instanceof java.util.Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		return false;
	}

	private static String stringifyTradeIdForQuery(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return Long.toString(n.longValue());
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return null;
		}
		return s;
	}
}
