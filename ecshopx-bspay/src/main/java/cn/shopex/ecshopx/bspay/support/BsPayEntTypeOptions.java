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

package cn.shopex.ecshopx.bspay.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BsPayEntTypeOptions {

	private static final Map<String, String> MAP;

	static {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("1", "政府机构");
		m.put("2", "国营企业");
		m.put("3", "私营企业");
		m.put("4", "外资企业");
		m.put("5", "个体工商户");
		m.put("6", "其它组织");
		m.put("7", "事业单位");
		m.put("8", "集体经济");
		MAP = Collections.unmodifiableMap(m);
	}

	private BsPayEntTypeOptions() {}

	public static Map<String, String> asMap() {
		return MAP;
	}
}
