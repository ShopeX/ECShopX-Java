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

package cn.shopex.ecshopx.workwechat.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.workwechat.service.WorkWechatMessageTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("workWechatAdminV1MessageTemplate")
@RequestMapping("/api/v1/workwechat/messagetemplate")
public class WorkWechatMessageTemplateController {

	private final WorkWechatMessageTemplateService workWechatMessageTemplateService;

	public WorkWechatMessageTemplateController(WorkWechatMessageTemplateService workWechatMessageTemplateService) {
		this.workWechatMessageTemplateService = workWechatMessageTemplateService;
	}

	@Activated(routeAlias = "workwechat.message.template.list")
	@GetMapping(name = "通知模板列表")
	public ApiResult<Map<String, Object>> getTemplateList(HttpServletRequest request) {
		long companyId = readCompanyId(request);
		Map<String, Object> data = workWechatMessageTemplateService.listTemplatesGroupedByTemplateId(companyId);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "workwechat.message.template.get")
	@GetMapping(value = "/{templateId}", name = "通知模板获取")
	public ApiResult<Object> getTemplate(HttpServletRequest request,
			@PathVariable("templateId") String templateId) {
		long companyId = readCompanyId(request);
		Object data = workWechatMessageTemplateService.getTemplateForApi(companyId, templateId);
		return ApiResult.ok(data);
	}

	@Activated(routeAlias = "workwechat.message.template.save")
	@PutMapping(value = "/{templateId}", name = "通知模板保存")
	public ApiResult<Map<String, Object>> saveTemplate(
			HttpServletRequest request,
			@PathVariable("templateId") String templateId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = readCompanyId(request);
		Map<String, Object> source = body == null ? Collections.emptyMap() : body;
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();
		params.put("title", source.containsKey("title") ? source.get("title") : null);
		params.put("description", source.containsKey("description") ? source.get("description") : null);
		params.put("content", source.containsKey("content") ? source.get("content") : null);
		params.put("emphasis_first_item", source.containsKey("emphasis_first_item") ? source.get("emphasis_first_item") : null);
		params.put("disabled", source.containsKey("disabled") ? source.get("disabled") : null);
		Map<String, Object> result = workWechatMessageTemplateService.saveTemplate(companyId, templateId, params);
		return ApiResult.ok(result);
	}

	@Activated(routeAlias = "workwechat.message.template.open")
	@PutMapping(value = "/open/{templateId}", name = "通知模板开启")
	public ApiResult<Map<String, Object>> openTemplate(HttpServletRequest request,
			@PathVariable("templateId") String templateId) {
		long companyId = readCompanyId(request);
		Map<String, Object> params = new HashMap<>();
		params.put("disabled", "false");
		Map<String, Object> result = workWechatMessageTemplateService.saveTemplate(companyId, templateId, params);
		return ApiResult.ok(result);
	}

	@Activated(routeAlias = "workwechat.message.template.close")
	@PutMapping(value = "/close/{templateId}", name = "通知模板关闭")
	public ApiResult<Map<String, Object>> closeTemplate(HttpServletRequest request,
			@PathVariable("templateId") String templateId) {
		long companyId = readCompanyId(request);
		Map<String, Object> params = new HashMap<>();
		params.put("disabled", "true");
		Map<String, Object> result = workWechatMessageTemplateService.saveTemplate(companyId, templateId, params);
		return ApiResult.ok(result);
	}

	private static long readCompanyId(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Object v = ud.get("company_id");
		if (v == null) {
			throw new UnauthorizedException("未登录");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}
	}
}
