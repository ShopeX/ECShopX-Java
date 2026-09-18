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

package cn.shopex.ecshopx.hfpay.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.hfpay.HfpayEnterapplyOpenSplitPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayEnterapplyKaiHuService;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayEnterapplyReadService;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayEnterapplySaveService;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayHfFileFacadeService;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayHfFileRequestMergeService;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
@RestController("hfpayEnterapplyAdminV1")
@RequestMapping("/api/v1")
public class HfpayEnterapplyController {

	private final HfpayHfFileRequestMergeService hfpayHfFileRequestMergeService;
	private final HfpayHfFileFacadeService hfpayHfFileFacadeService;
	private final HfpayEnterapplyKaiHuService hfpayEnterapplyKaiHuService;
	private final HfpayEnterapplySaveService hfpayEnterapplySaveService;
	private final HfpayEnterapplyReadService hfpayEnterapplyReadService;
	private final HfpayEnterapplyOpenSplitPort hfpayEnterapplyOpenSplitPort;
	private final HfPayPaymentSettingService hfPayPaymentSettingService;
	private final LangueProperties langueProperties;

	public HfpayEnterapplyController(
			HfpayHfFileRequestMergeService hfpayHfFileRequestMergeService,
			HfpayHfFileFacadeService hfpayHfFileFacadeService,
			HfpayEnterapplyKaiHuService hfpayEnterapplyKaiHuService,
			HfpayEnterapplySaveService hfpayEnterapplySaveService,
			HfpayEnterapplyReadService hfpayEnterapplyReadService,
			HfpayEnterapplyOpenSplitPort hfpayEnterapplyOpenSplitPort,
			HfPayPaymentSettingService hfPayPaymentSettingService,
			LangueProperties langueProperties) {
		this.hfpayHfFileRequestMergeService = hfpayHfFileRequestMergeService;
		this.hfpayHfFileFacadeService = hfpayHfFileFacadeService;
		this.hfpayEnterapplyKaiHuService = hfpayEnterapplyKaiHuService;
		this.hfpayEnterapplySaveService = hfpayEnterapplySaveService;
		this.hfpayEnterapplyReadService = hfpayEnterapplyReadService;
		this.hfpayEnterapplyOpenSplitPort = hfpayEnterapplyOpenSplitPort;
		this.hfPayPaymentSettingService = hfPayPaymentSettingService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "hfpay.enterapply.apply")
	@GetMapping(value = "/hfpay/enterapply/apply", name = "获取入驻信息")
	public ResponseEntity<ApiResult<Object>> apply(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) Long distributorId,
			@RequestParam(value = "apply_type", defaultValue = "1") String applyType) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		String at = applyType == null ? "1" : applyType.trim();
		if (at.isEmpty()) {
			at = "1";
		}
		Map<String, Object> data = hfpayEnterapplyReadService.getEnterapplyForApply(companyId, distributorId, at);
		if (data == null) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "hfpay.enterapply.save")
	@PostMapping(value = "/hfpay/enterapply/save", name = "保存入驻信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveEnterapply(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, Object> merged = hfpayHfFileRequestMergeService.merge(request, body);
		merged.put("company_id", companyId);
		Map<String, Object> data = hfpayEnterapplySaveService.saveEnterapply(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "hfpay.enterapply.getlist")
	@GetMapping(value = "/hfpay/enterapply/getList", name = "获取店铺入驻列表信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getApplyList(
			HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "province", required = false) String province,
			@RequestParam(value = "city", required = false) String city,
			@RequestParam(value = "area", required = false) String area,
			@RequestParam(value = "mobile", required = false) String mobileIgnored,
			@RequestParam(value = "distributor_id", required = false) Long distributorId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, Object> result = hfpayEnterapplyReadService.getApplyList(
				companyId, page, pageSize, name, province, city, area, distributorId);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "hfpay.enterapply.hfkaihu")
	@PostMapping(value = "/hfpay/enterapply/hfkaihu", name = "企业个体户开户")
	public ResponseEntity<ApiResult<Map<String, Object>>> hfKaiHu(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, Object> merged = hfpayHfFileRequestMergeService.merge(request, body);
		Map<String, Object> result = hfpayEnterapplyKaiHuService.kaiHu(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@Activated(routeAlias = "hfpay.enterapply.hffile")
	@PostMapping(value = "/hfpay/enterapply/hffile", name = "汇付文件上传")
	public ResponseEntity<ApiResult<Map<String, Object>>> hfFile(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body,
			@RequestPart(value = "file", required = false) MultipartFile file) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		// Loads payment settings before binary file validation (ordering matches legacy wizard).
		hfPayPaymentSettingService.loadForCompany(companyId);

		if (file == null || file.isEmpty()) {
			throw new BadRequestException("文件不能为空");
		}

		Map<String, Object> merged = hfpayHfFileRequestMergeService.merge(request, body);
		String transType = toTrimmedString(merged.get("trans_type"));
		String attachType = toTrimmedString(merged.get("attach_type"));
		if (!StringUtils.hasText(transType)) {
			throw new BadRequestException("缺少必填参数: trans_type");
		}
		if (!StringUtils.hasText(attachType)) {
			throw new BadRequestException("缺少必填参数: attach_type");
		}

		Map<String, Object> result = hfpayHfFileFacadeService.upload(companyId, merged, file);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static String toTrimmedString(Object v) {
		if (v == null) {
			return null;
		}
		String s = String.valueOf(v).trim();
		return s.isEmpty() ? null : s;
	}

	@Activated(routeAlias = "hfpay.enterapply.opensplit")
	@PostMapping(value = "/hfpay/enterapply/opensplit", name = "店铺分账开关")
	public ResponseEntity<ApiResult<Map<String, Object>>> openSplit(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> jwtMap) || jwtMap.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Object companyIdRaw = operatorJwt.get("company_id");
		if (!(companyIdRaw instanceof Number) || ((Number) companyIdRaw).longValue() <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		long companyId = ((Number) companyIdRaw).longValue();

		Map<String, Object> merged = hfpayHfFileRequestMergeService.merge(request, body);
		String requestLangTag = RequestLangTag.current(langueProperties);
		Map<String, Object> operatorUser = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : jwtMap.entrySet()) {
			operatorUser.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> data = hfpayEnterapplyOpenSplitPort.openSplit(companyId, merged, operatorUser, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

}
