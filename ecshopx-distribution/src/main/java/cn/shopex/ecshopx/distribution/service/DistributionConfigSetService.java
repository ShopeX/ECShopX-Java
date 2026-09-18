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

package cn.shopex.ecshopx.distribution.service;

import static cn.shopex.ecshopx.distribution.service.DistributionConfigValidationSupport.buildValidatorRoot;
import static cn.shopex.ecshopx.distribution.service.DistributionConfigValidationSupport.distributorSectionToMap;
import static cn.shopex.ecshopx.distribution.service.DistributionConfigValidationSupport.distributorShowFailsRequiredWith01;
import static cn.shopex.ecshopx.distribution.service.DistributionConfigValidationSupport.hasEffectiveValidatorValue;

import cn.shopex.ecshopx.common.distribution.DistributionPopularizeProfitApplyPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.distribution.api.admin.v1.dto.SetDistributionConfigRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DistributionConfigSetService {

	private static final String MSG_SHOW = "是否开启分润配置";
	private static final String MSG_DISTRIBUTOR = "拉新店铺分润配置必填";
	private static final String MSG_SELLER = "拉新导购分润配置必填";
	private static final String MSG_POPULARIZE_SELLER = "推广导购分润配置必填";
	private static final String MSG_DISTRIBUTOR_SELLER = "门店开单分润配置必填";
	private static final String MSG_PLAN_LIMIT_TIME = "结算时间必填";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final DistributionPopularizeProfitApplyPort distributionPopularizeProfitApplyPort;
	private final DistributionConfigRedisReadService distributionConfigRedisReadService;

	public DistributionConfigSetService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			DistributionPopularizeProfitApplyPort distributionPopularizeProfitApplyPort,
			DistributionConfigRedisReadService distributionConfigRedisReadService) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.distributionPopularizeProfitApplyPort = distributionPopularizeProfitApplyPort;
		this.distributionConfigRedisReadService = distributionConfigRedisReadService;
	}

	public Map<String, Object> setConfig(long companyId, SetDistributionConfigRequest req) {
		Map<String, Object> validatorRoot = buildValidatorRoot(req);
		@SuppressWarnings("unchecked")
		Map<String, Object> distMap = (Map<String, Object>) validatorRoot.get("distributor");

		Object showVal = distMap.get("show");
		if (distributorShowFailsRequiredWith01(distMap, showVal)) {
			throw new BadRequestException(MSG_SHOW);
		}
		if (!hasEffectiveValidatorValue(distMap.get("distributor"))) {
			throw new BadRequestException(MSG_DISTRIBUTOR);
		}
		if (!hasEffectiveValidatorValue(distMap.get("seller"))) {
			throw new BadRequestException(MSG_SELLER);
		}
		if (!hasEffectiveValidatorValue(distMap.get("popularize_seller"))) {
			throw new BadRequestException(MSG_POPULARIZE_SELLER);
		}
		if (!hasEffectiveValidatorValue(distMap.get("distributor_seller"))) {
			throw new BadRequestException(MSG_DISTRIBUTOR_SELLER);
		}
		if (!hasEffectiveValidatorValue(distMap.get("plan_limit_time"))) {
			throw new BadRequestException(MSG_PLAN_LIMIT_TIME);
		}

		SetDistributionConfigRequest.DistributorSection d = req.getDistributor();
		BigDecimal popularizeSeller = toBigDecimalForScale(d.getPopularize_seller());
		String profitScalePlain = popularizeSeller
				.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
				.setScale(4, RoundingMode.HALF_UP)
				.toPlainString();

		Map<String, Object> distributorPayload = buildRedisDistributorMap(d);
		Map<String, Object> redisBody = new LinkedHashMap<>();
		redisBody.put("company_id", companyId);
		redisBody.put("distributor", distributorPayload);

		String key = distributionConfigRedisReadService.buildDistributionConfigRedisKey(companyId);
		String json;
		try {
			json = objectMapper.writeValueAsString(redisBody);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
		companysRedisTemplate.opsForValue().set(key, json);

		distributionPopularizeProfitApplyPort.applyPopularizeSellerScaleForDefaultItems(companyId, profitScalePlain);

		return distributionConfigRedisReadService.getMergedDistributionConfig(companyId);
	}

	private static Map<String, Object> buildRedisDistributorMap(SetDistributionConfigRequest.DistributorSection d) {
		Map<String, Object> out = new LinkedHashMap<>();
		Map<String, Object> src = distributorSectionToMap(d);
		for (Map.Entry<String, Object> e : src.entrySet()) {
			out.put(e.getKey(), normalizeStoredValue(e.getValue()));
		}
		return out;
	}

	private static Object normalizeStoredValue(Object v) {
		if (v instanceof BigDecimal bd) {
			try {
				return bd.intValueExact();
			} catch (ArithmeticException ex) {
				return bd.longValue();
			}
		}
		if (v instanceof Number n) {
			double d0 = n.doubleValue();
			if (d0 == Math.rint(d0) && d0 >= Integer.MIN_VALUE && d0 <= Integer.MAX_VALUE) {
				return (int) d0;
			}
			return n.longValue();
		}
		if (v instanceof String s && !s.isEmpty()) {
			try {
				return Integer.parseInt(s.trim());
			} catch (NumberFormatException e1) {
				try {
					return new BigDecimal(s.trim()).intValueExact();
				} catch (Exception e2) {
					return new BigDecimal(s.trim()).longValue();
				}
			}
		}
		return v;
	}

	private static BigDecimal toBigDecimalForScale(Object v) {
		if (v == null) {
			throw new BadRequestException(MSG_POPULARIZE_SELLER);
		}
		if (v instanceof BigDecimal bd) {
			return bd;
		}
		if (v instanceof Number n) {
			return new BigDecimal(n.toString());
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException(MSG_POPULARIZE_SELLER);
			}
			try {
				return new BigDecimal(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException(MSG_POPULARIZE_SELLER);
			}
		}
		try {
			return new BigDecimal(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(MSG_POPULARIZE_SELLER);
		}
	}
}
