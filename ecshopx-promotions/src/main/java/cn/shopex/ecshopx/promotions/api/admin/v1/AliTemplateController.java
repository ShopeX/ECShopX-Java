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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.promotions.service.AliTemplateListService;
import cn.shopex.ecshopx.promotions.service.AliTemplateOpenService;
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
@RestController("promotionsAdminV1AliTemplate")
@RequestMapping("/api/v1/ali/notice")
public class AliTemplateController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final AliTemplateOpenService aliTemplateOpenService;
	private final AliTemplateListService aliTemplateListService;

	public AliTemplateController(
			AliTemplateOpenService aliTemplateOpenService, AliTemplateListService aliTemplateListService) {
		this.aliTemplateOpenService = aliTemplateOpenService;
		this.aliTemplateListService = aliTemplateListService;
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

	private static boolean parseIsOpenQueryLoose(@Nullable String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return false;
		}
		return "true".equals(raw.trim());
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
	@GetMapping(value = "/templates", name = "阿里小程序通知模版")
	public ResponseEntity<ApiResult<Map<String, Object>>> getAliTemplateList(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		Map<String, Object> data = aliTemplateListService.getAliTemplateList(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "wechat.wxa.notice.templates.open")
	@PutMapping(value = "/templates", name = "开通阿里通知模版")
	public ResponseEntity<ApiResult<Map<String, Object>>> openAliTemplate(
			HttpServletRequest request,
			@RequestParam("template_id") String templateId,
			@RequestParam("scenes_name") String scenesName,
			@RequestParam("is_open") String isOpenRaw,
			@RequestParam(value = "send_time", required = false, defaultValue = "0") String sendTimeRaw) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		boolean isOpen = parseIsOpenQueryLoose(isOpenRaw);
		long sendTime = parseSendTimeDefaultZero(sendTimeRaw);
		aliTemplateOpenService.openAliTemplate(companyId, scenesName, isOpen, templateId, sendTime);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
