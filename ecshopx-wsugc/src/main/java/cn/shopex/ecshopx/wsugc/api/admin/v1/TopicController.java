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
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import cn.shopex.ecshopx.wsugc.service.topic.TopicCreateService;
import cn.shopex.ecshopx.wsugc.service.topic.TopicDeleteService;
import cn.shopex.ecshopx.wsugc.service.topic.TopicDetailService;
import cn.shopex.ecshopx.wsugc.service.topic.TopicListService;
import cn.shopex.ecshopx.wsugc.service.topic.TopicSetTopService;
import cn.shopex.ecshopx.wsugc.service.topic.TopicVerifyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
@RestController("wsugcAdminV1Topic")
@RequestMapping("/api/v1/ugc/topic")
public class TopicController {

	private final TopicCreateService topicCreateService;
	private final TopicDetailService topicDetailService;
	private final TopicListService topicListService;
	private final TopicSetTopService topicSetTopService;
	private final TopicVerifyService topicVerifyService;
	private final TopicDeleteService topicDeleteService;
	private final OperatorLogsWriteService operatorLogsWriteService;
	private final ObjectMapper objectMapper;
	private final Environment environment;

	public TopicController(
			TopicCreateService topicCreateService,
			TopicDetailService topicDetailService,
			TopicListService topicListService,
			TopicSetTopService topicSetTopService,
			TopicVerifyService topicVerifyService,
			TopicDeleteService topicDeleteService,
			OperatorLogsWriteService operatorLogsWriteService,
			ObjectMapper objectMapper,
			Environment environment) {
		this.topicCreateService = topicCreateService;
		this.topicDetailService = topicDetailService;
		this.topicListService = topicListService;
		this.topicSetTopService = topicSetTopService;
		this.topicVerifyService = topicVerifyService;
		this.topicDeleteService = topicDeleteService;
		this.operatorLogsWriteService = operatorLogsWriteService;
		this.objectMapper = objectMapper;
		this.environment = Objects.requireNonNull(environment, "environment");
	}

	@PostMapping(value = "/create", name = "新建话题")
	public ResponseEntity<ApiResult<Map<String, Object>>> createTopic(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		Map<String, Object> data = topicCreateService.createOrUpdate(merged, operatorJwt);

		long companyId = readLong(ud, "company_id", 1L);
		long operatorId = readLong(ud, "operator_id", 0L);
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/ugc/topic/create");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "新建话题");
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

	private static long readLong(Map<?, ?> ud, String key, long defaultVal) {
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

	@PostMapping(value = "/verify", name = "审核话题")
	public ResponseEntity<ApiResult<Map<String, Object>>> verifyTopic(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawJwt = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) rawJwt;
		boolean local = environment.acceptsProfiles(Profiles.of("local"));
		Map<String, Object> data = topicVerifyService.verify(merged, operatorJwt, local);

		long companyId = readLong(ud, "company_id", 1L);
		long operatorId = readLong(ud, "operator_id", 0L);
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/ugc/topic/verify");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "审核话题");
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

	@PostMapping(value = "/top", name = "置顶话题")
	public ResponseEntity<Void> topTopic() {
		return ResponseEntity.ok().build();
	}

	@PostMapping(value = "/enable", name = "发布话题")
	public ResponseEntity<Void> enableTopic() {
		return ResponseEntity.ok().build();
	}

	@Activated(routeAlias = "ugc.topic.list")
	@GetMapping(value = "/list", name = "话题列表")
	public ResponseEntity<ApiResult<Object>> getTopicList(
			HttpServletRequest request,
			@RequestParam(value = "page", required = false, defaultValue = "1") int page,
			@RequestParam(value = "pageSize", required = false, defaultValue = "30") int pageSize,
			@RequestParam(value = "topic_name", required = false) String topicName,
			@RequestParam(value = "is_top", required = false) String isTop,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "source", required = false) String source,
			@RequestParam(value = "nickname", required = false) String nickname,
			@RequestParam(value = "mobile", required = false) String mobile,
			@RequestParam(value = "sort", required = false) String sort) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		long companyId = readLong(operatorJwt, "company_id", 1L);
		String langTag = resolveAdminLangTag(operatorJwt);
		Object data =
				topicListService.buildList(
						companyId, langTag, page, pageSize, topicName, isTop, status, source, nickname, mobile, sort);
		if (data instanceof List<?> list && list.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(Collections.emptyList()));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) data;
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "ugc.topic.detail")
	@GetMapping(value = "/detail", name = "话题详情")
	public ResponseEntity<ApiResult<Map<String, Object>>> getTopicDetail(
			HttpServletRequest request,
			@RequestParam(value = "topic_id", required = false) String topicId) {
		Object raw = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(raw instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> operatorJwt = (Map<String, Object>) raw;
		String langTag = resolveAdminLangTag(operatorJwt);
		Map<String, Object> data = topicDetailService.buildResponse(topicId, operatorJwt, langTag);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static String resolveAdminLangTag(Map<String, Object> operatorJwt) {
		Object cc = operatorJwt.get("country_code");
		if (cc != null && StringUtils.hasText(cc.toString())) {
			return cc.toString().trim();
		}
		return "zh-CN";
	}

	@PostMapping(value = "/delete", name = "删除话题")
	public ResponseEntity<ApiResult<Map<String, Object>>> deleteTopic(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawJwt = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeDeleteTopicInput(request, body);
		Map<String, Object> data = topicDeleteService.deleteTopics(merged);

		long companyId = readLong(ud, "company_id", 1L);
		long operatorId = readLong(ud, "operator_id", 0L);
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/ugc/topic/delete");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "删除话题");
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

	private Map<String, Object> mergeDeleteTopicInput(HttpServletRequest request, Map<String, Object> body) {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			LinkedHashMap<String, Object> input = new LinkedHashMap<>(parameterMapToMapForTopicDelete(request));
			if (body != null) {
				input.putAll(body);
			}
			return input;
		}
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>(parameterMapToMapForTopicDelete(request));
		if (body != null) {
			merged.putAll(body);
		}
		List<String> formIds = dedupeFormTopicIdStrings(request);
		if (!formIds.isEmpty()) {
			if (formIds.size() > 1) {
				merged.put("topic_id", formIds);
			} else {
				merged.put("topic_id", formIds.get(0));
			}
		}
		return merged;
	}

	private static Map<String, Object> parameterMapToMapForTopicDelete(HttpServletRequest request) {
		Map<String, Object> m = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> {
			if (v != null && v.length > 0 && StringUtils.hasText(v[0])) {
				m.put(k, v[0]);
			}
		});
		return m;
	}

	private static List<String> dedupeFormTopicIdStrings(HttpServletRequest request) {
		Set<String> set = new LinkedHashSet<>();
		addTrimmedNonBlank(request, "topic_id", set);
		addTrimmedNonBlank(request, "topic_id[]", set);
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

	@PostMapping(value = "/settop", name = "话题置顶")
	public ResponseEntity<ApiResult<Map<String, Object>>> setTopTopic(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawJwt = request.getAttribute(OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> ud)) {
			throw new UnauthorizedException("未登录");
		}
		Map<String, Object> merged = mergeInputLikeFlexibleResolver(request, body);
		List<Long> topicIds = parseTopicIdListForSetTop(merged);
		Map<String, Object> data = topicSetTopService.setTop(topicIds);

		long companyId = readLong(ud, "company_id", 1L);
		long operatorId = readLong(ud, "operator_id", 0L);
		Map<String, Object> logCtx = new LinkedHashMap<>();
		logCtx.put("company_id", companyId);
		logCtx.put("operator_id", (int) operatorId);
		logCtx.put("request_uri", "/api/v1/ugc/topic/settop");
		logCtx.put("ip", clientIp(request));
		try {
			logCtx.put("params", objectMapper.writeValueAsString(merged));
		} catch (Exception e) {
			logCtx.put("params", merged.toString());
		}
		logCtx.put("operator_name", "话题置顶");
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

	private static List<Long> parseTopicIdListForSetTop(Map<String, Object> input) {
		if (!input.containsKey("topic_id") || input.get("topic_id") == null) {
			throw new ResourceException("topic_id参数不能为空");
		}
		Object raw = input.get("topic_id");
		if (raw instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new ResourceException("topic_id参数不能为空");
			}
			return List.of(parseTopicIdElementForSetTop(t));
		}
		if (raw instanceof Number n) {
			return List.of(parseTopicIdElementForSetTop(n));
		}
		if (raw instanceof Collection<?> c) {
			// 空集合视为未提供有效 topic_id
			if (c.isEmpty()) {
				throw new ResourceException("topic_id参数不能为空");
			}
			List<Long> out = new ArrayList<>();
			for (Object el : c) {
				out.add(parseTopicIdElementForSetTop(el));
			}
			return out;
		}
		throw new BadRequestException("topic_id 参数格式错误");
	}

	private static Long parseTopicIdElementForSetTop(Object el) {
		if (el instanceof Number n) {
			long v = n.longValue();
			if (v <= 0) {
				throw new BadRequestException("topic_id 参数格式错误");
			}
			return v;
		}
		if (el instanceof String s) {
			String t = s.trim();
			try {
				long v = Long.parseLong(t);
				if (v <= 0) {
					throw new BadRequestException("topic_id 参数格式错误");
				}
				return v;
			} catch (NumberFormatException e) {
				// 非数字 id 与下游「无匹配行」统一为资源类错误语义
				throw new ResourceException("未查询到更新数据");
			}
		}
		throw new BadRequestException("topic_id 参数格式错误");
	}
}
