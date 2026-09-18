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

package cn.shopex.ecshopx.theme.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PcLoginPageSettingSaveRequest;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PcTemplateAddRequest;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PcTemplateContentSaveRequest;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PcTemplateEditRequest;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PcTemplateHeaderFooterSaveRequest;
import cn.shopex.ecshopx.theme.service.PcLoginPageSettingGetService;
import cn.shopex.ecshopx.theme.service.PcLoginPageSettingSaveService;
import cn.shopex.ecshopx.theme.service.PcTemplateAddService;
import cn.shopex.ecshopx.theme.service.PcTemplateContentSaveService;
import cn.shopex.ecshopx.theme.service.PcTemplateDeleteService;
import cn.shopex.ecshopx.theme.service.PcTemplateEditService;
import cn.shopex.ecshopx.theme.service.PcTemplateGetDecorationContentService;
import cn.shopex.ecshopx.theme.service.PcTemplateGetHeaderOrFooterService;
import cn.shopex.ecshopx.theme.service.PcTemplateGetTemplateContentService;
import cn.shopex.ecshopx.theme.service.PcTemplateHeaderFooterSaveService;
import cn.shopex.ecshopx.theme.service.PcTemplateListService;
import cn.shopex.ecshopx.theme.service.dto.PcTemplateListQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RequiredArgsConstructor
@RestController("themeAdminV1PcTemplate")
@RequestMapping("/api/v1/pctemplate")
public class PcTemplateController {

	private final PcTemplateAddService pcTemplateAddService;
	private final PcTemplateEditService pcTemplateEditService;
	private final PcLoginPageSettingSaveService pcLoginPageSettingSaveService;
	private final PcLoginPageSettingGetService pcLoginPageSettingGetService;
	private final PcTemplateHeaderFooterSaveService pcTemplateHeaderFooterSaveService;
	private final PcTemplateGetHeaderOrFooterService pcTemplateGetHeaderOrFooterService;
	private final PcTemplateGetDecorationContentService pcTemplateGetDecorationContentService;
	private final PcTemplateGetTemplateContentService pcTemplateGetTemplateContentService;
	private final PcTemplateContentSaveService pcTemplateContentSaveService;
	private final PcTemplateListService pcTemplateListService;
	private final PcTemplateDeleteService pcTemplateDeleteService;
	private final LangueProperties langueProperties;
	private final ObjectMapper objectMapper;

	@Activated(routeAlias = "pctemplate.lists")
	@GetMapping(value = "/lists", name = "pc模板列表")
	public ApiResult<Map<String, Object>> lists(
			HttpServletRequest request,
			@RequestParam(value = "page_no", required = false) String pageNoStr,
			@RequestParam(value = "page_size", required = false) String pageSizeStr) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		int pageNo = parsePcTemplateListPageNo(pageNoStr);
		int pageSize = parsePcTemplateListPageSize(pageSizeStr);
		PcTemplateListQuery query = PcTemplateListQuery.fromHttpServletRequest(request, companyId, pageNo, pageSize);
		String requestLang = RequestLangTag.current(langueProperties);
		return ApiResult.ok(pcTemplateListService.lists(query, requestLang));
	}

	@Activated(routeAlias = "pctemplate.add")
	@PostMapping(value = "/add", name = "新增pc模板")
	public ApiResult<Map<String, Object>> add(HttpServletRequest request, @FlexibleBody PcTemplateAddRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String requestLang = RequestLangTag.current(langueProperties);
		return ApiResult.ok(pcTemplateAddService.add(companyId, requestLang, body));
	}

	@Activated(routeAlias = "pctemplate.edit")
	@PutMapping(value = "/edit", name = "编辑pc模板")
	public ApiResult<Map<String, Object>> edit(HttpServletRequest request, @FlexibleBody(required = false) JsonNode body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String requestLang = RequestLangTag.current(langueProperties);
		if (body == null) {
			throw new BadRequestException("请求体不能为空");
		}
		PcTemplateEditRequest req = objectMapper.convertValue(body, PcTemplateEditRequest.class);
		Integer st = req.getStatus();
		if (st != null && st.intValue() != 0 && st.intValue() != 1 && st.intValue() != 2) {
			throw new BadRequestException("启用状态不合法");
		}
		return ApiResult.ok(pcTemplateEditService.edit(companyId, requestLang, req));
	}

	@Activated(routeAlias = "pctemplate.delete")
	@DeleteMapping(value = "/delete/{theme_pc_template_id}", name = "删除pc模板")
	public ApiResult<Map<String, Object>> delete(HttpServletRequest request, @PathVariable("theme_pc_template_id") String id) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		return ApiResult.ok(pcTemplateDeleteService.delete(companyId, id));
	}

	@Activated(routeAlias = "pctemplate.getHeaderOrFooter")
	@GetMapping(value = "/getHeaderOrFooter", name = "头部尾部")
	public ApiResult<Object> getHeaderOrFooter(
			HttpServletRequest request,
			@RequestParam(value = "page_name", required = false) String pageName) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String requestLang = RequestLangTag.current(langueProperties);
		Object data = pcTemplateGetHeaderOrFooterService.getHeaderOrFooter(companyId, requestLang, pageName);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "pctemplate.saveHeaderOrFooter")
	@PostMapping(value = "/saveHeaderOrFooter", name = "头尾部保存")
	public ApiResult<Map<String, Object>> saveHeaderOrFooter(
			HttpServletRequest request, @FlexibleBody PcTemplateHeaderFooterSaveRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String requestLang = RequestLangTag.current(langueProperties);
		if (body == null) {
			throw new BadRequestException("请求体不能为空");
		}
		return ApiResult.ok(pcTemplateHeaderFooterSaveService.saveHeaderOrFooter(companyId, requestLang, body));
	}

	@Activated(routeAlias = "pctemplate.getTemplateContent")
	@GetMapping(value = "/getTemplateContent", name = "pc模板内容")
	public ApiResult<List<Map<String, Object>>> getTemplateContent(
			HttpServletRequest request,
			@RequestParam(value = "theme_pc_template_id", required = false) String themePcTemplateId) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String requestLang = RequestLangTag.current(langueProperties);
		List<Map<String, Object>> data =
				pcTemplateGetTemplateContentService.getTemplateContent(
						companyId, requestLang, themePcTemplateId, null, 0L);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "pctemplate.getDecorationContent")
	@GetMapping(value = "/getDecorationContent", name = "获取pc模板装修内容")
	public ApiResult<Map<String, Object>> getDecorationContent(
			HttpServletRequest request,
			@RequestParam(value = "page_name", required = false, defaultValue = "page") String pageName,
			@RequestParam(value = "theme_pc_template_id", required = false) String themePcTemplateId,
			@RequestParam(value = "page_type", required = false) String pageType,
			@RequestParam(value = "page_id", required = false) String pageId,
			@RequestParam(value = "distributor_id", required = false) String distributorId) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String requestLang = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				pcTemplateGetDecorationContentService.getDecorationContent(
						companyId,
						requestLang,
						themePcTemplateId,
						pageType,
						pageId,
						parseDistributorIdOrZero(distributorId));
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "pctemplate.saveTemplateContent")
	@PostMapping(value = "/saveTemplateContent", name = "保存pc模板内容")
	public ApiResult<Map<String, Object>> saveTemplateContent(
			HttpServletRequest request, @FlexibleBody PcTemplateContentSaveRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String requestLang = RequestLangTag.current(langueProperties);
		if (body == null) {
			throw new BadRequestException("请求体不能为空");
		}
		pcTemplateContentSaveService.saveTemplateContent(companyId, requestLang, body);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "pctemplate.getLoginPageSetting")
	@GetMapping(value = "/loginPage/setting", name = "pc登录页设置查询")
	public ApiResult<Map<String, Object>> getLoginPageSetting(HttpServletRequest request) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		return ApiResult.ok(pcLoginPageSettingGetService.getLoginPageSetting(companyId));
	}

	@Activated(routeAlias = "pctemplate.saveLoginPageSetting")
	@PostMapping(value = "/loginPage/setting", name = "pc登录页设置保存")
	public ApiResult<Map<String, Object>> saveLoginPageSetting(
			HttpServletRequest request, @FlexibleBody PcLoginPageSettingSaveRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		pcLoginPageSettingSaveService.saveLoginPageSetting(companyId, body);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ApiResult.ok(data);
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> m) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	private static long parsePositiveLongClaim(Map<String, Object> jwt, String key, String invalidMsg) {
		Object cid = jwt.get(key);
		if (cid == null) {
			throw new BadRequestException(invalidMsg);
		}
		long result;
		if (cid instanceof Number n) {
			result = n.longValue();
		} else if (cid instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException(invalidMsg);
			}
			try {
				result = Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(invalidMsg);
			}
		} else {
			try {
				result = Long.parseLong(String.valueOf(cid).trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(invalidMsg);
			}
		}
		if (result <= 0L) {
			throw new BadRequestException(invalidMsg);
		}
		return result;
	}

	private static long parseDistributorIdOrZero(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return 0L;
		}
		try {
			long v = Long.parseLong(raw.trim());
			return v < 0L ? 0L : v;
		} catch (NumberFormatException ex) {
			return 0L;
		}
	}

	private static int parsePcTemplateListPageNo(String pageNoStr) {
		String raw = (pageNoStr != null && StringUtils.hasText(pageNoStr)) ? pageNoStr.trim() : "1";
		if (!StringUtils.hasText(raw)) {
			raw = "1";
		}
		try {
			int v = Integer.parseInt(raw.trim());
			if (v < 1) {
				throw new BadRequestException("page_no 无效");
			}
			return v;
		} catch (NumberFormatException ex) {
			throw new BadRequestException("page_no 无效");
		}
	}

	private static int parsePcTemplateListPageSize(String pageSizeStr) {
		String raw = (pageSizeStr != null && StringUtils.hasText(pageSizeStr)) ? pageSizeStr.trim() : "20";
		if (!StringUtils.hasText(raw)) {
			raw = "20";
		}
		try {
			int v = Integer.parseInt(raw.trim());
			if (v < 1) {
				throw new BadRequestException("page_size 无效");
			}
			return v;
		} catch (NumberFormatException ex) {
			throw new BadRequestException("page_size 无效");
		}
	}
}
