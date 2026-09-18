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

package cn.shopex.ecshopx.espier.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.espier.service.offline.OfflineBankAccountApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@AdminAuth
@ShopLog
@RestController("espierAdminV1OfflineBankAccount")
@RequestMapping("/api/v1/espier/offline/backaccount")
public class OfflineBankAccountController {

	private static final Pattern INTEGER_PREFIX_PATTERN = Pattern.compile("^([+-]?\\d+)");

	private final OfflineBankAccountApplicationService offlineBankAccountApplicationService;

	public OfflineBankAccountController(OfflineBankAccountApplicationService offlineBankAccountApplicationService) {
		this.offlineBankAccountApplicationService = offlineBankAccountApplicationService;
	}

	@Activated(routeAlias = "espier.offline.backaccount.lists")
	@GetMapping(value = "/lists", name = "获取线下收款账户列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getLists(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "country_code", required = false) String countryCodeRaw) {
		long companyId = extractCompanyId(request);
		String lang = (countryCodeRaw == null || !StringUtils.hasText(countryCodeRaw.trim()))
				? "zh-CN"
				: countryCodeRaw.trim();
		int page = parseIntQueryLoose(pageRaw, 1);
		int pageSize = parseIntQueryLoose(pageSizeRaw, 20);
		Map<String, Object> body = offlineBankAccountApplicationService.lists(companyId, page, pageSize, lang);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	private static int parseIntQueryLoose(String raw, int defaultWhenKeyAbsent) {
		if (raw == null) {
			return defaultWhenKeyAbsent;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 0;
		}
		Matcher m = INTEGER_PREFIX_PATTERN.matcher(t);
		if (!m.find()) {
			return 0;
		}
		try {
			return Integer.parseInt(m.group(1));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	@Activated(routeAlias = "espier.offline.backaccount.info")
	@GetMapping(value = "/{id}", name = "获取线下收款账户信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getInfo(
			HttpServletRequest request,
			@PathVariable("id") String idRaw,
			@RequestParam(value = "country_code", required = false) String countryCodeRaw) {
		long companyId = extractCompanyId(request);
		String lang = (countryCodeRaw == null || !StringUtils.hasText(countryCodeRaw.trim()))
				? "zh-CN"
				: countryCodeRaw.trim();
		long accountId = parseRequiredPositiveLongPathId(idRaw);
		Map<String, Object> data = offlineBankAccountApplicationService.getInfo(companyId, accountId, lang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseRequiredPositiveLongPathId(String idRaw) {
		if (idRaw == null) {
			throw new BadRequestException("id必填");
		}
		String t = idRaw.trim();
		if (!StringUtils.hasText(t)) {
			throw new BadRequestException("id必填");
		}
		if (!t.matches("^\\d+$")) {
			throw new BadRequestException("id格式不正确");
		}
		try {
			long v = Long.parseLong(t);
			if (v <= 0L) {
				throw new BadRequestException("id格式不正确");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("id格式不正确");
		}
	}

	@Activated(routeAlias = "espier.offline.backaccount.delete")
	@DeleteMapping(value = "/{id}", name = "删除线下收款账户")
	public ResponseEntity<ApiResult<Map<String, Object>>> delete(
			HttpServletRequest request,
			@PathVariable("id") String idRaw) {
		long companyId = extractCompanyId(request);
		String idPath = idRaw == null ? "" : idRaw;
		offlineBankAccountApplicationService.delete(companyId, idPath);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "espier.offline.backaccount.create")
	@PostMapping(value = "/create", name = "创建线下收款账户")
	public ResponseEntity<ApiResult<Map<String, Object>>> create(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		long companyId = extractCompanyId(request);

		Object cc = merged.get("country_code");
		String requestLangTag = (cc == null || !StringUtils.hasText(String.valueOf(cc).trim()))
				? "zh-CN"
				: String.valueOf(cc).trim();

		requireNonBlankMerged(merged, "bank_account_name", "收款账户名称必填");
		requireNonBlankMerged(merged, "bank_account_no", "银行账号必填");
		requireNonBlankMerged(merged, "bank_name", "开户银行必填");
		requireNonBlankMerged(merged, "china_ums_no", "银联号必填");

		String pic = blankToEmpty(merged, "pic");
		String remark = blankToEmpty(merged, "remark");

		Object rawDefault = merged.get("is_default");
		int isDefaultInt = (rawDefault instanceof String s && "true".equals(s)) ? 1 : 0;

		Map<String, Object> normalizedParams = new LinkedHashMap<>();
		normalizedParams.put("company_id", companyId);
		normalizedParams.put("bank_account_name", String.valueOf(merged.get("bank_account_name")).trim());
		normalizedParams.put("bank_account_no", String.valueOf(merged.get("bank_account_no")).trim());
		normalizedParams.put("bank_name", String.valueOf(merged.get("bank_name")).trim());
		normalizedParams.put("china_ums_no", String.valueOf(merged.get("china_ums_no")).trim());
		normalizedParams.put("pic", pic);
		normalizedParams.put("remark", remark);
		normalizedParams.put("is_default", isDefaultInt);

		offlineBankAccountApplicationService.create(companyId, normalizedParams, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static void requireNonBlankMerged(Map<String, Object> merged, String key, String message) {
		if (!merged.containsKey(key)
				|| merged.get(key) == null
				|| !StringUtils.hasText(String.valueOf(merged.get(key)).trim())) {
			throw new BadRequestException(message);
		}
	}

	private static String blankToEmpty(Map<String, Object> merged, String key) {
		if (!merged.containsKey(key) || merged.get(key) == null) {
			return "";
		}
		String s = String.valueOf(merged.get(key)).trim();
		return StringUtils.hasText(s) ? s : "";
	}

	@SuppressWarnings("unchecked")
	private long extractCompanyId(HttpServletRequest request) {
		Map<String, Object> user = (Map<String, Object>) request.getAttribute(
				OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (user == null) {
			throw new ResourceException("未登录");
		}
		Object v = user.get("company_id");
		if (v == null) {
			throw new ResourceException("无法获取公司ID");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(v.toString());
	}

	@Activated(routeAlias = "espier.offline.backaccount.update")
	@PostMapping(value = "/update", name = "更新线下收款账户")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		long companyId = extractCompanyId(request);

		Object cc = merged.get("country_code");
		String requestLangTag = (cc == null || !StringUtils.hasText(String.valueOf(cc).trim()))
				? "zh-CN"
				: String.valueOf(cc).trim();

		requireNonBlankMerged(merged, "bank_account_name", "收款账户名称必填");
		requireNonBlankMerged(merged, "bank_account_no", "银行账号必填");
		requireNonBlankMerged(merged, "bank_name", "开户银行必填");
		requireNonBlankMerged(merged, "china_ums_no", "银联号必填");
		requireNonBlankMerged(merged, "is_default", "是否默认必填");

		String pic = blankToEmpty(merged, "pic");
		String remark = blankToEmpty(merged, "remark");

		Object rawDefault = merged.get("is_default");
		int isDefaultInt = (rawDefault instanceof String s && "true".equals(s)) ? 1 : 0;

		Long accountId = parseAccountIdOrNullForUpdate(merged.get("id"));

		Map<String, Object> normalizedParams = new LinkedHashMap<>();
		normalizedParams.put("company_id", companyId);
		normalizedParams.put("bank_account_name", String.valueOf(merged.get("bank_account_name")).trim());
		normalizedParams.put("bank_account_no", String.valueOf(merged.get("bank_account_no")).trim());
		normalizedParams.put("bank_name", String.valueOf(merged.get("bank_name")).trim());
		normalizedParams.put("china_ums_no", String.valueOf(merged.get("china_ums_no")).trim());
		normalizedParams.put("pic", pic);
		normalizedParams.put("remark", remark);
		normalizedParams.put("is_default", isDefaultInt);

		offlineBankAccountApplicationService.update(companyId, accountId, normalizedParams, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	private static Long parseAccountIdOrNullForUpdate(Object idRaw) {
		if (idRaw == null) {
			return null;
		}
		String trimmed = String.valueOf(idRaw).trim();
		if (!StringUtils.hasText(trimmed)) {
			return null;
		}
		if (idRaw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("未查询到更新数据");
		}
	}
}
