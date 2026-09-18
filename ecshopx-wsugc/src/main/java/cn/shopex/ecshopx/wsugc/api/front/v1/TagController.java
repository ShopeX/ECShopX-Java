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
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.members.service.h5.H5BearerJwtClaimsService;
import cn.shopex.ecshopx.wsugc.service.tag.FrontUgcTagCreateService;
import cn.shopex.ecshopx.wsugc.service.tag.FrontUgcTagListService;
import cn.shopex.ecshopx.wsugc.service.tag.TagDetailService;
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

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true)
@RestController("wsugcFrontV1Tag")
@RequestMapping("/api/v1/h5app")
public class TagController {

	private final FrontUgcTagCreateService frontUgcTagCreateService;
	private final TagDetailService tagDetailService;
	private final FrontUgcTagListService frontUgcTagListService;
	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final Environment environment;

	public TagController(
			FrontUgcTagCreateService frontUgcTagCreateService,
			TagDetailService tagDetailService,
			FrontUgcTagListService frontUgcTagListService,
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			Environment environment) {
		this.frontUgcTagCreateService = frontUgcTagCreateService;
		this.tagDetailService = tagDetailService;
		this.frontUgcTagListService = frontUgcTagListService;
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.environment = environment;
	}

	@FrontAuth
	@PostMapping(value = "/wxapp/ugc/tag/create", name = "创建图片标签")
	public ResponseEntity<ApiResult<Map<String, Object>>> createTag(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> params = extractFilterParams(merged);

		long userId = 0L;
		long companyId = parseLongFlexible(claims.get("company_id"), 1L);
		Object claimUid = claims.get("user_id");
		if (claimUid != null && authUserIdTruthy(claimUid)) {
			userId = parseLongFlexible(claimUid, 0L);
		}

		boolean local = this.environment.acceptsProfiles(Profiles.of("local"));
		if (userId <= 0L && local && params.containsKey("user_id")) {
			userId = parseLongFlexible(params.get("user_id"), 0L);
		}

		if (userId <= 0L) {
			throw new ResourceException("只有会员才可以发布图片标签");
		}

		Object mob = claims.get("mobile");
		String mobile = mob == null ? "0" : mob.toString();

		Map<String, Object> data = frontUgcTagCreateService.createForMember(params, userId, companyId, mobile);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Map<String, Object> extractFilterParams(Map<String, Object> merged) {
		Map<String, Object> params = new LinkedHashMap<>();
		if (merged.containsKey("tag_id")) {
			params.put("tag_id", merged.get("tag_id"));
		}
		if (merged.containsKey("tag_name")) {
			params.put("tag_name", merged.get("tag_name"));
		}
		if (merged.containsKey("user_id")) {
			params.put("user_id", merged.get("user_id"));
		}
		return params;
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

	@FrontNoAuth
	@GetMapping(value = "/wxapp/ugc/tag/detail", name = "标签详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getTagDetail(
			HttpServletRequest request,
			@RequestParam(value = "tag_id", required = false) String tagId) {
		Object cidAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (cidAttr instanceof Integer i && i > 0) {
			companyId = i.longValue();
		} else if (cidAttr instanceof Long l && l > 0L) {
			companyId = l;
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		Map<String, Object> inner = tagDetailService.buildH5TagDetailResponse(companyId, tagId);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>(inner);
		Map<String, Object> data = new LinkedHashMap<>(new TreeMap<>(body));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@GetMapping(value = "/wxapp/ugc/tag/list", name = "标签列表")
	public ResponseEntity<ApiResult<Object>> getTagList(
			HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "30") int pageSize,
			@RequestParam(value = "tag_name", required = false) String tagName,
			@RequestParam(value = "sort", required = false) String sort) {
		Object cidAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (cidAttr instanceof Integer i && i > 0) {
			companyId = i.longValue();
		} else if (cidAttr instanceof Long l && l > 0L) {
			companyId = l;
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		Object raw = frontUgcTagListService.buildH5TagList(companyId, page, pageSize, tagName, sort);
		if (raw instanceof List<?>) {
			return ResponseEntity.ok(ApiResult.ok(raw));
		}
		if (raw instanceof Map<?, ?> rawMap) {
			TreeMap<String, Object> sorted = new TreeMap<>();
			for (Map.Entry<?, ?> e : rawMap.entrySet()) {
				Object k = e.getKey();
				if (k instanceof String sk) {
					sorted.put(sk, e.getValue());
				}
			}
			return ResponseEntity.ok(ApiResult.ok(new LinkedHashMap<>(sorted)));
		}
		throw new IllegalStateException("unexpected tag list payload type");
	}
}
