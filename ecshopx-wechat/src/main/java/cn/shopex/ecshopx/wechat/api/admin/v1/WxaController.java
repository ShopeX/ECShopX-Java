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

package cn.shopex.ecshopx.wechat.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.service.WechatOpenUserPlatformService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaAuthorizerListService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaCartremindSettingService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaGetDomainListService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaGetWxaDetailService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaGetTemplateListService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaGetTestQrcodeService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaGetPrivacySettingService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaGetTemplateWeappDetailService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaGetTemplateWeappListService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaSaveDomainService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaPageParamsSettingService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaSetPrivacySettingService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaUploadPrivacyExtFileService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaSaveConfigService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaRevertCodeReleaseService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaTryReleaseService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaUndocodeauditService;
import cn.shopex.ecshopx.wechat.service.wxa.OffiaccountForeverQrcodeService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaUploadWxaCodeUnlimitService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaUploadWxaInputMergeService;
import cn.shopex.ecshopx.wechat.service.wxa.WxaUploadWxaService;
import cn.shopex.ecshopx.wechat.service.wxa.model.MergedUploadWxaInput;
import cn.shopex.ecshopx.wechat.service.wxa.model.SavePageAllParamsRow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@AdminAuth
@ShopLog
@RestController("wechatAdminV1Wxa")
@RequestMapping("/api/v1")
public class WxaController {

	private final WxaUploadWxaInputMergeService mergeService;
	private final WxaUploadWxaService wxaUploadWxaService;
	private final WechatAuthQueryService wechatAuthQueryService;
	private final WxaUploadPrivacyExtFileService wxaUploadPrivacyExtFileService;
	private final WxaCartremindSettingService wxaCartremindSettingService;
	private final WxaGetDomainListService wxaGetDomainListService;
	private final WxaSaveDomainService wxaSaveDomainService;
	private final WxaPageParamsSettingService wxaPageParamsSettingService;
	private final WxaSetPrivacySettingService wxaSetPrivacySettingService;
	private final WxaGetPrivacySettingService wxaGetPrivacySettingService;
	private final WxaTryReleaseService wxaTryReleaseService;
	private final WxaRevertCodeReleaseService wxaRevertCodeReleaseService;
	private final WxaSaveConfigService wxaSaveConfigService;
	private final OffiaccountForeverQrcodeService offiaccountForeverQrcodeService;
	private final WxaAuthorizerListService wxaAuthorizerListService;
	private final WxaGetWxaDetailService wxaGetWxaDetailService;
	private final ObjectMapper objectMapper;
	private final WxaUploadWxaCodeUnlimitService wxaUploadWxaCodeUnlimitService;
	private final WxaGetTemplateWeappDetailService wxaGetTemplateWeappDetailService;
	private final WxaGetTemplateWeappListService wxaGetTemplateWeappListService;
	private final WxaGetTemplateListService wxaGetTemplateListService;
	private final WxaGetTestQrcodeService wxaGetTestQrcodeService;
	private final WxaUndocodeauditService wxaUndocodeauditService;

	public WxaController(
			WxaUploadWxaInputMergeService mergeService,
			WxaUploadWxaService wxaUploadWxaService,
			WechatAuthQueryService wechatAuthQueryService,
			WxaUploadPrivacyExtFileService wxaUploadPrivacyExtFileService,
			WxaCartremindSettingService wxaCartremindSettingService,
			WxaGetDomainListService wxaGetDomainListService,
			WxaSaveDomainService wxaSaveDomainService,
			WxaPageParamsSettingService wxaPageParamsSettingService,
			WxaSetPrivacySettingService wxaSetPrivacySettingService,
			WxaGetPrivacySettingService wxaGetPrivacySettingService,
			WxaTryReleaseService wxaTryReleaseService,
			WxaRevertCodeReleaseService wxaRevertCodeReleaseService,
			WxaSaveConfigService wxaSaveConfigService,
			OffiaccountForeverQrcodeService offiaccountForeverQrcodeService,
			WxaAuthorizerListService wxaAuthorizerListService,
			WxaGetWxaDetailService wxaGetWxaDetailService,
			ObjectMapper objectMapper,
			WxaUploadWxaCodeUnlimitService wxaUploadWxaCodeUnlimitService,
			WxaGetTemplateWeappDetailService wxaGetTemplateWeappDetailService,
			WxaGetTemplateWeappListService wxaGetTemplateWeappListService,
			WxaGetTemplateListService wxaGetTemplateListService,
			WxaGetTestQrcodeService wxaGetTestQrcodeService,
			WxaUndocodeauditService wxaUndocodeauditService) {
		this.mergeService = mergeService;
		this.wxaUploadWxaService = wxaUploadWxaService;
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wxaUploadPrivacyExtFileService = wxaUploadPrivacyExtFileService;
		this.wxaCartremindSettingService = wxaCartremindSettingService;
		this.wxaGetDomainListService = wxaGetDomainListService;
		this.wxaSaveDomainService = wxaSaveDomainService;
		this.wxaPageParamsSettingService = wxaPageParamsSettingService;
		this.wxaSetPrivacySettingService = wxaSetPrivacySettingService;
		this.wxaGetPrivacySettingService = wxaGetPrivacySettingService;
		this.wxaTryReleaseService = wxaTryReleaseService;
		this.wxaRevertCodeReleaseService = wxaRevertCodeReleaseService;
		this.wxaSaveConfigService = wxaSaveConfigService;
		this.offiaccountForeverQrcodeService = offiaccountForeverQrcodeService;
		this.wxaAuthorizerListService = wxaAuthorizerListService;
		this.wxaGetWxaDetailService = wxaGetWxaDetailService;
		this.objectMapper = objectMapper;
		this.wxaUploadWxaCodeUnlimitService = wxaUploadWxaCodeUnlimitService;
		this.wxaGetTemplateWeappDetailService = wxaGetTemplateWeappDetailService;
		this.wxaGetTemplateWeappListService = wxaGetTemplateWeappListService;
		this.wxaGetTemplateListService = wxaGetTemplateListService;
		this.wxaGetTestQrcodeService = wxaGetTestQrcodeService;
		this.wxaUndocodeauditService = wxaUndocodeauditService;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.codeunlimit")
	@GetMapping(
			value = "/wechat/offiaccountcodeforever",
			name = "公众号永久二维码",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getOffiaccountCodeForever(
			@RequestParam(value = "authorizer_appid", required = false) String authorizerAppid,
			@RequestParam(value = "is_base64", required = false) String isBase64Raw) {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		String appid = authorizerAppid == null ? "" : authorizerAppid.trim();
		Map<String, Object> data = offiaccountForeverQrcodeService.getOffiaccountCodeForever(
				companyId, appid, isBase64QueryTruthyLikeWechat(isBase64Raw));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static boolean isBase64QueryTruthyLikeWechat(String raw) {
		if (raw == null) {
			return false;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return false;
		}
		if ("0".equals(s)) {
			return false;
		}
		return true;
	}

	// 本接口直接返回统一 ApiResult 封装，由 Spring MVC 消息转换器写入 JSON 响应体。
	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.authorizer")
	@GetMapping(
			value = "/wxa/authorizer",
			name = "获取授权小程序列表",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getWxaList() {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		List<Map<String, Object>> list = wxaAuthorizerListService.getWxaList(companyId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.template.list")
	@GetMapping(
			value = "/wxa/gettemplateweapplist",
			name = "获取小程序模版列表",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getTemplateWeappList() {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		List<Map<String, Object>> list = wxaGetTemplateWeappListService.getTemplateWeappList(companyId);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.template.detail")
	@GetMapping(
			value = "/wxa/gettemplateweappdetail",
			name = "获取小程序模版详情",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getTemplateWeappDetail(
			@RequestParam(value = "template_id", required = false) String templateIdRaw) {
		Integer templateIdNullable;
		if (templateIdRaw == null) {
			templateIdNullable = null;
		} else {
			String s = templateIdRaw.trim();
			templateIdNullable = (int) LeadingNumberParser.parseAsLong(s);
		}
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		LinkedHashMap<String, Object> data =
				wxaGetTemplateWeappDetailService.getTemplateWeappDetail(companyId, templateIdNullable);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.create")
	@PostMapping(value = "/wxa", name = "上架小程序审核", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> uploadWxa(
			HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		Object operatorIdObj = map.get("operator_id");
		if (operatorIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long operatorId;
		try {
			operatorId = Long.parseLong(String.valueOf(operatorIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		Object rawAuth = map.get("authorizer_appid");
		String jwtAuthorizer = rawAuth == null ? null : String.valueOf(rawAuth).trim();
		String authorizerAppid;
		if (!WechatOpenUserPlatformService.hasEffectiveAuthorizerAppid(jwtAuthorizer)) {
			authorizerAppid =
					Objects.requireNonNullElse(wechatAuthQueryService.findBoundMiniProgramAuthorizerAppid(companyId), "");
		} else {
			authorizerAppid = jwtAuthorizer;
		}
		MergedUploadWxaInput merged = mergeService.merge(request, body);
		wxaUploadWxaService.uploadWxa(companyId, authorizerAppid, operatorId, merged);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", true);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.codeunlimit")
	@GetMapping(
			value = "/wxa/codeunlimit",
			name = "上传小程序码base64",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> uploadWxaCodeUnlimit(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		Object rawAuth = map.get("authorizer_appid");
		String jwtAuthorizer = rawAuth == null ? null : String.valueOf(rawAuth).trim();
		Map<String, Object> data = wxaUploadWxaCodeUnlimitService.uploadWxaCodeUnlimit(companyId, jwtAuthorizer, request);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.testqrcode")
	@GetMapping(
			value = "/wxa/testqrcode",
			name = "体验二维码base64",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getTestQrcode(
			HttpServletRequest request,
			@RequestParam(value = "wxaAppId", required = false) String wxaAppId,
			@RequestParam(value = "is_direct", defaultValue = "0") String isDirect) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		Map<String, Object> data = wxaGetTestQrcodeService.getTestQrcode(companyId, wxaAppId, isDirect);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.tryrelease")
	@PostMapping(value = "/wxa/tryrelease", name = "尝试发布", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> tryRelease(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		MergedUploadWxaInput merged = mergeService.merge(request, body);
		String raw = merged.wxaAppId() == null ? "" : merged.wxaAppId().trim();
		if (!WxaUploadWxaService.wxaAppIdHasText(raw)) {
			throw new BadRequestException("wxaAppId 不能为空");
		}
		if (!wechatAuthQueryService.isMiniProgramWxaBoundToCompany(companyId, raw)) {
			throw new ResourceException("小程序未绑定，请重新绑定", 400, 400001);
		}
		String message = wxaTryReleaseService.tryRelease(companyId, raw);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", message);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.undocodeaudit")
	@GetMapping(
			value = "/wxa/undocodeaudit",
			name = "审核撤回",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> undocodeaudit(
			HttpServletRequest request,
			@RequestParam(value = "wxaAppId", required = false) String wxaAppId) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		String raw = wxaAppId == null ? "" : wxaAppId.trim();
		if (WxaUploadWxaService.wxaAppIdRawParamRequiresBindingCheck(wxaAppId)) {
			if (!wechatAuthQueryService.isMiniProgramWxaBoundToCompany(companyId, raw)) {
				throw new ResourceException("小程序未绑定，请重新绑定", 400, 400001);
			}
		}
		wxaUndocodeauditService.undocodeaudit(companyId, raw);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("message", null);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.revertcoderelease")
	@GetMapping(
			value = "/wxa/revertcoderelease",
			name = "回退版本",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> revertcoderelease(
			HttpServletRequest request,
			@RequestParam(value = "wxaAppId", required = false) String wxaAppId) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		String raw = wxaAppId == null ? "" : wxaAppId.trim();
		if (!WxaUploadWxaService.wxaAppIdHasText(raw)) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("message", null);
			return ResponseEntity.ok(ApiResult.ok(data));
		}
		if (!wechatAuthQueryService.isMiniProgramWxaBoundToCompany(companyId, raw)) {
			throw new ResourceException("小程序未绑定，请重新绑定", 400, 400001);
		}
		JsonNode message = wxaRevertCodeReleaseService.revertcoderelease(companyId, raw);
		Map<String, Object> data = new LinkedHashMap<>();
		if (message == null) {
			data.put("message", null);
		} else {
			data.put("message", message);
		}
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.pageparams.setting.set")
	@PostMapping(
			value = "/wxa/pageparams/setting",
			name = "保存页面挂件配置",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> setPageParams(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		String templateName = mergedString(request, body, "template_name");
		String pageName = mergedString(request, body, "page_name");
		if (pageName == null || pageName.isBlank()) {
			pageName = "index";
		}
		String configName = mergedString(request, body, "name");
		LinkedHashMap<String, Object> params = normalizePageParams(request, body);
		wxaPageParamsSettingService.setPageParams(companyId, templateName, pageName, configName, params);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String mergedString(HttpServletRequest request, Map<String, Object> body, String key) {
		String v = request.getParameter(key);
		if (body != null && body.containsKey(key) && body.get(key) != null) {
			v = String.valueOf(body.get(key));
		}
		return v;
	}

	private static Object mergedObject(HttpServletRequest request, Map<String, Object> body, String key) {
		if (body != null && body.containsKey(key) && body.get(key) != null) {
			return body.get(key);
		}
		if (request.getParameterMap().containsKey(key)) {
			return request.getParameter(key);
		}
		return null;
	}

	private static boolean isDistributorIdTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return Boolean.TRUE.equals(b);
		}
		if (v instanceof Number n) {
			return n.longValue() != 0;
		}
		String s = String.valueOf(v).trim();
		return !s.isEmpty() && !"0".equals(s);
	}

	private static String distributorIdToShopVersionSuffix(Object v) {
		if (v instanceof Boolean b && Boolean.TRUE.equals(b)) {
			return "1";
		}
		if (v instanceof Number n) {
			if (n.doubleValue() == (double) n.longValue()) {
				return Long.toString(n.longValue());
			}
			return String.valueOf(v).trim();
		}
		return String.valueOf(v).trim();
	}

	private static int oneBasedRowIndexFromKey(Object key) {
		if (key instanceof Number n) {
			return n.intValue() + 1;
		}
		if (key == null) {
			return 1;
		}
		if (key instanceof String s) {
			s = s.trim();
			if (s.isEmpty()) {
				return 1;
			}
			if (s.matches("-?(0|[1-9]\\d*)")) {
				return Integer.parseInt(s) + 1;
			}
			return 1;
		}
		return oneBasedRowIndexFromKey(String.valueOf(key));
	}

	private List<SavePageAllParamsRow> parseConfigRowsForSavePageAllParams(
			HttpServletRequest request, Map<String, Object> body) {
		Object raw = null;
		if (body != null && body.containsKey("config")) {
			raw = body.get("config");
		} else if (request.getParameterMap().containsKey("config")) {
			raw = request.getParameter("config");
		}
		if (raw == null) {
			throw new BadRequestException("config 须为合法 JSON");
		}
		if (raw instanceof List<?> list) {
			List<SavePageAllParamsRow> out = new ArrayList<>(list.size());
			for (int i = 0; i < list.size(); i++) {
				Object item = list.get(i);
				if (!(item instanceof Map<?, ?> m)) {
					throw new BadRequestException("config 须为合法 JSON");
				}
				out.add(new SavePageAllParamsRow(i + 1, deepCopyParamMap(m)));
			}
			return out;
		}
		if (raw instanceof Map<?, ?>) {
			JsonNode root = objectMapper.valueToTree(raw);
			if (!root.isObject()) {
				throw new BadRequestException("config 须为合法 JSON");
			}
			List<SavePageAllParamsRow> out = new ArrayList<>();
			for (Iterator<Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
				Entry<String, JsonNode> e = it.next();
				String k = e.getKey();
				JsonNode vNode = e.getValue();
				if (!vNode.isObject()) {
					throw new BadRequestException("config 须为合法 JSON");
				}
				out.add(
						new SavePageAllParamsRow(
								oneBasedRowIndexFromKey(k), deepCopyParamMap(jsonObjectToParamMap(vNode))));
			}
			return out;
		}
		JsonNode root;
		if (raw instanceof String s) {
			try {
				root = objectMapper.readTree(s.trim());
			} catch (Exception e) {
				throw new BadRequestException("config 须为合法 JSON");
			}
		} else if (raw instanceof JsonNode jn) {
			root = jn;
		} else {
			throw new BadRequestException("config 须为合法 JSON");
		}
		if (root == null || root.isNull()) {
			throw new BadRequestException("config 须为合法 JSON");
		}
		if (root.isArray()) {
			List<SavePageAllParamsRow> out = new ArrayList<>(root.size());
			for (int i = 0; i < root.size(); i++) {
				JsonNode el = root.get(i);
				if (!el.isObject()) {
					throw new BadRequestException("config 须为合法 JSON");
				}
				out.add(new SavePageAllParamsRow(i + 1, jsonObjectToParamMap(el)));
			}
			return out;
		}
		if (root.isObject()) {
			List<SavePageAllParamsRow> out = new ArrayList<>();
			for (Iterator<Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
				Entry<String, JsonNode> e = it.next();
				JsonNode vNode = e.getValue();
				if (!vNode.isObject()) {
					throw new BadRequestException("config 须为合法 JSON");
				}
				out.add(new SavePageAllParamsRow(oneBasedRowIndexFromKey(e.getKey()), jsonObjectToParamMap(vNode)));
			}
			return out;
		}
		throw new BadRequestException("config 须为合法 JSON");
	}

	private LinkedHashMap<String, Object> normalizePageParams(HttpServletRequest request, Map<String, Object> body) {
		Object raw = null;
		if (body != null && body.containsKey("params")) {
			raw = body.get("params");
		} else if (request.getParameterMap().containsKey("params")) {
			raw = request.getParameter("params");
		}
		if (raw == null) {
			return new LinkedHashMap<>();
		}
		if (raw instanceof Map<?, ?> m) {
			return deepCopyParamMap(m);
		}
		if (raw instanceof List<?> list) {
			return listToIndexedParamMap(list);
		}
		if (raw instanceof String s) {
			JsonNode root;
			try {
				root = objectMapper.readTree(s.trim());
			} catch (Exception e) {
				throw new BadRequestException("params 须为 JSON 对象或数组");
			}
			if (root.isObject()) {
				return jsonObjectToParamMap(root);
			}
			if (root.isArray()) {
				return jsonArrayToIndexedParamMap(root);
			}
			throw new BadRequestException("params 须为 JSON 对象或数组");
		}
		throw new BadRequestException("params 须为 JSON 对象或数组");
	}

	private LinkedHashMap<String, Object> deepCopyParamMap(Map<?, ?> m) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), normalizeParamValue(e.getValue()));
		}
		return out;
	}

	private Object normalizeParamValue(Object v) {
		if (v instanceof Map<?, ?> m) {
			return deepCopyParamMap(m);
		}
		if (v instanceof List<?> list) {
			List<Object> out = new ArrayList<>(list.size());
			for (Object item : list) {
				out.add(normalizeParamValue(item));
			}
			return out;
		}
		return v;
	}

	private LinkedHashMap<String, Object> listToIndexedParamMap(List<?> list) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (int i = 0; i < list.size(); i++) {
			out.put(String.valueOf(i), normalizeParamValue(list.get(i)));
		}
		return out;
	}

	private LinkedHashMap<String, Object> jsonObjectToParamMap(JsonNode node) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		node.fields().forEachRemaining(e -> out.put(e.getKey(), jsonNodeToParamValue(e.getValue())));
		return out;
	}

	private LinkedHashMap<String, Object> jsonArrayToIndexedParamMap(JsonNode arr) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (int i = 0; i < arr.size(); i++) {
			out.put(String.valueOf(i), jsonNodeToParamValue(arr.get(i)));
		}
		return out;
	}

	private Object jsonNodeToParamValue(JsonNode n) {
		if (n.isObject()) {
			return jsonObjectToParamMap(n);
		}
		if (n.isArray()) {
			List<Object> out = new ArrayList<>(n.size());
			for (JsonNode c : n) {
				out.add(jsonNodeToParamValue(c));
			}
			return out;
		}
		if (n.isNull()) {
			return null;
		}
		if (n.isBoolean()) {
			return n.booleanValue();
		}
		if (n.isNumber()) {
			if (n.isIntegralNumber()) {
				return n.longValue();
			}
			return n.doubleValue();
		}
		if (n.isTextual()) {
			return n.asText();
		}
		throw new BadRequestException("params 须为 JSON 对象或数组");
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.pageparams.setting.get")
	@GetMapping(
			value = "/wxa/pageparams/setting",
			name = "获取页面配置",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Object>> getParamByTempName(
			HttpServletRequest request,
			@RequestParam(value = "template_name", required = false) String templateName,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "page_name", required = false) String pageName,
			@RequestParam(value = "distributor_id", required = false) String distributorId,
			@RequestParam(value = "version", required = false) String version,
			@RequestParam(value = "country_code", required = false) String countryCode) {
		long companyId = requireCompanyIdFromOperatorJwt(request);
		String localeTagForI18n =
				(countryCode == null || countryCode.isBlank()) ? "zh-CN" : countryCode.trim();
		Object body =
				wxaPageParamsSettingService.getParamByTempName(
						companyId, templateName, name, pageName, distributorId, version, localeTagForI18n);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	private static long requireCompanyIdFromOperatorJwt(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		try {
			return Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.pageparams.setting.update")
	@PutMapping(
			value = "/wxa/pageparams/setting",
			name = "更新页面配置",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateParamsById(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		Object idRaw = mergedObject(request, body, "id");
		Object paramsRaw = mergedObject(request, body, "params");
		wxaPageParamsSettingService.updateParamsById(companyId, idRaw, paramsRaw);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.pageparams.setting.all.set")
	@PostMapping(
			value = "/wxa/pageparams/setting_all",
			name = "保存页面配置全集",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> savePageAllParams(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		String templateName = mergedString(request, body, "template_name");
		String pageName = mergedString(request, body, "page_name");
		if (pageName == null || pageName.isBlank()) {
			pageName = "index";
		}
		Object distributorRaw = mergedObject(request, body, "distributor_id");
		String resolvedVersion;
		if (isDistributorIdTruthy(distributorRaw)) {
			resolvedVersion = "shop_" + distributorIdToShopVersionSuffix(distributorRaw);
		} else {
			String ver = mergedString(request, body, "version");
			if (ver == null || ver.isBlank()) {
				resolvedVersion = "v1.0.1";
			} else {
				resolvedVersion = ver;
			}
		}
		List<SavePageAllParamsRow> configRows = parseConfigRowsForSavePageAllParams(request, body);
		wxaPageParamsSettingService.savePageAllParams(
				companyId, templateName, pageName, resolvedVersion, configRows);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.wxa.opentemplate.list")
	@GetMapping(value = "/wxa/templates/openlist", name = "已有模版列表")
	public ResponseEntity<Void> getOpenTemplateList() {
		return ResponseEntity.ok().build();
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.template.list")
	@GetMapping(
			value = "/wxa/templates/list",
			name = "模版列表",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getTemplateList(HttpServletRequest request) {
		String authorizerAppid = null;
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (attr instanceof Map<?, ?> map) {
			Object rawAuth = map.get("authorizer_appid");
			authorizerAppid = rawAuth == null ? null : String.valueOf(rawAuth).trim();
			if (authorizerAppid != null && authorizerAppid.isEmpty()) {
				authorizerAppid = null;
			}
		}
		List<Map<String, Object>> list = wxaGetTemplateListService.getTemplateList(authorizerAppid);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.wxa.opentemplate")
	@PostMapping(value = "/wxa/templates/open", name = "开通模版")
	public ResponseEntity<Void> openTemplate() {
		return ResponseEntity.ok().build();
	}

	@Activated(routeAlias = "wechat.wxa.weappid")
	@GetMapping(value = "/wxa/templates/weappid", name = "weappid")
	public ResponseEntity<Void> getWeappId() {
		return ResponseEntity.ok().build();
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.config")
	@PutMapping(value = "/wxa/config/{wxaAppId}", name = "小程序配置保存", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> saveConfig(
			HttpServletRequest request,
			@PathVariable("wxaAppId") String wxaAppId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object rawAutoPublish = mergedObject(request, body, "auto_publish");
		int autoPublish = parseSaveConfigAutoPublish(rawAutoPublish);
		String authorizerAppsecret = mergedString(request, body, "authorizer_appsecret");
		String secretTrimmed = authorizerAppsecret == null ? "" : authorizerAppsecret.trim();
		wxaSaveConfigService.saveConfig(wxaAppId, autoPublish, secretTrimmed);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", true);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parseSaveConfigAutoPublish(Object raw) {
		if (raw == null) {
			throw new BadRequestException("自动发布参数错误", 422);
		}
		if (raw instanceof Boolean b) {
			return Boolean.TRUE.equals(b) ? 1 : 0;
		}
		if (raw instanceof Number n) {
			int v = n.intValue();
			if (v == 0 || v == 1) {
				return v;
			}
			throw new BadRequestException("自动发布参数错误", 422);
		}
		String s = String.valueOf(raw).trim();
		if ("0".equals(s)) {
			return 0;
		}
		if ("1".equals(s)) {
			return 1;
		}
		throw new BadRequestException("自动发布参数错误", 422);
	}

	@Activated(routeAlias = "wechat.wxa.config.detail")
	@GetMapping(value = "/wxa/config/{wxaAppId}", name = "获取上架配置")
	public ResponseEntity<Void> getConfigDetail(@PathVariable("wxaAppId") String wxaAppId) {
		return ResponseEntity.ok().build();
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.onlycode")
	@PostMapping(
			value = "/wxa/onlycode",
			name = "仅上传代码",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> commitTempCode(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		Object operatorIdObj = map.get("operator_id");
		if (operatorIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long operatorId;
		try {
			operatorId = Long.parseLong(String.valueOf(operatorIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		Object rawAuth = map.get("authorizer_appid");
		String authorizerAppid = rawAuth == null ? "" : String.valueOf(rawAuth).trim();
		MergedUploadWxaInput merged = mergeService.merge(request, body);
		wxaUploadWxaService.commitTempCode(companyId, authorizerAppid, operatorId, merged);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.submitreview")
	@PostMapping(
			value = "/wxa/submitreview",
			name = "提交审核",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> submitReview(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		Object operatorIdObj = map.get("operator_id");
		if (operatorIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long operatorId;
		try {
			operatorId = Long.parseLong(String.valueOf(operatorIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		Object rawAuth = map.get("authorizer_appid");
		String jwtAuthorizer = rawAuth == null ? null : String.valueOf(rawAuth).trim();
		String authorizerAppid;
		if (!WechatOpenUserPlatformService.hasEffectiveAuthorizerAppid(jwtAuthorizer)) {
			authorizerAppid =
					Objects.requireNonNullElse(wechatAuthQueryService.findBoundMiniProgramAuthorizerAppid(companyId), "");
		} else {
			authorizerAppid = jwtAuthorizer;
		}
		MergedUploadWxaInput merged = mergeService.mergeForSubmitReview(request, body);
		wxaUploadWxaService.submitReview(companyId, authorizerAppid, operatorId, merged);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.getdomainlist")
	@PostMapping(value = "/wxa/getdomainlist", name = "获取域名", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getDomainList(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		String wxaAppId = request.getParameter("wxaAppId");
		if (body != null && body.containsKey("wxaAppId") && body.get("wxaAppId") != null) {
			wxaAppId = String.valueOf(body.get("wxaAppId"));
		}
		String templateNameRaw = request.getParameter("templateName");
		if (body != null && body.containsKey("templateName") && body.get("templateName") != null) {
			templateNameRaw = String.valueOf(body.get("templateName"));
		}
		Map<String, Object> data = wxaGetDomainListService.getDomainList(
				companyId,
				wxaAppId == null ? "" : wxaAppId.trim(),
				templateNameRaw == null ? "" : templateNameRaw.trim());
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.saveDomain")
	@PostMapping(value = "/wxa/savedomain", name = "保存域名", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> saveDomain(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		String wxaAppId = request.getParameter("wxaAppId");
		if (body != null && body.containsKey("wxaAppId") && body.get("wxaAppId") != null) {
			wxaAppId = String.valueOf(body.get("wxaAppId"));
		}
		String templateNameRaw = request.getParameter("templateName");
		if (body != null && body.containsKey("templateName") && body.get("templateName") != null) {
			templateNameRaw = String.valueOf(body.get("templateName"));
		}
		String mergedWxaAppId = wxaAppId == null ? "" : wxaAppId.trim();
		String templateNameTrimmed = templateNameRaw == null ? "" : templateNameRaw.trim();
		wxaSaveDomainService.saveDomain(companyId, mergedWxaAppId, templateNameTrimmed);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.cartremind.setting.set")
	@PostMapping(
			value = "/wxa/cartremind/setting",
			name = "购物车提醒保存",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> setCartremindSetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		String isOpenRaw = request.getParameter("is_open");
		if (body != null && body.containsKey("is_open") && body.get("is_open") != null) {
			isOpenRaw = String.valueOf(body.get("is_open"));
		}
		boolean isOpen = isOpenRaw != null && "true".equals(isOpenRaw);
		boolean remindContentKeyPresent = request.getParameterMap().containsKey("remind_content")
				|| (body != null && body.containsKey("remind_content"));
		if (!remindContentKeyPresent) {
			throw new ResourceException("提醒内容必填");
		}
		if (body != null && body.containsKey("remind_content") && body.get("remind_content") == null) {
			throw new ResourceException("提醒内容必填");
		}
		String mergedRemindContent = request.getParameter("remind_content");
		if (body != null && body.containsKey("remind_content") && body.get("remind_content") != null) {
			mergedRemindContent = String.valueOf(body.get("remind_content"));
		}
		wxaCartremindSettingService.setCartremindSetting(companyId, isOpen, mergedRemindContent);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", true);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.cartremind.setting.get")
	@GetMapping(
			value = "/wxa/cartremind/setting",
			name = "购物车提醒获取",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getCartremindSetting(HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		Map<String, Object> data = wxaCartremindSettingService.getCartremindSetting(companyId);
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.privacy.setting.get")
	@GetMapping(
			value = "/wxa/privacy/setting",
			name = "隐私指引查询",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getPrivacySetting(
			HttpServletRequest request,
			@RequestParam(value = "wxaAppId", required = false) String wxaAppId) {
		long companyId = requireCompanyIdFromOperatorJwt(request);
		if (wxaAppId == null || !StringUtils.hasText(wxaAppId.trim())) {
			throw new BadRequestException("缺少小程序 appid");
		}
		Map<String, Object> data = wxaGetPrivacySettingService.getPrivacySetting(companyId, wxaAppId.trim());
		return ApiResult.ok(data);
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.privacy.setting.set")
	@PostMapping(value = "/wxa/privacy/setting", name = "隐私指引设置", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> setPrivacySetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		String wxaAppId = request.getParameter("wxaAppId");
		if (body != null && body.containsKey("wxaAppId") && body.get("wxaAppId") != null) {
			wxaAppId = String.valueOf(body.get("wxaAppId"));
		}
		if (wxaAppId == null || !StringUtils.hasText(wxaAppId.trim())) {
			throw new BadRequestException("缺少小程序 appid");
		}
		String ownerSettingRaw = mergeJsonRawFromRequest(request, body, "owner_setting", "owner_setting 不是合法 JSON");
		String settingListRaw =
				mergeJsonRawFromRequest(request, body, "setting_list", "setting_list 不是合法 JSON");
		wxaSetPrivacySettingService.setPrivacySetting(companyId, wxaAppId.trim(), ownerSettingRaw, settingListRaw);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", true);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private String mergeJsonRawFromRequest(
			HttpServletRequest request, Map<String, Object> body, String key, String illegalJsonMessage) {
		String raw = request.getParameter(key);
		if (body != null && body.containsKey(key) && body.get(key) != null) {
			Object v = body.get(key);
			if (v instanceof Map<?, ?> || v instanceof List<?>) {
				try {
					raw = objectMapper.writeValueAsString(v);
				} catch (JsonProcessingException e) {
					throw new BadRequestException(illegalJsonMessage);
				}
			} else {
				raw = String.valueOf(v);
			}
		}
		return raw;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.privacy.extfile.upload")
	@PostMapping(
			value = "/wxa/uploadprivacy/extfile",
			name = "上传隐私指引文件",
			consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> uploadPrivacyExtFile(
			HttpServletRequest request,
			@RequestParam(value = "wxaAppId", required = false) String wxaAppId,
			@RequestParam(value = "file", required = false) MultipartFile file) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		if (wxaAppId == null || !StringUtils.hasText(wxaAppId.trim())) {
			throw new BadRequestException("缺少小程序 appid");
		}
		Map<String, Object> result =
				wxaUploadPrivacyExtFileService.uploadPrivacyExtFile(companyId, wxaAppId.trim(), file);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "wechat.wxa.detail")
	@GetMapping(
			value = "/wxa/{wxaAppId}",
			name = "授权小程序详情",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> getWxaDetail(@PathVariable("wxaAppId") String wxaAppId) {
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?> map)) {
			throw new BadRequestException("登录上下文无效");
		}
		Object companyIdObj = map.get("company_id");
		if (companyIdObj == null) {
			throw new BadRequestException("登录上下文无效");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("登录上下文无效");
		}
		return ApiResult.ok(wxaGetWxaDetailService.getWxaDetail(companyId, wxaAppId));
	}
}
