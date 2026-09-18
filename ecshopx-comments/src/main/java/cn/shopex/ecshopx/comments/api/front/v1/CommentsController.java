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

package cn.shopex.ecshopx.comments.api.front.v1;

import cn.shopex.ecshopx.comments.service.ShopCommentCreateService;
import cn.shopex.ecshopx.comments.service.ShopCommentListService;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.members.service.h5.H5BearerJwtClaimsService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@FrontAuth
@DingoResponse(
    resource = DingoResponse.ResourceStyle.DINGO,
    badRequest = DingoResponse.BadRequestStyle.DINGO_400,
    unauthorized = true
)
@RestController("commentsFrontV1")
@RequestMapping("/api/v1/h5app")
public class CommentsController {

	private final ShopCommentCreateService shopCommentCreateService;
	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final ShopCommentListService shopCommentListService;

	public CommentsController(
			ShopCommentCreateService shopCommentCreateService,
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			ShopCommentListService shopCommentListService) {
		this.shopCommentCreateService = shopCommentCreateService;
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.shopCommentListService = shopCommentListService;
	}

	@PostMapping("/wxapp/comment")
	public ResponseEntity<ApiResult<Map<String, Object>>> createComment(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}
		long companyIdLong = readCompanyIdForComment(claims);
		String userIdStr = normalizeUserIdOrThrow(claims.get("user_id"));
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		merged.put("company_id", String.valueOf(companyIdLong));
		merged.put("user_id", userIdStr);
		if (!merged.containsKey("pics")) {
			merged.put("pics", Collections.emptyList());
		}
		if (!merged.containsKey("content") || merged.get("content") == null) {
			throw new BadRequestException("content 必填");
		}
		Map<String, Object> data = shopCommentCreateService.create(companyIdLong, merged);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping("/wxapp/comments")
	public ResponseEntity<ApiResult<Map<String, Object>>> getComments(HttpServletRequest request) {
		Map<String, Object> claims = h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}
		long companyIdLong = readCompanyIdForComment(claims);
		Map<String, Object> mergedQuery = parameterMapToMap(request);
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

	private static long readCompanyIdForComment(Map<?, ?> claims) {
		Object v = claims.get("company_id");
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

	private static String normalizeUserIdOrThrow(Object raw) {
		String s = raw == null ? "" : raw.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			throw new BadRequestException("成为会员才能评论！", 411);
		}
		return s;
	}

	private static boolean isAccountDisabled(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
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
		return 50;
	}
}
