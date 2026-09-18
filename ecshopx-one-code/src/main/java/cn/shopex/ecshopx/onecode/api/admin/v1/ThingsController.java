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

package cn.shopex.ecshopx.onecode.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.onecode.service.OneCodeShopOperatorContextGate;
import cn.shopex.ecshopx.onecode.service.ThingsAddService;
import cn.shopex.ecshopx.onecode.service.ThingsDeleteService;
import cn.shopex.ecshopx.onecode.service.ThingsDetailService;
import cn.shopex.ecshopx.onecode.service.ThingsListService;
import cn.shopex.ecshopx.onecode.service.ThingsUpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422
)
@RestController("onecodeThingsAdminV1")
@RequestMapping("/api/v1")
public class ThingsController {

	private static final String THINGS_LIST_VALIDATION_MESSAGE = "获取商品列表出错.";

	private static final String THINGS_DETAIL_VALIDATION_MESSAGE = "获取物品详情出错.";

	private static final String THINGS_DELETE_VALIDATION_MESSAGE = "删除物品出错.";

	private static final Pattern SIGNED_INTEGER_STRING = Pattern.compile("^-?\\d+$");

	private final ThingsAddService thingsAddService;
	private final ThingsDeleteService thingsDeleteService;
	private final ThingsUpdateService thingsUpdateService;
	private final ThingsListService thingsListService;
	private final ThingsDetailService thingsDetailService;
	private final OneCodeShopOperatorContextGate oneCodeShopOperatorContextGate;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public ThingsController(
			ThingsAddService thingsAddService,
			ThingsDeleteService thingsDeleteService,
			ThingsUpdateService thingsUpdateService,
			ThingsListService thingsListService,
			ThingsDetailService thingsDetailService,
			OneCodeShopOperatorContextGate oneCodeShopOperatorContextGate,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.thingsAddService = thingsAddService;
		this.thingsDeleteService = thingsDeleteService;
		this.thingsUpdateService = thingsUpdateService;
		this.thingsListService = thingsListService;
		this.thingsDetailService = thingsDetailService;
		this.oneCodeShopOperatorContextGate = oneCodeShopOperatorContextGate;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "onecode.things.create")
	@PostMapping(value = "/onecode/things", name = "添加物品")
	public ResponseEntity<ApiResult<Map<String, Object>>> createThings(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		oneCodeShopOperatorContextGate.assertCreateThings(request, merged);

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}

		long companyId = toLong(ud.get("company_id"));

		String thingName = requiredNonEmptyString(merged.get("thing_name"), "请填写物品名称");
		String pic = requiredNonEmptyString(merged.get("pic"), "请上传物品图片");
		BigDecimal priceYuan = parseRequiredPriceYuan(merged.get("price"));
		int priceInCents;
		try {
			priceInCents =
					priceYuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValueExact();
		} catch (ArithmeticException e) {
			throw new BadRequestException("价格必填,且要大于0");
		}
		String intro = requiredNonEmptyString(merged.get("intro"), "请填写图文详情");

		Map<String, Object> data = thingsAddService.addThings(companyId, thingName, pic, priceInCents, intro);

		Map<String, Object> logParams = new LinkedHashMap<>(merged);
		logParams.put("company_id", companyId);
		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/onecode/things");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "添加物品");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static BigDecimal parseRequiredPriceYuan(Object raw) {
		if (raw == null) {
			throw new BadRequestException("价格必填,且要大于0");
		}
		BigDecimal bd;
		try {
			if (raw instanceof Number n) {
				bd = new BigDecimal(n.toString());
			} else {
				String t = raw.toString().trim();
				if (!StringUtils.hasText(t)) {
					throw new BadRequestException("价格必填,且要大于0");
				}
				bd = new BigDecimal(t);
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException("价格必填,且要大于0");
		}
		if (bd.compareTo(new BigDecimal("0.01")) < 0) {
			throw new BadRequestException("价格必填,且要大于0");
		}
		return bd;
	}

	private static String requiredNonEmptyString(Object raw, String message) {
		if (raw == null) {
			throw new BadRequestException(message);
		}
		String s = raw instanceof String str ? str : raw.toString();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(message);
		}
		return s;
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString());
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
	}

	private Map<String, Object> mergeInputLikeFlexibleResolver(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		return body != null ? body : new LinkedHashMap<>();
	}

	private static Map<String, Object> parameterMapToMap(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.PLAIN,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422
	)
	@Activated(routeAlias = "onecode.things.lists")
	@GetMapping(value = "/onecode/things", name = "获取物品列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getThingsList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "pageSize", required = false) String pageSizeParam,
			@RequestParam(name = "thing_name", required = false) String thingNameParam) {
		Map<String, Object> merged = RequestParamToMapResolver.toMergedMap(request);

		oneCodeShopOperatorContextGate.assertThingsList(request, merged);

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}

		assertThingsListPagination(pageParam, pageSizeParam);

		long companyId = toLong(ud.get("company_id"));
		long pageVal = Long.parseLong(pageParam.trim());
		int page = pageVal > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pageVal;
		int pageSize = Integer.parseInt(pageSizeParam.trim());

		String nameFilter = resolveThingNameFilterFromMerged(merged);
		Map<String, Object> data = thingsListService.list(companyId, nameFilter, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static void assertThingsListPagination(String pageParam, String pageSizeParam) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		appendThingsListPageErrors("page", pageParam, errors);
		appendThingsListPageSizeErrors("pageSize", pageSizeParam, errors);
		if (!errors.isEmpty()) {
			throw new ResourceException(THINGS_LIST_VALIDATION_MESSAGE);
		}
	}

	private static void appendThingsListPageErrors(String field, String param, Map<String, List<String>> errors) {
		if (param == null || param.trim().isEmpty()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "不能为空。");
			return;
		}
		String p = param.trim();
		if (!SIGNED_INTEGER_STRING.matcher(p).matches()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须是整数。");
			return;
		}
		long v = Long.parseLong(p);
		if (v < 1) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须大于或等于1。");
		}
	}

	private static void appendThingsListPageSizeErrors(String field, String param, Map<String, List<String>> errors) {
		if (param == null || param.trim().isEmpty()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "不能为空。");
			return;
		}
		String p = param.trim();
		if (!SIGNED_INTEGER_STRING.matcher(p).matches()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须是整数。");
			return;
		}
		long v = Long.parseLong(p);
		if (v < 1) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须大于或等于1。");
		}
		if (v > 50) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "不能大于50。");
		}
	}

	private static String resolveThingNameFilterFromMerged(Map<String, Object> merged) {
		if (!merged.containsKey("thing_name")) {
			return null;
		}
		Object raw = merged.get("thing_name");
		String s = raw == null ? "" : raw.toString().trim();
		return StringUtils.hasText(s) ? s : null;
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422
	)
	@Activated(routeAlias = "onecode.things.detail")
	@GetMapping(value = "/onecode/things/{thing_id}", name = "获取物品详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getThingsDetail(
			HttpServletRequest request, @PathVariable("thing_id") String thingId) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("thing_id", thingId);

		oneCodeShopOperatorContextGate.assertThingsDetail(request, merged);

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}

		long companyId = toLong(ud.get("company_id"));
		assertThingsDetailPathThingId(thingId);
		long id = Long.parseLong(thingId.trim());
		Map<String, Object> data = thingsDetailService.getDetail(companyId, id);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static void assertThingsDetailPathThingId(String thingId) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		appendThingsDetailThingIdErrors("thing_id", thingId, errors);
		if (!errors.isEmpty()) {
			throw new ResourceException(THINGS_DETAIL_VALIDATION_MESSAGE);
		}
	}

	private static void appendThingsDetailThingIdErrors(
			String field, String param, Map<String, List<String>> errors) {
		if (param == null || param.trim().isEmpty()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "不能为空。");
			return;
		}
		String p = param.trim();
		if (!SIGNED_INTEGER_STRING.matcher(p).matches()) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须是整数。");
			return;
		}
		long v = Long.parseLong(p);
		if (v < 1) {
			errors.computeIfAbsent(field, k -> new ArrayList<>()).add(field + "必须大于或等于1。");
		}
	}

	@DingoResponse(
			resource = DingoResponse.ResourceStyle.DINGO,
			badRequest = DingoResponse.BadRequestStyle.DINGO_422
	)
	@Activated(routeAlias = "onecode.things.delete")
	@DeleteMapping(value = "/onecode/things/{thing_id}", name = "删除物品")
	public ResponseEntity<Void> deleteThings(HttpServletRequest request, @PathVariable("thing_id") String thingId) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("thing_id", thingId);
		oneCodeShopOperatorContextGate.assertDeleteThings(request, merged);

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}

		long companyId = toLong(ud.get("company_id"));
		assertDeleteThingsPathThingId(thingId);
		thingsDeleteService.deleteThings(companyId, Long.parseLong(thingId.trim()));

		Map<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("thing_id", thingId);
		logParams.put("company_id", companyId);
		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/onecode/things/" + thingId);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "删除物品");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok().build();
	}

	private static void assertDeleteThingsPathThingId(String thingId) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		appendThingsDetailThingIdErrors("thing_id", thingId, errors);
		if (!errors.isEmpty()) {
			throw new ResourceException(THINGS_DELETE_VALIDATION_MESSAGE);
		}
	}

	@Activated(routeAlias = "onecode.things.update")
	@PutMapping(value = "/onecode/things/{thing_id}", name = "更新物品")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateThings(
			HttpServletRequest request,
			@PathVariable("thing_id") String thingId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		merged.put("thing_id", thingId);
		oneCodeShopOperatorContextGate.assertUpdateThings(request, merged);

		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}

		long companyId = toLong(ud.get("company_id"));

		long pathThingIdLong = parseRequiredPositiveLong(thingId, "请确认您所编辑的物品是否存在");

		String thingName = requiredNonEmptyString(merged.get("thing_name"), "物品名称必填");
		String pic = requiredNonEmptyString(merged.get("pic"), "请上传物品图片");
		BigDecimal priceYuan = parseRequiredPriceYuan(merged.get("price"));
		int priceInCents;
		try {
			priceInCents =
					priceYuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValueExact();
		} catch (ArithmeticException e) {
			throw new BadRequestException("价格必填,且要大于0");
		}

		boolean introKeyPresent = merged.containsKey("intro");
		boolean introColumnUpdateRequested = false;
		String introValueIfUpdating = null;
		if (merged.containsKey("intro") && merged.get("intro") != null) {
			Object introRaw = merged.get("intro");
			String s = introRaw instanceof String str ? str : introRaw.toString();
			if (StringUtils.hasText(s)) {
				introColumnUpdateRequested = true;
				introValueIfUpdating = s;
			}
		}

		Map<String, Object> data =
				thingsUpdateService.updateThings(
						companyId,
						pathThingIdLong,
						thingName,
						pic,
						priceInCents,
						introKeyPresent,
						introColumnUpdateRequested,
						introValueIfUpdating);

		Map<String, Object> logParams = new LinkedHashMap<>(merged);
		logParams.put("company_id", companyId);
		long operatorId = toLong(ud.get("operator_id"));
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/onecode/things/" + thingId);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(logParams));
		} catch (Exception e) {
			logCtx.put("params", logParams.toString());
		}
		logCtx.put("operator_name", "更新物品");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put(
					"merchant_id",
					merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		try {
			operatorLogsWriteService.addLogs(logCtx);
		} catch (Exception ignored) {
		}

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static long parseRequiredPositiveLong(Object raw, String message) {
		if (raw == null) {
			throw new BadRequestException(message);
		}
		long v;
		try {
			if (raw instanceof Number n) {
				v = n.longValue();
			} else {
				String t = raw.toString().trim();
				if (!StringUtils.hasText(t)) {
					throw new BadRequestException(message);
				}
				v = Long.parseLong(t);
			}
		} catch (NumberFormatException e) {
			throw new BadRequestException(message);
		}
		if (v < 1) {
			throw new BadRequestException(message);
		}
		return v;
	}
}
