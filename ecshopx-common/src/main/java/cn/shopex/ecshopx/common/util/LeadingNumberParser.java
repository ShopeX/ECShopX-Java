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

package cn.shopex.ecshopx.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从字符串中提取前导数字段。
 */
public final class LeadingNumberParser {

    private static final Pattern LEADING_DIGITS = Pattern.compile("^[+-]?\\d+");

    private LeadingNumberParser() {}

    public static String parseAsString(String s) {
        if (s == null || s.isEmpty()) return "0";
        Matcher m = LEADING_DIGITS.matcher(s.trim());
        return m.find() ? m.group() : "0";
    }

    public static long parseAsLong(String s) {
        String digits = parseAsString(s);
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return digits.startsWith("-") ? Long.MIN_VALUE : Long.MAX_VALUE;
        }
    }
}
