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

import java.util.Map;
import org.springframework.util.StringUtils;

public final class DistributorTagsCreateParamValidator {

	private DistributorTagsCreateParamValidator() {
	}

	/**
	 * Validates create-tag input in field order.
	 *
	 * @return first validation message, or {@code null} if valid
	 */
	public static String firstValidationErrorOrNull(Map<String, Object> merged) {
		String err = requireNonBlankTrimmed(merged, "tag_name", "标签名称不能为空");
		if (err != null) {
			return err;
		}
		err = requireNonBlankTrimmed(merged, "tag_color", "标签颜色");
		if (err != null) {
			return err;
		}
		err = requireNonBlankTrimmed(merged, "font_color", "标签字体颜色");
		if (err != null) {
			return err;
		}
		return validateFrontShow(merged);
	}

	/**
	 * Update-tag validation: required name/colors; {@code front_show} only when present in {@code merged}.
	 *
	 * @return first validation message, or {@code null} if valid
	 */
	public static String firstValidationErrorOrNullForUpdate(Map<String, Object> merged) {
		String err = requireNonBlankTrimmed(merged, "tag_name", "标签名称不能为空");
		if (err != null) {
			return err;
		}
		err = requireNonBlankTrimmed(merged, "tag_color", "标签颜色");
		if (err != null) {
			return err;
		}
		err = requireNonBlankTrimmed(merged, "font_color", "标签字体颜色");
		if (err != null) {
			return err;
		}
		if (merged.containsKey("front_show")) {
			return validateFrontShowInRuleOrNull(merged);
		}
		return null;
	}

	private static String requireNonBlankTrimmed(Map<String, Object> merged, String key, String message) {
		if (!merged.containsKey(key)) {
			return message;
		}
		Object v = merged.get(key);
		if (v == null) {
			return message;
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return message;
		}
		return null;
	}

	/** {@code front_show} 必须为 0 或 1，缺失/空/无效 → 前台显示类型错误 */
	private static String validateFrontShow(Map<String, Object> merged) {
		if (!merged.containsKey("front_show")) {
			return "前台显示类型错误";
		}
		Object v = merged.get("front_show");
		if (v == null) {
			return "前台显示类型错误";
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return "前台显示类型错误";
		}
		if (v instanceof Number n) {
			int iv = n.intValue();
			if (iv != 0 && iv != 1) {
				return "前台显示类型错误";
			}
			return null;
		}
		if (!"0".equals(s) && !"1".equals(s)) {
			return "前台显示类型错误";
		}
		return null;
	}

	/** {@code in:0,1} when {@code front_show} is present; does not require the key to exist. */
	private static String validateFrontShowInRuleOrNull(Map<String, Object> merged) {
		Object v = merged.get("front_show");
		if (v == null) {
			return "前台显示类型错误";
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return "前台显示类型错误";
		}
		if (v instanceof Number n) {
			int iv = n.intValue();
			if (iv != 0 && iv != 1) {
				return "前台显示类型错误";
			}
			return null;
		}
		if (!"0".equals(s) && !"1".equals(s)) {
			return "前台显示类型错误";
		}
		return null;
	}
}
