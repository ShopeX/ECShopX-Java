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

package cn.shopex.ecshopx.promotions.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.promotions.service.WxaTemplateOpenService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@RestController("promotionsAdminV1WxaTemplate")
@RequestMapping("/api/v1/wxa/notice")
public class WxaTemplateController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final WxaTemplateOpenService wxaTemplateOpenService;

	public WxaTemplateController(WxaTemplateOpenService wxaTemplateOpenService) {
		this.wxaTemplateOpenService = wxaTemplateOpenService;
	}

	private static Map<String, Object> readOperatorJwtMap(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			Object k = e.getKey();
			if (k != null) {
				out.put(k.toString(), e.getValue());
			}
		}
		return out;
	}

	private static long readCompanyIdFromOperatorJwtMap(Map<String, Object> ud) {
		Object v = ud.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			if (id <= 0L) {
				throw new UnauthorizedException("无权访问该API,非法访问！");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}
	}

	private static String pickString(@Nullable String requestParam, Map<String, Object> body, String snakeKey) {
		if (requestParam != null && StringUtils.hasText(requestParam.trim())) {
			return requestParam.trim();
		}
		if (body != null) {
			Object v = body.get(snakeKey);
			if (v != null) {
				String s = String.valueOf(v).trim();
				if (StringUtils.hasText(s)) {
					return s;
				}
			}
		}
		return null;
	}

	private static long parseSendTimeDefaultZero(@Nullable String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@Activated(routeAlias = "wechat.wxa.notice.templates")
	@GetMapping(value = "/templates", name = "小程序通知模版")
	public ResponseEntity<ApiResult<Map<String, Object>>> getWxaTemplateList(
			@RequestParam(value = "template_name", required = false) String templateNameParam,
			@RequestParam(value = "wxapp_appid", required = false) String wxappAppidParam,
			@FlexibleBody(required = false) Map<String, Object> body,
			HttpServletRequest request) {
		Map<String, Object> ud = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(ud);
		String templateName = pickString(templateNameParam, body, "template_name");
		String wxappAppid = pickString(wxappAppidParam, body, "wxapp_appid");
		LinkedHashMap<String, Map<String, Object>> list =
				wxaTemplateOpenService.getWxaTemplateList(companyId, templateName, wxappAppid);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("list", list);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.wxa.notice.templates.open")
	@PutMapping(value = "/templates", name = "开通小程序通知模版")
	public ResponseEntity<ApiResult<Map<String, Object>>> openWxaTemplate(
			HttpServletRequest request,
			@RequestParam(value = "template_name", required = false) String templateNameParam,
			@RequestParam(value = "wxapp_appid", required = false) String wxappAppidParam,
			@RequestParam(value = "scenes_name", required = false) String scenesNameParam,
			@RequestParam(value = "is_open", required = false) String isOpenParam,
			@RequestParam(value = "send_time", required = false, defaultValue = "0") String sendTimeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		String templateName = pickString(templateNameParam, body, "template_name");
		String wxappAppid = pickString(wxappAppidParam, body, "wxapp_appid");
		String scenesName = pickString(scenesNameParam, body, "scenes_name");
		String isOpenRaw = pickString(isOpenParam, body, "is_open");
		String sendTimeRaw = pickString(sendTimeParam, body, "send_time");
		if (sendTimeRaw == null) {
			sendTimeRaw = sendTimeParam != null ? sendTimeParam : "0";
		}

		if (!StringUtils.hasText(templateName)) {
			throw new BadRequestException("template_name 必填");
		}
		if (!StringUtils.hasText(wxappAppid)) {
			throw new BadRequestException("wxapp_appid 必填");
		}
		if (!StringUtils.hasText(scenesName)) {
			throw new BadRequestException("scenes_name 必填");
		}
		if (!StringUtils.hasText(isOpenRaw)) {
			throw new BadRequestException("is_open 必填");
		}

		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		boolean isOpen = "true".equals(isOpenRaw.trim());
		long sendTime = parseSendTimeDefaultZero(sendTimeRaw);

		wxaTemplateOpenService.openWxaTemplate(companyId, templateName, wxappAppid, scenesName, isOpen, sendTime);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
