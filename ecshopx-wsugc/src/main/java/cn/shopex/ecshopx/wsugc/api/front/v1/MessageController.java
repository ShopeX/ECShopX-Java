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
import cn.shopex.ecshopx.wsugc.service.message.FrontUgcMessageDashboardService;
import cn.shopex.ecshopx.wsugc.service.message.FrontUgcMessageDetailService;
import cn.shopex.ecshopx.wsugc.service.message.FrontUgcMessageListService;
import cn.shopex.ecshopx.wsugc.service.message.FrontUgcMessageMarkReadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@FrontAuth
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true)
@RestController("wsugcFrontV1Message")
@RequestMapping("/api/v1/h5app")
public class MessageController {

	private final FrontUgcMessageMarkReadService frontUgcMessageMarkReadService;
	private final FrontUgcMessageDashboardService frontUgcMessageDashboardService;
	private final FrontUgcMessageDetailService frontUgcMessageDetailService;
	private final FrontUgcMessageListService frontUgcMessageListService;
	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final Environment environment;

	public MessageController(
			FrontUgcMessageMarkReadService frontUgcMessageMarkReadService,
			FrontUgcMessageDashboardService frontUgcMessageDashboardService,
			FrontUgcMessageDetailService frontUgcMessageDetailService,
			FrontUgcMessageListService frontUgcMessageListService,
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			Environment environment) {
		this.frontUgcMessageMarkReadService = frontUgcMessageMarkReadService;
		this.frontUgcMessageDashboardService = frontUgcMessageDashboardService;
		this.frontUgcMessageDetailService = frontUgcMessageDetailService;
		this.frontUgcMessageListService = frontUgcMessageListService;
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.environment = environment;
	}

	@GetMapping(value = "/wxapp/ugc/message/dashboard", name = "消息桌面")
	public ResponseEntity<ApiResult<Map<String, Object>>> getMessageDashboard(HttpServletRequest request) {
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
			throw new ResourceException("会员id不能为空！", 422);
		}

		long companyId = parseLongFlexible(claims.get("company_id"), 1L);
		if (companyId <= 0L) {
			companyId = 1L;
		}

		List<Map<String, Object>> messageInfo =
				frontUgcMessageDashboardService.buildMessageDashboard(companyId, authUserId);

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("dashboard_info", null);
		data.put("message_info", messageInfo);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/ugc/message/list", name = "消息列表")
	public ResponseEntity<ApiResult<Object>> getMessageList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false) String page,
			@RequestParam(value = "pageSize", required = false) String pageSize,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "sort", required = false) String sort) {
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
			throw new ResourceException("会员id不能为空！", 422);
		}

		long companyId = parseLongFlexible(claims.get("company_id"), 1L);
		if (companyId <= 0L) {
			companyId = 1L;
		}

		int pageNo = parsePageQueryInt(page, 1);
		int pageSz = parsePageQueryInt(pageSize, 30);

		String typeFilter = null;
		if (type != null && StringUtils.hasText(type.trim())) {
			typeFilter = type.trim();
		}

		Object data =
				frontUgcMessageListService.buildMessageList(
						companyId, authUserId, typeFilter, pageNo, pageSz, sort, "zh-CN");

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/wxapp/ugc/message/setTohasRead", name = "设置已读")
	public ResponseEntity<ApiResult<Map<String, Object>>> setTohasRead(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		long userId = 0L;
		Object claimUid = claims.get("user_id");
		if (claimUid != null && authUserIdTruthy(claimUid)) {
			userId = parseLongFlexible(claimUid, 0L);
		}

		boolean local = this.environment.acceptsProfiles(Profiles.of("local"));
		if (userId <= 0L && local && merged.containsKey("user_id")) {
			userId = parseLongFlexible(merged.get("user_id"), 0L);
		}

		if (userId <= 0L) {
			throw new UnauthorizedException("只有会员才可以设置已读");
		}

		Object typeRaw = merged.get("type");
		String type = typeRaw == null ? "" : typeRaw.toString().trim();
		if (!StringUtils.hasText(type)) {
			throw new BadRequestException("消息类型type不能为空");
		}

		frontUgcMessageMarkReadService.markRead(userId, type);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("hasread", 1);
		data.put("message", "设置已读成功");
		data.put("to_user_id", userId);
		data.put("type", type);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/wxapp/ugc/message/detail", name = "消息详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getMessageDetail(
			HttpServletRequest request,
			@RequestParam(value = "message_id", required = false) String messageId) {
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
			throw new ResourceException("会员id不能为空！", 422);
		}

		long companyId = parseLongFlexible(claims.get("company_id"), 1L);
		if (companyId <= 0L) {
			companyId = 1L;
		}

		Map<String, Object> messageInfo =
				frontUgcMessageDetailService.buildMessageDetail(companyId, messageId, "zh-CN");

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("message_info", messageInfo);
		Map<String, Object> sorted = new LinkedHashMap<>(new TreeMap<>(data));
		return ResponseEntity.ok(ApiResult.ok(sorted));
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

	private static int parsePageQueryInt(String raw, int defaultVal) {
		if (raw == null || !StringUtils.hasText(raw.trim())) {
			return defaultVal;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return v <= 0 ? defaultVal : v;
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
