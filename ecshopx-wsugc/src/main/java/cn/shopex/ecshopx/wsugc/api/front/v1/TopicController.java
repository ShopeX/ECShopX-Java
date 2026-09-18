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
import cn.shopex.ecshopx.wsugc.service.topic.FrontUgcTopicCreateService;
import cn.shopex.ecshopx.wsugc.service.topic.FrontUgcTopicListService;
import cn.shopex.ecshopx.wsugc.service.topic.TopicDetailService;
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
@RestController("wsugcFrontV1Topic")
@RequestMapping("/api/v1/h5app")
public class TopicController {

	private final FrontUgcTopicCreateService frontUgcTopicCreateService;
	private final FrontUgcTopicListService frontUgcTopicListService;
	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final TopicDetailService topicDetailService;
	private final Environment environment;

	public TopicController(
			FrontUgcTopicCreateService frontUgcTopicCreateService,
			FrontUgcTopicListService frontUgcTopicListService,
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			TopicDetailService topicDetailService,
			Environment environment) {
		this.frontUgcTopicCreateService = frontUgcTopicCreateService;
		this.frontUgcTopicListService = frontUgcTopicListService;
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.topicDetailService = topicDetailService;
		this.environment = environment;
	}

	@FrontAuth
	@PostMapping(value = "/wxapp/ugc/topic/create", name = "创建话题")
	public ResponseEntity<ApiResult<Map<String, Object>>> createTopic(
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
			throw new ResourceException("只有会员才可以创建话题");
		}

		Object mob = claims.get("mobile");
		String mobile = mob == null ? "0" : mob.toString();

		Map<String, Object> data = frontUgcTopicCreateService.createForMember(params, userId, companyId, mobile);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Map<String, Object> extractFilterParams(Map<String, Object> merged) {
		Map<String, Object> params = new LinkedHashMap<>();
		if (merged.containsKey("topic_id")) {
			params.put("topic_id", merged.get("topic_id"));
		}
		if (merged.containsKey("topic_name")) {
			params.put("topic_name", merged.get("topic_name"));
		}
		if (merged.containsKey("user_id")) {
			params.put("user_id", merged.get("user_id"));
		}
		if (merged.containsKey("company_id")) {
			params.put("company_id", merged.get("company_id"));
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
	@GetMapping(value = "/wxapp/ugc/topic/detail", name = "话题详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getTopicDetail(
			HttpServletRequest request,
			@RequestParam(value = "topic_id", required = false) String topicId) {
		Object cidAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (cidAttr instanceof Integer i && i > 0) {
			companyId = i.longValue();
		} else if (cidAttr instanceof Long l && l > 0L) {
			companyId = l;
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		final String langTag = "zh-CN";
		LinkedHashMap<String, Object> inner = topicDetailService.buildH5TopicDetailRowMap(companyId, topicId, langTag);

		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("topic_info", null);
		if (inner != null && !inner.isEmpty()) {
			Object st = inner.get("status");
			if (!Integer.valueOf(1).equals(normalizedTopicStatus(st))) {
				body.put("topic_info", List.of());
				body.put("message", "话题的状态不是已审核");
			} else {
				body.put("topic_info", inner);
			}
		}

		Map<String, Object> data = new LinkedHashMap<>(new TreeMap<>(body));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Integer normalizedTopicStatus(Object st) {
		if (st == null) {
			return null;
		}
		if (st instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(st.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@FrontNoAuth
	@GetMapping(value = "/wxapp/ugc/topic/list", name = "话题列表")
	public ResponseEntity<ApiResult<Object>> getTopicList(
			HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "30") int pageSize,
			@RequestParam(value = "topic_name", required = false) String topicName,
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

		Object raw = frontUgcTopicListService.buildH5TopicList(companyId, page, pageSize, topicName, sort);
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
		throw new IllegalStateException("unexpected topic list payload type");
	}
}
