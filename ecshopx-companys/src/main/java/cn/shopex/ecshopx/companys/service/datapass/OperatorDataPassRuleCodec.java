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

package cn.shopex.ecshopx.companys.service.datapass;

/**
 * Encodes and decodes {@code operator_data_pass.rule} (time window + weekday part).
 */
public final class OperatorDataPassRuleCodec {

	private OperatorDataPassRuleCodec() {}

	public record RuleParts(String range, int dateType) {}

	public record TimeWeek(String time, String week) {}

	public static RuleParts parseStoredRule(String rule) {
		if (rule == null || rule.isBlank()) {
			return new RuleParts("", 0);
		}
		String[] sp = rule.trim().split("\\s+", 2);
		String rangePart = sp[0];
		String datePart = sp.length > 1 ? sp[1] : "*";
		String range = "*".equals(rangePart) ? "" : rangePart;
		int dateType = "*".equals(datePart) ? 0 : 1;
		return new RuleParts(range, dateType);
	}

	/**
	 * @param rangeNormalizedOrEmpty normalized {@code HH:MM-HH:MM} or empty for all-day
	 */
	public static String transToRule(String rangeNormalizedOrEmpty, int dateType) {
		String r =
				(rangeNormalizedOrEmpty == null || rangeNormalizedOrEmpty.isEmpty()) ? "*" : rangeNormalizedOrEmpty;
		String week = dateType == 1 ? "1-5" : "*";
		return r + " " + week;
	}

	public static TimeWeek splitTimeAndWeek(String rule) {
		if (rule == null || rule.isBlank()) {
			return new TimeWeek("*", "*");
		}
		String[] sp = rule.trim().split("\\s+", 2);
		String time = sp[0];
		String week = sp.length > 1 ? sp[1] : "*";
		return new TimeWeek(time, week);
	}
}
