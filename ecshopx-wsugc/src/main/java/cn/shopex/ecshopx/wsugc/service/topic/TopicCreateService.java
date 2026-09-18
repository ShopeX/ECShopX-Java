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

package cn.shopex.ecshopx.wsugc.service.topic;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Topic;
import cn.shopex.ecshopx.wsugc.mapper.TopicMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TopicCreateService {

	private final TopicMapper topicMapper;

	public TopicCreateService(TopicMapper topicMapper) {
		this.topicMapper = topicMapper;
	}

	public Map<String, Object> createOrUpdate(Map<String, Object> input, Map<String, Object> operatorJwt) {
		Long requestTopicId = parseOptionalTopicId(input);
		String topicName = requireTopicName(input);
		Long inputCompanyIdForDup = parseNullableCompanyIdFromInput(input);

		LambdaQueryWrapper<Topic> w = new LambdaQueryWrapper<>();
		w.eq(Topic::getTopicName, topicName);
		if (inputCompanyIdForDup == null) {
			w.isNull(Topic::getCompanyId);
		} else {
			w.eq(Topic::getCompanyId, inputCompanyIdForDup);
		}
		if (topicMapper.selectCount(w) > 0) {
			throw new ResourceException("同名话题已存在");
		}

		long operatorId = readLong(operatorJwt.get("operator_id"), 0L);
		long companyId = readLong(operatorJwt.get("company_id"), 1L);
		int now = (int) (System.currentTimeMillis() / 1000);

		if (requestTopicId == null) {
			Topic entity = new Topic();
			entity.setTopicName(topicName);
			entity.setUserId(0L);
			entity.setCompanyId(companyId);
			entity.setOperatorId(operatorId);
			entity.setPOrder(0);
			entity.setIsTop(0);
			entity.setSource(2);
			entity.setCreated(now);
			entity.setUpdated(now);
			topicMapper.insert(entity);
			return toSortedResultMap(entity, "创建话题成功");
		}

		Topic loaded = topicMapper.selectById(requestTopicId);
		if (loaded == null) {
			throw new ResourceException("未查询到更新数据");
		}
		loaded.setTopicName(topicName);
		loaded.setUserId(0L);
		loaded.setCompanyId(companyId);
		loaded.setOperatorId(operatorId);
		loaded.setPOrder(0);
		loaded.setIsTop(0);
		loaded.setSource(2);
		loaded.setUpdated(now);
		topicMapper.updateById(loaded);

		return toSortedResultMap(loaded, "更新话题成功");
	}

	private static Long parseOptionalTopicId(Map<String, Object> input) {
		if (!input.containsKey("topic_id")) {
			return null;
		}
		Object v = input.get("topic_id");
		if (v == null) {
			return null;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			long id = Long.parseLong(s);
			if (id <= 0L) {
				return null;
			}
			return id;
		} catch (NumberFormatException e) {
			throw new BadRequestException("topic_id 格式无效");
		}
	}

	private static String requireTopicName(Map<String, Object> input) {
		Object v = input.get("topic_name");
		if (v == null) {
			throw new ResourceException("话题名称不能为空");
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("话题名称不能为空");
		}
		return s;
	}

	private static Long parseNullableCompanyIdFromInput(Map<String, Object> input) {
		if (!input.containsKey("company_id")) {
			return null;
		}
		Object v = input.get("company_id");
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
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

	private static TreeMap<String, Object> toSortedResultMap(Topic e, String message) {
		TreeMap<String, Object> m = new TreeMap<>();
		m.put("ai_refuse_reason", e.getAiRefuseReason());
		m.put("ai_verify_time", e.getAiVerifyTime());
		m.put("company_id", e.getCompanyId());
		m.put("created", e.getCreated());
		m.put("enabled", e.getEnabled());
		m.put("is_top", e.getIsTop());
		m.put("manual_refuse_reason", e.getManualRefuseReason());
		m.put("manual_verify_time", e.getManualVerifyTime());
		m.put("message", message);
		m.put("mobile", e.getMobile());
		m.put("operator_id", e.getOperatorId());
		m.put("p_order", e.getPOrder());
		m.put("source", e.getSource());
		m.put("status", e.getStatus());
		m.put("topic_id", e.getTopicId());
		m.put("topic_name", e.getTopicName());
		m.put("updated", e.getUpdated());
		m.put("user_id", e.getUserId() != null ? e.getUserId().intValue() : 0);
		return m;
	}
}
