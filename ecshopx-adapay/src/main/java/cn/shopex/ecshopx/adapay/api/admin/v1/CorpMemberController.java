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

import cn.shopex.ecshopx.adapay.service.AdapayCorpMemberCreateService;
import cn.shopex.ecshopx.adapay.service.AdapayCorpMemberModifyService;
import cn.shopex.ecshopx.adapay.service.AdapayCorpMemberReadService;
import cn.shopex.ecshopx.adapay.service.AdapayCorpMemberUpdateService;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("corpMemberAdminV1")
@RequestMapping("/api/v1/adapay")
public class CorpMemberController {

	private final AdapayCorpMemberCreateService adapayCorpMemberCreateService;
	private final AdapayCorpMemberModifyService adapayCorpMemberModifyService;
	private final AdapayCorpMemberUpdateService adapayCorpMemberUpdateService;
	private final AdapayCorpMemberReadService adapayCorpMemberReadService;

	public CorpMemberController(
			AdapayCorpMemberCreateService adapayCorpMemberCreateService,
			AdapayCorpMemberModifyService adapayCorpMemberModifyService,
			AdapayCorpMemberUpdateService adapayCorpMemberUpdateService,
			AdapayCorpMemberReadService adapayCorpMemberReadService) {
		this.adapayCorpMemberCreateService = adapayCorpMemberCreateService;
		this.adapayCorpMemberModifyService = adapayCorpMemberModifyService;
		this.adapayCorpMemberUpdateService = adapayCorpMemberUpdateService;
		this.adapayCorpMemberReadService = adapayCorpMemberReadService;
	}

	@Activated(routeAlias = "adapay.corp_member.info")
	@GetMapping(value = "/corp_member/get", name = "获取企业用户对象")
	public ResponseEntity<ApiResult<Map<String, Object>>> get(HttpServletRequest request) {
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
		Object operatorIdRaw = jwtMap.get("operator_id");
		if (!(operatorIdRaw instanceof Number)) {
			throw new UnauthorizedException("未登录");
		}
		int operatorId = ((Number) operatorIdRaw).intValue();

		Map<String, Object> data = adapayCorpMemberReadService.get(companyId, operatorId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "adapay.corp_member.create")
	@PostMapping(value = "/corp_member/create", name = "创建企业用户对象")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
			HttpServletRequest request,
			@FlexibleBody Map<String, Object> body,
			@RequestPart(value = "attach_file", required = false) MultipartFile attachFile,
			@RequestPart(value = "confirm_letter_file", required = false) MultipartFile confirmLetterFile) {
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

		Map<String, Object> effectiveBody = body == null ? Map.of() : body;
		Map<String, String> formFields = new LinkedHashMap<>();
		formFields.put("name", trimToEmpty(stringVal(effectiveBody.get("name"))));
		formFields.put("area", trimToEmpty(stringVal(effectiveBody.get("area"))));
		formFields.put("email", trimToEmpty(stringVal(effectiveBody.get("email"))));
		formFields.put("bank_acct_type", trimToEmpty(stringVal(effectiveBody.get("bank_acct_type"))));
		formFields.put("social_credit_code", trimToEmpty(stringVal(effectiveBody.get("social_credit_code"))));
		formFields.put(
				"social_credit_code_expires",
				trimToEmpty(stringVal(effectiveBody.get("social_credit_code_expires"))));
		formFields.put("business_scope", trimToEmpty(stringVal(effectiveBody.get("business_scope"))));
		formFields.put("legal_person", trimToEmpty(stringVal(effectiveBody.get("legal_person"))));
		formFields.put("legal_cert_id", trimToEmpty(stringVal(effectiveBody.get("legal_cert_id"))));
		formFields.put(
				"legal_cert_id_expires",
				trimToEmpty(stringVal(effectiveBody.get("legal_cert_id_expires"))));
		formFields.put("legal_mp", trimToEmpty(stringVal(effectiveBody.get("legal_mp"))));
		formFields.put("address", trimToEmpty(stringVal(effectiveBody.get("address"))));
		formFields.put("zip_code", trimToEmpty(stringVal(effectiveBody.get("zip_code"))));
		formFields.put("telphone", trimToEmpty(stringVal(effectiveBody.get("telphone"))));
		formFields.put("bank_code", trimToEmpty(stringVal(effectiveBody.get("bank_code"))));
		formFields.put("card_no", trimToEmpty(stringVal(effectiveBody.get("card_no"))));
		formFields.put("card_name", trimToEmpty(stringVal(effectiveBody.get("card_name"))));
		formFields.put("submit_review", trimToEmpty(stringVal(effectiveBody.get("submit_review"))));

		if (!adapayCorpMemberCreateService.createFromMultipart(companyId, jwtMap, formFields, attachFile, confirmLetterFile)) {
			throw new ResourceException("用户创建失败");
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "adapay.corp_member.modify")
	@PostMapping(value = "/corp_member/modify", name = "修改企业用户对象(未开户)")
	public ResponseEntity<ApiResult<Map<String, Object>>> modify(
			HttpServletRequest request,
			@FlexibleBody Map<String, Object> body,
			@RequestPart(value = "attach_file", required = false) MultipartFile attachFile,
			@RequestPart(value = "confirm_letter_file", required = false) MultipartFile confirmLetterFile) {
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

		Map<String, Object> effectiveBody = body == null ? Map.of() : body;
		Map<String, String> formFields = new LinkedHashMap<>();
		formFields.put("member_id", trimToEmpty(stringVal(effectiveBody.get("member_id"))));
		formFields.put("name", trimToEmpty(stringVal(effectiveBody.get("name"))));
		formFields.put("area", trimToEmpty(stringVal(effectiveBody.get("area"))));
		formFields.put("bank_acct_type", trimToEmpty(stringVal(effectiveBody.get("bank_acct_type"))));
		formFields.put("social_credit_code", trimToEmpty(stringVal(effectiveBody.get("social_credit_code"))));
		formFields.put(
				"social_credit_code_expires",
				trimToEmpty(stringVal(effectiveBody.get("social_credit_code_expires"))));
		formFields.put("business_scope", trimToEmpty(stringVal(effectiveBody.get("business_scope"))));
		formFields.put("legal_person", trimToEmpty(stringVal(effectiveBody.get("legal_person"))));
		formFields.put("legal_cert_id", trimToEmpty(stringVal(effectiveBody.get("legal_cert_id"))));
		formFields.put(
				"legal_cert_id_expires",
				trimToEmpty(stringVal(effectiveBody.get("legal_cert_id_expires"))));
		formFields.put("legal_mp", trimToEmpty(stringVal(effectiveBody.get("legal_mp"))));
		formFields.put("address", trimToEmpty(stringVal(effectiveBody.get("address"))));
		formFields.put("zip_code", trimToEmpty(stringVal(effectiveBody.get("zip_code"))));
		formFields.put("telphone", trimToEmpty(stringVal(effectiveBody.get("telphone"))));
		formFields.put("email", trimToEmpty(stringVal(effectiveBody.get("email"))));
		formFields.put("bank_code", trimToEmpty(stringVal(effectiveBody.get("bank_code"))));
		formFields.put("card_no", trimToEmpty(stringVal(effectiveBody.get("card_no"))));
		formFields.put("card_name", trimToEmpty(stringVal(effectiveBody.get("card_name"))));

		if (!adapayCorpMemberModifyService.modifyFromMultipart(
				companyId, jwtMap, formFields, attachFile, confirmLetterFile)) {
			throw new ResourceException("用户更新失败");
		}
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "adapay.corp_member.update")
	@PostMapping(value = "/corp_member/update", name = "更新企业用户对象")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			HttpServletRequest request,
			@FlexibleBody Map<String, Object> body,
			@RequestPart(value = "attach_file", required = false) MultipartFile attachFile) {
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

		Map<String, Object> effectiveBody = body == null ? Map.of() : body;
		Map<String, String> formFields = new LinkedHashMap<>();
		formFields.put("member_id", trimToEmpty(stringVal(effectiveBody.get("member_id"))));
		formFields.put("name", trimToEmpty(stringVal(effectiveBody.get("name"))));
		formFields.put("area", normalizeArea(effectiveBody.get("area")));
		formFields.put("bank_acct_type", trimToEmpty(stringVal(effectiveBody.get("bank_acct_type"))));
		formFields.put("social_credit_code", trimToEmpty(stringVal(effectiveBody.get("social_credit_code"))));
		formFields.put(
				"social_credit_code_expires",
				trimToEmpty(stringVal(effectiveBody.get("social_credit_code_expires"))));
		formFields.put("business_scope", trimToEmpty(stringVal(effectiveBody.get("business_scope"))));
		formFields.put("legal_person", trimToEmpty(stringVal(effectiveBody.get("legal_person"))));
		formFields.put("legal_cert_id", trimToEmpty(stringVal(effectiveBody.get("legal_cert_id"))));
		formFields.put(
				"legal_cert_id_expires",
				trimToEmpty(stringVal(effectiveBody.get("legal_cert_id_expires"))));
		formFields.put("legal_mp", trimToEmpty(stringVal(effectiveBody.get("legal_mp"))));
		formFields.put("address", trimToEmpty(stringVal(effectiveBody.get("address"))));
		formFields.put("zip_code", trimToEmpty(stringVal(effectiveBody.get("zip_code"))));
		formFields.put("telphone", trimToEmpty(stringVal(effectiveBody.get("telphone"))));
		formFields.put("email", trimToEmpty(stringVal(effectiveBody.get("email"))));
		formFields.put("bank_code", trimToEmpty(stringVal(effectiveBody.get("bank_code"))));
		formFields.put("card_no", trimToEmpty(stringVal(effectiveBody.get("card_no"))));
		formFields.put("card_name", trimToEmpty(stringVal(effectiveBody.get("card_name"))));

		adapayCorpMemberUpdateService.update(companyId, jwtMap, formFields, attachFile);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static String stringVal(Object o) {
		return o == null ? "" : o.toString();
	}

	private static String trimToEmpty(String s) {
		return s == null ? "" : s.trim();
	}

	private static String normalizeArea(Object areaRaw) {
		if (areaRaw instanceof Collection<?> col) {
			StringJoiner joiner = new StringJoiner(",");
			for (Object el : col) {
				joiner.add(String.valueOf(el));
			}
			return joiner.toString().trim();
		}
		if (areaRaw instanceof String s) {
			return s.trim();
		}
		return trimToEmpty(stringVal(areaRaw));
	}
}
