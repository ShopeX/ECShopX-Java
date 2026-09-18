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

package cn.shopex.ecshopx.aftersales.api.admin.v1;

import cn.shopex.ecshopx.aftersales.dto.AftersalesAdminListQuery;
import cn.shopex.ecshopx.aftersales.dto.AftersalesLogExportQuery;
import cn.shopex.ecshopx.aftersales.dto.AftersalesApplyParams;
import cn.shopex.ecshopx.aftersales.service.AftersalesAdminDetailService;
import cn.shopex.ecshopx.aftersales.service.AftersalesAdminListService;
import cn.shopex.ecshopx.aftersales.service.AftersalesAdminLogExportService;
import cn.shopex.ecshopx.aftersales.service.AftersalesApplyService;
import cn.shopex.ecshopx.aftersales.service.AftersalesApplyShopApplyByNumService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundConfirmService;
import cn.shopex.ecshopx.aftersales.service.AftersalesRemindService;
import cn.shopex.ecshopx.aftersales.service.AftersalesReviewService;
import cn.shopex.ecshopx.aftersales.service.AftersalesSendbackService;
import cn.shopex.ecshopx.aftersales.service.AftersalesService;
import cn.shopex.ecshopx.aftersales.service.export.AftersalesFinancialExportService;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("aftersalesAdminV1")
@RequestMapping("/api/v1")
public class AftersalesController {

	private final AftersalesApplyService aftersalesApplyService;
	private final AftersalesRefundConfirmService aftersalesRefundConfirmService;
	private final AftersalesReviewService aftersalesReviewService;
	private final AftersalesRemindService aftersalesRemindService;
	private final AftersalesSendbackService aftersalesSendbackService;
	private final AftersalesService aftersalesService;
	private final AftersalesAdminListService aftersalesAdminListService;
	private final AftersalesAdminDetailService aftersalesAdminDetailService;
	private final AftersalesFinancialExportService aftersalesFinancialExportService;
	private final AftersalesAdminLogExportService aftersalesAdminLogExportService;

	public AftersalesController(
			AftersalesApplyService aftersalesApplyService,
			AftersalesRefundConfirmService aftersalesRefundConfirmService,
			AftersalesReviewService aftersalesReviewService,
			AftersalesRemindService aftersalesRemindService,
			AftersalesSendbackService aftersalesSendbackService,
			AftersalesService aftersalesService,
			AftersalesAdminListService aftersalesAdminListService,
			AftersalesAdminDetailService aftersalesAdminDetailService,
			AftersalesFinancialExportService aftersalesFinancialExportService,
			AftersalesAdminLogExportService aftersalesAdminLogExportService) {
		this.aftersalesApplyService = aftersalesApplyService;
		this.aftersalesRefundConfirmService = aftersalesRefundConfirmService;
		this.aftersalesReviewService = aftersalesReviewService;
		this.aftersalesRemindService = aftersalesRemindService;
		this.aftersalesSendbackService = aftersalesSendbackService;
		this.aftersalesService = aftersalesService;
		this.aftersalesAdminListService = aftersalesAdminListService;
		this.aftersalesAdminDetailService = aftersalesAdminDetailService;
		this.aftersalesFinancialExportService = aftersalesFinancialExportService;
		this.aftersalesAdminLogExportService = aftersalesAdminLogExportService;
	}

	@DataPass
	@Activated(routeAlias = "aftersales.list")
	@GetMapping(value = "/aftersales", name = "获取售后列表")
	public ApiResult<Map<String, Object>> getAftersalesList(
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "time_start_begin", required = false) String timeStartBegin,
			@RequestParam(value = "time_start_end", required = false) String timeStartEnd,
			@RequestParam(value = "aftersales_status", required = false) String aftersalesStatus,
			@RequestParam(value = "aftersales_type", required = false) String aftersalesType,
			@RequestParam(value = "aftersales_bn", required = false) String aftersalesBn,
			@RequestParam(value = "item_id", required = false) String itemId,
			@RequestParam(value = "item_bn", required = false) String itemBn,
			@RequestParam(value = "order_id", required = false) String orderId,
			@RequestParam(value = "receiver_mobile", required = false) String receiverMobile,
			@RequestParam(value = "shop_id", required = false) String shopId,
			@RequestParam(value = "user_id", required = false) String userId,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "distributor_id", required = false) String distributorId,
			@RequestParam(value = "distributorIds", required = false) List<String> distributorIds,
			@RequestParam(value = "is_prescription_order", required = false) String isPrescriptionOrder,
			@RequestParam(value = "order_class", required = false) String orderClass,
			@RequestParam(value = "order_by", required = false) String orderBy,
			@RequestParam(value = "item_name", required = false) String itemName) {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		String validatorMsg = validateAdminListPagination(pageRaw, pageSizeRaw);
		if (validatorMsg != null) {
			throw new BadRequestException(validatorMsg);
		}
		int page = Integer.parseInt(pageRaw.trim());
		int pageSize = Integer.parseInt(pageSizeRaw.trim());
		AftersalesAdminListQuery q = new AftersalesAdminListQuery();
		q.setPage(page);
		q.setPageSize(pageSize);
		q.setOrderByCreateTimeAsc("asc".equalsIgnoreCase(orderBy == null ? "" : orderBy.trim()));
		q.setPrescriptionOrderFilterActive(request.getParameterMap().containsKey("is_prescription_order"));
		q.setIsPrescriptionOrderValue(isPrescriptionOrder);
		q.setTimeStartBegin(timeStartBegin);
		q.setTimeStartEnd(timeStartEnd);
		q.setAftersalesStatus(aftersalesStatus);
		q.setAftersalesType(aftersalesType);
		q.setAftersalesBn(aftersalesBn);
		q.setItemId(itemId);
		q.setItemBn(itemBn);
		q.setOrderId(orderId);
		q.setReceiverMobile(receiverMobile);
		q.setShopId(shopId);
		q.setUserId(userId);
		q.setMobile(mobile);
		q.setDistributorId(distributorId);
		q.setDistributorIds(distributorIds);
		q.setOrderClass(orderClass);
		q.setItemName(itemName);
		return ApiResult.ok(aftersalesAdminListService.getAftersalesList(q, request));
	}

	/** @return 错误文案；null 表示通过 */
	private static String validateAdminListPagination(String pageRaw, String pageSizeRaw) {
		List<String> errs = new ArrayList<>();
		if (pageRaw == null || pageRaw.isBlank()) {
			errs.add("page 字段必填");
		} else {
			try {
				int p = Integer.parseInt(pageRaw.trim());
				if (p < 1) {
					errs.add("page 必须至少为 1");
				}
			} catch (NumberFormatException e) {
				errs.add("page 必须是整数");
			}
		}
		if (pageSizeRaw == null || pageSizeRaw.isBlank()) {
			errs.add("pageSize 字段必填");
		} else {
			try {
				int ps = Integer.parseInt(pageSizeRaw.trim());
				if (ps < 1) {
					errs.add("pageSize 必须至少为 1");
				} else if (ps > 50) {
					errs.add("pageSize 不能超过 50");
				}
			} catch (NumberFormatException e) {
				errs.add("pageSize 必须是整数");
			}
		}
		if (errs.isEmpty()) {
			return null;
		}
		return String.join("，", errs).replaceAll("，$", "");
	}

	@Activated(routeAlias = "aftersales.logexport")
	@GetMapping(value = "/aftersales/logExport", name = "导出售后列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> logExport(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorId,
			@RequestParam(value = "time_start_begin", required = false) String timeStartBegin,
			@RequestParam(value = "time_start_end", required = false) String timeStartEnd,
			@RequestParam(value = "order_id", required = false) String orderId,
			@RequestParam(value = "receiver_mobile", required = false) String receiverMobile,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "aftersales_bn", required = false) String aftersalesBn,
			@RequestParam(value = "aftersales_status", required = false) String aftersalesStatus,
			@RequestParam(value = "aftersales_type", required = false) String aftersalesType,
			@RequestParam(value = "order_class", required = false) String orderClass) {
		AftersalesLogExportQuery q = new AftersalesLogExportQuery();
		q.setDistributorId(distributorId);
		q.setTimeStartBegin(timeStartBegin);
		q.setTimeStartEnd(timeStartEnd);
		q.setOrderId(orderId);
		q.setReceiverMobile(receiverMobile);
		q.setMobile(mobile);
		q.setAftersalesBn(aftersalesBn);
		q.setAftersalesStatus(aftersalesStatus);
		q.setAftersalesType(aftersalesType);
		q.setOrderClass(orderClass);
		aftersalesAdminLogExportService.logExport(request, q);
		return ApiResult.ok(Map.of("status", true));
	}

	@Activated(routeAlias = "aftersales.info")
	@GetMapping(value = "/aftersales/{aftersales_bn}", name = "获取售后详情")
	public ApiResult<Map<String, Object>> getAftersalesDetail(
			@PathVariable("aftersales_bn") String aftersalesBn, HttpServletRequest request) {
		if (aftersalesBn == null || aftersalesBn.isBlank() || "0".equals(aftersalesBn.trim())) {
			throw new BadRequestException("没有填写售后单号");
		}
		String bn = aftersalesBn.trim();
		long companyId = readCompanyIdFromJwt(request);
		return ApiResult.ok(aftersalesAdminDetailService.getAftersalesDetail(companyId, bn));
	}

	/**
	 * Aftersales approval for operator consoles: admin-console and seller-console API route groups both resolve to this
	 * {@code POST /api/v1/aftersales/review} handler. The type is declared under the {@code admin.v1} package following
	 * module layout; execution delegates to {@link AftersalesReviewService#aftersalesReview(java.util.LinkedHashMap,
	 * jakarta.servlet.http.HttpServletRequest)}.
	 *
	 * <p>Admin-console traffic carries operator identity via {@link OperatorJwtRequestAttributes#OPERATOR_JWT_USER_DATA}
	 * ({@code operator_type} is typically {@code "admin"}); that value is merged into review parameters and thus into
	 * {@code OrderProcessLog} payloads produced inside the service layer.
	 *
	 * @apiNote Async dispatch / queue publishing for this flow stays inside {@link AftersalesReviewService}; this REST
	 *     endpoint does not call the unified dispatch bus, {@code DispatchFacade}, {@code JushuitanTradeAftersalesDispatchPublisher},
	 *     {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher}, or
	 *     {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher} directly.
	 *     <p>On approve ({@code is_approved} true), SaaS ERP trade-aftersales update messages are published inside
	 *     {@link AftersalesReviewService} via {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher}
	 *     as {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyDispatchEventNames#EVENT_TRADE_AFTERSALES_SAAS_ERP}, consumed
	 *     asynchronously by {@link cn.shopex.ecshopx.thirdparty.dispatch.TradeAftersalesSendSaasErpDispatchListener}.
	 *     <p>On reject ({@code is_approved} false), SaaS ERP aftersales cancel is published inside {@link AftersalesReviewService}
	 *     before the surrounding transaction commits, via {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher}
	 *     onto the shared dispatch bus as {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyDispatchEventNames#EVENT_TRADE_AFTERSALES_CANCEL_SAAS_ERP},
	 *     consumed asynchronously by {@link cn.shopex.ecshopx.thirdparty.dispatch.TradeAftersaleCancelSendSaasErpDispatchListener}.
	 */
	@Activated(routeAlias = "aftersales.review")
	@PostMapping(value = "/aftersales/review", name = "售后审核", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> aftersalesReview(@FlexibleBody(required = false) Map<String, Object> body) {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		return ApiResult.ok(aftersalesReviewService.aftersalesReview(merged, request));
	}

	/**
	 * Refund audit for seller-facing wxapp admin flows historically exposed {@code POST .../aftersales/refundCheck} under a
	 * wxapp-specific URL prefix and authentication provider. Operator consoles use the same refund-confirm pipeline; this
	 * endpoint consolidates that behavior at {@code POST /api/v1/aftersales/refundCheck}.
	 * Admin-console refund audit shares this endpoint with merchant-facing paths; InvoiceRed post-commit scheduling uses the same delegation as those routes.
	 * Execution delegates to
	 * {@link AftersalesRefundConfirmService#refundCheck(java.util.LinkedHashMap, jakarta.servlet.http.HttpServletRequest)}.
	 *
	 * @apiNote Async dispatch / queue publishing for this flow stays inside {@link AftersalesRefundConfirmService}; this REST
	 *     endpoint does not call the unified dispatch bus, {@code DispatchFacade},
	 *     {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher}, or
	 *     {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher} directly.
	 *     <p>On approve ({@code check_refund} truthy), SaaS ERP trade-aftersales update messages are published inside
	 *     {@link AftersalesRefundConfirmService} (within its refund transaction) via
	 *     {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher} as
	 *     {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyDispatchEventNames#EVENT_TRADE_AFTERSALES_SAAS_ERP}, consumed
	 *     asynchronously by {@link cn.shopex.ecshopx.thirdparty.dispatch.TradeAftersalesSendSaasErpDispatchListener}.
	 *     <p>On reject ({@code check_refund} falsey), SaaS ERP aftersales cancel is published inside
	 *     {@link AftersalesRefundConfirmService} before the surrounding transaction commits, via
	 *     {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher} onto the shared
	 *     dispatch bus as {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyDispatchEventNames#EVENT_TRADE_AFTERSALES_CANCEL_SAAS_ERP},
	 *     consumed asynchronously by {@link cn.shopex.ecshopx.thirdparty.dispatch.TradeAftersaleCancelSendSaasErpDispatchListener}.
	 */
	@Activated(routeAlias = "aftersales.review")
	@PostMapping(value = "/aftersales/refundCheck", name = "售后退款审核", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> refundCheck(@FlexibleBody(required = false) Map<String, Object> body) {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		return ApiResult.ok(aftersalesRefundConfirmService.refundCheck(merged, request));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "aftersales.financial.export")
	@GetMapping(value = "/aftersales/financial/export", name = "导出售后报表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> financialExport(
			HttpServletRequest request,
			@RequestParam(value = "time_start_begin", required = false) String timeStartBegin,
			@RequestParam(value = "time_start_end", required = false) String timeStartEnd,
			@RequestParam(value = "order_id", required = false) String orderId) {
		long companyId = readCompanyIdFromJwt(request);
		long operatorId = readOperatorIdFromJwt(request);
		aftersalesFinancialExportService.financialExport(companyId, operatorId, timeStartBegin, timeStartEnd, orderId);
		return ApiResult.ok(Map.of("status", true));
	}

	private static Map<?, ?> readJwtMap(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (attr instanceof Map<?, ?> jwt) {
			return jwt;
		}
		throw new UnauthorizedException("未登录");
	}

	private static long readCompanyIdFromJwt(HttpServletRequest request) {
		Map<?, ?> jwt = readJwtMap(request);
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new ResourceException("登录验证错误");
		}
		try {
			return Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("登录验证错误");
		}
	}

	private static long readOperatorIdFromJwt(HttpServletRequest request) {
		Map<?, ?> jwt = readJwtMap(request);
		Object operatorIdObj = jwt.get("operator_id");
		if (operatorIdObj == null) {
			throw new ResourceException("登录验证错误");
		}
		try {
			return Long.parseLong(String.valueOf(operatorIdObj).trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("登录验证错误");
		}
	}

	@Activated(routeAlias = "aftersales.remind.get")
	@GetMapping(value = "/aftersales/remind/detail", name = "售后提醒内容获取")
	public ApiResult<Map<String, Object>> getRemind() {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		return ApiResult.ok(aftersalesRemindService.getRemind(request, true));
	}

	@Activated(routeAlias = "aftersales.remind.set")
	@PostMapping(value = "/aftersales/remind", name = "售后提醒内容设置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> setRemind(@FlexibleBody(required = false) Map<String, Object> body) {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		return ApiResult.ok(aftersalesRemindService.setRemind(merged, request));
	}

	@Activated(routeAlias = "aftersales.remark.update")
	@PutMapping(value = "/aftersales/remark", name = "更新售后备注", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<List<Map<String, Object>>> updateRemark(@FlexibleBody(required = false) Map<String, Object> body) {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		return ApiResult.ok(aftersalesService.updateRemark(merged, request));
	}

	/**
	 * Admin {@code POST /api/v1/aftersales/apply}: merges request and body into {@link AftersalesApplyParams}, then
	 * delegates to {@link AftersalesApplyService#apply} which routes to {@link AftersalesApplyShopApplyByNumService#shopApplyByNum}.
	 *
	 * <p>For most aftersales types, after the surrounding transaction commits, asynchronous trade-refund work is published
	 * via {@link cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher} (async listener fan-out on the dispatch bus).
	 *
	 * <p>When {@code aftersales_type} is {@code REFUND_GOODS} or {@code EXCHANGING_GOODS}, after commit the apply-by-number
	 * handle flow ({@link cn.shopex.ecshopx.aftersales.service.AftersalesApplyShopApplyByNumHandleService}, ordered
	 * post-commit step {@code performPostCommitOrdered}) publishes in sequence through {@link cn.shopex.ecshopx.common.dispatch.TradeAftersalesDispatchPublisher}
	 * (system-link trade-aftersales event) and {@link cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher}
	 * (third-party trade-aftersales SaaS ERP event), each routed via {@code DispatchFacade.publishEvent} on the shared dispatch bus.
	 */
	@Activated(routeAlias = "aftersales.apply")
	@PostMapping(value = "/aftersales/apply", name = "创建售后申请", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> apply(@FlexibleBody(required = false) Map<String, Object> body) {
		// Avoid HttpServletRequest as a handler parameter together with @FlexibleBody (Undertow may commit an empty 200).
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		AftersalesApplyParams params = buildApplyParams(merged, request);
		return ApiResult.ok(aftersalesApplyService.apply(request, params));
	}

	/**
	 * Admin Open API entry for recording return logistics. Delegates exclusively to
	 * {@link cn.shopex.ecshopx.aftersales.service.AftersalesSendbackService#sendback}; the controller does not publish
	 * dispatch-bus messages itself.
	 */
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = false)
	@Activated(routeAlias = "aftersales.sendback")
	@PostMapping(value = "/aftersales/sendback", name = "售后管理员填写寄回信息", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> sendback(@FlexibleBody(required = false) Map<String, Object> body) {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		LinkedHashMap<String, Object> whitelist = new LinkedHashMap<>();
		whitelist.put("aftersales_bn", merged.get("aftersales_bn"));
		whitelist.put("corp_code", merged.get("corp_code"));
		whitelist.put("logi_no", merged.get("logi_no"));
		return ApiResult.ok(aftersalesSendbackService.sendback(whitelist, request));
	}

	private static AftersalesApplyParams buildApplyParams(LinkedHashMap<String, Object> merged, HttpServletRequest request) {
		AftersalesApplyParams p = new AftersalesApplyParams();
		p.setDetail(merged.get("detail"));
		p.setOrderId(longVal(merged.get("order_id")));
		p.setAftersalesType(str(merged.get("aftersales_type")));
		p.setGoodsReturnedRaw(merged.get("goods_returned"));
		p.setReason(str(merged.get("reason")));
		p.setDescription(str(merged.get("description")));
		p.setEvidencePic(merged.get("evidence_pic"));
		Object rf = merged.get("refund_fee");
		p.setRefundFeeRaw(rf == null ? "" : String.valueOf(rf).trim());
		Object rp = merged.get("refund_point");
		p.setRefundPointRaw(rp == null ? "" : String.valueOf(rp).trim());
		Object fr = merged.get("freight");
		if (fr instanceof Number n) {
			p.setFreight(n.intValue());
		} else if (fr != null && !String.valueOf(fr).isBlank()) {
			try {
				p.setFreight(Integer.parseInt(String.valueOf(fr).trim()));
			} catch (NumberFormatException e) {
				p.setFreight(0);
			}
		}
		Object sm = merged.get("salesman_id");
		if (sm != null) {
			p.setSalesmanId(longVal(sm));
		}
		String dist = request.getParameter("distributor_id");
		if (dist != null && !dist.isBlank()) {
			p.setDistributorId(longVal(dist));
		}
		return p;
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
