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
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.members.service.h5.H5BearerJwtClaimsService;
import cn.shopex.ecshopx.wsugc.service.follower.FrontUgcFollowerCreateService;
import cn.shopex.ecshopx.wsugc.service.follower.FrontUgcFollowerListService;
import cn.shopex.ecshopx.wsugc.service.follower.FrontUgcFollowerStatService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
		unauthorized = true)
@RestController("wsugcFrontV1Follower")
@RequestMapping("/api/v1/h5app")
public class FollowerController {

	private final FrontUgcFollowerCreateService frontUgcFollowerCreateService;
	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final Environment environment;
	private final FrontUgcFollowerListService frontUgcFollowerListService;
	private final FrontUgcFollowerStatService frontUgcFollowerStatService;

	public FollowerController(
			FrontUgcFollowerCreateService frontUgcFollowerCreateService,
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			Environment environment,
			FrontUgcFollowerListService frontUgcFollowerListService,
			FrontUgcFollowerStatService frontUgcFollowerStatService) {
		this.frontUgcFollowerCreateService = frontUgcFollowerCreateService;
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.environment = environment;
		this.frontUgcFollowerListService = frontUgcFollowerListService;
		this.frontUgcFollowerStatService = frontUgcFollowerStatService;
	}

	@PostMapping(value = "/wxapp/ugc/follower/create", name = "关注取消关注")
	public ResponseEntity<ApiResult<Map<String, Object>>> createFollow(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		long followerUserId = 0L;
		long companyId = 0L;
		Object claimUid = claims.get("user_id");
		if (claimUid != null && authUserIdTruthy(claimUid)) {
			followerUserId = parseLongFlexible(claimUid, 0L);
			companyId = parseLongFlexible(claims.get("company_id"), 0L);
		}

		boolean local = this.environment.acceptsProfiles(Profiles.of("local"));
		if (followerUserId <= 0L && local && merged.containsKey("follower_user_id")) {
			followerUserId = parseLongFlexible(merged.get("follower_user_id"), 0L);
			companyId = 1L;
		}

		if (followerUserId <= 0L) {
			throw new ResourceException("粉丝id不能为空！");
		}

		if (!merged.containsKey("user_id")) {
			throw new BadRequestException("缺少必填字段: user_id");
		}

		long bloggerUserId = parseLongFlexible(merged.get("user_id"), 0L);
		if (bloggerUserId == followerUserId) {
			throw new ResourceException("不能关注自己！");
		}

		Map<String, Object> data =
				frontUgcFollowerCreateService.create(merged, bloggerUserId, followerUserId, companyId);
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

	@GetMapping(value = "/wxapp/ugc/follower/list", name = "关注粉丝列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getFollowerList(HttpServletRequest request) {
		Map<String, Object> claims = h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long jwtSubjectUserId = 0L;
		Object claimUid = claims.get("user_id");
		if (claimUid != null && authUserIdTruthy(claimUid)) {
			jwtSubjectUserId = parseLongFlexible(claimUid, 0L);
		}

		boolean local = this.environment.acceptsProfiles(Profiles.of("local"));
		if (jwtSubjectUserId <= 0L && local) {
			String localUid = request.getParameter("user_id");
			if (StringUtils.hasText(localUid)) {
				jwtSubjectUserId = parseLongFlexible(localUid, 0L);
			}
		}

		if (jwtSubjectUserId <= 0L) {
			throw new ResourceException("会员id不能为空！");
		}

		Long listUserId = jwtSubjectUserId;
		String q = request.getParameter("user_id");
		if (!(q != null && q.isEmpty())) {
			if (q == null) {
				listUserId = null;
			} else {
				listUserId = parseLongFlexible(q.trim(), jwtSubjectUserId);
			}
		}

		long companyIdFilter = parseLongFlexible(request.getParameter("company_id"), 1L);
		if (companyIdFilter <= 0L) {
			companyIdFilter = 1L;
		}

		int pageNo = 1;
		String pno = request.getParameter("page_no");
		if (StringUtils.hasText(pno)) {
			try {
				int v = Integer.parseInt(pno.trim());
				if (v > 0) {
					pageNo = v;
				}
			} catch (NumberFormatException ignored) {
				// keep default
			}
		}
		int pageSize = 20;
		String psz = request.getParameter("page_size");
		if (StringUtils.hasText(psz)) {
			try {
				int v = Integer.parseInt(psz.trim());
				if (v > 0 && v <= 50) {
					pageSize = v;
				}
			} catch (NumberFormatException ignored) {
				// keep default
			}
		}

		String userType = "user".equals(request.getParameter("user_type")) ? "user" : "follower";

		if (listUserId == null) {
			throw new ResourceException("用户信息不完整");
		}

		Map<String, Object> data =
				frontUgcFollowerListService.buildList(listUserId, companyIdFilter, userType, pageNo, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/ugc/follower/stat", name = "粉丝统计")
	public ResponseEntity<ApiResult<Map<String, Object>>> getFollowerStat(HttpServletRequest request) {
		Map<String, Object> claims = h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long authUserId = 0L;
		Object claimUid = claims.get("user_id");
		if (claimUid != null && authUserIdTruthy(claimUid)) {
			authUserId = parseLongFlexible(claimUid, 0L);
		}
		if (authUserId <= 0L) {
			throw new ResourceException("会员id不能为空！");
		}

		long companyId = parseLongFlexible(claims.get("company_id"), 1L);
		if (companyId <= 0L) {
			companyId = 1L;
		}

		long targetUserId = authUserId;
		String q = request.getParameter("user_id");
		if (q != null) {
			String t = q.trim();
			if (!t.isEmpty() && !"0".equals(t)) {
				targetUserId = parseLongFlexible(t, authUserId);
			}
		}
		if (targetUserId <= 0L) {
			throw new ResourceException("会员id不能为空！");
		}

		Map<String, Object> data =
				frontUgcFollowerStatService.buildStat(request, authUserId, companyId, targetUserId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}
}
