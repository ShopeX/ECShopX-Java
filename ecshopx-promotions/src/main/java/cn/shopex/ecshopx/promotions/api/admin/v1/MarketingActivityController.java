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
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestCountryCode;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.MarketingActivityCreateService;
import cn.shopex.ecshopx.promotions.service.MarketingActivityDeleteService;
import cn.shopex.ecshopx.promotions.service.MarketingActivityInfoService;
import cn.shopex.ecshopx.promotions.service.MarketingActivityItemListService;
import cn.shopex.ecshopx.promotions.service.MarketingActivityListService;
import cn.shopex.ecshopx.promotions.service.MarketingActivityUpdateService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = false)
@AdminAuth
@ShopLog
@RestController("promotionsAdminV1MarketingActivity")
@RequestMapping("/api/v1/marketing")
public class MarketingActivityController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final MarketingActivityCreateService marketingActivityCreateService;
	private final MarketingActivityUpdateService marketingActivityUpdateService;
	private final MarketingActivityItemListService marketingActivityItemListService;
	private final MarketingActivityInfoService marketingActivityInfoService;
	private final MarketingActivityListService marketingActivityListService;
	private final MarketingActivityDeleteService marketingActivityDeleteService;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public MarketingActivityController(
			MarketingActivityCreateService marketingActivityCreateService,
			MarketingActivityUpdateService marketingActivityUpdateService,
			MarketingActivityItemListService marketingActivityItemListService,
			MarketingActivityInfoService marketingActivityInfoService,
			MarketingActivityListService marketingActivityListService,
			MarketingActivityDeleteService marketingActivityDeleteService,
			ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.marketingActivityCreateService = marketingActivityCreateService;
		this.marketingActivityUpdateService = marketingActivityUpdateService;
		this.marketingActivityItemListService = marketingActivityItemListService;
		this.marketingActivityInfoService = marketingActivityInfoService;
		this.marketingActivityListService = marketingActivityListService;
		this.marketingActivityDeleteService = marketingActivityDeleteService;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "marketing.add")
	@PostMapping(value = "/create", name = "创建满减满折")
	public ResponseEntity<ApiResult<Map<String, Object>>> createMarketingActivity(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		String operatorType = stringify(jwt.get("operator_type"));
		merged.put("operator_type", operatorType);
		Object dist = jwt.get("distributor_id");
		merged.put("source_id", dist instanceof Number n ? n.longValue() : 0L);
		merged.put("source_type", operatorType);
		parseJsonStringFields(merged);
		if ("distributor".equals(operatorType)) {
			Object q = merged.get("distributor_id");
			if (q == null || !StringUtils.hasText(String.valueOf(q))) {
				q = dist;
			}
			long distributorId = readNonNegativeLong(q, "distributor_id");
			merged.put("shop_ids", List.of(distributorId));
		} else {
			if (!merged.containsKey("shop_ids") || merged.get("shop_ids") == null || isEmptyCollection(merged.get("shop_ids"))) {
				merged.put("shop_ids", List.of(0L));
			}
		}
		String requestLangTag = RequestCountryCode.resolve(langueProperties, merged);
		Map<String, Object> row = marketingActivityCreateService.createMarketingActivity(merged, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(row));
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

	private void parseJsonStringFields(Map<String, Object> m) {
		parseToStructure(m, "condition_value", "活动规则格式错误");
		parseToStructure(m, "shop_ids", "shop_ids 格式错误");
		parseToStructure(m, "item_ids", "item_ids 格式错误");
		parseToStructure(m, "item_category", "item_category 格式错误");
		parseToStructure(m, "tag_ids", "tag_ids 格式错误");
		parseToStructure(m, "brand_ids", "brand_ids 格式错误");
		parseToStructure(m, "gifts", "gifts 格式错误");
		parseToStructure(m, "valid_grade", "valid_grade 格式错误");
	}

	private void parseToStructure(Map<String, Object> m, String key, String err) {
		Object v = m.get(key);
		if (!(v instanceof String s) || !StringUtils.hasText(s)) {
			return;
		}
		try {
			Object parsed = objectMapper.readValue(s, new TypeReference<Object>() {});
			m.put(key, parsed);
		} catch (Exception e) {
			throw new BadRequestException(err);
		}
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

	private static long readNonNegativeLong(Object v, String fieldLabel) {
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v).trim());
			if (id < 0L) {
				throw new BadRequestException(fieldLabel + " 格式错误");
			}
			return id;
		} catch (NumberFormatException e) {
			throw new BadRequestException(fieldLabel + " 格式错误");
		}
	}

	private static String stringify(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private static boolean isEmptyCollection(Object v) {
		return v instanceof java.util.Collection<?> c && c.isEmpty();
	}

	private static void requirePositiveMarketingId(Map<String, Object> merged) {
		Object v = merged.get("marketing_id");
		if (v == null) {
			throw new BadRequestException("marketing_id 必填");
		}
		try {
			long id = v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v).trim());
			if (id <= 0L) {
				throw new BadRequestException("marketing_id 必填");
			}
			merged.put("marketing_id", id);
		} catch (NumberFormatException e) {
			throw new BadRequestException("marketing_id 必填");
		}
	}

	@Activated(routeAlias = "marketing.delete")
	@DeleteMapping(value = "/delete", name = "删除满减满折")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteMarketingActivity(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body == null ? Map.of() : body);
		long companyId = readCompanyIdFromOperatorJwtMap(readOperatorJwtMap(request));
		requirePositiveMarketingId(merged);
		long marketingId = ((Number) merged.get("marketing_id")).longValue();
		String queryPhaseIsEnd = readIsEndFromMergeInputQueryPhase(request);
		boolean physical = isPhysicalDeleteBranch(merged.get("isEnd"), queryPhaseIsEnd);
		String requestLangTag = RequestCountryCode.resolve(langueProperties, merged);
		Object statusPayload =
				marketingActivityDeleteService.deleteMarketingActivity(
						companyId, marketingId, physical, requestLangTag);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", statusPayload);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String readIsEndFromMergeInputQueryPhase(HttpServletRequest request) {
		String[] v = request.getParameterMap().get("isEnd");
		if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
			return v[0];
		}
		return null;
	}

	private static boolean isPhysicalDeleteBranch(Object mergedIsEnd, String queryPhaseIsEnd) {
		return mergedInputMeansIsEndUnsetOrFalsy(mergedIsEnd)
				|| (queryPhaseIsEnd != null && "false".equals(queryPhaseIsEnd));
	}

	private static boolean mergedInputMeansIsEndUnsetOrFalsy(Object in) {
		if (in == null) {
			return true;
		}
		if (in instanceof Boolean b) {
			return !b;
		}
		if (in instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (in instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return true;
			}
			if ("0".equals(t)) {
				return true;
			}
			if ("false".equals(t)) {
				return true;
			}
			return false;
		}
		return false;
	}

	@Activated(routeAlias = "marketing.update")
	@PutMapping(value = "/update", name = "修改满减满折")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateMarketingActivity(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		String operatorType = stringify(jwt.get("operator_type"));
		merged.put("operator_type", operatorType);
		Object dist = jwt.get("distributor_id");
		merged.put("source_id", dist instanceof Number n ? n.longValue() : 0L);
		merged.put("source_type", operatorType);
		parseJsonStringFields(merged);
		if ("distributor".equals(operatorType)) {
			Object q = merged.get("distributor_id");
			if (q == null || !StringUtils.hasText(String.valueOf(q))) {
				q = dist;
			}
			long distributorId = readNonNegativeLong(q, "distributor_id");
			merged.put("shop_ids", List.of(distributorId));
		} else {
			if (!merged.containsKey("shop_ids") || merged.get("shop_ids") == null || isEmptyCollection(merged.get("shop_ids"))) {
				merged.put("shop_ids", List.of(0L));
			}
		}
		requirePositiveMarketingId(merged);
		String requestLangTag = RequestCountryCode.resolve(langueProperties, merged);
		Map<String, Object> row = marketingActivityUpdateService.updateMarketingActivity(merged, requestLangTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "marketing.list")
	@GetMapping(value = "/getlist", name = "满减满折列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getMarketingActivityList(
			HttpServletRequest request,
			@RequestParam(name = "marketing_type", required = false) String marketingType,
			@RequestParam(name = "marketing_id", required = false) String marketingIdRaw,
			@RequestParam(name = "marketing_name", required = false) String marketingName,
			@RequestParam(name = "start_time", required = false) String startTime,
			@RequestParam(name = "end_time", required = false) String endTime,
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "item_type", required = false) String itemType,
			@RequestParam(name = "store_id", required = false) String storeIdRaw,
			@RequestParam(name = "distributor_id", required = false) String distributorIdRaw,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(name = "pageSize", required = false, defaultValue = "20") String pageSizeRaw) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		String operatorType = stringify(jwt.get("operator_type"));
		Long jwtDistributorId =
				jwt.get("distributor_id") instanceof Number n
						? n.longValue()
						: parseOptionalLong(stringify(jwt.get("distributor_id")));
		if (!StringUtils.hasText(marketingType)) {
			throw new BadRequestException("营销类型必填");
		}
		int page = parsePositivePagingInt(pageRaw, 1);
		int pageSize = parsePositivePagingInt(pageSizeRaw, 20);
		String acceptLang = RequestLangTag.current(langueProperties);
		Map<String, Object> data =
				marketingActivityListService.getMarketingActivityList(
						companyId,
						operatorType,
						jwtDistributorId,
						marketingType.trim(),
						marketingIdRaw,
						marketingName,
						startTime,
						endTime,
						status,
						itemType,
						storeIdRaw,
						distributorIdRaw,
						page,
						pageSize,
						acceptLang);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "marketing.info")
	@GetMapping(value = "/getinfo", name = "满减满折详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getMarketingActivityInfo(
			HttpServletRequest request,
			@RequestParam(name = "marketing_id", required = false) String marketingIdRaw) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Long marketingId = parseOptionalLong(marketingIdRaw);
		if (marketingId == null || marketingId <= 0L) {
			throw new BadRequestException("marketing_id 必填");
		}
		Map<String, Object> data = marketingActivityInfoService.getMarketingActivityInfo(companyId, marketingId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "marketing.item.list")
	@GetMapping(value = "/getItemList", name = "活动商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getActivityItemList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(name = "pageSize", required = false, defaultValue = "20") String pageSizeRaw,
			@RequestParam(name = "marketing_id", required = false) String marketingIdRaw) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		int page = parsePositivePagingInt(pageRaw, 1);
		int pageSize = parsePositivePagingInt(pageSizeRaw, 20);
		Long marketingId = parseOptionalLong(marketingIdRaw);
		Map<String, Object> data =
				marketingActivityItemListService.getActivityItemList(companyId, marketingId, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parsePositivePagingInt(String raw, int defaultVal) {
		try {
			String s = raw == null ? "" : raw.trim();
			String digits = LeadingNumberParser.parseAsString(s);
			int v = Integer.parseInt(digits);
			return Math.max(1, v);
		} catch (Exception e) {
			return defaultVal;
		}
	}

	private static Long parseOptionalLong(String raw) {
		if (raw == null) {
			return null;
		}
		String t = raw.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		char c0 = t.charAt(0);
		if (!Character.isDigit(c0) && c0 != '+' && c0 != '-') {
			return null;
		}
		try {
			return Long.parseLong(LeadingNumberParser.parseAsString(t));
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
