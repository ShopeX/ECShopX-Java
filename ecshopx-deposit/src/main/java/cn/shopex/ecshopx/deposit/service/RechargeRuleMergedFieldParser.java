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

package cn.shopex.ecshopx.deposit.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public final class RechargeRuleMergedFieldParser {

	private RechargeRuleMergedFieldParser() {
	}

	public static long parseFixedMoneyToFen(Object raw) {
		if (raw == null) {
			throw new ResourceException("请填写正确的充值金额", 400);
		}
		String text = raw instanceof String s ? s.trim() : raw.toString().trim();
		if (text.isEmpty()) {
			throw new ResourceException("请填写正确的充值金额", 400);
		}
		BigDecimal yuan;
		try {
			yuan = new BigDecimal(text);
		} catch (NumberFormatException e) {
			throw new ResourceException("请填写正确的充值金额", 400);
		}
		if (yuan.scale() > 2) {
			throw new ResourceException("请填写正确的充值金额", 400);
		}
		BigDecimal stripped = yuan.stripTrailingZeros();
		if (stripped.scale() > 2) {
			throw new ResourceException("请填写正确的充值金额", 400);
		}
		BigDecimal scaledYuan = yuan.setScale(2, RoundingMode.HALF_UP);
		BigDecimal centsBd = scaledYuan.multiply(BigDecimal.valueOf(100));
		BigDecimal centsWhole;
		try {
			centsWhole = centsBd.setScale(0, RoundingMode.UNNECESSARY);
		} catch (ArithmeticException e) {
			throw new ResourceException("请填写正确的充值金额", 400);
		}
		try {
			return centsWhole.longValueExact();
		} catch (ArithmeticException e) {
			throw new ResourceException("请填写正确的充值金额", 400);
		}
	}

	public static String ruleDataMoneyPointString(Object rd, String errMsg) {
		if (rd == null) {
			return "0";
		}
		if (rd instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return "0";
			}
			return parseNonNegativeIntegerString(t, errMsg);
		}
		if (rd instanceof Number n) {
			if (n instanceof Double d) {
				double dv = d.doubleValue();
				if (!Double.isFinite(dv)) {
					throw new ResourceException(errMsg, 400);
				}
				if (dv < 0 || dv != Math.rint(dv)) {
					throw new ResourceException(errMsg, 400);
				}
				return String.valueOf(d.longValue());
			}
			if (n instanceof Float f) {
				float fv = f.floatValue();
				if (!Float.isFinite(fv)) {
					throw new ResourceException(errMsg, 400);
				}
				double dv = fv;
				if (dv < 0 || dv != Math.rint(dv)) {
					throw new ResourceException(errMsg, 400);
				}
				return String.valueOf(f.longValue());
			}
			long v = n.longValue();
			if (v < 0) {
				throw new ResourceException(errMsg, 400);
			}
			return String.valueOf(v);
		}
		String t = rd.toString().trim();
		if (t.isEmpty()) {
			return "0";
		}
		return parseNonNegativeIntegerString(t, errMsg);
	}

	public static String parseNonNegativeIntegerString(String text, String errMsg) {
		if (text.indexOf('.') >= 0 || text.indexOf('e') >= 0 || text.indexOf('E') >= 0) {
			throw new ResourceException(errMsg, 400);
		}
		try {
			long v = Long.parseLong(text);
			if (v < 0) {
				throw new ResourceException(errMsg, 400);
			}
			return String.valueOf(v);
		} catch (NumberFormatException e) {
			throw new ResourceException(errMsg, 400);
		}
	}

	public static String normalizeRuleDataForType(String ruleType, Object ruleDataRaw) {
		if ("money".equals(ruleType)) {
			return ruleDataMoneyPointString(ruleDataRaw, "请输入正确的赠送金额");
		}
		if ("point".equals(ruleType)) {
			return ruleDataMoneyPointString(ruleDataRaw, "请输入正确的赠送积分");
		}
		return ruleDataRaw == null ? "" : ruleDataRaw.toString();
	}

	public static String requireRuleType(Map<String, Object> merged) {
		Object ruleTypeObj = merged.get("rule_type");
		if (ruleTypeObj == null) {
			throw new ResourceException("请选择正确的充值赠送类型", 400);
		}
		String ruleType = ruleTypeObj.toString().trim();
		if (!"gift".equals(ruleType) && !"money".equals(ruleType) && !"point".equals(ruleType)) {
			throw new ResourceException("请选择正确的充值赠送类型", 400);
		}
		return ruleType;
	}
}
