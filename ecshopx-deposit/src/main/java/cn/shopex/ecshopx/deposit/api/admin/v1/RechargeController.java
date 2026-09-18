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

package cn.shopex.ecshopx.deposit.api.admin.v1;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.DateExpressionParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import cn.shopex.ecshopx.deposit.service.DepositAdminRechargeService;
import cn.shopex.ecshopx.deposit.service.DepositCountIndexReadService;
import cn.shopex.ecshopx.deposit.service.DepositShopRoutePermissionService;
import cn.shopex.ecshopx.deposit.service.DepositTradeListDatapassService;
import cn.shopex.ecshopx.deposit.service.DepositTradeListQuery;
import cn.shopex.ecshopx.deposit.service.DepositTradeListService;
import cn.shopex.ecshopx.deposit.service.RechargeAgreementReadService;
import cn.shopex.ecshopx.deposit.service.RechargeAgreementWriteService;
import cn.shopex.ecshopx.deposit.service.RechargeMultipleWriteService;
import cn.shopex.ecshopx.deposit.service.RechargeRuleCreateService;
import cn.shopex.ecshopx.deposit.service.RechargeRuleDeleteService;
import cn.shopex.ecshopx.deposit.service.RechargeRuleListService;
import cn.shopex.ecshopx.deposit.service.RechargeRuleUpdateService;
import cn.shopex.ecshopx.deposit.web.DepositAdminFlexibleInputMerge;
import cn.shopex.ecshopx.deposit.web.DepositDatapassBlockResolver;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.ShopLog;

@AdminAuth
@ShopLog
@DingoResponse(
    resource = DingoResponse.ResourceStyle.DINGO,
    badRequest = DingoResponse.BadRequestStyle.DINGO_422
)
@RestController("depositRechargeAdminV1")
@RequestMapping("/api/v1")
public class RechargeController {


	private final CompanysActivationService companysActivationService;
	private final DepositShopRoutePermissionService depositShopRoutePermissionService;
	private final RechargeRuleCreateService rechargeRuleCreateService;
	private final RechargeRuleUpdateService rechargeRuleUpdateService;
	private final RechargeRuleDeleteService rechargeRuleDeleteService;
	private final RechargeAgreementReadService rechargeAgreementReadService;
	private final RechargeAgreementWriteService rechargeAgreementWriteService;
	private final DepositAdminRechargeService depositAdminRechargeService;
	private final RechargeMultipleWriteService rechargeMultipleWriteService;
	private final DepositCountIndexReadService depositCountIndexReadService;
	private final RechargeRuleListService rechargeRuleListService;
	private final DepositTradeListService depositTradeListService;
	private final DepositTradeListDatapassService depositTradeListDatapassService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public RechargeController(
			CompanysActivationService companysActivationService,
			DepositShopRoutePermissionService depositShopRoutePermissionService,
			RechargeRuleCreateService rechargeRuleCreateService,
			RechargeRuleUpdateService rechargeRuleUpdateService,
			RechargeRuleDeleteService rechargeRuleDeleteService,
			RechargeAgreementReadService rechargeAgreementReadService,
			RechargeAgreementWriteService rechargeAgreementWriteService,
			DepositAdminRechargeService depositAdminRechargeService,
			RechargeMultipleWriteService rechargeMultipleWriteService,
			DepositCountIndexReadService depositCountIndexReadService,
			RechargeRuleListService rechargeRuleListService,
			DepositTradeListService depositTradeListService,
			DepositTradeListDatapassService depositTradeListDatapassService,
				OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.companysActivationService = companysActivationService;
		this.depositShopRoutePermissionService = depositShopRoutePermissionService;
		this.rechargeRuleCreateService = rechargeRuleCreateService;
		this.rechargeRuleUpdateService = rechargeRuleUpdateService;
		this.rechargeRuleDeleteService = rechargeRuleDeleteService;
		this.rechargeAgreementReadService = rechargeAgreementReadService;
		this.rechargeAgreementWriteService = rechargeAgreementWriteService;
		this.depositAdminRechargeService = depositAdminRechargeService;
		this.rechargeMultipleWriteService = rechargeMultipleWriteService;
		this.depositCountIndexReadService = depositCountIndexReadService;
		this.rechargeRuleListService = rechargeRuleListService;
		this.depositTradeListService = depositTradeListService;
		this.depositTradeListDatapassService = depositTradeListDatapassService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "deposit.rule.create")
	@PostMapping(value = "/deposit/rechargerule", name = "deposit.rule.create")
	public ResponseEntity<ApiResult<Map<String, Object>>> createRechargeRule(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		depositShopRoutePermissionService.assertRechargeRuleCreate(user);

		Map<String, Object> merged = DepositAdminFlexibleInputMerge.merge(request, body);
		rechargeRuleCreateService.create(companyId, merged);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/deposit/rechargerule");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "创建充值面额规则");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "deposit.rule.list")
	@GetMapping(value = "/deposit/rechargerules", name = "deposit.rule.list")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRechargeRuleList(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		depositShopRoutePermissionService.assertRechargeRuleList(user);

		int pageSize = parsePositiveIntQuery(request, "pageSize", 20);
		int page = parsePositiveIntQuery(request, "page", 1);
		String companyIdStr = String.valueOf(companyId);
		Map<String, Object> payload =
				rechargeRuleListService.getRechargeRuleListPage(companyIdStr, pageSize, page);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "deposit.rule.delete")
	@DeleteMapping(value = "/deposit/rechargerule/{id}", name = "deposit.rule.delete")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteRechargeRuleById(
			HttpServletRequest request, @PathVariable("id") String id) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		depositShopRoutePermissionService.assertRechargeRuleDelete(user);

		rechargeRuleDeleteService.deleteById(companyId, id);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/deposit/rechargerule/" + id);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(Map.of("id", id)));
		} catch (Exception e) {
			logCtx.put("params", String.valueOf(id));
		}
		logCtx.put("operator_name", "删除充值面额规则");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "deposit.rule.edit")
	@PutMapping(value = "/deposit/rechargerule", name = "deposit.rule.edit")
	public ResponseEntity<ApiResult<Map<String, Object>>> editRechargeRuleById(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		depositShopRoutePermissionService.assertRechargeRuleEdit(user);

		Map<String, Object> merged = DepositAdminFlexibleInputMerge.merge(request, body);
		rechargeRuleUpdateService.update(companyId, merged);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/deposit/rechargerule");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "编辑充值面额规则");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "deposit.recharge.agreement.set")
	@PostMapping(value = "/deposit/recharge/agreement", name = "deposit.recharge.agreement.set")
	public ResponseEntity<ApiResult<Map<String, Object>>> setRechargeAgreement(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		depositShopRoutePermissionService.assertRechargeAgreementSet(user);

		Map<String, Object> merged = DepositAdminFlexibleInputMerge.merge(request, body);
		Object rawContent = merged.get("content");
		String content;
		if (rawContent == null) {
			content = null;
		} else if (rawContent instanceof String s) {
			content = s;
		} else {
			content = String.valueOf(rawContent);
		}
		rechargeAgreementWriteService.setAgreement(companyId, content);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/deposit/recharge/agreement");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "设置储值协议");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "deposit.recharge.agreement.get")
	@GetMapping(value = "/deposit/recharge/agreement", name = "deposit.recharge.agreement.get")
	public ResponseEntity<ApiResult<Object>> getRechargeAgreementByCompanyId(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		depositShopRoutePermissionService.assertRechargeAgreementGet(user);

		Object payload = rechargeAgreementReadService.getAgreementPayload(companyId);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "deposit.recharge.multiple.set")
	@PostMapping(value = "/deposit/recharge/multiple", name = "deposit.recharge.multiple.set")
	public ResponseEntity<ApiResult<Map<String, Object>>> setRechargeMultiple(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		depositShopRoutePermissionService.assertRechargeMultipleSet(user);

		Map<String, Object> merged = DepositAdminFlexibleInputMerge.merge(request, body);

		ZoneId zone = ZoneId.systemDefault();
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("start_time", startOrEndTimeFromMerged(merged, "start_time", zone));
		data.put("end_time", startOrEndTimeFromMerged(merged, "end_time", zone));
		data.put("is_open", isOpenFromMerged(merged));
		data.put("multiple", parseIntFromInput(merged, "multiple", 1));

		try {
			rechargeMultipleWriteService.setRechargeMultiple(companyId, data);
		} catch (JsonProcessingException e) {
			throw new ResourceException("数据序列化失败");
		}

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/deposit/recharge/multiple");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "设置充值送积分");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "deposit.recharge.multiple.get")
	@GetMapping(value = "/deposit/recharge/multiple", name = "deposit.recharge.multiple.get")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRechargeMultipleByCompanyId(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		depositShopRoutePermissionService.assertRechargeMultipleGet(user);

		Map<String, Object> payload = rechargeMultipleWriteService.getRechargeMultipleByCompanyId(companyId);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@DataPass
	@Activated(routeAlias = "deposit.trades")
	@GetMapping(value = "/deposit/trades", name = "deposit.trades")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDepositTradeList(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		depositShopRoutePermissionService.assertDepositTrades(user);
		depositTradeListDatapassService.apply(request, user);

		int pageSize = parsePositiveIntQuery(request, "pageSize", 20);
		int page = parsePositiveIntQuery(request, "page", 1);

		DepositTradeListQuery query = new DepositTradeListQuery();
		query.setCompanyId(String.valueOf(companyId));

		String mobileParam = request.getParameter("mobile");
		if (mobileParam != null && !mobileParam.trim().isEmpty()) {
			String m = mobileParam.trim();
			if (m.length() == 11) {
				query.setMobileIs11DigitPhone(true);
				query.setMobileOrTradeIdRaw(m);
			} else {
				query.setMobileIs11DigitPhone(false);
				query.setMobileOrTradeIdRaw(m);
			}
		}

		String userIdParam = request.getParameter("user_id");
		if (userIdParam != null && !userIdParam.trim().isEmpty()) {
			query.setUserId(userIdParam.trim());
		}

		String shopNameParam = request.getParameter("shop_name");
		if (shopNameParam != null && !shopNameParam.trim().isEmpty()) {
			query.setShopName(shopNameParam.trim());
		}

		String dateBeginParam = request.getParameter("date_begin");
		if (dateBeginParam != null && !dateBeginParam.trim().isEmpty()) {
			query.setDateBegin(dateBeginParam.trim());
			query.setDateEnd(request.getParameter("date_end"));
		}

		String tradeTypeParam = request.getParameter("trade_type");
		if (tradeTypeParam != null && !tradeTypeParam.trim().isEmpty()) {
			String[] parts = tradeTypeParam.split(",");
			List<String> types = new ArrayList<>();
			for (String part : parts) {
				String t = part.trim();
				if (!t.isEmpty()) {
					types.add(t);
				}
			}
			if (!types.isEmpty()) {
				query.setTradeTypes(types);
			}
		}

		Map<String, Object> payload = depositTradeListService.getDepositTradeListPage(query, pageSize, page);

		if (DepositDatapassBlockResolver.isBlocked(request)) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = (List<Map<String, Object>>) payload.get("list");
			if (list != null && !list.isEmpty()) {
				List<Map<String, Object>> masked = new ArrayList<>();
				for (Map<String, Object> src : list) {
					LinkedHashMap<String, Object> copy = new LinkedHashMap<>(src);
					Object mob = copy.get("mobile");
					if (mob != null) {
						copy.put("mobile", DataMasking.maskMobile(mob.toString()));
					}
					masked.add(copy);
				}
				payload.put("list", masked);
			}
		}

		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "deposit.count.index")
	@GetMapping(value = "/deposit/count/index", name = "deposit.count.index")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDepositCountIndex(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		depositShopRoutePermissionService.assertDepositCountIndex(user);

		Map<String, Object> payload = depositCountIndexReadService.getDepositCountIndex(companyId);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "deposit.recharge")
	@PostMapping(value = "/deposit/recharge", name = "deposit.recharge")
	public ResponseEntity<ApiResult<Map<String, Object>>> recharge(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> user = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			user.put(String.valueOf(e.getKey()), e.getValue());
		}

		Object companyObj = user.get("company_id");
		if (companyObj == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(companyObj);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}

		companysActivationService.assertShopOperatorCompanyActive(companyId);
		depositShopRoutePermissionService.assertDepositRecharge(user);

		Map<String, Object> merged = DepositAdminFlexibleInputMerge.merge(request, body);
		depositAdminRechargeService.recharge(companyId, merged);

		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/deposit/recharge");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "后台充值");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static int parsePositiveIntQuery(HttpServletRequest request, String name, int defaultValue) {
		String v = request.getParameter(name);
		if (v == null || v.isEmpty()) {
			return defaultValue;
		}
		try {
			int n = Integer.parseInt(String.valueOf(v).trim());
			return n > 0 ? n : defaultValue;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static Long startOrEndTimeFromMerged(Map<String, Object> merged, String key, ZoneId zone) {
		if (!merged.containsKey(key)) {
			return null;
		}
		Object v = merged.get(key);
		if (v == null) {
			return null;
		}
		return DateExpressionParser.parseToEpochSecond(v, zone);
	}

	private static boolean isOpenFromMerged(Map<String, Object> merged) {
		if (!merged.containsKey("is_open")) {
			return false;
		}
		Object v = merged.get("is_open");
		if (v == null) {
			return false;
		}
		return !"false".equals(String.valueOf(v));
	}

	private static int parseIntFromInput(Map<String, Object> merged, String key, int defaultVal) {
		if (!merged.containsKey(key)) {
			return defaultVal;
		}
		Object v = merged.get(key);
		if (v == null) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
	}
}
