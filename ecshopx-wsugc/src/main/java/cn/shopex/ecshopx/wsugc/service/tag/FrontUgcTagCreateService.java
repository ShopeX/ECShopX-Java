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
import cn.shopex.ecshopx.wsugc.service.content.UgcWxContentTextCheckService;
import cn.shopex.ecshopx.wsugc.service.post.UgcPostOpenIdResolveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrontUgcTagCreateService {

	private final TagMapper tagMapper;
	private final UgcPostOpenIdResolveService ugcPostOpenIdResolveService;
	private final UgcWxContentTextCheckService ugcWxContentTextCheckService;

	public FrontUgcTagCreateService(
			TagMapper tagMapper,
			UgcPostOpenIdResolveService ugcPostOpenIdResolveService,
			UgcWxContentTextCheckService ugcWxContentTextCheckService) {
		this.tagMapper = tagMapper;
		this.ugcPostOpenIdResolveService = ugcPostOpenIdResolveService;
		this.ugcWxContentTextCheckService = ugcWxContentTextCheckService;
	}

	public Map<String, Object> createForMember(
			Map<String, Object> params,
			long userId,
			long companyId,
			String mobile) {
		Object tn = params.get("tag_name");
		if (tn == null) {
			throw new ResourceException("标签名称不能为空");
		}
		String tagName = tn.toString().trim();
		if (!StringUtils.hasText(tagName)) {
			throw new ResourceException("标签名称不能为空");
		}

		LambdaQueryWrapper<Tag> w = new LambdaQueryWrapper<>();
		w.eq(Tag::getTagName, tagName);
		if (tagMapper.selectCount(w) > 0) {
			throw new ResourceException("同名标签已存在");
		}

		String openId = ugcPostOpenIdResolveService.resolveOpenId(userId, companyId);
		int titleStatus = ugcWxContentTextCheckService.checkTextStatus(companyId, tagName, openId);

		int now = (int) (System.currentTimeMillis() / 1000);

		Tag entity = new Tag();
		entity.setTagName(tagName);
		entity.setUserId(userId);
		entity.setMobile(mobile);
		entity.setCompanyId(companyId);
		entity.setPOrder(0);
		entity.setStatus(titleStatus);
		entity.setOperatorId(0L);
		entity.setSource(1);
		entity.setCreated(now);
		entity.setUpdated(now);

		tagMapper.insert(entity);

		return toSortedResultMapWithMessage(entity);
	}

	private static TreeMap<String, Object> toSortedResultMapWithMessage(Tag e) {
		TreeMap<String, Object> m = new TreeMap<>();
		m.put("ai_refuse_reason", e.getAiRefuseReason());
		m.put("ai_verify_time", e.getAiVerifyTime());
		m.put("company_id", e.getCompanyId());
		m.put("created", e.getCreated());
		m.put("enabled", e.getEnabled());
		m.put("manual_refuse_reason", e.getManualRefuseReason());
		m.put("manual_verify_time", e.getManualVerifyTime());
		m.put("message", resolveSuccessMessage(e.getStatus()));
		// 成功响应：不返回 mobile；source/operator_id 固定为 null（DB 实体仍为 1/0）
		m.put("operator_id", null);
		m.put("p_order", e.getPOrder());
		m.put("source", null);
		m.put("status", e.getStatus());
		m.put("tag_id", e.getTagId());
		m.put("tag_name", e.getTagName());
		m.put("updated", e.getUpdated());
		m.put("user_id", e.getUserId() != null ? e.getUserId().intValue() : 0);
		return m;
	}

	private static String resolveSuccessMessage(Integer status) {
		if (status != null && status == 1) {
			return "创建成功";
		}
		if (status != null && status == 4) {
			return "标签违规，审核失败，请修改后重新提交";
		}
		if (status != null && status == 0) {
			return "标签已提交，人工审核中";
		}
		return "标签已提交，人工审核中";
	}
}
