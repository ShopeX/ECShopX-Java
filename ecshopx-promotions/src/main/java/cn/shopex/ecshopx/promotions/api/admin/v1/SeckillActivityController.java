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
import cn.shopex.ecshopx.common.web.ActivatedRequestAttributes;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.service.SeckillActivityCreateService;
import cn.shopex.ecshopx.promotions.service.SeckillActivityInfoService;
import cn.shopex.ecshopx.promotions.service.SeckillActivityItemListService;
import cn.shopex.ecshopx.promotions.service.SeckillActivityListService;
import cn.shopex.ecshopx.promotions.service.SeckillActivitySearchItemsService;
import cn.shopex.ecshopx.promotions.service.SeckillActivityWxaCodeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
@RestController("promotionsAdminV1SeckillActivity")
@RequestMapping("/api/v1/promotions/seckillactivity")
public class SeckillActivityController {

	private static final String OPERATOR_JWT_USER_DATA =
			"cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA";

	private final SeckillActivityCreateService seckillActivityCreateService;
	private final SeckillActivityInfoService seckillActivityInfoService;
	private final SeckillActivityItemListService seckillActivityItemListService;
	private final SeckillActivityListService seckillActivityListService;
	private final SeckillActivitySearchItemsService seckillActivitySearchItemsService;
	private final SeckillActivityWxaCodeService seckillActivityWxaCodeService;
	private final MessageSource messageSource;
	private final LangueProperties langueProperties;

	public SeckillActivityController(
			SeckillActivityCreateService seckillActivityCreateService,
			SeckillActivityInfoService seckillActivityInfoService,
			SeckillActivityItemListService seckillActivityItemListService,
			SeckillActivityListService seckillActivityListService,
			SeckillActivitySearchItemsService seckillActivitySearchItemsService,
			SeckillActivityWxaCodeService seckillActivityWxaCodeService,
			MessageSource messageSource,
			LangueProperties langueProperties) {
		this.seckillActivityCreateService = seckillActivityCreateService;
		this.seckillActivityInfoService = seckillActivityInfoService;
		this.seckillActivityItemListService = seckillActivityItemListService;
		this.seckillActivityListService = seckillActivityListService;
		this.seckillActivitySearchItemsService = seckillActivitySearchItemsService;
		this.seckillActivityWxaCodeService = seckillActivityWxaCodeService;
		this.messageSource = messageSource;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "Promotions.seckill.add")
	@PostMapping(value = "/create", name = "创建秒杀")
	public ResponseEntity<ApiResult<Map<String, Object>>> createSeckillActivity(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		merged.put("company_id", companyId);
		normalizeDistributorId(merged, request, jwt);
		merged.put("source_id", readSourceIdFromOperatorJwt(jwt));
		merged.put("source_type", Objects.toString(jwt.get("operator_type"), ""));
		String langTag = resolveLangTag(request, merged);
		Map<String, Object> row = seckillActivityCreateService.createSeckillActivity(merged, langTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	private static void normalizeDistributorId(
			Map<String, Object> merged, HttpServletRequest request, Map<String, Object> jwt) {
		Object distObj = merged.get("distributor_id");
		if (distObj instanceof List<?> list) {
			String joined =
					list.stream()
							.filter(Objects::nonNull)
							.map(Object::toString)
							.map(String::trim)
							.filter(StringUtils::hasText)
							.collect(Collectors.joining(","));
			merged.put("distributor_id", StringUtils.hasText(joined) ? joined : null);
		}
		if (!StringUtils.hasText(Objects.toString(merged.get("distributor_id"), "").trim())) {
			String qp = request.getParameter("distributor_id");
			if (StringUtils.hasText(qp)) {
				merged.put("distributor_id", qp.trim());
			}
		}
		if (!StringUtils.hasText(Objects.toString(merged.get("distributor_id"), "").trim())) {
			Object activated = request.getAttribute(ActivatedRequestAttributes.DISTRIBUTOR_ID);
			if (activated != null) {
				String activatedText = activated.toString().trim();
				if (StringUtils.hasText(activatedText) && !"0".equals(activatedText)) {
					merged.put("distributor_id", activatedText);
				}
			}
		}
		if (!StringUtils.hasText(Objects.toString(merged.get("distributor_id"), "").trim())) {
			long fromJwt = readSourceIdFromOperatorJwt(jwt);
			if (fromJwt > 0L) {
				merged.put("distributor_id", String.valueOf(fromJwt));
			}
		}
	}

	private static long readSourceIdFromOperatorJwt(Map<String, Object> jwt) {
		Object v = jwt.get("distributor_id");
		if (v == null) {
			return 0L;
		}
		try {
			return v instanceof Number n ? n.longValue() : Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
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

	@Activated(routeAlias = "Promotions.seckill.update")
	@PutMapping(value = "/update", name = "修改秒杀")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateSeckillActivity(
			HttpServletRequest request, @FlexibleBody Map<String, Object> body) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		merged.put("company_id", readCompanyIdFromOperatorJwtMap(jwt));
		normalizeDistributorId(merged, request, jwt);
		merged.put("source_id", readSourceIdFromOperatorJwt(jwt));
		merged.put("source_type", Objects.toString(jwt.get("operator_type"), ""));
		String langTag = resolveLangTag(request, merged);
		long seckillId = 0L;
		try {
			Object sidObj = merged.get("seckill_id");
			if (sidObj instanceof Number n) {
				seckillId = n.longValue();
			} else if (sidObj != null && StringUtils.hasText(sidObj.toString().trim())) {
				seckillId = Long.parseLong(sidObj.toString().trim());
			}
		} catch (NumberFormatException ignored) {
			seckillId = 0L;
		}
		if (seckillId <= 0L) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.seckill_id_required", null, locale));
		}
		Map<String, Object> row = seckillActivityCreateService.updateSeckillActivity(merged, langTag);
		return ResponseEntity.ok(ApiResult.ok(row));
	}

	@Activated(routeAlias = "Promotions.seckill.list")
	@GetMapping(value = "/getlist", name = "秒杀列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSeckillActivityList(HttpServletRequest request) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		String rawDistributorId = request.getParameter("distributor_id");
		long sourceIdFromQuery = parseSourceIdAsTruncatedLong(rawDistributorId);
		if (sourceIdFromQuery == 0L) {
			sourceIdFromQuery = readSourceIdFromOperatorJwt(jwt);
			if (sourceIdFromQuery > 0L) {
				rawDistributorId = String.valueOf(sourceIdFromQuery);
			}
		}
		Map<String, String> whitelistInput = buildSeckillListWhitelistInput(request);
		String langTag = resolveLangTag(request, Map.of());
		Map<String, Object> result =
				seckillActivityListService.getSeckillActivityList(
						companyId, sourceIdFromQuery, rawDistributorId, whitelistInput, langTag);
		return ResponseEntity.ok(ApiResult.ok(result));
	}

	private static long parseSourceIdAsTruncatedLong(String raw) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return 0L;
		}
		try {
			return (long) Double.parseDouble(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static Map<String, String> buildSeckillListWhitelistInput(HttpServletRequest request) {
		String[] keys = {
			"seckill_id",
			"keywords",
			"name",
			"item_title",
			"start_time",
			"end_time",
			"is_free_shipping",
			"status",
			"seckill_type",
			"page",
			"pageSize"
		};
		LinkedHashMap<String, String> out = new LinkedHashMap<>();
		for (String key : keys) {
			String[] vals = request.getParameterMap().get(key);
			if (vals != null && vals.length > 0 && StringUtils.hasText(vals[0].trim())) {
				out.put(key, vals[0].trim());
			}
		}
		return out;
	}

	@Activated(routeAlias = "Promotions.seckill.info")
	@GetMapping(value = "/getinfo", name = "秒杀详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSeckillActivityInfo(
			HttpServletRequest request,
			@RequestParam(value = "seckill_id", required = false) String seckillIdRaw) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		long seckillId = 0L;
		try {
			if (seckillIdRaw != null && StringUtils.hasText(seckillIdRaw.trim())) {
				seckillId = Long.parseLong(seckillIdRaw.trim());
			}
		} catch (NumberFormatException ignored) {
			seckillId = 0L;
		}
		if (seckillId <= 0L) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.seckill_id_required", null, locale));
		}
		String langTag = resolveLangTag(request, Map.of());
		Map<String, Object> data =
				seckillActivityInfoService.getSeckillActivityInfo(companyId, seckillId, langTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "Promotions.seckill.statusupdate")
	@PutMapping(value = "/updatestatus", name = "秒杀状态")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateStatus(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		Map<String, Object> merged = mergeInput(request, body);
		Map<String, Object> jwt = readOperatorJwtMap(request);
		merged.put("company_id", readCompanyIdFromOperatorJwtMap(jwt));
		long seckillId = 0L;
		try {
			Object sidObj = merged.get("seckill_id");
			if (sidObj instanceof Number n) {
				seckillId = n.longValue();
			} else if (sidObj != null && StringUtils.hasText(sidObj.toString().trim())) {
				seckillId = Long.parseLong(sidObj.toString().trim());
			}
		} catch (NumberFormatException ignored) {
			seckillId = 0L;
		}
		if (seckillId <= 0L) {
			throw new BadRequestException(
					messageSource.getMessage("promotions.seckill.seckill_id_required", null, locale));
		}
		String langTag = resolveLangTag(request, merged);
		seckillActivityCreateService.updateStatus(merged, langTag);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "Promotions.seckill.item.list")
	@GetMapping(value = "/getIteminfo", name = "秒杀商品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSeckillItemList(
			HttpServletRequest request,
			@RequestParam(value = "seckill_id", required = false) Long seckillId,
			@RequestParam(value = "page", required = false, defaultValue = "1") String pageRaw,
			@RequestParam(value = "pageSize", required = false, defaultValue = "20") String pageSizeRaw,
			@RequestParam(value = "is_sku", required = false) String isSkuRaw) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		int page = parsePositivePageParam(pageRaw);
		int pageSize = parsePositivePageParam(pageSizeRaw);
		boolean isSku = parseIsSkuFromLooseString(isSkuRaw);
		Map<String, Object> data =
				seckillActivityItemListService.getSeckillItemList(companyId, seckillId, page, pageSize, isSku);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static int parsePositivePageParam(String raw) {
		if (raw == null) {
			throw new BadRequestException("参数非法");
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			throw new BadRequestException("参数非法");
		}
		try {
			int v = Integer.parseInt(t);
			if (v < 1) {
				throw new BadRequestException("参数非法");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数非法");
		}
	}

	private static boolean parseIsSkuFromLooseString(String raw) {
		if (raw == null) {
			return true;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("0".equalsIgnoreCase(t)) {
			return false;
		}
		String lower = t.toLowerCase();
		if ("false".equals(lower) || "off".equals(lower) || "no".equals(lower)) {
			return false;
		}
		return true;
	}

	@Activated(routeAlias = "Promotions.seckill.wxcode")
	@GetMapping(value = "/wxcode", name = "秒杀小程序码")
	public ResponseEntity<ApiResult<Map<String, Object>>> getSeckillWxaCode(
			HttpServletRequest request,
			@RequestParam(value = "seckill_id", required = false) String seckillIdRaw,
			@RequestParam(value = "seckill_type", required = false, defaultValue = "normal")
					String seckillTypeRaw,
			@RequestParam(value = "distributor_id", required = false, defaultValue = "0")
					String distributorIdRaw) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Map<String, Object> data =
				seckillActivityWxaCodeService.getSeckillWxaCode(
						companyId, seckillIdRaw, seckillTypeRaw, distributorIdRaw);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "Promotions.seckill.search.item.list")
	@GetMapping(value = "/search/items", name = "搜索商品")
	public ResponseEntity<ApiResult<Map<String, Object>>> searchItems(HttpServletRequest request) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = readCompanyIdFromOperatorJwtMap(jwt);
		Locale locale = Optional.ofNullable(LocaleContextHolder.getLocale()).orElse(Locale.SIMPLIFIED_CHINESE);
		Map<String, Object> data =
				seckillActivitySearchItemsService.searchItems(request, companyId, jwt, locale);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
