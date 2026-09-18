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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.espier.service.printer.PrinterCompanyConfigApplicationService;
import cn.shopex.ecshopx.espier.service.printer.PrinterShopApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
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
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("espierAdminV1Printer")
@RequestMapping("/api/v1/espier/printer")
public class PrinterController {

	private static final Pattern INTEGER_PREFIX_PATTERN = Pattern.compile("^([+-]?\\d+)");

	private final PrinterShopApplicationService printerShopApplicationService;
	private final PrinterCompanyConfigApplicationService printerCompanyConfigApplicationService;
	private final LangueProperties langueProperties;

	public PrinterController(
			PrinterShopApplicationService printerShopApplicationService,
			PrinterCompanyConfigApplicationService printerCompanyConfigApplicationService,
			LangueProperties langueProperties) {
		this.printerShopApplicationService = printerShopApplicationService;
		this.printerCompanyConfigApplicationService = printerCompanyConfigApplicationService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "espier.printer.info")
	@GetMapping(name = "获取易联云配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> info(
			HttpServletRequest request,
			@RequestParam(value = "type", required = false) String type) {
		long companyId = extractCompanyId(request);
		Map<String, Object> data = printerCompanyConfigApplicationService.info(companyId, type);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "espier.printer.save")
	@PostMapping(name = "保存易联云配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> update(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("is_open", merged.get("is_open"));
		payload.put("person_id", merged.get("person_id"));
		payload.put("app_id", merged.get("app_id"));
		payload.put("app_key", merged.get("app_key"));
		payload.put("is_hide", merged.get("is_hide"));
		payload.put("type", merged.get("type"));
		payload.put("is_open", looseTrueForOpenFlag(payload.get("is_open")) ? "true" : "false");
		payload.put("is_hide", looseTrueForOpenFlag(payload.get("is_hide")) ? "true" : "false");
		long companyId = extractCompanyId(request);
		Map<String, Object> data = printerCompanyConfigApplicationService.update(companyId, payload);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "espier.printer.shop.list")
	@GetMapping(value = "/shop", name = "获取商家易联云列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPrinterList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pagesize", required = false) String pagesizeRaw) {
		long companyId = extractCompanyId(request);
		String requestLocaleTag = RequestLangTag.current(langueProperties);
		int pageOneBasedIndex = parseIntQueryLoose(pageRaw, 1);
		int pageSize = parseIntQueryLoose(pagesizeRaw, 100);
		Map<String, Object> data =
				printerShopApplicationService.lists(companyId, pageOneBasedIndex, pageSize, requestLocaleTag);
		return ResponseEntity.ok(ApiResult.ok(data));
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

	@Activated(routeAlias = "espier.printer.shop.created")
	@PostMapping(value = "/shop", name = "添加商家易联云打印机")
	public ResponseEntity<ApiResult<Map<String, Object>>> createPrinter(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		long companyId = extractCompanyId(request);
		Map<String, Object> data = printerShopApplicationService.createPrinter(companyId, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "espier.printer.shop.updated")
	@PutMapping(value = "/shop/{id}", name = "更新商家易联云打印机")
	public ResponseEntity<ApiResult<Map<String, Object>>> updatePrinter(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> raw = new LinkedHashMap<>();
		raw.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			raw.putAll(body);
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		for (String key : Arrays.asList("name", "distributor_id", "app_terminal", "app_key", "type")) {
			if (raw.containsKey(key)) {
				merged.put(key, raw.get(key));
			}
		}
		long companyId = extractCompanyId(request);
		Map<String, Object> data = printerShopApplicationService.updatePrinter(companyId, id, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "espier.printer.shop.deleted")
	@DeleteMapping(value = "/shop/{id}", name = "删除商家易联云打印机")
	public ResponseEntity<Void> deletePrinter(HttpServletRequest request, @PathVariable("id") String id) {
		long companyId = extractCompanyId(request);
		printerShopApplicationService.deletePrinter(companyId, id);
		return ResponseEntity.ok().build();
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

	private static boolean looseTrueForOpenFlag(Object v) {
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		if (v != null && "true".equals(String.valueOf(v).trim())) {
			return true;
		}
		return false;
	}
}
