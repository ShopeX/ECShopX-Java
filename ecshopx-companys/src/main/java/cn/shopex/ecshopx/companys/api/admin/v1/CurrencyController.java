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

package cn.shopex.ecshopx.companys.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.companys.service.currency.CurrencyExchangeRateCreateService;
import cn.shopex.ecshopx.companys.service.currency.CurrencyExchangeRateDeleteService;
import cn.shopex.ecshopx.companys.service.currency.CurrencyExchangeRateGetInfoService;
import cn.shopex.ecshopx.companys.service.currency.CurrencyExchangeRateListService;
import cn.shopex.ecshopx.companys.service.currency.CurrencyExchangeRateSetDefaultService;
import cn.shopex.ecshopx.companys.service.currency.CurrencyExchangeRateUpdateService;
import cn.shopex.ecshopx.companys.service.currency.CurrencyOptionsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
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
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("companysAdminV1Currency")
@RequestMapping("/api/v1")
public class CurrencyController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private static final String[] CURRENCY_WRITE_PARAM_KEYS =
			{"currency", "title", "symbol", "rate", "is_default", "use_platform"};

	private final CurrencyExchangeRateCreateService currencyExchangeRateCreateService;
	private final CurrencyExchangeRateUpdateService currencyExchangeRateUpdateService;
	private final CurrencyExchangeRateSetDefaultService currencyExchangeRateSetDefaultService;
	private final CurrencyExchangeRateListService currencyExchangeRateListService;
	private final CurrencyExchangeRateGetInfoService currencyExchangeRateGetInfoService;
	private final CurrencyExchangeRateDeleteService currencyExchangeRateDeleteService;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final CurrencyOptionsService currencyOptionsService;

	public CurrencyController(
			CurrencyExchangeRateCreateService currencyExchangeRateCreateService,
			CurrencyExchangeRateUpdateService currencyExchangeRateUpdateService,
			CurrencyExchangeRateSetDefaultService currencyExchangeRateSetDefaultService,
			CurrencyExchangeRateListService currencyExchangeRateListService,
			CurrencyExchangeRateGetInfoService currencyExchangeRateGetInfoService,
			CurrencyExchangeRateDeleteService currencyExchangeRateDeleteService,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			CurrencyOptionsService currencyOptionsService) {
		this.currencyExchangeRateCreateService = currencyExchangeRateCreateService;
		this.currencyExchangeRateUpdateService = currencyExchangeRateUpdateService;
		this.currencyExchangeRateSetDefaultService = currencyExchangeRateSetDefaultService;
		this.currencyExchangeRateListService = currencyExchangeRateListService;
		this.currencyExchangeRateGetInfoService = currencyExchangeRateGetInfoService;
		this.currencyExchangeRateDeleteService = currencyExchangeRateDeleteService;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.currencyOptionsService = currencyOptionsService;
	}

	@Activated(routeAlias = "currency.create")
	@PostMapping(value = "/currency", name = "货币信息新增")
	public ResponseEntity<ApiResult<Map<String, Object>>> createData(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> data = currencyExchangeRateCreateService.create(body == null ? Map.of() : body, companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long readCompanyIdFromOperatorJwt(HttpServletRequest request) {
		Object raw = request.getAttribute(OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud) || ud.isEmpty()) {
			throw new UnauthorizedException("未登录");
		}
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

	@Activated(routeAlias = "currency.delete")
	@DeleteMapping(value = "/currency/{id}", name = "删除货币")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteData(
			HttpServletRequest request, @PathVariable("id") String id) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		currencyExchangeRateDeleteService.deleteData(companyId, id);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "currency.update")
	@PutMapping(value = "/currency/{id}", name = "更新货币")
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> updateData(
			HttpServletRequest request,
			@PathVariable("id") String id,
			@FlexibleBody(required = false) Map<String, Object> body) {
		String t = id == null ? "" : id.trim();
		if (t.isEmpty()) {
			throw new BadRequestException("id不能为空");
		}
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> merged = mergeCurrencyWriteParams(request, body);
		List<Map<String, Object>> out = currencyExchangeRateUpdateService.updateData(companyId, t, merged);
		return ResponseEntity.ok(ApiResult.ok(out));
	}

	private static LinkedHashMap<String, Object> mergeCurrencyWriteParams(
			HttpServletRequest request, Map<String, Object> body) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		for (String k : CURRENCY_WRITE_PARAM_KEYS) {
			if (request.getParameter(k) != null) {
				merged.put(k, request.getParameter(k));
			} else if (body != null && body.containsKey(k)) {
				merged.put(k, body.get(k));
			}
		}
		return merged;
	}

	/**
	 * 列表同时注册 {@code /currency} 与 {@code /currency/}（含尾部斜杠）；详情为 {@code /currency/{id:.+}}（至少一段路径）。
	 * 空或非法 id 在详情 Service 中返回空数组，避免与列表路径误匹配。
	 */
	@Activated(routeAlias = "currency.list")
	@GetMapping(
			value = {"/currency", "/currency/"},
			produces = MediaType.APPLICATION_JSON_VALUE,
			name = "获取货币列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDataList(
			HttpServletRequest request,
			@RequestParam(value = "currency", required = false) String currency,
			@RequestParam(value = "title", required = false) String title,
			@RequestParam(value = "is_default", required = false) String is_default) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Map<String, Object> data =
				currencyExchangeRateListService.getDataList(companyId, currency, title, is_default);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "currency.info")
	@GetMapping(value = "/currency/{id:.+}", produces = MediaType.APPLICATION_JSON_VALUE, name = "获取货币详情")
	public ResponseEntity<ApiResult<Object>> getDataInfo(HttpServletRequest request, @PathVariable("id") String id) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		Object payload = currencyExchangeRateGetInfoService.getDataInfo(companyId, id);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "currency.set.default")
	@PutMapping(value = "/currencySetDefault/{id}", name = "设置默认货币")
	public ResponseEntity<ApiResult<Map<String, Object>>> setDefaultCurrency(
			HttpServletRequest request, @PathVariable("id") String id) {
		String t = id == null ? "" : id.trim();
		if (t.isEmpty()) {
			throw new BadRequestException("id不能为空");
		}
		long companyId = readCompanyIdFromOperatorJwt(request);
		currencyExchangeRateSetDefaultService.setDefaultCurrency(companyId, t);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", true)));
	}

	@Activated(routeAlias = "currency.get.default")
	@GetMapping(value = "/currencyGetDefault", name = "获取默认货币配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDefaultCurrency(HttpServletRequest request) {
		long companyId = readCompanyIdFromOperatorJwt(request);
		CurrencyExchangeRate row = companyDefaultCurrencyService.getCur(companyId);
		Map<String, Object> data = companyDefaultCurrencyService.toCurResponseMap(row);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "currency.list")
	@GetMapping(value = "/currencyOptions", produces = MediaType.APPLICATION_JSON_VALUE, name = "获取可选货币列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCurrencyOptions() {
		return ResponseEntity.ok(ApiResult.ok(currencyOptionsService.getOptions()));
	}
}
