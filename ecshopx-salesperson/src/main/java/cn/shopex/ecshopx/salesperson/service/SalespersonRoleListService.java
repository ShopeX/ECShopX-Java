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

package cn.shopex.ecshopx.salesperson.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SalespersonRoleListService {

	public LinkedHashMap<String, Map<String, String>> getSalesmanRoleList() {
		LinkedHashMap<String, Map<String, String>> out = new LinkedHashMap<>();
		out.put("1", entry("1", "发货管理"));
		out.put("2", entry("2", "导购数据"));
		out.put("3", entry("3", "售后管理"));
		out.put("4", entry("4", "订单操作记录"));
		return out;
	}

	private static LinkedHashMap<String, String> entry(String key, String name) {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("key", key);
		m.put("name", name);
		return m;
	}
}
