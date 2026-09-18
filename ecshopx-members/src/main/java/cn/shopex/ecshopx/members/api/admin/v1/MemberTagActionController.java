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

package cn.shopex.ecshopx.members.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.members.service.admin.MemberTagSegmentRuleCreateService;
import cn.shopex.ecshopx.members.service.admin.MemberTagSegmentRuleGetService;
import cn.shopex.ecshopx.members.service.admin.MemberTagSegmentRuleListService;
import cn.shopex.ecshopx.members.service.admin.MemberTagSegmentRuleStructureService;
import cn.shopex.ecshopx.members.service.admin.MemberTagSegmentRuleUpdateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("membersAdminV1MemberTagAction")
@RequestMapping("/api/v1/member/segment-rule")
public class MemberTagActionController {

	private final MemberTagSegmentRuleCreateService segmentRuleCreateService;
	private final ObjectMapper objectMapper;
	private final MemberTagSegmentRuleUpdateService segmentRuleUpdateService;
	private final MemberTagSegmentRuleListService segmentRuleListService;
	private final MemberTagSegmentRuleGetService segmentRuleGetService;
	private final MemberTagSegmentRuleStructureService memberTagSegmentRuleStructureService;

	public MemberTagActionController(
			MemberTagSegmentRuleCreateService segmentRuleCreateService,
			ObjectMapper objectMapper,
			MemberTagSegmentRuleUpdateService segmentRuleUpdateService,
			MemberTagSegmentRuleListService segmentRuleListService,
			MemberTagSegmentRuleGetService segmentRuleGetService,
			MemberTagSegmentRuleStructureService memberTagSegmentRuleStructureService) {
		this.segmentRuleCreateService = segmentRuleCreateService;
		this.objectMapper = objectMapper;
		this.segmentRuleUpdateService = segmentRuleUpdateService;
		this.segmentRuleListService = segmentRuleListService;
		this.segmentRuleGetService = segmentRuleGetService;
		this.memberTagSegmentRuleStructureService = memberTagSegmentRuleStructureService;
	}

	@Activated(routeAlias = "member.segment.rule.structure")
	@GetMapping(value = "/structure", name = "规则结构", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<java.util.List<java.util.Map<String, Object>>>> getRuleStructure(
			HttpServletRequest request) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}

		List<Map<String, Object>> body = memberTagSegmentRuleStructureService.getRuleStructure(companyId);
		return ResponseEntity.ok(ApiResult.ok(body));
	}

	@Activated(routeAlias = "member.segment.rule.preview")
	@PostMapping(value = "/preview", name = "预览人群规则", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> previewSegmentRule(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}

		String operatorType = trimToEmpty(jwt.get("operator_type"));
		long jwtDistributorId = parseLongOrZero(jwt.get("distributor_id"));

		Map<String, Object> serviceResult =
				segmentRuleCreateService.previewSegmentRule(companyId, operatorType, jwtDistributorId, merged);

		Map<String, Object> ordered = new LinkedHashMap<>();
		ordered.put("matched_count", serviceResult.get("matched_count"));
		ordered.put("user_ids", serviceResult.get("user_ids"));

		return ResponseEntity.ok(ApiResult.ok(ordered));
	}

	@Activated(routeAlias = "member.segment.rule.list")
	@GetMapping(name = "人群规则列表", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getSegmentRuleList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String page,
			@RequestParam(name = "page_size", required = false) String pageSize,
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "rule_name", required = false) String ruleName,
			@RequestParam(name = "tag_name", required = false) String tagName,
			@RequestParam(name = "created_start", required = false) String createdStart,
			@RequestParam(name = "created_end", required = false) String createdEnd,
			@RequestParam(name = "distributor_id", required = false) String distributorId) {
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}

		String operatorType = trimToEmpty(jwt.get("operator_type"));
		long jwtDistributorId = parseLongOrZero(jwt.get("distributor_id"));

		Map<String, Object> data =
				segmentRuleListService.getSegmentRuleList(
						companyId,
						operatorType,
						jwtDistributorId,
						page,
						pageSize,
						status,
						tagName,
						ruleName,
						createdStart,
						createdEnd,
						distributorId);

		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "member.segment.rule.create")
	@PostMapping(name = "创建人群规则", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> createSegmentRule(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}

		JsonNode conditionJson = preprocessCondition(merged);

		String operatorType = trimToEmpty(jwt.get("operator_type"));
		long jwtDistributorId = parseLongOrZero(jwt.get("distributor_id"));

		Map<String, Object> serviceResult =
				segmentRuleCreateService.createSegmentRule(
						companyId, operatorType, jwtDistributorId, merged, conditionJson);

		Map<String, Object> ordered = new LinkedHashMap<>();
		ordered.put("rule_id", serviceResult.get("rule_id"));
		ordered.put("rule_name", serviceResult.get("rule_name"));
		ordered.put("description", serviceResult.get("description"));
		ordered.put("matched_count", serviceResult.get("matched_count"));
		ordered.put("tagged_count", serviceResult.get("tagged_count"));
		ordered.put("status", serviceResult.get("status"));

		return ResponseEntity.ok(ApiResult.ok(ordered));
	}

	private JsonNode preprocessCondition(Map<String, Object> merged) {
		Object raw = merged.get("condition");
		if (raw == null) {
			return null;
		}
		if (raw instanceof String str) {
			if (!StringUtils.hasText(str)) {
				return null;
			}
			try {
				return objectMapper.readTree(str);
			} catch (JsonProcessingException e) {
				throw new BadRequestException("规则配置必填且必须是数组");
			}
		}
		if (raw instanceof Map<?, ?> || raw instanceof List<?>) {
			return objectMapper.valueToTree(raw);
		}
		if (raw instanceof JsonNode j) {
			return j;
		}
		throw new BadRequestException("规则配置必填且必须是数组");
	}

	private static String trimToEmpty(Object v) {
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
	}

	private static long parseLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@Activated(routeAlias = "member.segment.rule.update")
	@PutMapping(value = "/{rule_id}", name = "编辑人群规则", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> updateSegmentRule(
			HttpServletRequest request,
			@PathVariable("rule_id") String ruleId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		String t = ruleId == null ? "" : ruleId.trim();
		if (!StringUtils.hasText(t) || !t.matches("^-?\\d+$")) {
			throw new ResourceException("规则不存在或无权限编辑");
		}
		long rid = Long.parseLong(t);
		if (rid <= 0) {
			throw new ResourceException("规则不存在或无权限编辑");
		}

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}

		boolean distributorIdQueryKeyPresent = request.getParameterMap().containsKey("distributor_id");
		String distributorIdQueryParamRaw = request.getParameter("distributor_id");

		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(attr instanceof Map<?, ?>)) {
			throw new UnauthorizedException("未登录");
		}
		Map<?, ?> jwt = (Map<?, ?>) attr;
		Object companyIdObj = jwt.get("company_id");
		if (companyIdObj == null) {
			throw new UnauthorizedException("未登录");
		}
		long companyId;
		try {
			companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
		} catch (NumberFormatException e) {
			throw new UnauthorizedException("未登录");
		}

		String operatorType = trimToEmpty(jwt.get("operator_type"));
		long jwtDistributorId = parseLongOrZero(jwt.get("distributor_id"));

		Map<String, Object> serviceResult =
				segmentRuleUpdateService.updateSegmentRule(
						companyId,
						operatorType,
						jwtDistributorId,
						rid,
						distributorIdQueryKeyPresent,
						distributorIdQueryParamRaw,
						merged);

		Map<String, Object> ordered = new LinkedHashMap<>();
		ordered.put("rule_id", String.valueOf(serviceResult.get("rule_id")));
		ordered.put("rule_name", String.valueOf(serviceResult.get("rule_name")));
		ordered.put(
				"description",
				serviceResult.get("description") != null
						? String.valueOf(serviceResult.get("description"))
						: "");
		ordered.put("status", "success");

		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(ApiResult.ok(ordered));
	}

	@Activated(routeAlias = "member.segment.rule.get")
	@GetMapping(value = "/{rule_id}", name = "人群规则详情", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResult<Map<String, Object>>> getSegmentRule(
			HttpServletRequest request, @PathVariable("rule_id") String ruleId) {
		String t = ruleId == null ? "" : ruleId.trim();
		if (!StringUtils.hasText(t) || !t.matches("^-?\\d+$")) {
			throw new ResourceException("规则不存在或无权限查看");
		}
		long rid = Long.parseLong(t);
		if (rid <= 0) {
			throw new ResourceException("规则不存在或无权限查看");
		}

		Object attr = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		long companyId = 0L;
		String operatorType = "";
		long jwtDistributorId = 0L;
		if (attr instanceof Map<?, ?> jwt) {
			Object companyIdObj = jwt.get("company_id");
			if (companyIdObj != null) {
				try {
					companyId = Long.parseLong(String.valueOf(companyIdObj).trim());
				} catch (NumberFormatException e) {
					companyId = 0L;
				}
			}
			operatorType = trimToEmpty(jwt.get("operator_type"));
			jwtDistributorId = parseLongOrZero(jwt.get("distributor_id"));
		}

		boolean distributorIdQueryKeyPresent = request.getParameterMap().containsKey("distributor_id");
		String distributorIdQueryParamRaw = request.getParameter("distributor_id");

		Map<String, Object> body =
				segmentRuleGetService.getSegmentRule(
						companyId,
						operatorType,
						jwtDistributorId,
						rid,
						distributorIdQueryKeyPresent,
						distributorIdQueryParamRaw);

		return ResponseEntity.ok(ApiResult.ok(body));
	}
}
