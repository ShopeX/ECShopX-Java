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

package cn.shopex.ecshopx.wsugc.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.wsugc.service.tag.TagCreateService;
import cn.shopex.ecshopx.wsugc.service.tag.TagDetailService;
import cn.shopex.ecshopx.wsugc.service.tag.TagListService;
import cn.shopex.ecshopx.wsugc.service.tag.TagVerifyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422)
@RestController("wsugcAdminV1Tag")
@RequestMapping("/api/v1/ugc/tag")
public class TagController {

	private final TagCreateService tagCreateService;
	private final TagVerifyService tagVerifyService;
	private final TagDetailService tagDetailService;
	private final TagListService tagListService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;
	private final Environment environment;

	public TagController(
			TagCreateService tagCreateService,
			TagVerifyService tagVerifyService,
			TagDetailService tagDetailService,
			TagListService tagListService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper,
			Environment environment) {
		this.tagCreateService = tagCreateService;
		this.tagVerifyService = tagVerifyService;
		this.tagDetailService = tagDetailService;
		this.tagListService = tagListService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
		this.environment = environment;
	}

	@PostMapping(value = "/create", name = "新建图片标签")
	public ResponseEntity<ApiResult<Map<String, Object>>> createTag(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		boolean local = environment.acceptsProfiles(Profiles.of("local"));
		Map<String, Object> data = tagCreateService.create(merged, operatorJwt, local);

		long companyId = readLong(ud, "company_id", 1L);
		long operatorId = readLong(ud, "operator_id", 0L);
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/ugc/tag/create");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "新建图片标签");
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
			// 操作日志失败不影响主流程
		}

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/verify", name = "审核标签")
	public ResponseEntity<ApiResult<Map<String, Object>>> verifyTag(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeVerifyTagInput(request, body);
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		Map<String, Object> operatorJwt;
		if (raw instanceof Map<?, ?> m) {
			@SuppressWarnings("unchecked")
			Map<String, Object> cast = (Map<String, Object>) m;
			operatorJwt = cast;
		} else {
			operatorJwt = Collections.emptyMap();
		}
		boolean local = environment.acceptsProfiles(Profiles.of("local"));
		Map<String, Object> data = tagVerifyService.verify(merged, operatorJwt, local);

		Map<String, Object> ud = operatorJwt;
		long companyId = readLong(ud, "company_id", 1L);
		long operatorId = readLong(ud, "operator_id", 0L);
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/ugc/tag/verify");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "审核标签");
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
			// 操作日志失败不影响主流程
		}

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private Map<String, Object> mergeVerifyTagInput(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMap(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(parameterMapToMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		List<Long> formIds = dedupeFormTagIds(request);
		if (!formIds.isEmpty()) {
			if (formIds.size() > 1) {
				merged.put("tag_id", formIds);
			} else {
				merged.put("tag_id", formIds.get(0));
			}
		}
		return merged;
	}

	private static List<Long> dedupeFormTagIds(HttpServletRequest request) {
		Set<Long> set = new LinkedHashSet<>();
		set.addAll(collectFormLongs(request, "tag_id"));
		set.addAll(collectFormLongs(request, "tag_id[]"));
		return new ArrayList<>(set);
	}

	private static List<Long> collectFormLongs(HttpServletRequest request, String paramName) {
		String[] vals = request.getParameterValues(paramName);
		if (vals == null) {
			return List.of();
		}
		List<Long> out = new ArrayList<>();
		for (String v : vals) {
			if (!StringUtils.hasText(v)) {
				continue;
			}
			try {
				out.add(Long.parseLong(v.trim()));
			} catch (NumberFormatException e) {
				throw new BadRequestException("tag_id 格式无效");
			}
		}
		return out;
	}

	private static long readLong(Map<?, ?> ud, String key, long defaultVal) {
		Object v = ud.get(key);
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
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

	@PostMapping(value = "/enable", name = "发布标签")
	public ResponseEntity<Void> enableTag() {
		return ResponseEntity.ok().build();
	}

	@Activated(routeAlias = "ugc.tag.list")
	@GetMapping(value = "/list", name = "标签列表")
	public ResponseEntity<ApiResult<Object>> getTagList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "30") int pageSize,
			@RequestParam(value = "tag_name", required = false) String tagName,
			@RequestParam(value = "nickname", required = false) String nickname,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "source", required = false) String source,
			@RequestParam(value = "sort", required = false) String sort) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		long companyId = readLong(operatorJwt, "company_id", 1L);
		Object data =
				tagListService.buildList(
						companyId, page, pageSize, tagName, nickname, mobile, status, source, sort);
		if (data instanceof List<?> list && list.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) data;
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "ugc.tag.detail")
	@GetMapping(value = "/detail", name = "标签详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getTagDetail(
			HttpServletRequest request, @RequestParam(value = "tag_id", required = false) String tagId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		String langTag = resolveAdminLangTag(operatorJwt);
		Map<String, Object> data = tagDetailService.buildResponse(tagId, operatorJwt, langTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String resolveAdminLangTag(Map<String, Object> operatorJwt) {
		Object cc = operatorJwt.get("country_code");
		if (cc != null && StringUtils.hasText(cc.toString())) {
			return cc.toString().trim();
		}
		return "zh-CN";
	}

	@DeleteMapping(value = "/delete", name = "删除标签")
	public ResponseEntity<Void> deleteTag() {
		return ResponseEntity.ok().build();
	}
}
