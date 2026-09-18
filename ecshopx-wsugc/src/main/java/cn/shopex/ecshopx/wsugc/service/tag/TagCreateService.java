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

package cn.shopex.ecshopx.wsugc.service.tag;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Tag;
import cn.shopex.ecshopx.wsugc.mapper.TagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TagCreateService {

	private final TagMapper tagMapper;

	public TagCreateService(TagMapper tagMapper) {
		this.tagMapper = tagMapper;
	}

	public Map<String, Object> create(
			Map<String, Object> input,
			Map<String, Object> operatorJwt,
			boolean localProfile) {
		Object tn = input.get("tag_name");
		if (tn == null) {
			throw new ResourceException("tag名称不能为空");
		}
		String tagName = tn.toString().trim();
		if (!StringUtils.hasText(tagName)) {
			throw new ResourceException("tag名称不能为空");
		}

		long operatorId = 0L;
		if (readLong(operatorJwt.get("operator_id"), 0L) != 0L) {
			operatorId = readLong(operatorJwt.get("operator_id"), 0L);
		} else if (localProfile && input.containsKey("user_id")) {
			operatorId = readLong(input.get("user_id"), 0L);
		}

		Object mob = operatorJwt.get("mobile");
		String mobile = mob == null ? "0" : mob.toString();

		long companyId = readLong(operatorJwt.get("company_id"), 1L);

		LambdaQueryWrapper<Tag> w = new LambdaQueryWrapper<>();
		w.eq(Tag::getTagName, tagName);
		if (tagMapper.selectCount(w) > 0) {
			throw new ResourceException("同名标签已存在");
		}

		int now = (int) (System.currentTimeMillis() / 1000);

		Tag entity = new Tag();
		entity.setTagName(tagName);
		entity.setUserId(0L);
		entity.setOperatorId(operatorId);
		entity.setMobile(mobile);
		entity.setCompanyId(companyId);
		entity.setPOrder(0);
		entity.setSource(2);
		entity.setCreated(now);
		entity.setUpdated(now);

		tagMapper.insert(entity);

		return toSortedResultMap(entity);
	}

	private static TreeMap<String, Object> toSortedResultMap(Tag e) {
		TreeMap<String, Object> m = new TreeMap<>();
		m.put("ai_refuse_reason", e.getAiRefuseReason());
		m.put("ai_verify_time", e.getAiVerifyTime());
		m.put("company_id", e.getCompanyId());
		m.put("created", e.getCreated());
		m.put("enabled", e.getEnabled());
		m.put("manual_refuse_reason", e.getManualRefuseReason());
		m.put("manual_verify_time", e.getManualVerifyTime());
		m.put("message", "创建成功");
		m.put("mobile", e.getMobile());
		m.put("operator_id", e.getOperatorId());
		m.put("p_order", e.getPOrder());
		m.put("source", e.getSource());
		m.put("status", e.getStatus());
		m.put("tag_id", e.getTagId());
		m.put("tag_name", e.getTagName());
		m.put("updated", e.getUpdated());
		m.put("user_id", e.getUserId() != null ? e.getUserId().intValue() : 0);
		return m;
	}

	private static long readLong(Object v, long defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
