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
import cn.shopex.ecshopx.members.domain.WechatFansBindWechatTag;
import cn.shopex.ecshopx.members.domain.WechatTags;
import cn.shopex.ecshopx.members.mapper.WechatFansBindWechatTagMapper;
import cn.shopex.ecshopx.members.mapper.WechatTagsMapper;
import cn.shopex.ecshopx.wechat.mp.OfficialAccountUserTagDeleteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WechatFansTagsDeleteService {

	private static final Logger log = LoggerFactory.getLogger(WechatFansTagsDeleteService.class);

	private final OfficialAccountUserTagDeleteService officialAccountUserTagDeleteService;
	private final WechatTagsMapper wechatTagsMapper;
	private final WechatFansBindWechatTagMapper wechatFansBindWechatTagMapper;
	private final TransactionTemplate tx;

	public WechatFansTagsDeleteService(
			OfficialAccountUserTagDeleteService officialAccountUserTagDeleteService,
			WechatTagsMapper wechatTagsMapper,
			WechatFansBindWechatTagMapper wechatFansBindWechatTagMapper,
			PlatformTransactionManager platformTransactionManager) {
		this.officialAccountUserTagDeleteService = officialAccountUserTagDeleteService;
		this.wechatTagsMapper = wechatTagsMapper;
		this.wechatFansBindWechatTagMapper = wechatFansBindWechatTagMapper;
		this.tx = new TransactionTemplate(platformTransactionManager);
	}

	public Map<String, Object> wxtagDelete(String authorizerAppid, long companyId, long tagId) {
		officialAccountUserTagDeleteService.deleteTag(authorizerAppid, tagId);
		return tx.execute(status -> {
			LambdaQueryWrapper<WechatTags> q = new LambdaQueryWrapper<WechatTags>()
					.eq(WechatTags::getTagId, tagId)
					.eq(WechatTags::getAuthorizerAppid, authorizerAppid)
					.eq(WechatTags::getCompanyId, companyId);
			WechatTags row = wechatTagsMapper.selectOne(q);
			if (row == null) {
				throw new ResourceException("tag_id=" + tagId + "的微信标签不存在");
			}
			try {
				int deletedTags = wechatTagsMapper.delete(q);
				if (deletedTags <= 0) {
					throw new ResourceException("删除标签失败");
				}
				LambdaQueryWrapper<WechatFansBindWechatTag> bq =
						new LambdaQueryWrapper<WechatFansBindWechatTag>()
								.eq(WechatFansBindWechatTag::getTagId, tagId)
								.eq(WechatFansBindWechatTag::getAuthorizerAppid, authorizerAppid)
								.eq(WechatFansBindWechatTag::getCompanyId, companyId);
				wechatFansBindWechatTagMapper.delete(bq);
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				log.warn("wechat_tags delete failed", e);
				throw new ResourceException("删除标签失败");
			}
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("tag_id", tagId);
			return out;
		});
	}
}
