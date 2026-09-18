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

package cn.shopex.ecshopx.pointsmall.validation;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PointsmallBaseSettingSaveParamValidator {

	public void validate(Map<String, Object> merged) {
		Object ftRaw = merged.get("freight_type");
		if (ftRaw == null) {
			throw new BadRequestException("物流费用必选");
		}
		String freightType = ftRaw.toString().trim();
		if (StringUtils.hasText(freightType) && !"cash".equals(freightType) && !"point".equals(freightType)) {
			throw new BadRequestException("物流费用必选");
		}

		if (!merged.containsKey("proportion")) {
			throw new BadRequestException("积分商城汇率设置必填");
		}
		Object propRaw = merged.get("proportion");
		if (propRaw == null) {
			throw new BadRequestException("积分商城汇率设置必填");
		}
		String propStr = propRaw.toString().trim();
		if (!StringUtils.hasText(propStr)) {
			return;
		}
		if ("cash".equals(freightType)) {
			return;
		}
		if (!"point".equals(freightType)) {
			return;
		}
		BigDecimal proportion;
		try {
			proportion = new BigDecimal(propStr);
		} catch (NumberFormatException e) {
			throw new BadRequestException("积分商城汇率设置必填");
		}
		if (proportion.compareTo(BigDecimal.ONE) < 0) {
			throw new BadRequestException("积分商城汇率设置必填");
		}
	}
}
