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

package cn.shopex.ecshopx.promotions.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableErrorCodes;
import cn.shopex.ecshopx.promotions.domain.turntable.TurntableErrorMessages;
import cn.shopex.ecshopx.promotions.service.TurntableFrontDrawResultService;
import cn.shopex.ecshopx.promotions.service.TurntableFrontJoinTurntableService;
import cn.shopex.ecshopx.promotions.service.TurntableFrontLuckyDrawActInfoService;
import cn.shopex.ecshopx.promotions.service.TurntableFrontLuckyDrawLogService;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("promotionsFrontV1Turntable")
@RequestMapping("/api/v1/h5app/wxapp")
public class TurntableController {

	private final TurntableFrontLuckyDrawActInfoService turntableFrontLuckyDrawActInfoService;
	private final TurntableFrontLuckyDrawLogService turntableFrontLuckyDrawLogService;
	private final TurntableFrontJoinTurntableService turntableFrontJoinTurntableService;
	private final TurntableFrontDrawResultService turntableFrontDrawResultService;
	private final LangueProperties langueProperties;
	private final MessageSource messageSource;

	public TurntableController(
			TurntableFrontLuckyDrawActInfoService turntableFrontLuckyDrawActInfoService,
			TurntableFrontLuckyDrawLogService turntableFrontLuckyDrawLogService,
			TurntableFrontJoinTurntableService turntableFrontJoinTurntableService,
			TurntableFrontDrawResultService turntableFrontDrawResultService,
			LangueProperties langueProperties,
			MessageSource messageSource) {
		this.turntableFrontLuckyDrawActInfoService = turntableFrontLuckyDrawActInfoService;
		this.turntableFrontLuckyDrawLogService = turntableFrontLuckyDrawLogService;
		this.turntableFrontJoinTurntableService = turntableFrontJoinTurntableService;
		this.turntableFrontDrawResultService = turntableFrontDrawResultService;
		this.langueProperties = langueProperties;
		this.messageSource = messageSource;
	}

	@GetMapping(value = "/promotion/turntableconfig", name = "获取大转盘配置")
	public ResponseEntity<Void> getTurntableConfig() {
		return ResponseEntity.ok().build();
	}

	@FrontNoAuth
	@GetMapping(path = "/promotion/getLuckyDrawData", name = "获取大转盘活动数据", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getLuckyDrawData(
			HttpServletRequest request, @RequestParam(value = "id", required = false) String id) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parseAuthUserIdFromClaims(claims);
		long companyId = parseCompanyIdFromClaims(claims);
		String langTag = RequestLangTag.current(langueProperties);
		if (!StringUtils.hasText(langTag)) {
			langTag = "zh-CN";
		}
		return ResponseEntity.ok(
				ApiResult.ok(
						turntableFrontLuckyDrawActInfoService.getLuckyDrawActInfo(
								userId, companyId, id, langTag)));
	}

	@FrontNoAuth
	@GetMapping(
			path = "/promotiontest/turntableconfig",
			name = "获取大转盘活动数据（promotiontest）",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getLuckyDrawActInfo(
			HttpServletRequest request, @RequestParam(value = "id", required = false) String id) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parseAuthUserIdFromClaims(claims);
		long companyId = parseCompanyIdFromClaims(claims);
		String langTag = RequestLangTag.current(langueProperties);
		if (!StringUtils.hasText(langTag)) {
			langTag = "zh-CN";
		}
		return ResponseEntity.ok(
				ApiResult.ok(
						turntableFrontLuckyDrawActInfoService.getLuckyDrawActInfo(
								userId, companyId, id, langTag)));
	}

	@SuppressWarnings("unused")
	@GetMapping(value = "/promotion/getLuckyDrawLog", name = "获取大转盘抽奖记录", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<List<Map<String, Object>>>> getTurnLog(
			HttpServletRequest request,
			@RequestParam(value = "id", required = false) String id,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "page_size", required = false) String pageSize) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parseAuthUserIdFromClaims(claims);
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		return ResponseEntity.ok(ApiResult.ok(turntableFrontLuckyDrawLogService.getTurnLog(userId, id)));
	}

	/** 新客户端：POST body activity_id + requestId */
	@PostMapping(value = "/promotion/turntable", name = "参与大转盘", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> joinTurntablePost(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parseAuthUserIdFromClaims(claims);
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		long companyId = parseCompanyIdFromClaims(claims);
		if (companyId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		Map<String, Object> merged = mergeQueryAndBody(request, body);
		Object actRaw = firstPresent(merged, "activity_id", "id");
		Object reqRaw = firstPresent(merged, "requestId", "request_id");
		if (reqRaw == null || !StringUtils.hasText(String.valueOf(reqRaw).trim())) {
			throw new BadRequestException(
					TurntableErrorMessages.message(
							messageSource,
							TurntableErrorCodes.REQUEST_ID_INVALID,
							resolveLocale(request)));
		}
		String langTag = RequestLangTag.current(langueProperties);
		Locale locale =
				StringUtils.hasText(langTag) ? Locale.forLanguageTag(langTag.trim()) : Locale.forLanguageTag("zh-CN");
		return ResponseEntity.ok(
				ApiResult.ok(
						turntableFrontJoinTurntableService.joinTurntable(
								userId,
								companyId,
								actRaw == null ? null : String.valueOf(actRaw),
								String.valueOf(reqRaw),
								locale)));
	}

	/** 旧 GET 短期兼容：必须带 requestId，禁止服务端生成以免重试连抽。 */
	@GetMapping(value = "/promotion/turntable", name = "参与大转盘(兼容)", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> joinTurntableGet(
			HttpServletRequest request,
			@RequestParam(value = "activity_id", required = false) String activityId,
			@RequestParam(value = "requestId", required = false) String requestId) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parseAuthUserIdFromClaims(claims);
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		long companyId = parseCompanyIdFromClaims(claims);
		if (companyId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		if (!StringUtils.hasText(requestId)) {
			throw new BadRequestException(
					TurntableErrorMessages.message(
							messageSource,
							TurntableErrorCodes.REQUEST_ID_INVALID,
							resolveLocale(request)));
		}
		String langTag = RequestLangTag.current(langueProperties);
		Locale locale =
				StringUtils.hasText(langTag) ? Locale.forLanguageTag(langTag.trim()) : Locale.forLanguageTag("zh-CN");
		return ResponseEntity.ok(
				ApiResult.ok(
						turntableFrontJoinTurntableService.joinTurntable(
								userId, companyId, activityId, requestId, locale)));
	}

	@GetMapping(value = "/promotion/turntable/result", name = "抽奖结果查询", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getDrawResult(
			HttpServletRequest request,
			@RequestParam(value = "requestId", required = false) String requestId,
			@RequestParam(value = "recordId", required = false) String recordId) {
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long userId = parseAuthUserIdFromClaims(claims);
		if (userId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		long companyId = parseCompanyIdFromClaims(claims);
		if (companyId <= 0L) {
			throw new UnauthorizedException("Unable to authenticate user.");
		}
		return ResponseEntity.ok(
				ApiResult.ok(
						turntableFrontDrawResultService.getDrawResult(userId, companyId, requestId, recordId)));
	}

	@GetMapping(value = "/promotion/loginaddtimes", name = "用户登陆赠送抽奖次数")
	public ResponseEntity<Void> loginAddSurplusTimes() {
		return ResponseEntity.ok().build();
	}

	private static Map<String, Object> mergeQueryAndBody(HttpServletRequest request, Map<String, Object> body) {
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

	private static Object firstPresent(Map<String, Object> map, String... keys) {
		for (String key : keys) {
			if (map.containsKey(key) && map.get(key) != null) {
				return map.get(key);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static long parseAuthUserIdFromClaims(Map<String, Object> claims) {
		Object v = claims == null ? null : claims.get("user_id");
		if (v == null) {
			return 0L;
		}
		try {
			long parsed = (v instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(v).trim());
			return parsed > 0L ? parsed : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long parseCompanyIdFromClaims(Map<String, Object> claims) {
		Object v = claims == null ? null : claims.get("company_id");
		if (v == null) {
			return 0L;
		}
		try {
			long parsed = (v instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(v).trim());
			return parsed > 0L ? parsed : 0L;
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private Locale resolveLocale(HttpServletRequest request) {
		String langTag = RequestLangTag.current(langueProperties);
		if (!StringUtils.hasText(langTag)) {
			langTag = "zh-CN";
		}
		return Locale.forLanguageTag(langTag.trim());
	}
}
