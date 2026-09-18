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
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.wechat.api.admin.v1.dto.WxappTemplateSetDomainRequest;
import cn.shopex.ecshopx.wechat.service.wxa.WxaTemplateDomainInfoService;
import cn.shopex.ecshopx.wechat.service.wxa.WxappTemplateDomainRedisPayload;
import cn.shopex.ecshopx.wechat.service.wxa.WxappTemplateUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("wechatAdminV1WxappTemplate")
@RequestMapping("/api/v1/wxappTemplate")
public class WxappTemplateController {

	private static final List<String> UPDATE_WXAPP_MERGE_KEYS = List.of(
			"id",
			"key_name",
			"name",
			"tag",
			"template_id",
			"template_id_2",
			"version",
			"description",
			"is_only",
			"domain",
			"is_disabled",
			"created",
			"updated");

	private final WxaTemplateDomainInfoService wxaTemplateDomainInfoService;
	private final WxappTemplateUpdateService wxappTemplateUpdateService;

	public WxappTemplateController(
			WxaTemplateDomainInfoService wxaTemplateDomainInfoService,
			WxappTemplateUpdateService wxappTemplateUpdateService) {
		this.wxaTemplateDomainInfoService = wxaTemplateDomainInfoService;
		this.wxappTemplateUpdateService = wxappTemplateUpdateService;
	}

	@Activated(routeAlias = "wechat.wxappTemplate.wxapp")
	@PutMapping(value = "/wxapp", name = "微信模板编辑", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> updateWxappTemplate(
			@FlexibleBody(required = false) Map<String, Object> body) {
		// Avoid HttpServletRequest as a method parameter with @FlexibleBody here: Undertow can return 200 with an empty body.
		HttpServletRequest request =
				((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		Map<String, Object> bodyMap = body;
		LinkedHashMap<String, Object> effectiveBody = new LinkedHashMap<>();
		for (String k : UPDATE_WXAPP_MERGE_KEYS) {
			if (bodyMap != null && bodyMap.containsKey(k)) {
				effectiveBody.put(k, bodyMap.get(k));
			} else if (request.getParameterMap().containsKey(k)) {
				effectiveBody.put(k, request.getParameter(k));
			}
		}
		return ApiResult.ok(wxappTemplateUpdateService.updateWxappTemplate(effectiveBody));
	}

	@Activated(routeAlias = "wechat.wxappTemplate.domain")
	@PutMapping(value = "/domain", name = "设置小程序域名", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> setDomain(
			@FlexibleBody(required = false) WxappTemplateSetDomainRequest body) {
		WxappTemplateSetDomainRequest.Domain domain = body == null ? null : body.getDomain();
		wxaTemplateDomainInfoService.setDomain(new WxappTemplateDomainRedisPayload(
				domain == null ? null : domain.getRequestdomain(),
				domain == null ? null : domain.getWsrequestdomain(),
				domain == null ? null : domain.getUploaddomain(),
				domain == null ? null : domain.getDownloaddomain(),
				domain == null ? null : domain.getWebviewdomain()));
		return ApiResult.ok(Map.of("status", Boolean.TRUE));
	}

	@Activated(routeAlias = "wechat.wxappTemplate.domain")
	@GetMapping(value = "/domain", name = "获取小程序域名", produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, String>> getDomain() {
		return ApiResult.ok(wxaTemplateDomainInfoService.getDomain());
	}
}
