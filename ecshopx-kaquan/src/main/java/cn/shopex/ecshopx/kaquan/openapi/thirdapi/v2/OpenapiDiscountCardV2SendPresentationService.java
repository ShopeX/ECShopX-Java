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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiKaquanV2FailException;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiDiscountCardV2SendPresentationService {

	private final UserDiscountMapper userDiscountMapper;

	public OpenapiDiscountCardV2SendPresentationService(UserDiscountMapper userDiscountMapper) {
		this.userDiscountMapper = userDiscountMapper;
	}

	public Map<String, Object> toOpenApiData(Map<String, Object> serviceResult) {
		String code = String.valueOf(serviceResult.get("code"));
		UserDiscount row =
				userDiscountMapper.selectOne(
						new LambdaQueryWrapper<UserDiscount>()
								.eq(UserDiscount::getCode, code)
								.last("LIMIT 1"));
		if (row == null) {
			throw new OpenapiKaquanV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS,
					OpenapiDiscountCardV2SendPhpMessages.MSG_FAILED_TO_RECEIVE);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("code", code);
		out.put("begin_date", row.getBeginDate());
		out.put("end_date", row.getEndDate());
		out.put("status", mapOpenApiStatus(row));

		Object dmCardCode = serviceResult.get("dm_card_code");
		if (dmCardCode != null && StringUtils.hasText(String.valueOf(dmCardCode).trim())) {
			out.put("dm_card_code", String.valueOf(dmCardCode).trim());
		}
		return out;
	}

	private static String mapOpenApiStatus(UserDiscount row) {
		return OpenapiDiscountCardV2OpenApiStatusSupport.mapFromEntity(row);
	}
}
