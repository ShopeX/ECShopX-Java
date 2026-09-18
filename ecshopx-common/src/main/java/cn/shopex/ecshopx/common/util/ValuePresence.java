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

import java.util.Collection;
import java.util.Map;

/**
 * 判断值是否具有实际意义（非空、非零、非空串、非空集合）。
 */
public final class ValuePresence {

    private ValuePresence() {}

    /**
     * 判断给定值是否有有效含义。
     * 以下情况返回 false：null、空字符串、"0"、数值 0、false、空集合/Map。
     * 其余返回 true。
     */
    public static boolean hasEffectiveValue(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0;
        if (v instanceof CharSequence cs) {
            String s = cs.toString().trim();
            return !s.isEmpty() && !"0".equals(s);
        }
        if (v instanceof Collection<?> c) return !c.isEmpty();
        if (v instanceof Map<?, ?> m) return !m.isEmpty();
        return true;
    }
}
