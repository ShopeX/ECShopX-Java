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
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.h5.H5BearerJwtClaimsService;
import cn.shopex.ecshopx.wsugc.service.post.FrontUgcPostDetailEmbedService;
import cn.shopex.ecshopx.wsugc.service.post.FrontUgcPostFavoriteService;
import cn.shopex.ecshopx.wsugc.service.post.FrontUgcPostListService;
import cn.shopex.ecshopx.wsugc.service.post.FrontUgcPostLikeService;
import cn.shopex.ecshopx.wsugc.service.post.FrontUgcPostCreateService;
import cn.shopex.ecshopx.wsugc.service.post.FrontUgcPostDeleteService;
import cn.shopex.ecshopx.wsugc.service.post.FrontUgcPostShareService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
		unauthorized = true,
		notFound = false)
@RestController("wsugcFrontV1Post")
@RequestMapping("/api/v1/h5app")
public class PostController {

	private static final String[] CREATE_POST_WHITELIST = {
		"post_id", "topic_id", "title", "content", "cover", "images", "image_tag", "image_path",
		"topics", "video", "video_ratio", "video_place", "video_thumb", "is_draft", "is_top",
		"p_order", "badges", "user_id", "goods"
	};

	private final FrontUgcPostFavoriteService frontUgcPostFavoriteService;
	private final FrontUgcPostLikeService frontUgcPostLikeService;
	private final FrontUgcPostShareService frontUgcPostShareService;
	private final FrontUgcPostDetailEmbedService frontUgcPostDetailEmbedService;
	private final FrontUgcPostListService frontUgcPostListService;
	private final FrontUgcPostDeleteService frontUgcPostDeleteService;
	private final FrontUgcPostCreateService frontUgcPostCreateService;
	private final H5BearerJwtClaimsService h5BearerJwtClaimsService;
	private final MemberAccountService memberAccountService;
	private final ObjectMapper objectMapper;
	private final Environment environment;

	public PostController(
			FrontUgcPostFavoriteService frontUgcPostFavoriteService,
			FrontUgcPostLikeService frontUgcPostLikeService,
			FrontUgcPostShareService frontUgcPostShareService,
			FrontUgcPostDetailEmbedService frontUgcPostDetailEmbedService,
			FrontUgcPostListService frontUgcPostListService,
			FrontUgcPostDeleteService frontUgcPostDeleteService,
			FrontUgcPostCreateService frontUgcPostCreateService,
			H5BearerJwtClaimsService h5BearerJwtClaimsService,
			MemberAccountService memberAccountService,
			ObjectMapper objectMapper,
			Environment environment) {
		this.frontUgcPostFavoriteService = frontUgcPostFavoriteService;
		this.frontUgcPostLikeService = frontUgcPostLikeService;
		this.frontUgcPostShareService = frontUgcPostShareService;
		this.frontUgcPostDetailEmbedService = frontUgcPostDetailEmbedService;
		this.frontUgcPostListService = frontUgcPostListService;
		this.frontUgcPostDeleteService = frontUgcPostDeleteService;
		this.frontUgcPostCreateService = frontUgcPostCreateService;
		this.h5BearerJwtClaimsService = h5BearerJwtClaimsService;
		this.memberAccountService = memberAccountService;
		this.objectMapper = objectMapper;
		this.environment = environment;
	}

	@FrontAuth
	@PostMapping(value = "/wxapp/ugc/post/create", name = "发布笔记")
	public ResponseEntity<ApiResult<Map<String, Object>>> createPost(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeCreatePostInput(request, body);
		normalizeCreatePostArrayKeys(merged, objectMapper);

		LinkedHashMap<String, Object> whitelistMap = new LinkedHashMap<>();
		for (String k : CREATE_POST_WHITELIST) {
			if (merged.containsKey(k)) {
				whitelistMap.put(k, merged.get(k));
			}
		}

		Map<String, Object> claims = h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long userId = 0L;
		long companyId = 0L;
		Object claimUid = claims.get("user_id");
		if (claimUid != null && authUserIdTruthy(claimUid)) {
			userId = parseLongFlexible(claimUid, 0L);
			companyId = parseLongFlexible(claims.get("company_id"), 0L);
		}
		if (companyId <= 0L) {
			companyId = 1L;
		}
		if (userId <= 0L) {
			throw new ResourceException("只有会员才可以发布笔记");
		}

		int isDraft = 0;
		if (whitelistMap.containsKey("is_draft")) {
			Integer id = parseIntLoose(whitelistMap.get("is_draft"));
			if (id != null && id == 1) {
				isDraft = 1;
			}
		}
		if (isDraft != 1) {
			Object cover = whitelistMap.get("cover");
			if (cover == null || !StringUtils.hasText(cover.toString().trim())) {
				throw new ResourceException("封面图不能为空");
			}
			if (!whitelistMap.containsKey("images")) {
				throw new ResourceException("images参数不能为空");
			}
			Object imgs = whitelistMap.get("images");
			if (!(imgs instanceof List<?> list) || list.isEmpty()) {
				throw new ResourceException("images参数不能为空");
			}
			if (!whitelistMap.containsKey("image_path")) {
				throw new ResourceException("image_path参数不能为空");
			}
			Object paths = whitelistMap.get("image_path");
			if (!(paths instanceof List<?> plist) || plist.isEmpty()) {
				throw new ResourceException("image_path参数不能为空");
			}
		}

		String mobileForPost = resolveMemberMobileForUgcPost(claims, userId, companyId);
		String clientIp = request.getRemoteAddr();

		Map<String, Object> data = frontUgcPostCreateService.createOrUpdateByMember(
				whitelistMap, userId, mobileForPost, companyId, clientIp);
		Map<String, Object> sorted = new LinkedHashMap<>(new TreeMap<>(data));
		return ResponseEntity.ok(ApiResult.ok(sorted));
	}

	@FrontAuth
	@SuppressWarnings("unused")
	@PostMapping(value = "/wxapp/ugc/post/delete", name = "删除笔记")
	public ResponseEntity<ApiResult<Map<String, Object>>> deletePost(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> claims = h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		Map<String, Object> merged = mergeDeletePostInput(request, body);

		long userId = 0L;
		long companyId = 0L;
		Object claimUid = claims.get("user_id");
		if (claimUid != null && authUserIdTruthy(claimUid)) {
			userId = parseLongFlexible(claimUid, 0L);
			companyId = parseLongFlexible(claims.get("company_id"), 0L);
		}

		if (userId <= 0L && environment.acceptsProfiles(Profiles.of("local")) && merged.containsKey("user_id")) {
			userId = parseLongFlexible(merged.get("user_id"), 0L);
			companyId = 1L;
		}

		if (companyId <= 0L) {
			companyId = 1L;
		}

		if (userId <= 0L) {
			throw new UnauthorizedException("未登录不可以删除笔记");
		}

		Object rawPostId = merged.get("post_id");
		Map<String, Object> data = frontUgcPostDeleteService.deleteByMember(userId, rawPostId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@PostMapping(value = "/wxapp/ugc/post/share", name = "分享笔记")
	public ResponseEntity<ApiResult<Map<String, Object>>> sharePost(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> claims = h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long userId = 0L;
		long companyId = 0L;
		Object claimUid = claims.get("user_id");
		if (claimUid != null && authUserIdTruthy(claimUid)) {
			userId = parseLongFlexible(claimUid, 0L);
			companyId = parseLongFlexible(claims.get("company_id"), 0L);
		}

		if (userId <= 0L && environment.acceptsProfiles(Profiles.of("local")) && merged.containsKey("user_id")) {
			userId = parseLongFlexible(merged.get("user_id"), 0L);
			companyId = 1L;
		}

		if (userId <= 0L) {
			throw new ResourceException("只有会员才可以分享笔记");
		}
		if (companyId <= 0L) {
			companyId = 1L;
		}

		if (!merged.containsKey("post_id") || parseLongFlexible(merged.get("post_id"), 0L) <= 0L) {
			throw new ResourceException("post_id参数不能为空");
		}
		long postId = parseLongFlexible(merged.get("post_id"), 0L);

		int shareNums = frontUgcPostShareService.share(userId, postId, companyId);

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("post_id", merged.containsKey("post_id") ? merged.get("post_id") : postId);
		data.put("user_id", userId);
		data.put("company_id", companyId);
		data.put("share_nums", shareNums);
		data.put("message", "分享成功");
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontAuth
	@PostMapping(value = "/wxapp/ugc/post/like", name = "笔记点赞")
	public ResponseEntity<ApiResult<Map<String, Object>>> likePost(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		Map<String, Object> claims = h5BearerJwtClaimsService.verifyAndExtractClaims(request)
				.orElseThrow(() -> new UnauthorizedException("无权访问该API,非法访问！"));
		if (isAccountDisabled(claims.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		long userId = 0L;
		long companyId = 0L;
		Object claimUid = claims.get("user_id");
		if (claimUid != null && authUserIdTruthy(claimUid)) {
			userId = parseLongFlexible(claimUid, 0L);
			companyId = parseLongFlexible(claims.get("company_id"), 0L);
		}

		if (userId <= 0L && environment.acceptsProfiles(Profiles.of("local")) && merged.containsKey("user_id")) {
			userId = parseLongFlexible(merged.get("user_id"), 0L);
			companyId = 1L;
		}

		if (userId <= 0L || companyId <= 0L) {
			throw new ResourceException("会员id不能为空！");
		}

		if (!merged.containsKey("post_id") || parseLongFlexible(merged.get("post_id"), 0L) <= 0L) {
			throw new ResourceException("笔记id不能为空！");
		}
		long postId = parseLongFlexible(merged.get("post_id"), 0L);

		Map<String, Object> data = frontUgcPostLikeService.like(userId, postId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@GetMapping(value = "/wxapp/ugc/post/detail", name = "笔记详情免登")
	public ResponseEntity<ApiResult<Map<String, Object>>> getPostDetail(
			HttpServletRequest request,
			@RequestParam(value = "post_id", required = false) String postId) {
		Object cidAttr = request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID);
		long companyId;
		if (cidAttr instanceof Integer i && i > 0) {
			companyId = i.longValue();
		} else if (cidAttr instanceof Long l && l > 0L) {
			companyId = l;
		} else {
			throw new UnauthorizedException("无权访问该API,非法访问！");
		}

		long visitorUserId = 0L;
		Optional<Map<String, Object>> claimsOpt = h5BearerJwtClaimsService.verifyAndExtractClaims(request);
		if (claimsOpt.isPresent()) {
			Map<String, Object> claims = claimsOpt.get();
			Object claimUid = claims.get("user_id");
			if (claimUid != null && authUserIdTruthy(claimUid)) {
				visitorUserId = parseLongFlexible(claimUid, 0L);
			}
		}

		final String langTag = "zh-CN";
		Map<String, Object> postInfo =
				frontUgcPostDetailEmbedService.buildPostInfoForH5Detail(companyId, postId, visitorUserId, langTag);

		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("post_info", postInfo);
		Map<String, Object> data = new LinkedHashMap<>(new TreeMap<>(body));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@FrontNoAuth
	@GetMapping(value = "/wxapp/ugc/post/list", name = "笔记列表免登")
	public ResponseEntity<ApiResult<Object>> getPostList(
			HttpServletRequest request,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "pageSize", defaultValue = "30") int pageSize,
			@RequestParam(value = "source", required = false) String source,
			@RequestParam(value = "content", required = false) String content,
			@RequestParam(value = "topics", required = false) List<Long> topicIds,
			@RequestParam(value = "user_id", required = false) String profileUserIdRaw,
			@RequestParam(value = "is_draft", required = false) String isDraftRaw,
			@RequestParam(value = "searchType", required = false) String searchType,
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

		long visitorUserId = 0L;
		Optional<Map<String, Object>> claimsOpt = h5BearerJwtClaimsService.verifyAndExtractClaims(request);
		if (claimsOpt.isPresent()) {
			Map<String, Object> claims = claimsOpt.get();
			Object claimUid = claims.get("user_id");
			if (claimUid != null && authUserIdTruthy(claimUid)) {
				visitorUserId = parseLongFlexible(claimUid, 0L);
			}
		}

		final String langTag = "zh-CN";
		Optional<Map<String, Object>> bodyOpt =
				frontUgcPostListService.buildH5List(
						companyId,
						visitorUserId,
						langTag,
						page,
						pageSize,
						source,
						content,
						topicIds,
						profileUserIdRaw,
						isDraftRaw,
						searchType,
						sort);
		if (bodyOpt.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		return ResponseEntity.ok(
				ApiResult.ok(new LinkedHashMap<>(new TreeMap<>(bodyOpt.get()))));
	}

	@FrontNoAuth
	@PostMapping(value = "/wxapp/ugc/post/favorite", name = "笔记收藏")
	public ResponseEntity<ApiResult<Map<String, Object>>> favoritePost(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);

		long userId = 0L;
		Optional<Map<String, Object>> claimsOpt = h5BearerJwtClaimsService.verifyAndExtractClaims(request);
		if (claimsOpt.isPresent()) {
			Map<String, Object> claims = claimsOpt.get();
			if (isAccountDisabled(claims.get("disabled"))) {
				throw new UnauthorizedException("该账号已被禁用.");
			}
			Object claimUid = claims.get("user_id");
			if (claimUid != null && authUserIdTruthy(claimUid)) {
				userId = parseLongFlexible(claimUid, 0L);
			}
		}

		if (userId <= 0L && environment.acceptsProfiles(Profiles.of("local")) && merged.containsKey("user_id")) {
			userId = parseLongFlexible(merged.get("user_id"), 0L);
		}

		if (userId <= 0L) {
			throw new ResourceException("会员id不能为空！");
		}

		if (!merged.containsKey("post_id") || parseLongFlexible(merged.get("post_id"), 0L) <= 0L) {
			throw new ResourceException("笔记id不能为空！");
		}
		long postId = parseLongFlexible(merged.get("post_id"), 0L);

		Map<String, Object> data = frontUgcPostFavoriteService.favorite(userId, postId);
		return ResponseEntity.ok(ApiResult.ok(data));
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

	private static List<Long> dedupeFormPostIds(HttpServletRequest request) {
		Set<Long> set = new LinkedHashSet<>();
		set.addAll(collectFormLongsH5(request, "post_id"));
		set.addAll(collectFormLongsH5(request, "post_id[]"));
		return new ArrayList<>(set);
	}

	private static List<Long> collectFormLongsH5(HttpServletRequest request, String paramName) {
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
				throw new ResourceException("post_id参数不能为空");
			}
		}
		return out;
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

	/**
	 * JWT may omit {@code mobile}; use member profile when absent so persisted and echoed mobile matches session.
	 */
	private String resolveMemberMobileForUgcPost(Map<String, Object> claims, long userId, long companyId) {
		Object cm = claims.get("mobile");
		if (cm != null) {
			String t = cm.toString().trim();
			if (StringUtils.hasText(t)) {
				return t;
			}
		}
		Map<String, Object> info = memberAccountService.getMemberInfo(userId, companyId);
		if (info != null && !info.isEmpty()) {
			Object rm = info.get("region_mobile");
			if (rm != null) {
				String t = rm.toString().trim();
				if (StringUtils.hasText(t)) {
					return t;
				}
			}
			Object mm = info.get("mobile");
			if (mm != null) {
				String t = mm.toString().trim();
				if (StringUtils.hasText(t)) {
					return t;
				}
			}
		}
		return "0";
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

	private Map<String, Object> mergeCreatePostInput(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			return mergeInputLikeFlexibleResolver(request, body);
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(parameterMapToMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		return merged;
	}

	private static void normalizeCreatePostArrayKeys(Map<String, Object> merged, ObjectMapper objectMapper) {
		String[] arrayKeys = {"images", "image_path", "topics", "badges", "goods"};
		for (String key : arrayKeys) {
			if (!merged.containsKey(key)) {
				continue;
			}
			merged.put(key, toStringListForCreate(key, merged.get(key), objectMapper));
		}
		if (merged.containsKey("image_tag")) {
			merged.put("image_tag", normalizeImageTagForCreate(merged.get("image_tag"), objectMapper));
		}
	}

	private static List<String> toStringListForCreate(String key, Object raw, ObjectMapper om) {
		if (raw == null) {
			return new ArrayList<>();
		}
		if (raw instanceof Collection<?> c) {
			List<String> out = new ArrayList<>();
			for (Object o : c) {
				if (o == null) {
					continue;
				}
				String s = String.valueOf(o).trim();
				if (s.isEmpty()) {
					continue;
				}
				out.add(s);
			}
			return out;
		}
		Class<?> cl = raw.getClass();
		if (cl.isArray()) {
			if (raw instanceof Object[] arr) {
				return toStringListForCreate(key, Arrays.asList(arr), om);
			}
			if (raw instanceof int[] arr) {
				List<String> out = new ArrayList<>();
				for (int v : arr) {
					String s = String.valueOf(v).trim();
					if (!s.isEmpty()) {
						out.add(s);
					}
				}
				return out;
			}
			if (raw instanceof long[] arr) {
				List<String> out = new ArrayList<>();
				for (long v : arr) {
					String s = String.valueOf(v).trim();
					if (!s.isEmpty()) {
						out.add(s);
					}
				}
				return out;
			}
			if (raw instanceof byte[] arr) {
				List<String> out = new ArrayList<>();
				for (byte v : arr) {
					String s = String.valueOf(v).trim();
					if (!s.isEmpty()) {
						out.add(s);
					}
				}
				return out;
			}
			if (raw instanceof short[] arr) {
				List<String> out = new ArrayList<>();
				for (short v : arr) {
					String s = String.valueOf(v).trim();
					if (!s.isEmpty()) {
						out.add(s);
					}
				}
				return out;
			}
			if (raw instanceof double[] arr) {
				List<String> out = new ArrayList<>();
				for (double v : arr) {
					String s = String.valueOf(v).trim();
					if (!s.isEmpty()) {
						out.add(s);
					}
				}
				return out;
			}
			if (raw instanceof float[] arr) {
				List<String> out = new ArrayList<>();
				for (float v : arr) {
					String s = String.valueOf(v).trim();
					if (!s.isEmpty()) {
						out.add(s);
					}
				}
				return out;
			}
			if (raw instanceof boolean[] arr) {
				List<String> out = new ArrayList<>();
				for (boolean v : arr) {
					String s = String.valueOf(v).trim();
					if (!s.isEmpty()) {
						out.add(s);
					}
				}
				return out;
			}
			if (raw instanceof char[] arr) {
				String s = new String(arr).trim();
				if (s.isEmpty()) {
					return new ArrayList<>();
				}
				return new ArrayList<>(List.of(s));
			}
			return new ArrayList<>();
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.startsWith("[") && t.endsWith("]")) {
				try {
					return om.readValue(t, new TypeReference<List<String>>() {});
				} catch (JsonProcessingException e) {
					throw new ResourceException("参数格式无效: " + key);
				}
			}
			if (t.isEmpty()) {
				return new ArrayList<>();
			}
			return new ArrayList<>(List.of(t));
		}
		if (raw instanceof Number || raw instanceof Boolean) {
			String t = String.valueOf(raw).trim();
			if (t.isEmpty()) {
				return new ArrayList<>();
			}
			return new ArrayList<>(List.of(t));
		}
		String t = String.valueOf(raw).trim();
		if (t.isEmpty()) {
			return new ArrayList<>();
		}
		return new ArrayList<>(List.of(t));
	}

	private static Object normalizeImageTagForCreate(Object raw, ObjectMapper om) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Collection<?> || raw instanceof Map<?, ?>) {
			return raw;
		}
		Class<?> cl = raw.getClass();
		if (cl.isArray()) {
			return raw;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return null;
			}
			try {
				om.readTree(t);
				return om.readValue(t, Object.class);
			} catch (JsonProcessingException e) {
				throw new ResourceException("image_tag 格式无效");
			}
		}
		return raw;
	}

	private static Integer parseIntLoose(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
