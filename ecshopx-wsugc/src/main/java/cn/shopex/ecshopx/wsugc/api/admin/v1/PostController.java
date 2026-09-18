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
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.wsugc.service.post.PostCreateService;
import cn.shopex.ecshopx.wsugc.service.post.PostDeleteService;
import cn.shopex.ecshopx.wsugc.service.post.PostDetailService;
import cn.shopex.ecshopx.wsugc.service.post.PostEditService;
import cn.shopex.ecshopx.wsugc.service.post.PostListService;
import cn.shopex.ecshopx.wsugc.service.post.PostSetBadgesService;
import cn.shopex.ecshopx.wsugc.service.post.PostVerifyService;
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
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422)
@RestController("wsugcAdminV1Post")
@RequestMapping("/api/v1/ugc/post")
public class PostController {

	private final PostCreateService postCreateService;
	private final PostEditService postEditService;
	private final PostSetBadgesService postSetBadgesService;
	private final PostVerifyService postVerifyService;
	private final PostDetailService postDetailService;
	private final PostListService postListService;
	private final PostDeleteService postDeleteService;
	private final Environment environment;

	public PostController(
			PostCreateService postCreateService,
			PostEditService postEditService,
			PostSetBadgesService postSetBadgesService,
			PostVerifyService postVerifyService,
			PostDetailService postDetailService,
			PostListService postListService,
			PostDeleteService postDeleteService,
			Environment environment) {
		this.postCreateService = postCreateService;
		this.postEditService = postEditService;
		this.postSetBadgesService = postSetBadgesService;
		this.postVerifyService = postVerifyService;
		this.postDetailService = postDetailService;
		this.postListService = postListService;
		this.postDeleteService = postDeleteService;
		this.environment = environment;
	}

	@PostMapping(value = "/create", name = "发布笔记")
	public ResponseEntity<ApiResult<Map<String, Object>>> createPost(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
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
		String ip = clientIp(request);
		Map<String, Object> data = postCreateService.createOrUpdate(merged, operatorJwt, ip, local);
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

	@PostMapping(value = "/edit", name = "笔记编辑")
	public ResponseEntity<ApiResult<Map<String, Object>>> editPost(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
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
		Map<String, Object> data = postEditService.edit(merged, operatorJwt, local);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/setBadges", name = "批量角标")
	public ResponseEntity<ApiResult<Map<String, Object>>> setBadges(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeSetBadgesInput(request, body);
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
		Map<String, Object> data = postSetBadgesService.setBadges(merged, operatorJwt, local);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/verify", name = "审核")
	public ResponseEntity<ApiResult<Map<String, Object>>> verifyPost(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeVerifyPostInput(request, body);
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
		Map<String, Object> data = postVerifyService.verify(merged, operatorJwt, local);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private Map<String, Object> mergeSetBadgesInput(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			return mergeInputLikeFlexibleResolver(request, body);
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(parameterMapToMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		List<Long> formIds = dedupeFormPostIds(request);
		if (!formIds.isEmpty()) {
			if (formIds.size() > 1) {
				merged.put("post_id", formIds);
			} else {
				merged.put("post_id", formIds.get(0));
			}
		}
		List<Object> formBadges = dedupeFormBadges(request);
		if (!formBadges.isEmpty()) {
			merged.put("badges", formBadges);
		}
		return merged;
	}

	private static List<Object> dedupeFormBadges(HttpServletRequest request) {
		LinkedHashSet<String> seenKeys = new LinkedHashSet<>();
		List<Object> out = new ArrayList<>();
		appendFormBadgeParams(request, "badges", seenKeys, out);
		appendFormBadgeParams(request, "badges[]", seenKeys, out);
		return out;
	}

	private static void appendFormBadgeParams(
			HttpServletRequest request,
			String paramName,
			LinkedHashSet<String> seenKeys,
			List<Object> out) {
		String[] vals = request.getParameterValues(paramName);
		if (vals == null) {
			return;
		}
		for (String raw : vals) {
			if (!StringUtils.hasText(raw)) {
				continue;
			}
			String t = raw.trim();
			try {
				long n = Long.parseLong(t);
				String key = "n:" + n;
				if (seenKeys.add(key)) {
					out.add(n);
				}
			} catch (NumberFormatException e) {
				String key = "s:" + t;
				if (seenKeys.add(key)) {
					out.add(t);
				}
			}
		}
	}

	private Map<String, Object> mergeDeletePostInput(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			return mergeInputLikeFlexibleResolver(request, body);
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(parameterMapToMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		List<Long> formIds = dedupeFormPostIds(request);
		if (!formIds.isEmpty()) {
			if (formIds.size() > 1) {
				merged.put("post_id", formIds);
			} else {
				merged.put("post_id", formIds.get(0));
			}
		}
		return merged;
	}

	private Map<String, Object> mergeVerifyPostInput(HttpServletRequest request, Map<String, Object> body) {
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
		List<Long> formIds = dedupeFormPostIds(request);
		if (!formIds.isEmpty()) {
			if (formIds.size() > 1) {
				merged.put("post_id", formIds);
			} else {
				merged.put("post_id", formIds.get(0));
			}
		}
		return merged;
	}

	private static List<Long> dedupeFormPostIds(HttpServletRequest request) {
		Set<Long> set = new LinkedHashSet<>();
		set.addAll(collectFormLongs(request, "post_id"));
		set.addAll(collectFormLongs(request, "post_id[]"));
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
				throw new BadRequestException("post_id 格式无效");
			}
		}
		return out;
	}

	@PostMapping(value = "/enable", name = "发布上架")
	public ResponseEntity<Void> enablePost() {
		return ResponseEntity.ok().build();
	}

	@Activated(routeAlias = "ugc.post.list")
	@GetMapping(value = "/list", name = "笔记列表")
	public ResponseEntity<ApiResult<Object>> getPostList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "30") int pageSize,
			@RequestParam(value = "source", required = false) String source,
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
		List<Long> topicIds = collectTopicIds(request);
		List<Long> badgeIds = collectBadgeIds(request);
		Object body =
				postListService.buildList(
						companyId,
						langTag,
						page,
						pageSize,
						source,
						status,
						topicIds,
						badgeIds,
						nickname,
						mobile,
						content,
						sort);
		if (body instanceof List<?> list && list.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	private static List<Long> collectTopicIds(HttpServletRequest request) {
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		addParsedLongs(request.getParameterValues("topics"), ids);
		addParsedLongs(request.getParameterValues("topics[]"), ids);
		return new ArrayList<>(ids);
	}

	private static List<Long> collectBadgeIds(HttpServletRequest request) {
		LinkedHashSet<Long> ids = new LinkedHashSet<>();
		addParsedLongs(request.getParameterValues("badges"), ids);
		addParsedLongs(request.getParameterValues("badges[]"), ids);
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

	@Activated(routeAlias = "ugc.post.detail")
	@GetMapping(value = "/detail", name = "笔记详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPostDetail(
			HttpServletRequest request,
			@RequestParam(value = "post_id", required = false) String postId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		String langTag = resolveAdminLangTag(operatorJwt);
		Map<String, Object> data = postDetailService.buildResponse(postId, operatorJwt, langTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String resolveAdminLangTag(Map<String, Object> operatorJwt) {
		Object cc = operatorJwt.get("country_code");
		if (cc != null && StringUtils.hasText(cc.toString())) {
			return cc.toString().trim();
		}
		return "zh-CN";
	}

	@PostMapping(value = "/delete", name = "删除笔记")
	public ResponseEntity<ApiResult<Map<String, Object>>> deletePost(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeDeletePostInput(request, body);
		Object rawPostId = merged.get("post_id");
		Map<String, Object> data = postDeleteService.softDeleteByPostId(rawPostId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/settop", name = "话题置顶")
	public ResponseEntity<Void> setTopTopic() {
		return ResponseEntity.ok().build();
	}
}
