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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.WechatTags;
import cn.shopex.ecshopx.members.mapper.WechatTagsMapper;
import cn.shopex.ecshopx.wechat.mp.OfficialAccountUserTagUpdateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WechatFansTagsUpdateService {

	private static final Logger log = LoggerFactory.getLogger(WechatFansTagsUpdateService.class);

	private final WechatTagsMapper wechatTagsMapper;
	private final OfficialAccountUserTagUpdateService officialAccountUserTagUpdateService;

	public WechatFansTagsUpdateService(
			WechatTagsMapper wechatTagsMapper,
			OfficialAccountUserTagUpdateService officialAccountUserTagUpdateService) {
		this.wechatTagsMapper = wechatTagsMapper;
		this.officialAccountUserTagUpdateService = officialAccountUserTagUpdateService;
	}

	public Map<String, Object> wxtagUpdate(
			String authorizerAppid, Long companyId, long tagId, String tagName) {
		LambdaQueryWrapper<WechatTags> q1 = new LambdaQueryWrapper<WechatTags>()
				.eq(WechatTags::getTagName, tagName)
				.eq(WechatTags::getCompanyId, companyId)
				.eq(WechatTags::getAuthorizerAppid, authorizerAppid);
		if (wechatTagsMapper.selectOne(q1) != null) {
			throw new ResourceException("标签名称已存在，请重新输入", 500);
		}

		officialAccountUserTagUpdateService.updateTag(authorizerAppid, tagId, tagName);

		LambdaQueryWrapper<WechatTags> q2 = new LambdaQueryWrapper<WechatTags>()
				.eq(WechatTags::getTagId, tagId)
				.eq(WechatTags::getAuthorizerAppid, authorizerAppid)
				.eq(WechatTags::getCompanyId, companyId);
		WechatTags row = wechatTagsMapper.selectOne(q2);
		if (row == null) {
			throw new ResourceException("tag_id=" + tagId + "的微信标签不存在");
		}

		row.setTagName(tagName);
		row.setUpdated(Instant.now().getEpochSecond());
		try {
			int updated = wechatTagsMapper.updateById(row);
			if (updated <= 0) {
				throw new ResourceException("保存标签失败");
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("wechat_tags update failed", e);
			throw new ResourceException("保存标签失败");
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("company_id", row.getCompanyId());
		out.put("tag_id", tagId);
		out.put("tag_name", tagName);
		return out;
	}
}
