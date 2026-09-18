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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.distribution.service.DistributorInfoResolveService;
import cn.shopex.ecshopx.salesperson.service.signin.SalespersonSigninWxcodeStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SalespersonSigninValidService {

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final SalespersonSigninDetailReadService salespersonSigninDetailReadService;
	private final DistributorInfoResolveService distributorInfoResolveService;

	public SalespersonSigninValidService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			SalespersonSigninDetailReadService salespersonSigninDetailReadService,
			DistributorInfoResolveService distributorInfoResolveService) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.salespersonSigninDetailReadService = salespersonSigninDetailReadService;
		this.distributorInfoResolveService = distributorInfoResolveService;
	}

	public Map<String, Object> validSignin(long companyId, String token) {
		String key = "salesperson:signin:" + token;
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (raw == null || raw.isBlank()) {
			return nothingPayload();
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (JsonProcessingException e) {
			return nothingPayload();
		}

		JsonNode statusNode = root.path("status");
		JsonNode expNode = root.path("exp");
		if (!statusNode.isIntegralNumber() || !expNode.isIntegralNumber()) {
			return nothingPayload();
		}

		long exp = expNode.asLong();
		int status = statusNode.asInt();

		long nowSec = Instant.now().getEpochSecond();
		if (nowSec > exp && status == SalespersonSigninWxcodeStatus.STATUS_WXCODE_WRIT) {
			return Map.of(
					"status", SalespersonSigninWxcodeStatus.STATUS_WXCODE_EXPIRED,
					"msg", "验证过期");
		}

		return switch (status) {
			case SalespersonSigninWxcodeStatus.STATUS_WXCODE_WRIT ->
					Map.of("status", 0, "msg", "等待扫码");
			case SalespersonSigninWxcodeStatus.STATUS_WXCODE_SWEEP ->
					Map.of("status", 1, "msg", "扫描成功");
			case SalespersonSigninWxcodeStatus.STATUS_WXCODE_SIGNIN -> buildSigninSuccess(companyId, root);
			case SalespersonSigninWxcodeStatus.STATUS_WXCODE_ERROR ->
					Map.of("status", 3, "msg", "取消确认");
			case SalespersonSigninWxcodeStatus.STATUS_WXCODE_SIGNOUT ->
					Map.of("status", 6, "msg", "签退成功");
			case SalespersonSigninWxcodeStatus.STATUS_WXCODE_AUTHFAIL ->
					Map.of("status", 7, "msg", "验证错误");
			default -> nothingPayload();
		};
	}

	private Map<String, Object> buildSigninSuccess(long companyId, JsonNode root) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("status", 2);
		data.put("msg", "签到成功");

		long sid = parseFlexibleLong(root.path("salesperson_id"));
		data.put("salesperson", salespersonSigninDetailReadService.getDetailForSigninPoll(companyId, sid));

		long distributorId = parseFlexibleLong(root.path("distributor_id"));
		Optional<Map<String, Object>> distOpt =
				distributorInfoResolveService.resolveStoreDetail(companyId, distributorId, "");
		if (distOpt.isPresent()) {
			data.put("distributor", distOpt.get());
		} else {
			data.put("distributor", Collections.emptyList());
		}

		return data;
	}

	private static Map<String, Object> nothingPayload() {
		return Map.of(
				"status", SalespersonSigninWxcodeStatus.STATUS_WXCODE_NOTHING,
				"msg", "二维码信息出错");
	}

	private static long parseFlexibleLong(JsonNode n) {
		if (n == null || n.isMissingNode() || n.isNull()) {
			return 0L;
		}
		if (n.isIntegralNumber()) {
			return n.asLong();
		}
		if (n.isTextual()) {
			String t = n.asText().trim();
			if (t.isEmpty()) {
				return 0L;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}
}
