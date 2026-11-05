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

package cn.shopex.ecshopx.members.service.systemtag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/** 标签组列表可选插入虚拟「系统标签」组（人群选择器 with_system）。 */
public final class MemberTagGroupWithSystemHelper {

	public static final String VIRTUAL_GROUP_ID = "system";
	public static final String VIRTUAL_GROUP_NAME = "系统标签";

	private MemberTagGroupWithSystemHelper() {}

	public static boolean parseWithSystem(String raw) {
		if (!StringUtils.hasText(raw)) {
			return false;
		}
		String t = raw.trim();
		return "1".equals(t) || "true".equalsIgnoreCase(t) || "yes".equalsIgnoreCase(t);
	}

	public static boolean shouldPrepend(boolean withSystem, int page) {
		return withSystem && page <= 1;
	}

	public static Map<String, Object> buildVirtualSystemGroup(long companyId, List<Map<String, Object>> tags) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("group_id", VIRTUAL_GROUP_ID);
		row.put("group_name", VIRTUAL_GROUP_NAME);
		row.put("description", "");
		row.put("company_id", companyId <= Integer.MAX_VALUE ? (int) companyId : companyId);
		row.put("distributor_id", 0);
		row.put("wechat_group_id", null);
		row.put("created", null);
		row.put("updated", null);
		row.put("tags", tags == null ? List.of() : new ArrayList<>(tags));
		return row;
	}
}
