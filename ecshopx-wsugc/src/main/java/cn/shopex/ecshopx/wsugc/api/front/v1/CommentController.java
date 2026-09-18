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

package cn.shopex.ecshopx.wsugc.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.members.service.h5.H5BearerJwtClaimsService;
import cn.shopex.ecshopx.wsugc.service.comment.FrontUgcCommentCreateService;
import cn.shopex.ecshopx.wsugc.service.comment.FrontUgcCommentDeleteService;
import cn.shopex.ecshopx.wsugc.service.comment.FrontUgcCommentLikeService;
import cn.shopex.ecshopx.wsugc.service.comment.FrontUgcCommentListService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
		unauthorized = true)
@RestController("wsugcFrontV1Comment")
@RequestMapping("/api/v1/h5app")
public class CommentController {

	private final FrontUgcCommentCreateService frontUgcCommentCreateService;
	private final FrontUgcCommentDeleteService frontUgcCommentDeleteService;
	private final FrontUgcCommentLikeService frontUgcCommentLikeService;
	private final FrontUgcCommentListService frontUgcCommentListService;
	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final Environment environment;

	public CommentController(
			FrontUgcCommentCreateService frontUgcCommentCreateService,
			FrontUgcCommentDeleteService frontUgcCommentDeleteService,
			FrontUgcCommentLikeService frontUgcCommentLikeService,
			FrontUgcCommentListService frontUgcCommentListService,
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			Environment environment) {
		this.frontUgcCommentCreateService = frontUgcCommentCreateService;
		this.frontUgcCommentDeleteService = frontUgcCommentDeleteService;
		this.frontUgcCommentLikeService = frontUgcCommentLikeService;
		this.frontUgcCommentListService = frontUgcCommentListService;
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.environment = environment;
	}

	@FrontAuth
	@PostMapping(value = "/wxapp/ugc/comment/create", name = "发表评论")
	public ResponseEntity<ApiResult<Map<String, Object>>> createComment(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		long userId = 0L;
		long companyId = 0L;
		Object claimUid = claims.get("user_id");
		if (claimUid != null && authUserIdTruthy(claimUid)) {
			userId = parseLongFlexible(claimUid, 0L);
			companyId = parseLongFlexible(claims.get("company_id"), 0L);
		}

		boolean local = this.environment.acceptsProfiles(Profiles.of("local"));
		if (userId <= 0L && local && merged.containsKey("user_id")) {
			userId = parseLongFlexible(merged.get("user_id"), 0L);
			companyId = 1L;
		}

		if (userId <= 0L || companyId <= 0L) {
			throw new ResourceException("会员id不能为空！");
		}

		String ip = clientIp(request);
		Map<String, Object> data = frontUgcCommentCreateService.create(merged, userId, companyId, ip);
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

	private static boolean authUserIdTruthy(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof Number n) {
			return n.longValue() > 0L;
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s) || "0".equals(s)) {
			return false;
		}
		try {
			return Long.parseLong(s) > 0L;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static long parseLongFlexible(Object raw, long defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	@FrontAuth
	@PostMapping(value = "/wxapp/ugc/comment/like", name = "评论点赞")
	public ResponseEntity<ApiResult<Map<String, Object>>> likeComment(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		long userId = 0L;
		long companyId = 0L;
		Object claimUid = claims.get("user_id");
		if (claimUid != null && authUserIdTruthy(claimUid)) {
			userId = parseLongFlexible(claimUid, 0L);
			companyId = parseLongFlexible(claims.get("company_id"), 0L);
		}

		boolean local = this.environment.acceptsProfiles(Profiles.of("local"));
		if (userId <= 0L && local && merged.containsKey("user_id")) {
			userId = parseLongFlexible(merged.get("user_id"), 0L);
			companyId = 1L;
		}

		if (userId <= 0L || companyId <= 0L) {
			throw new ResourceException("会员id不能为空！");
		}

		if (!merged.containsKey("post_id") || parseLongFlexible(merged.get("post_id"), 0L) <= 0L) {
			throw new ResourceException("笔记id不能为空！");
		}
		if (!merged.containsKey("comment_id") || parseLongFlexible(merged.get("comment_id"), 0L) <= 0L) {
			throw new ResourceException("评论id不能为空！");
		}

		long postId = parseLongFlexible(merged.get("post_id"), 0L);
		long commentId = parseLongFlexible(merged.get("comment_id"), 0L);
		Map<String, Object> data = frontUgcCommentLikeService.like(userId, postId, commentId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@PostMapping(value = "/wxapp/ugc/comment/delete", name = "删除评论")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteComment(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		long userId = 0L;
		long companyId = 0L;
		Object claimUid = claims.get("user_id");
		if (claimUid != null && authUserIdTruthy(claimUid)) {
			userId = parseLongFlexible(claimUid, 0L);
			companyId = parseLongFlexible(claims.get("company_id"), 0L);
		}

		boolean local = this.environment.acceptsProfiles(Profiles.of("local"));
		if (userId <= 0L && local && merged.containsKey("user_id")) {
			userId = parseLongFlexible(merged.get("user_id"), 0L);
			companyId = 1L;
		}

		if (userId <= 0L || companyId <= 0L) {
			throw new ResourceException("会员id不能为空！");
		}

		if (!merged.containsKey("comment_id") || parseLongFlexible(merged.get("comment_id"), 0L) <= 0L) {
			throw new ResourceException("评论id不能为空！");
		}

		long commentId = parseLongFlexible(merged.get("comment_id"), 0L);
		Map<String, Object> data = frontUgcCommentDeleteService.deleteByMember(userId, commentId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@GetMapping(value = "/wxapp/ugc/comment/list", name = "评论列表免登")
	public ResponseEntity<ApiResult<Map<String, Object>>> getCommentList(
			HttpServletRequest request,
			@RequestParam(value = "post_id", required = false) String postId,
			@RequestParam(value = "parent_comment_id", required = false, defaultValue = "0") String parentCommentId,
			@RequestParam(value = "page_no", required = false) String pageNoParam,
			@RequestParam(value = "page_size", required = false) String pageSizeParam) {
		long parsedPostId = parseLongFlexible(postId, 0L);
		if (!StringUtils.hasText(postId) || parsedPostId <= 0L) {
			throw new ResourceException("笔记id不能为空！");
		}

		long parentCid = parseLongFlexible(parentCommentId, 0L);
		if (parentCid < 0L) {
			parentCid = 0L;
		}

		int pageNo = 1;
		if (StringUtils.hasText(pageNoParam)) {
			try {
				int v = Integer.parseInt(pageNoParam.trim());
				if (v > 0) {
					pageNo = v;
				}
			} catch (NumberFormatException ignored) {
			}
		}

		int pageSize = 20;
		if (StringUtils.hasText(pageSizeParam)) {
			try {
				int v = Integer.parseInt(pageSizeParam.trim());
				if (v > 0 && v <= 50) {
					pageSize = v;
				}
			} catch (NumberFormatException ignored) {
			}
		}

		long userIdForLike = 0L;
		long companyIdForFilter = 0L;

		Optional<Map<String, Object>> jwtOpt = h5BearerJwtClaimsService.verifyAndExtractClaims(request);
		boolean jwtHasRealUser = false;
		if (jwtOpt.isPresent()) {
			Map<String, Object> claims = jwtOpt.get();
			Object claimUid = claims.get("user_id");
			if (authUserIdTruthy(claimUid)) {
				jwtHasRealUser = true;
				userIdForLike = parseLongFlexible(claimUid, 0L);
				companyIdForFilter = parseLongFlexible(claims.get("company_id"), 0L);
			}
		}

		if (!jwtHasRealUser) {
			boolean local = this.environment.acceptsProfiles(Profiles.of("local"));
			if (local) {
				companyIdForFilter = 1L;
				String uidQ = request.getParameter("user_id");
				if (StringUtils.hasText(uidQ)) {
					userIdForLike = parseLongFlexible(uidQ, 0L);
				}
			}
		}

		Map<String, Object> data =
				frontUgcCommentListService.buildList(
						parsedPostId, parentCid, pageNo, pageSize, userIdForLike, companyIdForFilter, "zh-CN");
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
