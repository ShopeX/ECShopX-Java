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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.goods.domain.ItemsTags;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ItemsTagsRowMaps {

	private ItemsTagsRowMaps() {
	}

	public static Map<String, Object> toTagRowMap(ItemsTags t) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("tag_id", t.getTagId());
		m.put("company_id", t.getCompanyId());
		m.put("distributor_id", t.getDistributorId() != null ? t.getDistributorId() : 0L);
		m.put("tag_name", t.getTagName());
		m.put("tag_color", t.getTagColor());
		m.put("font_color", t.getFontColor());
		m.put("description", t.getDescription());
		m.put("tag_icon", t.getTagIcon());
		m.put("front_show", t.getFrontShow() != null ? t.getFrontShow() : 0);
		m.put("created", t.getCreated() != null ? t.getCreated() : 0);
		m.put("updated", t.getUpdated() != null ? t.getUpdated() : 0);
		return m;
	}
}
