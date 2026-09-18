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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.domain.Topic;
import cn.shopex.ecshopx.wsugc.mapper.TopicMapper;
import cn.shopex.ecshopx.wsugc.service.content.UgcWxContentTextCheckService;
import cn.shopex.ecshopx.wsugc.service.post.UgcPostOpenIdResolveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrontUgcTopicCreateService {

	private final TopicMapper topicMapper;
	private final UgcPostOpenIdResolveService ugcPostOpenIdResolveService;
	private final UgcWxContentTextCheckService ugcWxContentTextCheckService;

	public FrontUgcTopicCreateService(
			TopicMapper topicMapper,
			UgcPostOpenIdResolveService ugcPostOpenIdResolveService,
			UgcWxContentTextCheckService ugcWxContentTextCheckService) {
		this.topicMapper = topicMapper;
		this.ugcPostOpenIdResolveService = ugcPostOpenIdResolveService;
		this.ugcWxContentTextCheckService = ugcWxContentTextCheckService;
	}

	public Map<String, Object> createForMember(
			Map<String, Object> params,
			long userId,
			long companyIdForInsert,
			String mobile) {
		Object tn = params.get("topic_name");
		if (tn == null) {
			throw new ResourceException("话题名称不能为空");
		}
		String topicName = tn.toString().trim();
		if (!StringUtils.hasText(topicName)) {
			throw new ResourceException("话题名称不能为空");
		}

		Long inputCompanyIdForDup = parseNullableCompanyIdFromInput(params);

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

		String openId = ugcPostOpenIdResolveService.resolveOpenId(userId, companyIdForInsert);
		int titleStatus = ugcWxContentTextCheckService.checkTextStatus(companyIdForInsert, topicName, openId);

		int now = (int) (System.currentTimeMillis() / 1000);

		Topic entity = new Topic();
		entity.setTopicName(topicName);
		entity.setUserId(userId);
		entity.setMobile(mobile);
		entity.setCompanyId(companyIdForInsert);
		entity.setPOrder(0);
		entity.setIsTop(0);
		entity.setStatus(titleStatus);
		entity.setOperatorId(0L);
		entity.setSource(1);
		entity.setCreated(now);
		entity.setUpdated(now);

		topicMapper.insert(entity);

		return toSortedResultMapWithMessage(entity);
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

	private static TreeMap<String, Object> toSortedResultMapWithMessage(Topic e) {
		TreeMap<String, Object> m = new TreeMap<>();
		m.put("ai_refuse_reason", e.getAiRefuseReason());
		m.put("ai_verify_time", e.getAiVerifyTime());
		m.put("company_id", e.getCompanyId());
		m.put("created", e.getCreated());
		m.put("enabled", e.getEnabled());
		m.put("is_top", e.getIsTop());
		m.put("manual_refuse_reason", e.getManualRefuseReason());
		m.put("manual_verify_time", e.getManualVerifyTime());
		m.put("message", resolveSuccessMessage(e.getStatus()));
		m.put("operator_id", null);
		m.put("p_order", e.getPOrder());
		m.put("source", e.getSource());
		m.put("status", e.getStatus());
		m.put("topic_id", e.getTopicId());
		m.put("topic_name", e.getTopicName());
		m.put("updated", e.getUpdated());
		m.put("user_id", e.getUserId() != null ? e.getUserId().intValue() : 0);
		return m;
	}

	private static String resolveSuccessMessage(Integer status) {
		if (status != null && status == 1) {
			return "创建成功";
		}
		if (status != null && status == 4) {
			return "话题违规，审核失败，请修改后重新提交";
		}
		if (status != null && status == 0) {
			return "话题已提交，人工审核中";
		}
		return "话题已提交，人工审核中";
	}

}
