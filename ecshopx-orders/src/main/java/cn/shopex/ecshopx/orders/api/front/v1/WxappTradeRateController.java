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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappTradeRateAddRateService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappTradeRateDetailService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappTradeRateListService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappTradeRatePraiseCheckService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappTradeRatePraiseNumService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappTradeRatePraiseService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappTradeRatePraiseStatusService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappTradeRateReplyListService;
import cn.shopex.ecshopx.orders.service.front.wxapp.WxappTradeRateReplyRateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@RestController("ordersFrontV1WxappTradeRate")
@RequestMapping("/api/v1/h5app/wxapp/order")
public class WxappTradeRateController {

	private final WxappTradeRateAddRateService wxappTradeRateAddRateService;
	private final WxappTradeRateDetailService wxappTradeRateDetailService;
	private final WxappTradeRateListService wxappTradeRateListService;
	private final WxappTradeRatePraiseService wxappTradeRatePraiseService;
	private final WxappTradeRatePraiseCheckService wxappTradeRatePraiseCheckService;
	private final WxappTradeRatePraiseNumService wxappTradeRatePraiseNumService;
	private final WxappTradeRatePraiseStatusService wxappTradeRatePraiseStatusService;
	private final WxappTradeRateReplyRateService wxappTradeRateReplyRateService;
	private final WxappTradeRateReplyListService wxappTradeRateReplyListService;
	private final ObjectMapper objectMapper;

	public WxappTradeRateController(
			WxappTradeRateAddRateService wxappTradeRateAddRateService,
			WxappTradeRateDetailService wxappTradeRateDetailService,
			WxappTradeRateListService wxappTradeRateListService,
			WxappTradeRatePraiseService wxappTradeRatePraiseService,
			WxappTradeRatePraiseCheckService wxappTradeRatePraiseCheckService,
			WxappTradeRatePraiseNumService wxappTradeRatePraiseNumService,
			WxappTradeRatePraiseStatusService wxappTradeRatePraiseStatusService,
			WxappTradeRateReplyRateService wxappTradeRateReplyRateService,
			WxappTradeRateReplyListService wxappTradeRateReplyListService,
			ObjectMapper objectMapper) {
		this.wxappTradeRateAddRateService = wxappTradeRateAddRateService;
		this.wxappTradeRateDetailService = wxappTradeRateDetailService;
		this.wxappTradeRateListService = wxappTradeRateListService;
		this.wxappTradeRatePraiseService = wxappTradeRatePraiseService;
		this.wxappTradeRatePraiseCheckService = wxappTradeRatePraiseCheckService;
		this.wxappTradeRatePraiseNumService = wxappTradeRatePraiseNumService;
		this.wxappTradeRatePraiseStatusService = wxappTradeRatePraiseStatusService;
		this.wxappTradeRateReplyRateService = wxappTradeRateReplyRateService;
		this.wxappTradeRateReplyListService = wxappTradeRateReplyListService;
		this.objectMapper = objectMapper;
	}

	@FrontAuth
	@PostMapping(
			value = "/rate/create",
			name = "用户评价",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> addRate(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object data = wxappTradeRateAddRateService.addRate(merged, auth);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@PostMapping(
			value = "/replyRate",
			name = "评价回复",
			consumes = {
				MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE,
				MediaType.APPLICATION_FORM_URLENCODED_VALUE
			},
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> replyRate(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		merged.put("company_id", auth.get("company_id"));
		merged.put("user_id", auth.get("user_id"));
		merged.put("unionid", auth.get("unionid"));
		Object data = wxappTradeRateReplyRateService.replyRate(merged, auth);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(
			value = "/rate/praise/{rate_id}",
			name = "评价点赞",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Integer>>> ratePraise(
			HttpServletRequest request, @PathVariable("rate_id") String rateIdRaw) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		long userId = longVal(auth.get("user_id"));
		if (userId <= 0L) {
			userId = longVal(merged.get("user_id"));
		}
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			companyId = longVal(merged.get("company_id"));
		}
		if (userId <= 0L || companyId <= 0L) {
			throw new ResourceException("参数错误");
		}
		validatePositiveRateIdFromPath(rateIdRaw);
		String trimmed = rateIdRaw.trim();
		Map<String, Integer> data = wxappTradeRatePraiseService.ratePraise(companyId, userId, trimmed);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@GetMapping(
			value = "/rate/praise/check/{rate_id}",
			name = "评价点赞验证",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Boolean>>> ratePraiseCheck(
			HttpServletRequest request, @PathVariable("rate_id") String rateIdRaw) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		long userId = longVal(auth.get("user_id"));
		if (userId <= 0L) {
			userId = longVal(merged.get("user_id"));
		}
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			companyId = longVal(merged.get("company_id"));
		}
		if (userId <= 0L || companyId <= 0L) {
			throw new ResourceException("参数错误");
		}
		if (rateIdRaw == null || !StringUtils.hasText(rateIdRaw.trim())) {
			throw new ResourceException("参数错误");
		}
		String trimmed = rateIdRaw.trim();
		long rateId;
		try {
			rateId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("参数错误");
		}
		if (rateId <= 0L) {
			throw new ResourceException("参数错误");
		}
		boolean status = wxappTradeRatePraiseCheckService.ratePraiseCheck(companyId, userId, rateId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", status)));
	}

	@FrontAuth
	@GetMapping(
			value = "/ratePraise/status",
			name = "点赞状态",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> ratePraiseStatus(
			HttpServletRequest request,
			@RequestParam(value = "rate_ids", required = false) String rateIdsRaw) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth == null) {
			rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		}
		if (!(rawAuth instanceof Map<?, ?> authRaw)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : authRaw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		long companyId = longVal(auth.get("company_id"));
		long userId = longVal(auth.get("user_id"));
		if (companyId <= 0L || userId <= 0L) {
			throw new ResourceException("参数错误");
		}

		if (rateIdsRaw == null || rateIdsRaw.isEmpty() || "0".equals(rateIdsRaw)) {
			throw new BadRequestException("参数异常");
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(rateIdsRaw);
		} catch (JsonProcessingException e) {
			throw new ResourceException("参数错误");
		}
		if (!root.isArray()) {
			throw new ResourceException("参数错误");
		}
		List<String> orderedKeys = new ArrayList<>();
		for (JsonNode node : root) {
			if (node == null || node.isNull()) {
				throw new ResourceException("参数错误");
			}
			if (node.isArray() || node.isObject()) {
				throw new ResourceException("参数错误");
			}
			if (node.isBoolean()) {
				throw new ResourceException("参数错误");
			}
			if (node.isNumber()) {
				if (node.isFloatingPointNumber()) {
					throw new ResourceException("参数错误");
				}
				if (!node.isIntegralNumber()) {
					throw new ResourceException("参数错误");
				}
				long lv = node.longValue();
				if (lv == 0L) {
					throw new ResourceException("参数错误");
				}
				orderedKeys.add(String.valueOf(lv));
				continue;
			}
			if (node.isTextual()) {
				String t = node.asText();
				if ("0".equals(t)) {
					throw new ResourceException("参数错误");
				}
				orderedKeys.add(t);
				continue;
			}
			throw new ResourceException("参数错误");
		}
		if (orderedKeys.isEmpty()) {
			throw new ResourceException("参数错误");
		}

		Map<String, Object> data =
				wxappTradeRatePraiseStatusService.ratePraiseStatus(companyId, userId, orderedKeys);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@GetMapping(
			value = "/rate/praise/num/{rate_id}",
			name = "点赞数量",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Integer>>> ratePraiseNum(
			HttpServletRequest request, @PathVariable("rate_id") String rateIdRaw) {
		Map<String, Object> auth = resolveFrontNoAuthClaims(request);
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			companyId = longVal(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		validatePositiveRateIdFromPath(rateIdRaw);
		long rateId = Long.parseLong(rateIdRaw.trim());

		int count = wxappTradeRatePraiseNumService.ratePraiseNum(String.valueOf(rateId));
		return ResponseEntity.ok(ApiResult.ok(Map.of("count", count)));
	}

	@FrontNoAuth
	@GetMapping(value = "/rate/list", name = "评价列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getRateList(
			HttpServletRequest request,
			@RequestParam(value = "item_id", required = false) String itemIdRaw,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSizeRaw,
			@RequestParam(value = "order_type", required = false) String orderType) {
		if (itemIdRaw == null || !StringUtils.hasText(itemIdRaw.trim())) {
			throw new BadRequestException("商品ID异常");
		}
		String trimmedItem = itemIdRaw.trim();
		long itemId;
		try {
			itemId = Long.parseLong(trimmedItem);
		} catch (NumberFormatException e) {
			throw new BadRequestException("商品ID异常");
		}
		if (itemId < 1L) {
			throw new BadRequestException("商品ID异常");
		}

		int page = parsePositiveIntWithDefault(pageRaw, 1);
		int pageSize = parsePositiveIntWithDefault(pageSizeRaw, 20);
		page = Math.max(1, page);
		pageSize = Math.max(1, pageSize);

		Map<String, Object> auth = resolveFrontNoAuthClaims(request);
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			companyId = longVal(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		Object data = wxappTradeRateListService.getRateList(companyId, itemId, page, pageSize, orderType);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@GetMapping(
			value = "/rate/detail/{rate_id}",
			name = "评价详情",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getRateDetail(
			HttpServletRequest request, @PathVariable("rate_id") String rateId) {
		if (rateId == null || rateId.isBlank()) {
			throw new BadRequestException("评价id参数异常");
		}
		String trimmed = rateId.trim();
		long rateIdLong;
		try {
			rateIdLong = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new BadRequestException("评价id参数异常");
		}

		Map<String, Object> auth = resolveFrontNoAuthClaims(request);
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			companyId = longVal(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		Map<String, Object> data = wxappTradeRateDetailService.getRateDetail(companyId, rateIdLong);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private Map<String, Object> resolveFrontNoAuthClaims(HttpServletRequest request) {
		Object rawAuth = request.getAttribute(WxappMemberAuthAttributes.REQUEST_ATTR);
		if (rawAuth instanceof Map<?, ?> authRaw) {
			return toStringKeyMap(authRaw);
		}
		rawAuth = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (rawAuth instanceof Map<?, ?> authRaw2) {
			return toStringKeyMap(authRaw2);
		}
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		auth.put("user_id", 0L);
		return auth;
	}

	private static void validatePositiveRateIdFromPath(String rateIdRaw) {
		if (rateIdRaw == null || !StringUtils.hasText(rateIdRaw.trim())) {
			throw new ResourceException("参数错误");
		}
		String trimmed = rateIdRaw.trim();
		if ("0".equals(trimmed)) {
			throw new ResourceException("参数错误");
		}
		long rateId;
		try {
			rateId = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("评价不存在");
		}
		if (rateId < 0L) {
			throw new ResourceException("评价不存在");
		}
		if (rateId == 0L) {
			throw new ResourceException("参数错误");
		}
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> raw) {
		Map<String, Object> auth = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : raw.entrySet()) {
			auth.put(String.valueOf(e.getKey()), e.getValue());
		}
		return auth;
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parsePositiveIntWithDefault(String raw, int defaultValue) {
		if (raw == null) {
			return defaultValue;
		}
		String trimmed = raw.trim();
		try {
			int v = Integer.parseInt(trimmed);
			return v <= 0 ? defaultValue : v;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@FrontNoAuth
	@GetMapping(
			value = "/replyRate/list",
			name = "回复列表",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getReplyRateList(
			HttpServletRequest request,
			@RequestParam(value = "rate_id", required = false) String rateIdRaw,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSizeRaw) {
		if (rateIdRaw == null) {
			throw new BadRequestException("参数异常");
		}
		String t = rateIdRaw.trim();
		if (t.isEmpty() || "0".equals(t)) {
			throw new BadRequestException("参数异常");
		}

		int page = parsePositiveIntWithDefault(pageRaw, 1);
		page = Math.max(1, page);
		int pageSize = parseReplyListPageSize(pageSizeRaw);

		Map<String, Object> auth = resolveFrontNoAuthClaims(request);
		Map<String, Object> query = FlexibleHttpServletParameterMap.toObjectMap(request);
		long companyId = longVal(auth.get("company_id"));
		if (companyId <= 0L) {
			companyId = longVal(query.get("company_id"));
		}
		if (companyId <= 0L) {
			companyId = longVal(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		Map<String, Object> data = wxappTradeRateReplyListService.getReplyRateList(companyId, t, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parseReplyListPageSize(String raw) {
		if (raw == null || raw.isBlank()) {
			return 20;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			if (v < 0) {
				return 20;
			}
			return v;
		} catch (NumberFormatException e) {
			return 20;
		}
	}
}
