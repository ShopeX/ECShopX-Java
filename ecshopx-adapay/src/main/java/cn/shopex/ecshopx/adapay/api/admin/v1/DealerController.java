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

package cn.shopex.ecshopx.adapay.api.admin.v1;

import cn.shopex.ecshopx.common.util.DataMasking;
import cn.shopex.ecshopx.adapay.service.DealerDelSubService;
import cn.shopex.ecshopx.adapay.service.DealerDistributorListService;
import cn.shopex.ecshopx.adapay.service.DealerInfoService;
import cn.shopex.ecshopx.adapay.service.DealerListService;
import cn.shopex.ecshopx.adapay.service.DealerOpenOrDisableService;
import cn.shopex.ecshopx.adapay.service.DealerParentIdService;
import cn.shopex.ecshopx.adapay.service.DealerRelDistributorService;
import cn.shopex.ecshopx.adapay.service.DealerResetPasswordService;
import cn.shopex.ecshopx.adapay.service.DealerUpdateService;
import cn.shopex.ecshopx.goods.web.DatapassBlockResolver;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("dealerAdminV1")
@RequestMapping("/api/v1/adapay")
public class DealerController {

	private final DealerOpenOrDisableService dealerOpenOrDisableService;
	private final DealerRelDistributorService dealerRelDistributorService;
	private final DealerResetPasswordService dealerResetPasswordService;
	private final DealerUpdateService dealerUpdateService;
	private final DealerParentIdService dealerParentIdService;
	private final DealerDistributorListService dealerDistributorListService;
	private final DealerListService dealerListService;
	private final DealerInfoService dealerInfoService;
	private final DealerDelSubService dealerDelSubService;

	public DealerController(
			DealerOpenOrDisableService dealerOpenOrDisableService,
			DealerRelDistributorService dealerRelDistributorService,
			DealerResetPasswordService dealerResetPasswordService,
			DealerUpdateService dealerUpdateService,
			DealerParentIdService dealerParentIdService,
			DealerDistributorListService dealerDistributorListService,
			DealerListService dealerListService,
			DealerInfoService dealerInfoService,
			DealerDelSubService dealerDelSubService) {
		this.dealerOpenOrDisableService = dealerOpenOrDisableService;
		this.dealerRelDistributorService = dealerRelDistributorService;
		this.dealerResetPasswordService = dealerResetPasswordService;
		this.dealerUpdateService = dealerUpdateService;
		this.dealerParentIdService = dealerParentIdService;
		this.dealerDistributorListService = dealerDistributorListService;
		this.dealerListService = dealerListService;
		this.dealerInfoService = dealerInfoService;
		this.dealerDelSubService = dealerDelSubService;
	}

	@DataPass
	@Activated(routeAlias = "adapay.dealer.list")
	@GetMapping(value = "/dealer/list", name = "经销商列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> dealerList(
			@RequestParam(value = "username", required = false) String username,
			@RequestParam(value = "contact", required = false) String contact,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "time_start", required = false) String timeStart,
			@RequestParam(value = "time_end", required = false) String timeEnd,
			@RequestParam(value = "open_account_start", required = false) String openAccountStart,
			@RequestParam(value = "open_account_end", required = false) String openAccountEnd,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "page_size", required = false, defaultValue = "20") int pageSize,
			HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> data = dealerListService.dealerList(
				companyId,
				jwtMap,
				username,
				contact,
				mobile,
				timeStart,
				timeEnd,
				openAccountStart,
				openAccountEnd,
				page,
				pageSize);
		Object operatorTypeObj = jwtMap.get("operator_type");
		boolean notDealer = operatorTypeObj == null || !"dealer".equals(operatorTypeObj.toString().trim());
		if (notDealer && DatapassBlockResolver.isBlocked(request)) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
			if (list != null) {
				for (Map<String, Object> row : list) {
					row.put(
							"mobile",
							DataMasking.maskMobile(String.valueOf(row.getOrDefault("mobile", ""))));
					row.put(
							"contact",
							DataMasking.maskTruename(
									String.valueOf(row.getOrDefault("contact", ""))));
				}
			}
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass
	@Activated(routeAlias = "adapay.dealer.distributorList")
	@GetMapping(value = "/dealer/distributors", name = "经销商关联店铺列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> distributorList(
			@RequestParam(value = "dealer_id", required = false) String dealerId,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "contact", required = false) String contact,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "audit_state", required = false) String auditState,
			@RequestParam(value = "province", required = false) String province,
			@RequestParam(value = "city", required = false) String city,
			@RequestParam(value = "area", required = false) String area,
			@RequestParam(value = "adapay_fee_mode", required = false) String adapayFeeMode,
			@RequestParam(value = "time_start", required = false) String timeStart,
			@RequestParam(value = "time_end", required = false) String timeEnd,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "page_size", required = false, defaultValue = "0") int pageSize,
			HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		boolean dealerIdPresent = request.getParameterMap().containsKey("dealer_id");
		Map<String, Object> data = dealerDistributorListService.distributorList(
				companyId,
				jwtMap,
				dealerIdPresent,
				dealerId,
				name,
				contact,
				mobile,
				auditState,
				province,
				city,
				area,
				adapayFeeMode,
				timeStart,
				timeEnd,
				page,
				pageSize);
		if (DatapassBlockResolver.isBlocked(request)) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
			if (list != null) {
				for (Map<String, Object> row : list) {
					row.put(
							"mobile",
							DataMasking.maskMobile(String.valueOf(row.getOrDefault("mobile", ""))));
					row.put(
							"contact",
							DataMasking.maskTruename(
									String.valueOf(row.getOrDefault("contact", ""))));
				}
			}
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DataPass
	@Activated(routeAlias = "adapay.dealer.info")
	@GetMapping(value = "/dealer/{id}", name = "经销商详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> dealerInfo(
			@PathVariable("id") String id, HttpServletRequest request) {
		long targetOperatorId;
		try {
			targetOperatorId = Long.parseLong(id.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("id 格式不正确");
		}
		if (targetOperatorId <= 0L) {
			throw new BadRequestException("id 格式不正确");
		}
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> data = dealerInfoService.dealerInfo(companyId, jwtMap, targetOperatorId);
		if (DatapassBlockResolver.isBlocked(request)) {
			Object biRaw = data.get("basicInfo");
			if (biRaw instanceof Map<?, ?>) {
				@SuppressWarnings("unchecked")
				Map<String, Object> bi = (Map<String, Object>) biRaw;
				bi.put(
						"contact",
						DataMasking.maskTruename(str(bi.get("contact"))));
				bi.put("tel_no", DataMasking.maskMobile(str(bi.get("tel_no"))));
			}
			if (data.containsKey("user_name") && StringUtils.hasText(str(data.get("user_name")))) {
				data.put("user_name", DataMasking.maskTruename(str(data.get("user_name"))));
			}
			if (data.containsKey("tel_no") && StringUtils.hasText(str(data.get("tel_no")))) {
				data.put("tel_no", DataMasking.maskMobile(str(data.get("tel_no"))));
			}
			if (data.containsKey("cert_id") && StringUtils.hasText(str(data.get("cert_id")))) {
				data.put("cert_id", DataMasking.maskIdcard(str(data.get("cert_id"))));
			}
			if (data.containsKey("bank_card_name") && StringUtils.hasText(str(data.get("bank_card_name")))) {
				data.put("bank_card_name", DataMasking.maskTruename(str(data.get("bank_card_name"))));
			}
			if (data.containsKey("bank_tel_no") && StringUtils.hasText(str(data.get("bank_tel_no")))) {
				data.put("bank_tel_no", DataMasking.maskMobile(str(data.get("bank_tel_no"))));
			}
			if (data.containsKey("bank_card_id") && StringUtils.hasText(str(data.get("bank_card_id")))) {
				data.put("bank_card_id", DataMasking.maskBankcard(str(data.get("bank_card_id"))));
			}
			if (data.containsKey("bank_cert_id") && StringUtils.hasText(str(data.get("bank_cert_id")))) {
				data.put("bank_cert_id", DataMasking.maskIdcard(str(data.get("bank_cert_id"))));
			}
			if ("corp".equals(String.valueOf(data.getOrDefault("member_type", "")).trim())) {
				if (data.containsKey("legal_person") && StringUtils.hasText(str(data.get("legal_person")))) {
					data.put("legal_person", DataMasking.maskTruename(str(data.get("legal_person"))));
				}
				if (data.containsKey("legal_cert_id") && StringUtils.hasText(str(data.get("legal_cert_id")))) {
					data.put("legal_cert_id", DataMasking.maskIdcard(str(data.get("legal_cert_id"))));
				}
				if (data.containsKey("card_no") && StringUtils.hasText(str(data.get("card_no")))) {
					data.put("card_no", DataMasking.maskBankcard(str(data.get("card_no"))));
				}
				if (data.containsKey("card_name") && StringUtils.hasText(str(data.get("card_name")))) {
					data.put("card_name", DataMasking.maskTruename(str(data.get("card_name"))));
				}
			}
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	@Activated(routeAlias = "adapay.dealer.disable")
	@PutMapping(value = "/dealer/disable", name = "经销商开启 禁用")
	public ResponseEntity<ApiResult<Map<String, Object>>> openOrDisable(
			@RequestParam(value = "operator_id", required = false) String operatorId,
			@RequestParam(value = "is_disable", required = false) String isDisable,
			HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		long jwtOperatorId = 0L;
		Object opRaw = jwtMap.get("operator_id");
		if (opRaw instanceof Number) {
			jwtOperatorId = ((Number) opRaw).longValue();
		}

		dealerOpenOrDisableService.openOrDisable(companyId, jwtOperatorId, operatorId, isDisable);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "adapay.dealer.rel")
	@PutMapping(value = "/dealer/rel", name = "经销商关联店铺")
	public ResponseEntity<ApiResult<Map<String, Object>>> dealerRelDistributor(
			@RequestParam(value = "operator_id", required = false) String operatorId,
			@RequestParam(value = "distributor_id", required = false) String distributorId,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "is_rel", required = false) String isRel,
			@RequestParam(value = "headquarters_proportion", required = false) String headquartersProportion,
			@RequestParam(value = "dealer_proportion", required = false) String dealerProportion,
			HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		long jwtOperatorId = 0L;
		Object opRaw = jwtMap.get("operator_id");
		if (opRaw instanceof Number) {
			jwtOperatorId = ((Number) opRaw).longValue();
		}

		dealerRelDistributorService.dealerRelDistributor(
				companyId,
				jwtOperatorId,
				operatorId,
				distributorId,
				name,
				isRel,
				headquartersProportion,
				dealerProportion);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "adapay.dealer.reset")
	@PutMapping(value = "/dealer/reset/{operatorId}", name = "经销商重置密码")
	public ResponseEntity<ApiResult<Map<String, Object>>> resetPassword(
			@PathVariable("operatorId") String operatorId, HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		long jwtOperatorId = 0L;
		Object opRaw = jwtMap.get("operator_id");
		if (opRaw instanceof Number) {
			jwtOperatorId = ((Number) opRaw).longValue();
		}

		dealerResetPasswordService.resetPassword(companyId, jwtOperatorId, operatorId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "adapay.dealer.update")
	@PutMapping(value = "/dealer/update/{operatorId}", name = "经销商端账号编辑")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			@PathVariable("operatorId") String operatorId,
			@RequestParam(value = "password", required = false) String passwordParam,
			@RequestBody(required = false) Map<String, Object> body,
			HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		long jwtOperatorId = 0L;
		Object opRaw = jwtMap.get("operator_id");
		if (opRaw instanceof Number) {
			jwtOperatorId = ((Number) opRaw).longValue();
		}

		String passwordPlain = null;
		if (body != null) {
			Object p = body.get("password");
			if (p != null) {
				String fromBody = String.valueOf(p).trim();
				if (!fromBody.isEmpty()) {
					passwordPlain = fromBody;
				}
			}
		}
		if (passwordPlain == null && passwordParam != null) {
			String fromQuery = passwordParam.trim();
			if (!fromQuery.isEmpty()) {
				passwordPlain = fromQuery;
			}
		}

		dealerUpdateService.update(companyId, operatorId, passwordPlain);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "adapay.dealer.del")
	@DeleteMapping(value = "/dealer/sub/del/{operatorId}", name = "删除经销商子账号")
	public ResponseEntity<ApiResult<Map<String, Object>>> delDealerSub(
			@PathVariable("operatorId") String operatorId) {
		dealerDelSubService.delDealerSub(operatorId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "adapay.dealer_parent.get")
	@GetMapping(value = "/dealer/dealer_parent/get", name = "获取经销商主账号id")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDealerParentId(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMapRaw) || jwtMapRaw.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> jwtMap = (Map<String, Object>) jwtMapRaw;
		Object companyIdRaw = jwtMap.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();
		Map<String, Object> data = dealerParentIdService.getDealerParentId(companyId, jwtMap);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
