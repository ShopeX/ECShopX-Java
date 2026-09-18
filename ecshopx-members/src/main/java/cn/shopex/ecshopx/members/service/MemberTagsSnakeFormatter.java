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

package cn.shopex.ecshopx.members.service;

import cn.shopex.ecshopx.members.domain.MemberTags;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MemberTagsSnakeFormatter {

	public Map<String, Object> toSnakeTagRow(MemberTags tag) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		Long tid = tag.getTagId();
		m.put("tag_id", tid == null ? null : String.valueOf(tid));
		Long cid = tag.getCompanyId();
		m.put("company_id", cid != null && cid <= Integer.MAX_VALUE ? cid.intValue() : cid);
		m.put("tag_name", tag.getTagName());
		m.put("description", tag.getDescription());
		m.put("tag_icon", tag.getTagIcon());
		m.put("saleman_id", tag.getSalemanId());
		m.put("tag_status", tag.getTagStatus());
		m.put("category_id", tag.getCategoryId());
		m.put("self_tag_count", tag.getSelfTagCount());
		m.put("tag_color", tag.getTagColor());
		m.put("font_color", tag.getFontColor());
		Long did = tag.getDistributorId();
		m.put("distributor_id", did != null && did <= Integer.MAX_VALUE ? did.intValue() : did);
		Long cr = tag.getCreated();
		m.put("created", cr != null && cr <= Integer.MAX_VALUE ? cr.intValue() : cr);
		Long up = tag.getUpdated();
		m.put("updated", up != null && up <= Integer.MAX_VALUE ? up.intValue() : up);
		m.put("source", tag.getSource());
		m.put("wechat_tag_id", tag.getWechatTagId());
		return m;
	}
}
