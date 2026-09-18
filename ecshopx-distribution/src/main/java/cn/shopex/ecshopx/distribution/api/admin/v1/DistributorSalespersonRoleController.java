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

package cn.shopex.ecshopx.distribution.api.admin.v1;

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.distribution.service.DistributorSalesmanRoleReadService;
import cn.shopex.ecshopx.distribution.service.DistributorSalesmanRoleWriteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
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
		notFound = false)
@AdminAuth
@ShopLog
@RestController("distributionAdminV1DistributorSalespersonRole")
@RequestMapping("/api/v1/distributor/salesperson/role")
public class DistributorSalespersonRoleController {

	private final DistributorSalesmanRoleWriteService distributorSalesmanRoleWriteService;
	private final DistributorSalesmanRoleReadService distributorSalesmanRoleReadService;
	private final ObjectMapper objectMapper;
	private final LangueProperties langueProperties;

	public DistributorSalespersonRoleController(
			DistributorSalesmanRoleWriteService distributorSalesmanRoleWriteService,
			DistributorSalesmanRoleReadService distributorSalesmanRoleReadService,
			ObjectMapper objectMapper,
			LangueProperties langueProperties) {
		this.distributorSalesmanRoleWriteService = distributorSalesmanRoleWriteService;
		this.distributorSalesmanRoleReadService = distributorSalesmanRoleReadService;
		this.objectMapper = objectMapper;
		this.langueProperties = langueProperties;
	}

	@Activated(routeAlias = "distribution.salesperson.role.list")
	@GetMapping(name = "获取门店角色列表")
	public ResponseEntity<ApiResult<Map<String, Object>>> getRoleList(
			HttpServletRequest request,
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "page_size", defaultValue = "10") int pageSize) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");

		if (page < 1 || pageSize < 1) {
			throw new BadRequestException("参数错误");
		}

		Map<String, Object> data = distributorSalesmanRoleReadService.getRoleList(companyId, page, pageSize);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "distribution.salesperson.role.get")
	@GetMapping(value = "/{salesmanRoleId}", name = "获取门店角色")
	public ResponseEntity<ApiResult<Object>> getRoleInfo(
			HttpServletRequest request, @PathVariable("salesmanRoleId") String salesmanRoleId) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		OptionalLong roleIdOpt = tryParsePathSalesmanRoleId(salesmanRoleId);
		if (roleIdOpt.isEmpty()) {
			return ResponseEntity.ok(ApiResult.ok(List.of()));
		}
		Optional<Map<String, Object>> row =
				distributorSalesmanRoleReadService.getRoleInfo(companyId, roleIdOpt.getAsLong());
		if (row.isPresent()) {
			return ResponseEntity.ok(ApiResult.ok(row.get()));
		}
		return ResponseEntity.ok(ApiResult.ok(List.of()));
	}

	@Activated(routeAlias = "distribution.salesperson.role.create")
	@PostMapping(name = "保存门店角色")
	public ResponseEntity<ApiResult<Map<String, Object>>> createRole(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");

		String roleName = normalizeRoleName(body == null ? null : body.get("role_name"));
		if (!StringUtils.hasText(roleName)) {
			throw new BadRequestException(resolveRoleNotEmptyMessage(request));
		}
		JsonNode ruleIdsNode = normalizeRuleIdsToArrayNode(body == null ? null : body.get("rule_ids"));

		Map<String, Object> data = distributorSalesmanRoleWriteService.createRole(companyId, roleName, ruleIdsNode);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "distribution.salesperson.role.update")
	@PutMapping(value = "/{salesmanRoleId}", name = "修改门店角色")
	public ResponseEntity<ApiResult<Map<String, Object>>> updateRole(
			HttpServletRequest request,
			@PathVariable("salesmanRoleId") String salesmanRoleId,
			@FlexibleBody(required = false) Map<String, Object> body) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");

		long roleId = parsePathSalesmanRoleId(salesmanRoleId);

		String roleName = normalizeRoleName(body == null ? null : body.get("role_name"));
		if (!StringUtils.hasText(roleName)) {
			throw new BadRequestException(resolveRoleNotEmptyMessage(request));
		}
		JsonNode ruleIdsNode = normalizeRuleIdsToArrayNode(body == null ? null : body.get("rule_ids"));

		Map<String, Object> data = distributorSalesmanRoleWriteService.updateRole(
				companyId, roleId, roleName, ruleIdsNode, resolveNoUpdateDataFoundMessage(request));
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "distribution.salesperson.role.delete")
	@DeleteMapping(value = "/{salesmanRoleId}", name = "删除门店角色")
	public ResponseEntity<ApiResult<Map<String, Object>>> delRole(
			HttpServletRequest request, @PathVariable("salesmanRoleId") String salesmanRoleId) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		Map<String, Object> jwt = toStringKeyMap(m);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		OptionalLong roleIdOpt = tryParsePathSalesmanRoleId(salesmanRoleId);
		distributorSalesmanRoleWriteService.delRole(companyId, roleIdOpt);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
	}

	private JsonNode normalizeRuleIdsToArrayNode(Object raw) {
		if (raw instanceof List<?>) {
			return objectMapper.valueToTree(raw);
		}
		return objectMapper.getNodeFactory().arrayNode();
	}

	private static Map<String, Object> toStringKeyMap(Map<?, ?> m) {
		Map<String, Object> out = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			out.put(String.valueOf(e.getKey()), e.getValue());
		}
		return out;
	}

	private static long parsePositiveLongClaim(Map<String, Object> jwt, String key, String invalidMsg) {
		Object cid = jwt.get(key);
		if (cid == null) {
			throw new BadRequestException(invalidMsg);
		}
		long result;
		if (cid instanceof Number n) {
			result = n.longValue();
		} else if (cid instanceof String s) {
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException(invalidMsg);
			}
			try {
				result = Long.parseLong(s.trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(invalidMsg);
			}
		} else {
			try {
				result = Long.parseLong(String.valueOf(cid).trim());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(invalidMsg);
			}
		}
		if (result <= 0L) {
			throw new BadRequestException(invalidMsg);
		}
		return result;
	}

	private String resolveRoleNotEmptyMessage(HttpServletRequest request) {
		String lang = RequestLangTag.current(langueProperties);
		if (lang != null && lang.toLowerCase().contains("en")) {
			return "Store role cannot be empty";
		}
		return "门店角色不能为空";
	}

	private String resolveNoUpdateDataFoundMessage(HttpServletRequest request) {
		String lang = RequestLangTag.current(langueProperties);
		if (lang != null && lang.toLowerCase().contains("en")) {
			return "No update data found";
		}
		return "未查询到更新数据";
	}

	private static long parsePathSalesmanRoleId(String salesmanRoleId) {
		OptionalLong parsed = tryParsePathSalesmanRoleId(salesmanRoleId);
		if (parsed.isEmpty()) {
			throw new BadRequestException("参数错误");
		}
		return parsed.getAsLong();
	}

	/**
	 * Same numeric rules as {@link #parsePathSalesmanRoleId} but returns empty when invalid — used by {@code getRoleInfo}
	 * no row yields {@code data: []}; path segments remain string keys (including non-numeric segments).
	 */
	private static OptionalLong tryParsePathSalesmanRoleId(String salesmanRoleId) {
		if (!StringUtils.hasText(salesmanRoleId)) {
			return OptionalLong.empty();
		}
		try {
			long id = Long.parseLong(salesmanRoleId.trim());
			if (id <= 0L) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(id);
		} catch (NumberFormatException ex) {
			return OptionalLong.empty();
		}
	}

	private static String normalizeRoleName(Object raw) {
		if (raw == null) {
			return "";
		}
		if (raw instanceof Number n) {
			if (n.longValue() == 0L) {
				return "";
			}
			return Long.toString(n.longValue());
		}
		if (raw instanceof String s) {
			return s.trim();
		}
		throw new BadRequestException("参数错误");
	}
}
