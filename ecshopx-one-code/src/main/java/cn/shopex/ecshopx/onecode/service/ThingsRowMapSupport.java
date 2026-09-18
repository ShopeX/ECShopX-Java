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

import cn.shopex.ecshopx.onecode.domain.Things;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ThingsRowMapSupport {

	private ThingsRowMapSupport() {
	}

	public static Map<String, Object> toThingRowMap(Things entity) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("thing_id", entity.getThingId());
		m.put("thing_name", entity.getThingName());
		m.put("company_id", entity.getCompanyId());
		m.put("price", entity.getPrice());
		m.put("pic", entity.getPic());
		m.put("intro", entity.getIntro());
		m.put("batch_total_count", defaultInt(entity.getBatchTotalCount()));
		m.put("batch_total_quantity", defaultInt(entity.getBatchTotalQuantity()));
		m.put("created", entity.getCreated());
		m.put("updated", entity.getUpdated());
		return m;
	}

	private static int defaultInt(Integer v) {
		return v == null ? 0 : v;
	}
}
