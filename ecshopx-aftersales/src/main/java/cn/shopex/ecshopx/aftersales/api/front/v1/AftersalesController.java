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

package cn.shopex.ecshopx.aftersales.api.front.v1;

import cn.shopex.ecshopx.aftersales.service.AftersalesAdminDetailService;
import cn.shopex.ecshopx.aftersales.service.AftersalesFrontRefundAmountService;
import cn.shopex.ecshopx.aftersales.service.AftersalesFrontH5ListService;
import cn.shopex.ecshopx.aftersales.service.AftersalesFrontWxappApplyService;
import cn.shopex.ecshopx.aftersales.service.AftersalesFrontWxappCloseService;
import cn.shopex.ecshopx.aftersales.service.AftersalesFrontWxappSendbackService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRemindService;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.WxappMemberAuthAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * H5 / wxapp aftersales HTTP surface. Apply flows delegate to domain services; asynchronous refund
 * work after a successful submit is published through the unified trade-refund dispatch fan-out
 * (same bus path as other refund producers), not inline in this controller.
 */
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("aftersalesFrontV1")
@RequestMapping("/api/v1/h5app")
public class AftersalesController {

	private static final List<String> WXAPP_APPLY_WHITELIST =
			List.of(
					"order_id",
					"self_delivery_operator_id",
					"user_id",
					"detail",
					"aftersales_type",
					"reason",
					"evidence_pic",
					"description",
					"return_type",
					"aftersales_address_id",
					"contact",
					"mobile",
					"refund_fee",
					"refund_point",
					"freight");

	private static final char FW_COMMA = '\uFF0C';

	private final AftersalesFrontWxappApplyService aftersalesFrontWxappApplyService;
	private final AftersalesFrontWxappCloseService aftersalesFrontWxappCloseService;
	private final AftersalesFrontWxappSendbackService aftersalesFrontWxappSendbackService;
	private final AftersalesFrontH5ListService aftersalesFrontH5ListService;
	private final AftersalesAdminDetailService aftersalesAdminDetailService;
	private final AftersalesFrontRefundAmountService aftersalesFrontRefundAmountService;
	private final AftersalesRemindService aftersalesRemindService;

	public AftersalesController(
			AftersalesFrontWxappApplyService aftersalesFrontWxappApplyService,
			AftersalesFrontWxappCloseService aftersalesFrontWxappCloseService,
			AftersalesFrontWxappSendbackService aftersalesFrontWxappSendbackService,
			AftersalesFrontH5ListService aftersalesFrontH5ListService,
			AftersalesAdminDetailService aftersalesAdminDetailService,
			AftersalesFrontRefundAmountService aftersalesFrontRefundAmountService,
			AftersalesRemindService aftersalesRemindService) {
		this.aftersalesFrontWxappApplyService = aftersalesFrontWxappApplyService;
		this.aftersalesFrontWxappCloseService = aftersalesFrontWxappCloseService;
		this.aftersalesFrontWxappSendbackService = aftersalesFrontWxappSendbackService;
		this.aftersalesFrontH5ListService = aftersalesFrontH5ListService;
		this.aftersalesAdminDetailService = aftersalesAdminDetailService;
		this.aftersalesFrontRefundAmountService = aftersalesFrontRefundAmountService;
		this.aftersalesRemindService = aftersalesRemindService;
	}

	/**
	 * Wxapp aftersales apply. Delegates to the shop quantity-split apply stack
	 * ({@link AftersalesFrontWxappApplyService} →
	 * {@link cn.shopex.ecshopx.aftersales.service.AftersalesApplyShopApplyByNumService}); each split
	 * iteration runs transactional persistence then ordered post-commit publishers from
	 * {@link cn.shopex.ecshopx.aftersales.service.AftersalesApplyShopApplyByNumHandleService}.
	 *
	 * <p>For {@code REFUND_GOODS} and {@code EXCHANGING_GOODS}, after a successful transaction the handle
	 * publishes {@code EVENT_TRADE_AFTERSALES} (system link fan-out) and the third-party trade-aftersales
	 * Saas ERP event on the dispatch bus, then wait-deal notice and other integration publishers—never
	 * from this controller directly. Other aftersales types use the trade-refund fan-out instead of that
	 * pair.
	 */
	@PostMapping(
			value = "/wxapp/aftersales",
			name = "订单售后申请",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> apply(
			@FlexibleBody(required = false) Map<String, Object> body) {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			throw new ResourceException("请求上下文不可用");
		}
		HttpServletRequest request = attrs.getRequest();
		Map<String, Object> raw = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			raw.putAll(body);
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		for (String k : WXAPP_APPLY_WHITELIST) {
			if (raw.containsKey(k)) {
				merged.put(k, raw.get(k));
			}
		}
		return ResponseEntity.ok(ApiResult.ok(aftersalesFrontWxappApplyService.apply(request, merged)));
	}

	@PostMapping(value = "/wxapp/aftersales/modify", name = "编辑售后单")
	public ResponseEntity<Void> modify() {
		return ResponseEntity.ok().build();
	}

	@GetMapping(value = "/wxapp/aftersales", name = "获取售后列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getAftersalesList(HttpServletRequest request) {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			throw new ResourceException("请求上下文不可用");
		}
		return ResponseEntity.ok(ApiResult.ok(aftersalesFrontH5ListService.getAftersalesList(request)));
	}

	@GetMapping(value = "/wxapp/aftersales/info", name = "获取售后单详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getAftersalesDetail(HttpServletRequest request) {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			throw new ResourceException("请求上下文不可用");
		}
		Map<String, Object> raw = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		Map<String, Object> auth = mergeAuth(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("aftersales_bn", raw.get("aftersales_bn"));
		merged.put("company_id", auth.get("company_id"));
		Object u = raw.get("user_id");
		merged.put("user_id", u != null ? u : auth.get("user_id"));
		validateAftersalesDetailMatrix(merged);
		long companyId = parseLongStrictResource(merged.get("company_id"), "企业id必填");
		String aftersalesBnString = String.valueOf(merged.get("aftersales_bn")).trim();
		long userIdForScope = parseLongStrictResource(merged.get("user_id"), "会员id必填");
		Map<String, Object> data =
				aftersalesAdminDetailService.loadFullAftersalesForMember(
						companyId, aftersalesBnString, userIdForScope);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(
			value = "/wxapp/aftersales/sendback",
			name = "售后消费者回寄",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> sendback(
			@FlexibleBody(required = false) Map<String, Object> body) {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			throw new ResourceException("请求上下文不可用");
		}
		HttpServletRequest request = attrs.getRequest();
		LinkedHashMap<String, Object> raw =
				new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			raw.putAll(body);
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(raw);
		Map<String, Object> auth = mergeAuth(request);
		merged.put("company_id", parseLongStrict(auth.get("company_id"), "企业ID必填"));
		return ResponseEntity.ok(ApiResult.ok(aftersalesFrontWxappSendbackService.sendback(request, merged)));
	}

	@PostMapping(value = "/wxapp/aftersales/close", name = "售后关闭")
	public ResponseEntity<ApiResult<Map<String, Object>>> closeConfirm(
			@FlexibleBody(required = false) Map<String, Object> body) {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			throw new ResourceException("请求上下文不可用");
		}
		HttpServletRequest request = attrs.getRequest();
		Map<String, Object> raw = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			raw.putAll(body);
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		if (raw.containsKey("aftersales_bn")) {
			merged.put("aftersales_bn", raw.get("aftersales_bn"));
		}
		if (raw.containsKey("user_id")) {
			merged.put("user_id", raw.get("user_id"));
		}
		Map<String, Object> auth = mergeAuth(request);
		merged.put("company_id", parseLongStrict(auth.get("company_id"), "企业id必填"));
		merged.put("user_id", merged.containsKey("user_id") ? merged.get("user_id") : auth.get("user_id"));
		return ResponseEntity.ok(ApiResult.ok(aftersalesFrontWxappCloseService.closeConfirm(request, merged)));
	}

	private Map<String, Object> mergeAuth(HttpServletRequest request) {
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
		return auth;
	}

	private static long parseLongStrict(Object o, String absentMessage) {
		if (o == null) {
			throw new BadRequestException(absentMessage);
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(absentMessage);
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException(absentMessage);
		}
	}

	private static void validateRefundAmountMatrix(LinkedHashMap<String, Object> merged) {
		List<String> segments = new ArrayList<>();
		if (isBlank(merged.get("order_id"))) {
			segments.add("订单号必传");
		}
		if (isBlank(merged.get("item_id"))) {
			segments.add("商品ID必传");
		}
		if (isBlank(merged.get("number"))) {
			segments.add("商品数量必传");
		}
		if (isBlank(merged.get("company_id"))) {
			segments.add("企业ID必填");
		}
		if (isBlank(merged.get("user_id"))) {
			segments.add("用户ID必填");
		}
		if (!segments.isEmpty()) {
			throw new ResourceException(trimEdgeFullWidthComma(String.join(String.valueOf(FW_COMMA), segments)));
		}
	}

	private static void validateAftersalesDetailMatrix(LinkedHashMap<String, Object> merged) {
		List<String> segments = new ArrayList<>();
		if (isBlank(merged.get("aftersales_bn"))) {
			segments.add("售后单号必填");
		}
		if (isBlank(merged.get("company_id"))) {
			segments.add("企业id必填");
		}
		if (isBlank(merged.get("user_id"))) {
			segments.add("会员id必填");
		}
		if (!segments.isEmpty()) {
			throw new ResourceException(trimEdgeFullWidthComma(String.join(String.valueOf(FW_COMMA), segments)));
		}
	}

	private static boolean isBlank(Object o) {
		return o == null || !StringUtils.hasText(String.valueOf(o).trim());
	}

	private static String trimEdgeFullWidthComma(String s) {
		if (s == null) {
			return "";
		}
		String r = s;
		while (r.startsWith(String.valueOf(FW_COMMA))) {
			r = r.substring(1);
		}
		while (r.endsWith(String.valueOf(FW_COMMA))) {
			r = r.substring(0, r.length() - 1);
		}
		return r;
	}

	private static long parseLongStrictResource(Object o, String absentMessage) {
		if (o == null) {
			throw new ResourceException(absentMessage);
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException(absentMessage);
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException(absentMessage);
		}
	}

	@GetMapping(
			value = "/wxapp/aftersales/item/price",
			name = "获取售后商品价格",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getRefundAmount(HttpServletRequest request) {
		ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			throw new ResourceException("请求上下文不可用");
		}
		Map<String, Object> raw = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		Map<String, Object> auth = mergeAuth(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", raw.get("order_id"));
		merged.put("item_id", raw.get("item_id"));
		merged.put("number", raw.get("number"));
		merged.put("company_id", auth.get("company_id"));
		merged.put("user_id", auth.get("user_id"));
		validateRefundAmountMatrix(merged);
		long companyId = parseLongStrictResource(merged.get("company_id"), "企业ID必填");
		long userId = parseLongStrictResource(merged.get("user_id"), "用户ID必填");
		String orderId = String.valueOf(merged.get("order_id")).trim();
		String itemId = String.valueOf(merged.get("item_id")).trim();
		int aftersalesItemNum;
		try {
			aftersalesItemNum = Integer.parseInt(String.valueOf(merged.get("number")).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("商品数量必传");
		}
		Object bnRaw = raw.get("aftersales_bn");
		int up =
				(bnRaw != null && StringUtils.hasText(String.valueOf(bnRaw).trim())) ? 1 : 0;
		String abnOpt = up == 1 ? String.valueOf(bnRaw).trim() : null;
		long price =
				aftersalesFrontRefundAmountService.getRefundAmount(
						companyId, userId, orderId, itemId, aftersalesItemNum, up, abnOpt);
		return ResponseEntity.ok(ApiResult.ok(Map.of("price", price)));
	}

	@GetMapping(value = "/wxapp/aftersales/remind/detail", name = "售后提醒内容获取")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRemind(HttpServletRequest request) {
		return ResponseEntity.ok(ApiResult.ok(aftersalesRemindService.getRemind(request, false)));
	}
}
