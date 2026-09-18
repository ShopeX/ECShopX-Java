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

package cn.shopex.ecshopx.comments.api.admin.v1;

import cn.shopex.ecshopx.comments.service.CommentsCreateRequestGate;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.comments.service.CommentsMenuPermissionService;
import cn.shopex.ecshopx.comments.service.ShopCommentCreateService;
import cn.shopex.ecshopx.comments.service.ShopCommentListService;
import cn.shopex.ecshopx.comments.service.ShopCommentUpdateService;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AdminAuth
@ShopLog
@DingoResponse(
    resource = DingoResponse.ResourceStyle.DINGO,
    badRequest = DingoResponse.BadRequestStyle.DINGO_400
)
@RestController("commentsAdminV1")
@RequestMapping("/api/v1")
public class CommentsController {

	private final CommentsCreateRequestGate commentsCreateRequestGate;
	private final ShopCommentCreateService shopCommentCreateService;
	private final ShopCommentUpdateService shopCommentUpdateService;
	private final ShopCommentListService shopCommentListService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;

	public CommentsController(
			CommentsCreateRequestGate commentsCreateRequestGate,
			ShopCommentCreateService shopCommentCreateService,
			ShopCommentUpdateService shopCommentUpdateService,
			ShopCommentListService shopCommentListService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper) {
		this.commentsCreateRequestGate = commentsCreateRequestGate;
		this.shopCommentCreateService = shopCommentCreateService;
		this.shopCommentUpdateService = shopCommentUpdateService;
		this.shopCommentListService = shopCommentListService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
	}

	@Activated(routeAlias = "comment.create")
	@PostMapping(value = "/comment", name = "创建评论")
	public ResponseEntity<ApiResult<Map<String, Object>>> createComment(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		long companyIdLong = readCompanyIdForComment(ud);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		commentsCreateRequestGate.validateBeforeCreate(request, merged);
		String companyIdStr = String.valueOf(companyIdLong);
		merged.put("company_id", companyIdStr);
		long operatorId = readRequiredLong(ud, "operator_id");
		Map<String, Object> data = shopCommentCreateService.create(companyIdLong, merged);

		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyIdLong);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/comment");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "创建评论");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put("merchant_id", merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "comment.update")
	@PatchMapping(value = "/comment/{comment_id}", name = "更新评论")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateComment(
			HttpServletRequest request,
			@PathVariable("comment_id") String commentId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		long companyIdLong = readCompanyIdForComment(ud);
		Object opTypeJwt = ud.get("operator_type");
		if (opTypeJwt != null) {
			merged.put("operator_type", opTypeJwt.toString());
		}
		commentsCreateRequestGate.validateBeforeUpdate(request, merged);
		long operatorId = readRequiredLong(ud, "operator_id");
		Map<String, Object> data = shopCommentUpdateService.update(commentId, merged);

		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyIdLong);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/comment/" + commentId);
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "更新评论");
		logCtx.put("log_type", "operator");
		Object merchantId = ud.get("merchant_id");
		if (merchantId != null) {
			logCtx.put("merchant_id", merchantId instanceof Number n ? n.longValue() : Long.parseLong(merchantId.toString()));
		}
		operatorLogsWriteService.addLogs(logCtx);

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "comments.list")
	@GetMapping(value = "/comments", name = "获取评论列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getComments(HttpServletRequest request) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> jwtClaims = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : ud.entrySet()) {
			jwtClaims.put(String.valueOf(e.getKey()), e.getValue());
		}
		Map<String, Object> mergedQuery = parameterMapToMap(request);
		Object opTypeJwt = jwtClaims.get("operator_type");
		if (opTypeJwt != null) {
			mergedQuery.put("operator_type", opTypeJwt.toString());
		}
		commentsCreateRequestGate.validateBeforeList(request, jwtClaims, CommentsMenuPermissionService.ROUTE_ALIAS_COMMENTS_LIST);
		long companyIdLong = readCompanyIdForComment(ud);

		Map<String, Object> filterParams = new LinkedHashMap<>();
		if (mergedQuery.containsKey("is_hide") && mergedQuery.get("is_hide") != null) {
			filterParams.put("hid", mergedQuery.get("is_hide"));
		}
		if (mergedQuery.containsKey("shop_id") && isTruthyShopId(mergedQuery.get("shop_id"))) {
			filterParams.put("shop_id", mergedQuery.get("shop_id").toString().trim());
		}
		int pageNo = parsePageNo(mergedQuery);
		int pageSize = parsePageSize(mergedQuery);
		Map<String, Object> data = shopCommentListService.list(companyIdLong, filterParams, pageNo, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
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

	private static long readCompanyIdForComment(Map<?, ?> ud) {
		Object v = ud.get("company_id");
		if (v == null) {
			throw new BadRequestException("无相关企业信息！", 411);
		}
		long id;
		if (v instanceof Number n) {
			id = n.longValue();
		} else {
			try {
				id = Long.parseLong(v.toString());
			} catch (NumberFormatException e) {
				throw new BadRequestException("无相关企业信息！", 411);
			}
		}
		if (id <= 0) {
			throw new BadRequestException("无相关企业信息！", 411);
		}
		return id;
	}

	private static boolean isTruthyShopId(Object shopIdRaw) {
		if (shopIdRaw == null) {
			return false;
		}
		String t = shopIdRaw.toString().trim();
		if (!StringUtils.hasText(t) || "0".equals(t)) {
			return false;
		}
		return true;
	}

	private static int parsePageNo(Map<String, Object> mergedQuery) {
		if (mergedQuery.containsKey("page_no")) {
			Object v = mergedQuery.get("page_no");
			if (v != null && StringUtils.hasText(v.toString())) {
				try {
					int p = Integer.parseInt(v.toString().trim());
					if (p > 0) {
						return p;
					}
					throw new BadRequestException("页码必须大于 0");
				} catch (NumberFormatException e) {
					throw new BadRequestException("页码格式无效");
				}
			}
		}
		if (mergedQuery.containsKey("pageNo")) {
			Object v = mergedQuery.get("pageNo");
			if (v != null && StringUtils.hasText(v.toString())) {
				try {
					int p = Integer.parseInt(v.toString().trim());
					if (p > 0) {
						return p;
					}
					throw new BadRequestException("页码必须大于 0");
				} catch (NumberFormatException e) {
					throw new BadRequestException("页码格式无效");
				}
			}
		}
		return 1;
	}

	private static int parsePageSize(Map<String, Object> mergedQuery) {
		if (mergedQuery.containsKey("pageSize")) {
			Object v = mergedQuery.get("pageSize");
			if (v != null && StringUtils.hasText(v.toString())) {
				try {
					int s = Integer.parseInt(v.toString().trim());
					if (s > 0) {
						return s;
					}
					throw new BadRequestException("每页条数必须大于 0");
				} catch (NumberFormatException e) {
					throw new BadRequestException("每页条数格式无效");
				}
			}
		}
		if (mergedQuery.containsKey("page_size")) {
			Object v = mergedQuery.get("page_size");
			if (v != null && StringUtils.hasText(v.toString())) {
				try {
					int s = Integer.parseInt(v.toString().trim());
					if (s > 0) {
						return s;
					}
					throw new BadRequestException("每页条数必须大于 0");
				} catch (NumberFormatException e) {
					throw new BadRequestException("每页条数格式无效");
				}
			}
		}
		return 50;
	}

	private static long readRequiredLong(Map<?, ?> ud, String key) {
		Object v = ud.get(key);
		if (v == null) {
			throw new BadRequestException(key + " 缺失或无效");
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			throw new BadRequestException(key + " 缺失或无效");
		}
	}
}
