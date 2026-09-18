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

package cn.shopex.ecshopx.onecode.service;

import cn.shopex.ecshopx.onecode.domain.Batchs;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BatchsRowMapSupport {

	private BatchsRowMapSupport() {}

	public static Map<String, Object> toBatchRowMap(Batchs entity) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("batch_id", entity.getBatchId());
		m.put("company_id", entity.getCompanyId());
		m.put("thing_id", entity.getThingId());
		m.put("batch_number", entity.getBatchNumber());
		m.put("batch_name", entity.getBatchName());
		m.put("batch_quantity", entity.getBatchQuantity());
		m.put("show_trace", entity.getShowTrace());
		// json_array 空列序列化为 []；DB null 时 Jackson 为 null，与创建/更新响应体一致使用空数组
		Object trace = entity.getTraceInfo();
		m.put("trace_info", trace != null ? trace : Collections.emptyList());
		m.put("created", entity.getCreated());
		m.put("updated", entity.getUpdated());
		return m;
	}
}
