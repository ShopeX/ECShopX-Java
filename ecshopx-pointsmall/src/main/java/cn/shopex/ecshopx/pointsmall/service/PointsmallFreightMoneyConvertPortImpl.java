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

package cn.shopex.ecshopx.pointsmall.service;

import cn.shopex.ecshopx.common.port.pointsmall.PointsmallFreightMoneyConvertPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PointsmallFreightMoneyConvertPortImpl implements PointsmallFreightMoneyConvertPort {

	private final PointsmallFrontSettingReadService pointsmallFrontSettingReadService;

	public PointsmallFreightMoneyConvertPortImpl(PointsmallFrontSettingReadService pointsmallFrontSettingReadService) {
		this.pointsmallFrontSettingReadService = pointsmallFrontSettingReadService;
	}

	@Override
	public Map<String, Object> moneyToPoint(long companyId, long moneyFen) {
		Map<String, Object> setting = pointsmallFrontSettingReadService.getAdminBaseSetting(companyId);
		String freightType = stringVal(setting.get("freight_type"));
		if (!"point".equals(freightType)) {
			LinkedHashMap<String, Object> cash = new LinkedHashMap<>();
			cash.put("freight_type", freightType.isEmpty() ? "cash" : freightType);
			cash.put("money", moneyFen);
			return cash;
		}
		BigDecimal proportion = parseProportion(setting.get("proportion"));
		String roundingMode = stringVal(setting.get("rounding_mode"));
		BigDecimal yuan = BigDecimal.valueOf(moneyFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		BigDecimal scaled = yuan.multiply(proportion);
		long converted;
		if ("up".equals(roundingMode)) {
			converted = scaled.setScale(0, RoundingMode.CEILING).longValue();
		} else {
			converted = scaled.setScale(0, RoundingMode.DOWN).longValue();
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("freight_type", "point");
		out.put("money", converted);
		return out;
	}

	private static BigDecimal parseProportion(Object raw) {
		if (raw == null) {
			return BigDecimal.ONE;
		}
		try {
			return new BigDecimal(raw.toString().trim());
		} catch (NumberFormatException e) {
			return BigDecimal.ONE;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
