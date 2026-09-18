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
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.theme.api.admin.v1.dto.PagesTemplateSetSetRequest;
import cn.shopex.ecshopx.theme.service.PagesTemplateSetGetInfoService;
import cn.shopex.ecshopx.theme.service.PagesTemplateSetSaveService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RequiredArgsConstructor
@RestController("themeAdminV1PagesTemplateSet")
@RequestMapping("/api/v1/pagestemplate")
public class PagesTemplateSetController {

	private final PagesTemplateSetSaveService pagesTemplateSetSaveService;
	private final PagesTemplateSetGetInfoService pagesTemplateSetGetInfoService;
	private final LangueProperties langueProperties;

	@Activated(routeAlias = "pagestemplateset.set")
	@PostMapping(value = "/set", name = "模板展示设置")
	public ApiResult<Map<String, Object>> set(
			HttpServletRequest request, @FlexibleBody(required = false) PagesTemplateSetSetRequest body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String requestLang = RequestLangTag.current(langueProperties);
		PagesTemplateSetSetRequest payload = body != null ? body : new PagesTemplateSetSetRequest();
		mergeQueryWhereBodyAbsent(payload, FlexibleHttpServletParameterMap.toObjectMap(request));
		return ApiResult.ok(pagesTemplateSetSaveService.set(companyId, requestLang, payload));
	}

	@Activated(routeAlias = "pagestemplateset.getInfo")
	@GetMapping(value = "/setInfo", name = "模板展示设置信息")
	public ApiResult<Object> getInfo(
			HttpServletRequest request,
			@RequestParam(name = "pages_template_id", required = false, defaultValue = "0") String pagesTemplateIdParam,
			@RequestParam(name = "regionauth_id", required = false, defaultValue = "0") String regionauthIdParam) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String requestLang = RequestLangTag.current(langueProperties);
		long pagesTemplateId = parseQueryLongDefaultZero(pagesTemplateIdParam);
		long regionauthId = parseQueryLongDefaultZero(regionauthIdParam);
		Optional<Map<String, Object>> optional =
				pagesTemplateSetGetInfoService.getInfo(companyId, requestLang, pagesTemplateId, regionauthId);
		return optional
				.map(rowMap -> ApiResult.<Object>ok(rowMap))
				.orElseGet(() -> ApiResult.<Object>ok(Collections.emptyList()));
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> m) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	private static long parseQueryLongDefaultZero(String raw) {
		if (raw == null) {
			return 0L;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return 0L;
		}
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

	private static void mergeQueryWhereBodyAbsent(PagesTemplateSetSetRequest payload, Map<String, Object> queryMap) {
		if (queryMap == null || queryMap.isEmpty()) {
			return;
		}
		if (payload.getRegionauthId() == null && queryMap.containsKey("regionauth_id")) {
			payload.setRegionauthId(parseLongNullable(queryMap.get("regionauth_id")));
		}
		if (payload.getPagesTemplateId() == null && queryMap.containsKey("pages_template_id")) {
			payload.setPagesTemplateId(parseLongNullable(queryMap.get("pages_template_id")));
		}
		if (payload.getIndexType() == null && queryMap.containsKey("index_type")) {
			payload.setIndexType(parseIntegerNullable(queryMap.get("index_type")));
		}
		if (payload.getIsEnforceSync() == null && queryMap.containsKey("is_enforce_sync")) {
			payload.setIsEnforceSync(parseIntegerNullable(queryMap.get("is_enforce_sync")));
		}
		if (payload.getIsOpenRecommend() == null && queryMap.containsKey("is_open_recommend")) {
			payload.setIsOpenRecommend(parseIntegerNullable(queryMap.get("is_open_recommend")));
		}
		if (payload.getIsOpenWechatappLocation() == null && queryMap.containsKey("is_open_wechatapp_location")) {
			payload.setIsOpenWechatappLocation(parseIntegerNullable(queryMap.get("is_open_wechatapp_location")));
		}
		if (payload.getIsOpenScanQrcode() == null && queryMap.containsKey("is_open_scan_qrcode")) {
			payload.setIsOpenScanQrcode(parseIntegerNullable(queryMap.get("is_open_scan_qrcode")));
		}
		if (payload.getIsOpenOfficialAccount() == null && queryMap.containsKey("is_open_official_account")) {
			payload.setIsOpenOfficialAccount(parseIntegerNullable(queryMap.get("is_open_official_account")));
		}
		if (payload.getTabBar() == null && queryMap.containsKey("tab_bar")) {
			Object v = queryMap.get("tab_bar");
			if (v == null) {
				payload.setTabBar(null);
			} else if (v instanceof String s) {
				payload.setTabBar(s);
			} else {
				payload.setTabBar(String.valueOf(v));
			}
		}
	}

	private static Long parseLongNullable(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return null;
			}
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				return null;
			}
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static Integer parseIntegerNullable(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		if (o instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return null;
			}
			try {
				return Integer.parseInt(s.trim());
			} catch (NumberFormatException ex) {
				return null;
			}
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}
