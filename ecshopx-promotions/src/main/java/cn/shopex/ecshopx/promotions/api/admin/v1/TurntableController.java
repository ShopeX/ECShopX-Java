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
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.TurntableActivityListService;
import cn.shopex.ecshopx.promotions.service.TurntableConfigService;
import cn.shopex.ecshopx.promotions.service.TurntableLuckyDrawLogListService;
import cn.shopex.ecshopx.promotions.service.TurntableLogCountService;
import cn.shopex.ecshopx.promotions.service.export.TurntableLuckyDrawLogExportService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
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
@RestController("promotionsAdminV1Turntable")
@RequestMapping("/api/v1/promotions")
public class TurntableController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final TurntableConfigService turntableConfigService;
	private final TurntableLuckyDrawLogExportService turntableLuckyDrawLogExportService;
	private final TurntableActivityListService turntableActivityListService;
	private final TurntableLogCountService turntableLogCountService;
	private final TurntableLuckyDrawLogListService turntableLuckyDrawLogListService;
	private final LangueProperties langueProperties;

	public TurntableController(
			TurntableConfigService turntableConfigService,
			TurntableLuckyDrawLogExportService turntableLuckyDrawLogExportService,
			TurntableActivityListService turntableActivityListService,
			TurntableLogCountService turntableLogCountService,
			TurntableLuckyDrawLogListService turntableLuckyDrawLogListService,
			LangueProperties langueProperties) {
		this.turntableConfigService = turntableConfigService;
		this.turntableLuckyDrawLogExportService = turntableLuckyDrawLogExportService;
		this.turntableActivityListService = turntableActivityListService;
		this.turntableLogCountService = turntableLogCountService;
		this.turntableLuckyDrawLogListService = turntableLuckyDrawLogListService;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "promotions.turntable.config.set")
	@PostMapping(value = "/turntableconfig", name = "大转盘配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> setTurntableConfig(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		merged.put("company_id", companyId);
		String langTag = resolveLangTag(request, merged);
		Map<String, Object> payload = turntableConfigService.setTurntableConfig(companyId, merged, langTag);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	private String resolveLangTag(HttpServletRequest request, Map<String, Object> merged) {
		String countryCodeTrimmed = Objects.toString(merged.get("country_code"), "").trim();
		String resolved = RequestLangTag.current(langueProperties);
		return StringUtils.hasText(countryCodeTrimmed) ? countryCodeTrimmed : resolved;
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

	private static long readOperatorId(Map<String, Object> ud) {
		Object v = ud.get("operator_id");
		if (v == null) {
			return 0L;
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			return id < 0L ? 0L : id;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long readMerchantId(Map<String, Object> ud) {
		Object v = ud.get("merchant_id");
		if (v == null) {
			return 0L;
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
			return id < 0L ? 0L : id;
		} catch (NumberFormatException e) {
			return 0L;
		}
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

	@Activated(routeAlias = "promotions.turntable.config.get")
	@GetMapping(value = "/turntableconfig", name = "大转盘配置")
	public ResponseEntity<ApiResult<Object>> getTurntableConfig(
			HttpServletRequest request, @RequestParam(value = "id", required = false) String id) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		String langTag = resolveLangTag(request, mergeInput(request, Map.of()));
		Optional<Map<String, Object>> row = turntableConfigService.getTurntableConfig(companyId, id, langTag);
		Object payload = row.map(m -> (Object) m).orElseGet(Collections::emptyList);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "promotions.turntable.list.get")
	@GetMapping(value = "/getturntableList", name = "大转盘列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getDrawActivityList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "pageSize", required = false) String pageSizeRaw,
			@RequestParam(value = "activity_id", required = false) String activityId,
			@RequestParam(value = "activity_name", required = false) String activityName,
			@RequestParam(value = "status", required = false) String status) {
		Map<String, Object> ud = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(ud);
		String langTag = resolveLangTag(request, mergeInput(request, Map.of()));
		Map<String, Object> payload =
				turntableActivityListService.getDrawActivityList(
						companyId, pageRaw, pageSizeRaw, activityId, activityName, status, langTag);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "promotions.turntable.detail.get")
	@GetMapping(value = "/getturntable", name = "大转盘详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getLuckyDrawDetail(
			HttpServletRequest request, @RequestParam(value = "id", required = false) String id) {
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		String langTag = resolveLangTag(request, mergeInput(request, Map.of()));
		Map<String, Object> data = turntableConfigService.getLuckyDrawDetail(companyId, id, langTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "promotions.turntable.log.list.get")
	@GetMapping(value = "/getturntable_log/byid", name = "大转盘日志")
	public ResponseEntity<ApiResult<Map<String, Object>>> getLogStatistics(
			HttpServletRequest request,
			@RequestParam(value = "activity_id", required = false) String activityId,
			@RequestParam(value = "page", required = false) String pageRaw,
			@RequestParam(value = "page_size", required = false) String pageSizeRaw) {
		Map<String, Object> ud = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(ud);
		long actId = turntableConfigService.parseRequiredActivityId(activityId);
		Map<String, Object> data =
				turntableLuckyDrawLogListService.getLogStatistics(companyId, actId, pageRaw, pageSizeRaw);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "promotions.turntable.log.count.get")
	@GetMapping(value = "/getturntable_count/byid", name = "大转盘统计")
	public ResponseEntity<ApiResult<Map<String, Object>>> getLogCount(
			@RequestParam(value = "activity_id", required = false) String activityId) {
		long actId = turntableConfigService.parseRequiredActivityId(activityId);
		Map<String, Object> data = turntableLogCountService.getLogCount(actId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "promotions.turntable.down")
	@GetMapping(value = "/down_lucky_draw/byid", name = "活动下架")
	public ResponseEntity<ApiResult<Map<String, Object>>> downLuckyDrawActivity(
			HttpServletRequest request, @RequestParam(value = "activity_id", required = false) String activityId) {
		Map<String, Object> ud = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(ud);
		turntableConfigService.downLuckyDrawActivity(companyId, activityId);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	@Activated(routeAlias = "promotions.turntable.copy")
	@PostMapping(value = "/turntable/copy", name = "复制大转盘活动")
	public ResponseEntity<ApiResult<Map<String, Object>>> copyTurntable(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		String langTag = resolveLangTag(request, merged);
		Object rawId = merged.get("activity_id");
		if (rawId == null) {
			rawId = merged.get("id");
		}
		Map<String, Object> payload =
				turntableConfigService.copyTurntableActivity(
						companyId, rawId == null ? null : String.valueOf(rawId), langTag);
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "promotions.turntable.log.export")
	@GetMapping(value = "/down_lucky_draw/export", name = "活动日志导出")
	public ResponseEntity<ApiResult<Map<String, Object>>> exportLog(
			HttpServletRequest request,
			@RequestParam(value = "activity_id", required = false) String activityId,
			@SuppressWarnings("unused") @RequestParam Map<String, String> allQueryParams) {
		Map<String, Object> ud = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(ud);
		long operatorId = readOperatorId(ud);
		long merchantId = readMerchantId(ud);
		String datapass = request.getHeader("x-datapass-block");
		turntableLuckyDrawLogExportService.exportLog(companyId, operatorId, merchantId, activityId, datapass);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}
}
