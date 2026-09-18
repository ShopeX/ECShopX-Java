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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.StringJoiner;

/**
 * 将多态值（Collection、Object[]、int[]、long[] 等）拼接为分隔字符串。
 */
public final class ElementJoiner {

    private ElementJoiner() {}

    public static String joinComma(Object v) {
        return join(",", v);
    }

    public static String join(String delimiter, Object v) {
        if (v == null) return "";
        if (v instanceof Collection<?> c) {
            StringJoiner sj = new StringJoiner(delimiter);
            for (Object e : c) sj.add(String.valueOf(e));
            return sj.toString();
        }
        if (v.getClass().isArray()) {
            int len = Array.getLength(v);
            StringJoiner sj = new StringJoiner(delimiter);
            for (int i = 0; i < len; i++) sj.add(String.valueOf(Array.get(v, i)));
            return sj.toString();
        }
        return String.valueOf(v);
    }
}
