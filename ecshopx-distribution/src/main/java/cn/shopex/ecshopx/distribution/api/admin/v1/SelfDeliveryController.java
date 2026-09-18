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

import cn.shopex.ecshopx.common.annotation.AdminAuth;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.distribution.service.SelfDeliverySettingReadService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
@RestController("distributionAdminV1SelfDelivery")
@RequestMapping("/api/v1/distributor/selfdelivery")
public class SelfDeliveryController {

	private final SelfDeliverySettingReadService selfDeliverySettingReadService;

	public SelfDeliveryController(SelfDeliverySettingReadService selfDeliverySettingReadService) {
		this.selfDeliverySettingReadService = selfDeliverySettingReadService;
	}

	@Activated(routeAlias = "distribution.selfdelivery.setting.get")
	@GetMapping(value = "/setting", name = "获取商家自配送配置")
	public ResponseEntity<?> getSelfDeliverySetting(
			HttpServletRequest request,
			@RequestParam(value = "distributor_id", required = false) String distributorIdOverride) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String override = distributorIdOverride != null ? distributorIdOverride : "";
		String jwtSuffix = jwtDistributorRedisSuffix(jwt);
		String distributorRedisSuffix =
				distributorQueryShouldOverrideJwtSuffix(override) ? override : jwtSuffix;
		Map<String, Object> payload =
				selfDeliverySettingReadService.getSelfDeliverySetting(companyId, distributorRedisSuffix);
		if (payload == null) {
			Map<String, Object> body = new LinkedHashMap<>(3);
			body.put("code", 200);
			body.put("msg", "success");
			body.put("data", null);
			return ResponseEntity.ok(body);
		}
		return ResponseEntity.ok(ApiResult.ok(payload));
	}

	@Activated(routeAlias = "distribution.selfdelivery.setting.create")
	@PostMapping(value = "/setting", name = "保存商家自配送配置")
	public ResponseEntity<ApiResult<Map<String, Object>>> setSelfDeliverySetting(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> jwt = readOperatorJwtMap(request);
		long companyId = parsePositiveLongClaim(jwt, "company_id", "company_id 无效");
		String distributorRedisSuffix = jwtDistributorRedisSuffix(jwt);
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		if (body != null) {
			merged.putAll(body);
		}
		selfDeliverySettingReadService.setSelfDeliverySetting(companyId, distributorRedisSuffix, merged);
		return ResponseEntity.ok(ApiResult.ok(Map.of("status", Boolean.TRUE)));
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

	/**
	 * JWT {@code distributor_id} 作为 Redis 键后缀：缺失或 null 为空串；数字为十进制字符串；字符串原样使用（不 trim、不解析）。
	 */
	/**
	 * Query 仅在「有覆盖意义」时替换 JWT 后缀：空串、字面 {@code "0"} 不覆盖。
	 * 不对入参 trim，与 {@link #jwtDistributorRedisSuffix} 对 JWT 值「原样」策略一致。
	 */
	private static boolean distributorQueryShouldOverrideJwtSuffix(String raw) {
		if (raw == null || raw.isEmpty()) {
			return false;
		}
		return !"0".equals(raw);
	}

	private static String jwtDistributorRedisSuffix(Map<String, Object> jwt) {
		Object v = jwt.get("distributor_id");
		if (v == null) {
			return "";
		}
		if (v instanceof Number n) {
			return Long.toString(n.longValue());
		}
		if (v instanceof String s) {
			return s;
		}
		return String.valueOf(v);
	}
}
