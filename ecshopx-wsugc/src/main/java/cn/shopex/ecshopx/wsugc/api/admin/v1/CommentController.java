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
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.wsugc.service.comment.CommentDeleteService;
import cn.shopex.ecshopx.wsugc.service.comment.CommentDetailService;
import cn.shopex.ecshopx.wsugc.service.comment.CommentListService;
import cn.shopex.ecshopx.wsugc.service.comment.CommentVerifyService;
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
@RestController("wsugcAdminV1Comment")
@RequestMapping("/api/v1/ugc/comment")
public class CommentController {

	private final CommentVerifyService commentVerifyService;
	private final CommentDeleteService commentDeleteService;
	private final CommentDetailService commentDetailService;
	private final CommentListService commentListService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;
	private final Environment environment;

	public CommentController(
			CommentVerifyService commentVerifyService,
			CommentDeleteService commentDeleteService,
			CommentDetailService commentDetailService,
			CommentListService commentListService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper,
			Environment environment) {
		this.commentVerifyService = commentVerifyService;
		this.commentDeleteService = commentDeleteService;
		this.commentDetailService = commentDetailService;
		this.commentListService = commentListService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
		this.environment = environment;
	}

	@PostMapping(value = "/verify", name = "审核评论")
	public ResponseEntity<ApiResult<Map<String, Object>>> verifyComment(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeVerifyCommentInput(request, body);
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		Map<String, Object> operatorJwt;
		if (raw instanceof Map<?, ?> m) {
			@SuppressWarnings("unchecked")
			Map<String, Object> cast = (Map<String, Object>) m;
			operatorJwt = cast;
		} else {
			operatorJwt = Collections.emptyMap();
		}
		boolean local = this.environment.acceptsProfiles(Profiles.of("local"));
		Map<String, Object> data = commentVerifyService.verify(merged, operatorJwt, local);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private Map<String, Object> mergeVerifyCommentInput(HttpServletRequest request, Map<String, Object> body) {
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
		List<String> formIds = dedupeFormCommentIdStrings(request);
		if (!formIds.isEmpty()) {
			if (formIds.size() > 1) {
				merged.put("comment_id", formIds);
			} else {
				merged.put("comment_id", formIds.get(0));
			}
		}
		return merged;
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

	/** Form 表单项保留原始非空串，不在此处做数值解析（与 JSON 分支统一在 Service 内规范化）。 */
	private static List<String> dedupeFormCommentIdStrings(HttpServletRequest request) {
		Set<String> set = new LinkedHashSet<>();
		addTrimmedNonBlank(request, "comment_id", set);
		addTrimmedNonBlank(request, "comment_id[]", set);
		return new ArrayList<>(set);
	}

	private static void addTrimmedNonBlank(HttpServletRequest request, String paramName, Set<String> into) {
		String[] vals = request.getParameterValues(paramName);
		if (vals == null) {
			return;
		}
		for (String v : vals) {
			if (StringUtils.hasText(v)) {
				into.add(v.trim());
			}
		}
	}

	@Activated(routeAlias = "ugc.comment.list")
	@GetMapping(value = "/list", name = "评论列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCommentList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "30") int pageSize,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "nickname", required = false) String nickname,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "content", required = false) String content,
			@RequestParam(value = "sort", required = false) String sort) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		long companyId = readLong(operatorJwt.get("company_id"), 1L);
		String langTag = resolveAdminLangTag(operatorJwt);
		List<Long> postIds = collectPostIds(request);
		Map<String, Object> data =
				commentListService.buildList(
						companyId, langTag, page, pageSize, status, postIds, nickname, mobile, content, sort);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static List<Long> collectPostIds(HttpServletRequest request) {
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		addParsedLongs(request.getParameterValues("post_id"), ids);
		addParsedLongs(request.getParameterValues("post_id[]"), ids);
		return new ArrayList<>(ids);
	}

	private static void addParsedLongs(String[] values, LinkedHashSet<Long> into) {
		if (values == null) {
			return;
		}
		for (String v : values) {
			if (!StringUtils.hasText(v)) {
				continue;
			}
			try {
				into.add(Long.parseLong(v.trim()));
			} catch (NumberFormatException ignored) {
				// drop invalid token
			}
		}
	}

	private static long readLong(Object v, long defaultVal) {
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

	@Activated(routeAlias = "ugc.comment.detail")
	@GetMapping(value = "/detail", name = "评论详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCommentDetail(
			HttpServletRequest request,
			@RequestParam(value = "comment_id", required = false) String commentId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		String langTag = resolveAdminLangTag(operatorJwt);
		Map<String, Object> data = commentDetailService.buildResponse(commentId, operatorJwt, langTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String resolveAdminLangTag(Map<String, Object> operatorJwt) {
		Object cc = operatorJwt.get("country_code");
		if (cc != null && StringUtils.hasText(cc.toString())) {
			return cc.toString().trim();
		}
		return "zh-CN";
	}

	@PostMapping(value = "/delete", name = "删除评论")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteComment(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeVerifyCommentInput(request, body);
		Map<String, Object> data = commentDeleteService.deleteComments(merged);

		long companyId = readLongFromMap(ud, "company_id", 1L);
		long operatorId = readLongFromMap(ud, "operator_id", 0L);
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/ugc/comment/delete");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "删除评论");
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

	private static long readLongFromMap(Map<?, ?> ud, String key, long defaultVal) {
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
}
