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
import cn.shopex.ecshopx.wechat.mp.OfficialAccountUserTagListService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WechatFansTagsSyncService {

	private static final Logger log = LoggerFactory.getLogger(WechatFansTagsSyncService.class);

	private final WechatTagsMapper wechatTagsMapper;
	private final OfficialAccountUserTagListService officialAccountUserTagListService;
	private final TransactionTemplate tx;

	public WechatFansTagsSyncService(
			WechatTagsMapper wechatTagsMapper,
			OfficialAccountUserTagListService officialAccountUserTagListService,
			PlatformTransactionManager platformTransactionManager) {
		this.wechatTagsMapper = wechatTagsMapper;
		this.officialAccountUserTagListService = officialAccountUserTagListService;
		this.tx = new TransactionTemplate(platformTransactionManager);
	}

	public void syncWechatTags(String authorizerAppid, long companyId) {
		List<Map<String, Object>> remoteList = officialAccountUserTagListService.listTags(authorizerAppid);

		Set<Long> syncIds = new LinkedHashSet<>();
		for (Map<String, Object> m : remoteList) {
			Object idObj = m.get("id");
			if (idObj instanceof Number n) {
				syncIds.add(n.longValue());
			} else if (idObj != null) {
				try {
					syncIds.add(Long.parseLong(String.valueOf(idObj).trim()));
				} catch (NumberFormatException ignored) {
					// skip malformed remote id
				}
			}
		}

		List<Map<String, Object>> localRows =
				wechatTagsMapper.selectTagsWithBindCount(authorizerAppid, companyId);

		Set<Long> localIds = new LinkedHashSet<>();
		for (Map<String, Object> row : localRows) {
			Object tid = row.get("tag_id");
			if (tid instanceof Number n) {
				localIds.add(n.longValue());
			} else if (tid != null) {
				try {
					localIds.add(Long.parseLong(String.valueOf(tid).trim()));
				} catch (NumberFormatException ignored) {
					// skip row with non-numeric tag_id
				}
			}
		}

		Set<Long> delIds = new HashSet<>(localIds);
		delIds.removeAll(syncIds);

		tx.executeWithoutResult(status -> {
			try {
				for (Map<String, Object> remote : remoteList) {
					Object idObj = remote.get("id");
					long tagId;
					if (idObj instanceof Number n) {
						tagId = n.longValue();
					} else if (idObj != null) {
						try {
							tagId = Long.parseLong(String.valueOf(idObj).trim());
						} catch (NumberFormatException e) {
							continue;
						}
					} else {
						continue;
					}
					String tagName = Objects.toString(remote.get("name"), "");

					LambdaQueryWrapper<WechatTags> wrapper = new LambdaQueryWrapper<WechatTags>()
							.eq(WechatTags::getTagId, tagId)
							.eq(WechatTags::getAuthorizerAppid, authorizerAppid)
							.eq(WechatTags::getCompanyId, companyId);

					WechatTags existing = wechatTagsMapper.selectOne(wrapper);
					long nowSec = Instant.now().getEpochSecond();
					if (existing == null) {
						WechatTags row = new WechatTags();
						row.setTagId(tagId);
						row.setAuthorizerAppid(authorizerAppid);
						row.setCompanyId(companyId);
						row.setTagName(tagName);
						row.setCreated(nowSec);
						row.setUpdated(nowSec);
						wechatTagsMapper.insert(row);
					} else {
						existing.setTagName(tagName);
						existing.setUpdated(nowSec);
						wechatTagsMapper.update(existing, wrapper);
					}
				}

				for (Long delId : delIds) {
					LambdaQueryWrapper<WechatTags> delWrapper = new LambdaQueryWrapper<WechatTags>()
							.eq(WechatTags::getTagId, delId)
							.eq(WechatTags::getAuthorizerAppid, authorizerAppid)
							.eq(WechatTags::getCompanyId, companyId);
					WechatTags delEntity = wechatTagsMapper.selectOne(delWrapper);
					if (delEntity == null) {
						throw new ResourceException("待删除的微信标签不存在");
					}
					wechatTagsMapper.delete(delWrapper);
				}
			} catch (ResourceException e) {
				throw e;
			} catch (Exception e) {
				log.warn("wechat_tags sync persist failed", e);
				throw new ResourceException("保存标签失败");
			}
		});
	}
}
