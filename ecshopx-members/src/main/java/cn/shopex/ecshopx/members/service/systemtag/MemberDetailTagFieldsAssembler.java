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

import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberTags;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MemberDetailTagFieldsAssembler {

	public Map<String, Object> assemble(
			List<MemberTags> tags, Map<Long, MemberRelTags> relByTagId, long snapshotAt) {
		List<Map<String, Object>> tagList = new ArrayList<>();
		List<Map<String, Object>> operationTags = new ArrayList<>();
		List<Map<String, Object>> systemTags = new ArrayList<>();
		if (tags != null) {
			for (MemberTags tag : tags) {
				if (tag == null || tag.getTagId() == null) {
					continue;
				}
				tagList.add(toMixedTagListItem(tag));
				if (MemberSystemTagCodes.isSystemKind(tag.getTagKind())) {
					systemTags.add(toSystemTagItem(tag, relByTagId.get(tag.getTagId())));
				} else {
					operationTags.add(toOperationTagItem(tag));
				}
			}
		}
		LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
		summary.put("operation_count", operationTags.size());
		summary.put("system_count", systemTags.size());
		summary.put("snapshot_at", snapshotAt);

		LinkedHashMap<String, Object> permissions = new LinkedHashMap<>();
		permissions.put("can_view", Boolean.TRUE);
		permissions.put("can_add_operation", Boolean.TRUE);
		permissions.put("can_remove_operation", Boolean.TRUE);
		permissions.put("can_edit_system", Boolean.FALSE);

		LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
		fields.put("tagList", tagList);
		fields.put("operation_tags", operationTags);
		fields.put("system_tags", systemTags);
		fields.put("tag_summary", summary);
		fields.put("tag_permissions", permissions);
		fields.put("tag_data_status", "ready");
		return fields;
	}

	private static LinkedHashMap<String, Object> toMixedTagListItem(MemberTags t) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("tag_id", t.getTagId());
		m.put("company_id", t.getCompanyId());
		m.put("tag_name", t.getTagName());
		m.put("description", t.getDescription());
		m.put("tag_icon", t.getTagIcon());
		m.put("saleman_id", t.getSalemanId());
		m.put("tag_status", t.getTagStatus());
		m.put("category_id", t.getCategoryId());
		m.put("self_tag_count", t.getSelfTagCount());
		m.put("tag_color", t.getTagColor());
		m.put("font_color", t.getFontColor());
		m.put("distributor_id", t.getDistributorId());
		m.put("created", t.getCreated());
		m.put("updated", t.getUpdated());
		m.put(
				"type",
				MemberSystemTagCodes.isSystemKind(t.getTagKind())
						? MemberSystemTagCodes.KIND_SYSTEM
						: MemberSystemTagCodes.KIND_MANUAL);
		m.put("code", t.getTagCode());
		return m;
	}

	private static LinkedHashMap<String, Object> toOperationTagItem(MemberTags t) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", t.getTagId());
		m.put("name", t.getTagName());
		m.put("type", MemberSystemTagCodes.KIND_MANUAL);
		m.put("updated_at", t.getUpdated());
		return m;
	}

	private static LinkedHashMap<String, Object> toSystemTagItem(MemberTags t, MemberRelTags rel) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", t.getTagId());
		m.put("code", t.getTagCode());
		m.put("name", t.getTagName());
		m.put("type", MemberSystemTagCodes.KIND_SYSTEM);
		m.put("description", t.getDescription());
		m.put("rule_version", t.getRuleVersion() == null ? 1 : t.getRuleVersion());
		m.put("effective_at", rel == null || rel.getEffectiveAt() == null ? 0L : rel.getEffectiveAt());
		long updatedAt = t.getUpdated() == null ? 0L : t.getUpdated();
		if (rel != null && rel.getUpdatedAt() != null && rel.getUpdatedAt() > 0L) {
			updatedAt = rel.getUpdatedAt();
		}
		m.put("updated_at", updatedAt);
		return m;
	}
}
