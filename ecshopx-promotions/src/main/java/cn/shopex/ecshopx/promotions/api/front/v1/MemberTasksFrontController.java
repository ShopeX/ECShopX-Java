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

package cn.shopex.ecshopx.promotions.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskBrowseCompletionService;
import cn.shopex.ecshopx.promotions.service.membertask.MemberTaskFrontListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端会员任务（SSOT G24 / §11.6 · ECX-10434 列表仍为骨架；browse 上报 ECX-10467）。
 */
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("promotionsFrontV1MemberTasks")
@RequestMapping("/api/v1/h5app/wxapp/promotion/member-tasks")
public class MemberTasksFrontController {

	private final MemberTaskFrontListService memberTaskFrontListService;
	private final MemberTaskBrowseCompletionService memberTaskBrowseCompletionService;
	private final LangueProperties langueProperties;

	public MemberTasksFrontController(
			MemberTaskFrontListService memberTaskFrontListService,
			MemberTaskBrowseCompletionService memberTaskBrowseCompletionService,
			LangueProperties langueProperties) {
		this.memberTaskFrontListService = memberTaskFrontListService;
		this.memberTaskBrowseCompletionService = memberTaskBrowseCompletionService;
		this.langueProperties = langueProperties;
	}

	@GetMapping(name = "C端会员任务列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> list(HttpServletRequest request) {
		Locale locale = LocaleContextHolder.getLocale();
		RequestLangTag.current(langueProperties);
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseAuthUserIdFromClaims(readH5AuthClaimsMap(request));
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		return ResponseEntity.ok(
				ApiResult.ok(memberTaskFrontListService.list(companyId, userId, locale)));
	}

	@PostMapping(value = "/browse-report", name = "浏览任务进度上报")
	public ResponseEntity<ApiResult<Map<String, Object>>> browseReport(
			HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
		Locale locale = LocaleContextHolder.getLocale();
		RequestLangTag.current(langueProperties);
		long companyId = parseCompanyIdFromRequest(request);
		long userId = parseAuthUserIdFromClaims(readH5AuthClaimsMap(request));
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		Map<String, Object> payload = body == null ? Map.of() : body;
		long taskId = longVal(payload.get("task_id"));
		String page = payload.get("page") == null ? "" : payload.get("page").toString().trim();
		int staySeconds = (int) Math.min(Integer.MAX_VALUE, longVal(payload.get("stay_seconds")));
		memberTaskBrowseCompletionService.reportBrowse(companyId, userId, taskId, page, staySeconds);
		return ResponseEntity.ok(ApiResult.ok(Map.of("accepted", true)));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long parseAuthUserIdFromClaims(Map<String, Object> claims) {
		Object v = claims == null ? null : claims.get("user_id");
		if (v == null) {
			return 0L;
		}
		try {
			long id = (v instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(v).trim());
			return id > 0L ? id : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parseCompanyIdFromRequest(HttpServletRequest request) {
		Object companyAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (companyAttr instanceof Number n) {
			companyId = n.longValue();
		} else if (companyAttr instanceof String s && StringUtils.hasText(s)) {
			companyId = Long.parseLong(s.trim());
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		if (companyId <= 0L) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		return companyId;
	}

	private static long longVal(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return LeadingNumberParser.parseAsLong(raw.toString().trim());
	}
}
