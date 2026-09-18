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

package cn.shopex.ecshopx.merchant.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontMerchantAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.merchant.service.MerchantBaseSettingQueryService;
import cn.shopex.ecshopx.merchant.service.MerchantTypeVisibleListQueryService;
import cn.shopex.ecshopx.merchant.service.MerchantSettlementApplyDetailQueryService;
import cn.shopex.ecshopx.merchant.service.wxapp.MerchantSettlementApplyProgressService;
import cn.shopex.ecshopx.merchant.service.wxapp.MerchantWxappAuthAttributes;
import cn.shopex.ecshopx.merchant.service.wxapp.MerchantWxappLoginService;
import cn.shopex.ecshopx.merchant.service.wxapp.MerchantWxappPasswordResetService;
import cn.shopex.ecshopx.merchant.service.wxapp.MerchantWxappSettlementApplyAuditstatusService;
import cn.shopex.ecshopx.merchant.service.wxapp.MerchantWxappSettlementApplySaveService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
    resource = DingoResponse.ResourceStyle.DINGO,
    badRequest = DingoResponse.BadRequestStyle.DINGO_400
)
@RestController("merchantFrontV1Merchant")
@RequestMapping("/api/v1/h5app/wxapp/merchant")
public class MerchantController {

	private final MerchantWxappLoginService merchantWxappLoginService;
	private final MerchantWxappPasswordResetService merchantWxappPasswordResetService;
	private final MerchantWxappSettlementApplySaveService merchantWxappSettlementApplySaveService;
	private final MerchantBaseSettingQueryService merchantBaseSettingQueryService;
	private final MerchantWxappSettlementApplyAuditstatusService merchantWxappSettlementApplyAuditstatusService;
	private final MerchantSettlementApplyDetailQueryService merchantSettlementApplyDetailQueryService;
	private final MerchantSettlementApplyProgressService merchantSettlementApplyProgressService;
	private final MerchantTypeVisibleListQueryService merchantTypeVisibleListQueryService;
	private final LangueProperties langueProperties;

	public MerchantController(
			MerchantWxappLoginService merchantWxappLoginService,
			MerchantWxappPasswordResetService merchantWxappPasswordResetService,
			MerchantWxappSettlementApplySaveService merchantWxappSettlementApplySaveService,
			MerchantBaseSettingQueryService merchantBaseSettingQueryService,
			MerchantWxappSettlementApplyAuditstatusService merchantWxappSettlementApplyAuditstatusService,
			MerchantSettlementApplyDetailQueryService merchantSettlementApplyDetailQueryService,
			MerchantSettlementApplyProgressService merchantSettlementApplyProgressService,
			MerchantTypeVisibleListQueryService merchantTypeVisibleListQueryService,
			LangueProperties langueProperties) {
		this.merchantWxappLoginService = merchantWxappLoginService;
		this.merchantWxappPasswordResetService = merchantWxappPasswordResetService;
		this.merchantWxappSettlementApplySaveService = merchantWxappSettlementApplySaveService;
		this.merchantBaseSettingQueryService = merchantBaseSettingQueryService;
		this.merchantWxappSettlementApplyAuditstatusService = merchantWxappSettlementApplyAuditstatusService;
		this.merchantSettlementApplyDetailQueryService = merchantSettlementApplyDetailQueryService;
		this.merchantSettlementApplyProgressService = merchantSettlementApplyProgressService;
		this.merchantTypeVisibleListQueryService = merchantTypeVisibleListQueryService;
		this.langueProperties = langueProperties;
	}

	@PostMapping(value = "/login", name = "商家登录")
	public ResponseEntity<Map<String, Object>> login(
			HttpServletRequest request,
			@RequestHeader(value = "Origin", required = false) String origin,
			@FlexibleBody(required = false) Map<String, Object> body) {
		return ResponseEntity.ok(merchantWxappLoginService.loginWxapp(body, request, origin));
	}

	@FrontNoAuth
	@GetMapping(value = "/basesetting", name = "获取商户基础设置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getBaseSetting(HttpServletRequest request) {
		Object attr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		if (!(attr instanceof Integer companyId)) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		Map<String, Object> full = merchantBaseSettingQueryService.getBaseSetting(companyId.longValue());

		LinkedHashMap<String, Object> subset = new LinkedHashMap<>();
		Object displayOnPc = full.get("display_on_pc");
		subset.put("display_on_pc", displayOnPc != null ? String.valueOf(displayOnPc) : "false");

		Object settledRaw = full.get("settled_type");
		List<String> settledList = new ArrayList<>();
		if (settledRaw instanceof List<?> list) {
			for (Object o : list) {
				settledList.add(String.valueOf(o));
			}
		}
		subset.put("settled_type", settledList);

		Object contentVal = full.get("content");
		String contentStr =
				contentVal == null
						? ""
						: (contentVal instanceof String cs ? cs : String.valueOf(contentVal));
		subset.put("content", contentStr);

		return ResponseEntity.ok(ApiResult.ok(subset));
	}

	@FrontMerchantAuth
	@GetMapping(value = "/settlementapply/step", name = "商户入驻当前步骤")
	public ResponseEntity<ApiResult<Map<String, Integer>>> getSettlementApplyStep(HttpServletRequest request) {
		Object attr = request.getAttribute(MerchantWxappAuthAttributes.REQUEST_ATTR);
		if (!(attr instanceof MerchantWxappAuthAttributes auth)) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		int step =
				merchantSettlementApplyProgressService.resolveProgressStepWithSideEffects(
						auth.companyId(), auth.accountId());
		Map<String, Integer> data = new LinkedHashMap<>();
		data.put("step", step);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@GetMapping(value = "/type/list", name = "商户类型列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getVisibleTypeList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "page_size", required = false) String pageSizeRaw,
			@RequestParam(value = "parent_id", required = false, defaultValue = "0") String parentIdRaw,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "country_code", required = false) String countryCode) {
		Object attr = request.getAttribute(MerchantWxappAuthAttributes.REQUEST_ATTR);
		if (!(attr instanceof MerchantWxappAuthAttributes auth)) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		int page = parseWxappPage(pageRaw);
		int pageSize = parseWxappPageSize(pageSizeRaw);
		long parentId = parseParentIdForVisibleTypeList(parentIdRaw);
		String nameContains =
				(name == null || name.isBlank()) ? null : name.trim();
		String langTag =
				StringUtils.hasText(countryCode)
						? langueProperties.resolveToSupportedTag(countryCode.trim())
						: RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				merchantTypeVisibleListQueryService.queryVisibleTypeListPaged(
						auth.companyId(), parentId, nameContains, langTag, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parseWxappPage(String pageRaw) {
		if (pageRaw == null || pageRaw.isBlank()) {
			throw new ResourceException("当前页数为大于0的整数");
		}
		try {
			int v = Integer.parseInt(pageRaw.trim());
			if (v < 1) {
				throw new ResourceException("当前页数为大于0的整数");
			}
			return v;
		} catch (NumberFormatException ex) {
			throw new ResourceException("当前页数为大于0的整数");
		}
	}

	private static int parseWxappPageSize(String pageSizeRaw) {
		if (pageSizeRaw == null || pageSizeRaw.isBlank()) {
			throw new ResourceException("每页数量为1-50的整数");
		}
		try {
			int v = Integer.parseInt(pageSizeRaw.trim());
			if (v < 1 || v > 50) {
				throw new ResourceException("每页数量为1-50的整数");
			}
			return v;
		} catch (NumberFormatException ex) {
			throw new ResourceException("每页数量为1-50的整数");
		}
	}

	private static long parseParentIdForVisibleTypeList(String parentIdRaw) {
		if (parentIdRaw == null) {
			return 0L;
		}
		String t = parentIdRaw.trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			long v = Long.parseLong(t);
			if (v < 0) {
				throw new BadRequestException("无效的 parent_id");
			}
			return v;
		} catch (NumberFormatException ex) {
			throw new BadRequestException("无效的 parent_id");
		}
	}

	@FrontMerchantAuth
	@PostMapping(value = "/settlementapply/{step}", name = "保存商户入驻信息")
	public ResponseEntity<Map<String, Object>> saveSettlementApply(
			HttpServletRequest request,
			@PathVariable("step") String step,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> queryOrForm = FlexibleHttpServletParameterMap.toObjectMap(request);
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(queryOrForm);
		if (body != null) {
			for (Map.Entry<String, Object> e : body.entrySet()) {
				merged.put(e.getKey(), e.getValue());
			}
		}
		Object attr = request.getAttribute(MerchantWxappAuthAttributes.REQUEST_ATTR);
		if (!(attr instanceof MerchantWxappAuthAttributes auth)) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		merchantWxappSettlementApplySaveService.save(auth, step, merged);
		return ResponseEntity.ok(Map.of("data", Map.of("status", true)));
	}

	@FrontMerchantAuth
	@GetMapping(value = "/settlementapply/detail", name = "商户入驻详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSettlementApplyDetail(HttpServletRequest request) {
		Object attr = request.getAttribute(MerchantWxappAuthAttributes.REQUEST_ATTR);
		if (!(attr instanceof MerchantWxappAuthAttributes auth)) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		Map<String, Object> data =
				merchantSettlementApplyDetailQueryService.getDetail(
						auth.accountId(), RequestLangTag.current(langueProperties));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontMerchantAuth
	@GetMapping(value = "/settlementapply/auditstatus", name = "商户入驻审核结果")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSettlementApplyAuditstatus(HttpServletRequest request) {
		Object attr = request.getAttribute(MerchantWxappAuthAttributes.REQUEST_ATTR);
		if (!(attr instanceof MerchantWxappAuthAttributes auth)) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		Map<String, Object> data =
				merchantWxappSettlementApplyAuditstatusService.build(
						auth.companyId(), auth.accountId(), RequestLangTag.current(langueProperties));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontMerchantAuth
	@PostMapping(value = "/password/reset", name = "重置商户登录密码")
	public ResponseEntity<?> resetMerchantPassword(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object attr = request.getAttribute(MerchantWxappAuthAttributes.REQUEST_ATTR);
		if (!(attr instanceof MerchantWxappAuthAttributes auth)) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		Object data = merchantWxappPasswordResetService.reset(auth.companyId(), auth.accountId());
		return ResponseEntity.ok(Map.of("data", data));
	}
}
