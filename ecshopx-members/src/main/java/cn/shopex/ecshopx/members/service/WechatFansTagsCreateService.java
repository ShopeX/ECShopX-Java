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
import cn.shopex.ecshopx.wechat.mp.OfficialAccountUserTagCreateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WechatFansTagsCreateService {

	private static final Logger log = LoggerFactory.getLogger(WechatFansTagsCreateService.class);

	private final WechatTagsMapper wechatTagsMapper;
	private final OfficialAccountUserTagCreateService officialAccountUserTagCreateService;

	public WechatFansTagsCreateService(
			WechatTagsMapper wechatTagsMapper,
			OfficialAccountUserTagCreateService officialAccountUserTagCreateService) {
		this.wechatTagsMapper = wechatTagsMapper;
		this.officialAccountUserTagCreateService = officialAccountUserTagCreateService;
	}

	public Map<String, Object> wxtagCreate(String authorizerAppid, long companyId, String tagName) {
		LambdaQueryWrapper<WechatTags> q = new LambdaQueryWrapper<WechatTags>()
				.eq(WechatTags::getTagName, tagName)
				.eq(WechatTags::getCompanyId, companyId)
				.eq(WechatTags::getAuthorizerAppid, authorizerAppid);
		if (wechatTagsMapper.selectOne(q) != null) {
			throw new ResourceException("标签名不能重复！");
		}

		Map<String, Object> wx = officialAccountUserTagCreateService.createTag(authorizerAppid, tagName);
		Object idObj = wx.get("id");
		long tagId;
		if (idObj instanceof Number n) {
			tagId = n.longValue();
		} else {
			throw new ResourceException("微信创建标签失败");
		}
		Object nameObj = wx.get("name");

		WechatTags row = new WechatTags();
		row.setTagId(tagId);
		row.setTagName(Objects.toString(nameObj, tagName));
		row.setAuthorizerAppid(authorizerAppid);
		row.setCompanyId(companyId);
		long nowSec = Instant.now().getEpochSecond();
		row.setCreated(nowSec);
		row.setUpdated(nowSec);

		try {
			int inserted = wechatTagsMapper.insert(row);
			if (inserted <= 0) {
				throw new ResourceException("保存标签失败");
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("wechat_tags insert failed", e);
			throw new ResourceException("保存标签失败");
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("tag_id", row.getTagId());
		out.put("tag_name", row.getTagName());
		return out;
	}
}
