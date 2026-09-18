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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.promotions.service.SmsBasicService;
import cn.shopex.ecshopx.promotions.service.SmsSendTestService;
import cn.shopex.ecshopx.promotions.service.SmsSignQueryService;
import cn.shopex.ecshopx.promotions.service.SmsSignSaveService;
import cn.shopex.ecshopx.promotions.service.SmsTemplateDetailService;
import cn.shopex.ecshopx.promotions.service.SmsTemplateListService;
import cn.shopex.ecshopx.promotions.service.SmsTemplateUpdateService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RestController("promotionsAdminV1Sms")
@RequestMapping("/api/v1/sms")
public class SmsController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final SmsTemplateUpdateService smsTemplateUpdateService;
	private final SmsSendTestService smsSendTestService;
	private final SmsSignSaveService smsSignSaveService;
	private final SmsSignQueryService smsSignQueryService;
	private final SmsBasicService smsBasicService;
	private final SmsTemplateDetailService smsTemplateDetailService;
	private final SmsTemplateListService smsTemplateListService;

	public SmsController(
			SmsTemplateUpdateService smsTemplateUpdateService,
			SmsSendTestService smsSendTestService,
			SmsSignSaveService smsSignSaveService,
			SmsSignQueryService smsSignQueryService,
			SmsBasicService smsBasicService,
			SmsTemplateDetailService smsTemplateDetailService,
			SmsTemplateListService smsTemplateListService) {
		this.smsTemplateUpdateService = smsTemplateUpdateService;
		this.smsSendTestService = smsSendTestService;
		this.smsSignSaveService = smsSignSaveService;
		this.smsSignQueryService = smsSignQueryService;
		this.smsBasicService = smsBasicService;
		this.smsTemplateDetailService = smsTemplateDetailService;
		this.smsTemplateListService = smsTemplateListService;
	}

	private static boolean effectiveTruthy(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b.booleanValue();
		}
		if (v instanceof Number n) {
			return n.doubleValue() != 0.0d;
		}
		if (v instanceof CharSequence s) {
			String t = s.toString().trim();
			if (t.isEmpty() || "0".equals(t)) {
				return false;
			}
			return true;
		}
		String t = String.valueOf(v).trim();
		if (t.isEmpty() || "0".equals(t)) {
			return false;
		}
		return true;
	}

	private static Map<String, Object> mergeInput(HttpServletRequest request, Map<String, Object> body) {
		LinkedHashMap<String, Object> input = new LinkedHashMap<>();
		request.getParameterMap()
				.forEach(
						(k, v) -> {
							if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
								input.put(k, v[0]);
							}
						});
		if (body != null) {
			input.putAll(body);
		}
		return input;
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

	private static String normalizeIsOpen(Object raw, boolean keyPresent) {
		if (!keyPresent) {
			return "false";
		}
		if (raw == null) {
			return "false";
		}
		if (raw instanceof Boolean b) {
			return b.booleanValue() ? "true" : "false";
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0 ? "true" : "false";
		}
		if (raw instanceof CharSequence s) {
			String t = s.toString().trim();
			if (t.isEmpty()) {
				return "false";
			}
			if (t.equalsIgnoreCase("true") || "1".equals(t)) {
				return "true";
			}
			if (t.equalsIgnoreCase("false") || "0".equals(t)) {
				return "false";
			}
			return t;
		}
		return raw.toString();
	}

	@Activated(routeAlias = "Promotions.sms.basic")
	@GetMapping(value = "/basic", name = "短信账户信息")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSmsBasic(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		return ResponseEntity.ok(ApiResult.ok(smsBasicService.getSmsBasic(companyId)));
	}

	@Activated(routeAlias = "Promotions.sms.templates.list")
	@GetMapping(value = "/templates", name = "短信模版列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSmsTemplateList(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		return ResponseEntity.ok(ApiResult.ok(smsTemplateListService.getSmsTemplateList(companyId)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "Promotions.sms.template.detail")
	@GetMapping(value = "/template/detail", name = "短信模版详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSmsTemplateDetail(
			HttpServletRequest request,
			@RequestParam(value = "tmpl_name", required = false) String tmplName) {
		if (tmplName == null || !StringUtils.hasText(tmplName.trim())) {
			throw new BadRequestException("短信模板名称必填");
		}
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		return ResponseEntity.ok(
				ApiResult.ok(smsTemplateDetailService.getSmsTemplateDetail(companyId, tmplName.trim())));
	}

	@Activated(routeAlias = "Promotions.sms.template.up")
	@PatchMapping(value = "/template", name = "更新短信模版")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateSmsTemplate(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);

		Object tn = merged.get("template_name");
		if (tn == null || !StringUtils.hasText(tn.toString().trim())) {
			throw new BadRequestException("短信模板名称必填");
		}
		String templateName = tn.toString().trim();

		Optional<String> contentOpt = Optional.empty();
		if (effectiveTruthy(merged.get("content"))) {
			Object co = merged.get("content");
			String trimmed =
					co instanceof CharSequence cs ? cs.toString().trim() : co.toString().trim();
			if (StringUtils.hasText(trimmed)) {
				contentOpt = Optional.of(trimmed);
			}
		}

		String isOpenStr = normalizeIsOpen(merged.get("is_open"), merged.containsKey("is_open"));

		smsTemplateUpdateService.updateSmsTemplate(companyId, templateName, isOpenStr, contentOpt);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "Promotions.sms.sign.get")
	@GetMapping(value = "/sign", name = "短信签名")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSmsSign(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		return ResponseEntity.ok(ApiResult.ok(smsSignQueryService.getSmsSign(companyId)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "Promotions.sms.sign.save")
	@PostMapping(value = "/sign", name = "设置短信签名")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveSmsSign(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Object signRaw = merged.get("sign");
		String sign = signRaw == null ? null : signRaw.toString();
		smsSignSaveService.saveSmsSign(companyId, sign);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_400,
			unauthorized = true,
			notFound = true)
	@Activated(routeAlias = "Promotions.sms.send.test")
	@PostMapping(value = "/send/test", name = "测试短信")
	public ResponseEntity<ApiResult<Map<String, Object>>> sendSmsTest(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);

		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);

		Object tmplRaw = merged.get("tmpl_name");
		if (tmplRaw == null || !StringUtils.hasText(tmplRaw.toString().trim())) {
			throw new ResourceException("短信模板名称必填");
		}
		String tmplTrimmed = tmplRaw.toString().trim();
		Object altRaw = merged.get("template_name");
		String altTrimmed =
				(altRaw != null && StringUtils.hasText(altRaw.toString().trim()))
						? altRaw.toString().trim()
						: "";
		String tmplName = StringUtils.hasText(tmplTrimmed) ? tmplTrimmed : altTrimmed;

		Object mo = merged.get("mobile");
		if (mo == null || !StringUtils.hasText(mo.toString().trim())) {
			throw new ResourceException("测试手机必填");
		}
		String mobile = mo.toString().trim();

		Object co = merged.get("content");
		if (co == null || !StringUtils.hasText(co.toString().trim())) {
			throw new ResourceException("模板内容必填");
		}
		String content = co.toString().trim();

		smsSendTestService.sendSmsTest(companyId, tmplName, mobile, content);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}
}
