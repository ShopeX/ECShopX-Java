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
import cn.shopex.ecshopx.distribution.api.admin.v1.dto.SetDistributionConfigRequest;
import cn.shopex.ecshopx.distribution.service.DistributionConfigRedisReadService;
import cn.shopex.ecshopx.distribution.service.DistributionConfigSetService;
import cn.shopex.ecshopx.distribution.service.DistributionStoreEntryRuleRedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_422,
		unauthorized = true,
		notFound = true)
@AdminAuth
@ShopLog
@RestController("distributionAdminV1DistributionConfig")
@RequestMapping("/api/v1")
public class DistributionConfigController {

	private final DistributionConfigRedisReadService distributionConfigRedisReadService;
	private final DistributionConfigSetService distributionConfigSetService;
	private final DistributionStoreEntryRuleRedisService distributionStoreEntryRuleRedisService;

	public DistributionConfigController(
			DistributionConfigRedisReadService distributionConfigRedisReadService,
			DistributionConfigSetService distributionConfigSetService,
			DistributionStoreEntryRuleRedisService distributionStoreEntryRuleRedisService) {
		this.distributionConfigRedisReadService = distributionConfigRedisReadService;
		this.distributionConfigSetService = distributionConfigSetService;
		this.distributionStoreEntryRuleRedisService = distributionStoreEntryRuleRedisService;
	}

	@Activated(routeAlias = "distribution.config.get")
	@GetMapping(value = "/distribution/config", name = "获取分润配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> getConfig(HttpServletRequest request) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Map<String, Object> data = distributionConfigRedisReadService.getMergedDistributionConfig(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@Activated(routeAlias = "distribution.config.save")
	@PostMapping(value = "/distribution/config", name = "保存分润配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> setConfig(
			HttpServletRequest request, @Valid @FlexibleBody SetDistributionConfigRequest body) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Map<String, Object> data = distributionConfigSetService.setConfig(companyId, body);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@GetMapping(value = "/distributor/config/inRule", name = "获取店铺进店规格")
	public ResponseEntity<ApiResult<Map<String, Object>>> getInRule(HttpServletRequest request) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Map<String, Object> data = distributionStoreEntryRuleRedisService.getInRule(companyId);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	@PostMapping(value = "/distributor/config/inRule", name = "保存店铺进店规格")
	public ResponseEntity<ApiResult<Map<String, Object>>> saveInRule(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		Map<String, Object> data = distributionStoreEntryRuleRedisService.saveInRule(companyId, body);
		return ResponseEntity.ok(ApiResult.ok(data));
	}

	private static Map<String, Object> readOperatorJwtMap(HttpServletRequest request) {
		Object rawJwt = request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA);
		if (!(rawJwt instanceof Map<?, ?> m) || m.isEmpty()) {
			throw new UnauthorizedException(
					"Failed to authenticate because of bad credentials or an invalid authorization header.");
		}
		return toStringKeyMap(m);
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
}
