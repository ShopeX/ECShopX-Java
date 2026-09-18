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

package cn.shopex.ecshopx.members.service.browse;

import cn.shopex.ecshopx.common.members.port.MemberBrowseHistoryItemLookupPort;
import cn.shopex.ecshopx.common.members.port.MemberBrowseHistoryItemLookupPort.ItemRow;
import cn.shopex.ecshopx.members.domain.MemberBrowseHistory;
import cn.shopex.ecshopx.members.mapper.MemberBrowseHistoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MemberBrowseHistorySaveService {

	private static final Logger log = LoggerFactory.getLogger(MemberBrowseHistorySaveService.class);

	private final MemberBrowseHistoryMapper memberBrowseHistoryMapper;

	private final MemberBrowseHistoryItemLookupPort itemLookupPort;

	private final StringRedisTemplate membersRedis;

	public MemberBrowseHistorySaveService(
			MemberBrowseHistoryMapper memberBrowseHistoryMapper,
			MemberBrowseHistoryItemLookupPort itemLookupPort,
			@Qualifier("membersStringRedisTemplate") StringRedisTemplate membersRedis) {
		this.memberBrowseHistoryMapper = memberBrowseHistoryMapper;
		this.itemLookupPort = itemLookupPort;
		this.membersRedis = membersRedis;
	}

	public boolean saveBrowseHistory(long companyId, long userId, long requestItemId) {
		if (companyId <= 0L || userId <= 0L || requestItemId <= 0L) {
			log.debug("saveBrowseHistory skipped: companyId={}, userId={}, requestItemId={}", companyId, userId, requestItemId);
			return false;
		}

		Optional<ItemRow> opt = itemLookupPort.findByItemId(requestItemId);
		if (opt.isEmpty()) {
			log.debug("saveBrowseHistory item not found: requestItemId={}", requestItemId);
			return false;
		}

		ItemRow row = opt.get();
		long filterItemId = row.normalizedItemIdForFilter();

		LambdaQueryWrapper<MemberBrowseHistory> q = new LambdaQueryWrapper<>();
		q.eq(MemberBrowseHistory::getCompanyId, companyId)
				.eq(MemberBrowseHistory::getUserId, userId)
				.eq(MemberBrowseHistory::getItemId, filterItemId)
				.last("LIMIT 1");
		MemberBrowseHistory existing = memberBrowseHistoryMapper.selectOne(q);

		long now = System.currentTimeMillis() / 1000L;

		if (existing != null) {
			try {
				LambdaUpdateWrapper<MemberBrowseHistory> u = new LambdaUpdateWrapper<>();
				u.eq(MemberBrowseHistory::getCompanyId, companyId)
						.eq(MemberBrowseHistory::getUserId, userId)
						.eq(MemberBrowseHistory::getItemId, filterItemId)
						.set(MemberBrowseHistory::getUpdated, now);
				int rows = memberBrowseHistoryMapper.update(null, u);
				if (rows < 1) {
					log.debug(
							"saveBrowseHistory update affected no row: companyId={}, userId={}, filterItemId={}",
							companyId,
							userId,
							filterItemId);
					return false;
				}
				return true;
			} catch (RuntimeException e) {
				return false;
			}
		}

		try {
			incrUserHistoryItemCount(companyId, userId);
			MemberBrowseHistory insert = new MemberBrowseHistory();
			insert.setCompanyId(companyId);
			insert.setUserId(userId);
			insert.setItemId(requestItemId);
			insert.setCreated(now);
			insert.setUpdated(now);
			memberBrowseHistoryMapper.insert(insert);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	private void incrUserHistoryItemCount(long companyId, long userId) {
		String key = "memberHistory:c:" + companyId + ":u:" + userId + ":sum";
		try {
			Long count = membersRedis.opsForValue().increment(key);
			if (count != null && count <= 1L) {
				LambdaQueryWrapper<MemberBrowseHistory> cw = new LambdaQueryWrapper<>();
				cw.eq(MemberBrowseHistory::getCompanyId, companyId).eq(MemberBrowseHistory::getUserId, userId);
				long dbCount = memberBrowseHistoryMapper.selectCount(cw);
				membersRedis.opsForValue().set(key, String.valueOf(dbCount));
			}
		} catch (RuntimeException e) {
			log.debug("incrUserHistoryItemCount redis/sync failed: companyId={}, userId={}", companyId, userId, e);
		}
	}
}
