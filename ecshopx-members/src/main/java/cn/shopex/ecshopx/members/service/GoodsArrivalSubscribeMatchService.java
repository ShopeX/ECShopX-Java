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

import cn.shopex.ecshopx.common.port.goods.DistributorItemTotalStorePort;
import cn.shopex.ecshopx.members.domain.SubscribeNotice;
import cn.shopex.ecshopx.members.mapper.SubscribeNoticeMapper;
import cn.shopex.ecshopx.members.service.wxapp.GoodsArrivalNoticeSubscribeRedisKeys;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Matches goods-arrival subscribe rows to an {@code ItemStoreUpdated} event.
 *
 * <p>Shop stock events ({@code distributorId > 0}) match that shop only. Headquarters stock events
 * ({@code distributorId == 0}) also match shop-scoped rows whose current {@code is_total_store} is
 * true.
 */
@Service
public class GoodsArrivalSubscribeMatchService {

	private final SubscribeNoticeMapper subscribeNoticeMapper;
	private final DistributorItemTotalStorePort distributorItemTotalStorePort;
	private final StringRedisTemplate sharedStringRedisTemplate;

	public GoodsArrivalSubscribeMatchService(
			SubscribeNoticeMapper subscribeNoticeMapper,
			DistributorItemTotalStorePort distributorItemTotalStorePort,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.subscribeNoticeMapper = subscribeNoticeMapper;
		this.distributorItemTotalStorePort = distributorItemTotalStorePort;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	public List<SubscribeNotice> listPending(
			long companyId, long itemId, int eventDistributorId, String source) {
		LambdaQueryWrapper<SubscribeNotice> q =
				basePendingQuery(companyId, itemId).eq(SubscribeNotice::getSource, source);
		if (eventDistributorId > 0) {
			q.eq(SubscribeNotice::getDistributorId, eventDistributorId);
			return subscribeNoticeMapper.selectList(q);
		}
		List<SubscribeNotice> all = subscribeNoticeMapper.selectList(q);
		return filterForHeadquartersEvent(companyId, itemId, all);
	}

	/** Marks matching pending rows SUCCESS (any source), aligned with send matching. */
	public void markSuccess(long companyId, long itemId, int eventDistributorId) {
		LambdaQueryWrapper<SubscribeNotice> q = basePendingQuery(companyId, itemId);
		if (eventDistributorId > 0) {
			q.eq(SubscribeNotice::getDistributorId, eventDistributorId);
		}
		List<SubscribeNotice> candidates = subscribeNoticeMapper.selectList(q);
		List<SubscribeNotice> matched =
				eventDistributorId > 0
						? candidates
						: filterForHeadquartersEvent(companyId, itemId, candidates);
		if (matched.isEmpty()) {
			return;
		}
		List<Long> subIds = new ArrayList<>(matched.size());
		for (SubscribeNotice row : matched) {
			if (row.getSubId() != null) {
				subIds.add(row.getSubId());
			}
		}
		if (subIds.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		subscribeNoticeMapper.update(
				null,
				new LambdaUpdateWrapper<SubscribeNotice>()
						.in(SubscribeNotice::getSubId, subIds)
						.eq(SubscribeNotice::getSubStatus, "NO")
						.set(SubscribeNotice::getSubStatus, "SUCCESS")
						.set(SubscribeNotice::getUpdated, (long) now));
		clearArrivalNoticeSubscribeRedis(companyId, matched);
	}

	/**
	 * Clears mini-program {@code is_subscribe} Redis markers after notify, so detail / SKU panel no
	 * longer show「已订阅到货通知」while stock is still zero.
	 */
	private void clearArrivalNoticeSubscribeRedis(long companyId, List<SubscribeNotice> matched) {
		for (SubscribeNotice row : matched) {
			if (row == null || row.getUserId() == null || row.getRelId() == null) {
				continue;
			}
			String source = StringUtils.hasText(row.getSource()) ? row.getSource().trim() : "wechat";
			int dist = row.getDistributorId() == null ? 0 : row.getDistributorId();
			String redisKey =
					GoodsArrivalNoticeSubscribeRedisKeys.hashKey(source, companyId, row.getRelId(), dist);
			sharedStringRedisTemplate.opsForHash().delete(redisKey, String.valueOf(row.getUserId()));
		}
	}

	private List<SubscribeNotice> filterForHeadquartersEvent(
			long companyId, long itemId, List<SubscribeNotice> rows) {
		if (rows == null || rows.isEmpty()) {
			return List.of();
		}
		Set<Integer> shopCandidates = new HashSet<>();
		for (SubscribeNotice row : rows) {
			Integer d = row.getDistributorId();
			if (d != null && d > 0) {
				shopCandidates.add(d);
			}
		}
		Set<Integer> totalStoreShops =
				distributorItemTotalStorePort.filterDistributorIdsUsingTotalStore(
						companyId, itemId, shopCandidates);
		List<SubscribeNotice> out = new ArrayList<>();
		for (SubscribeNotice row : rows) {
			int d = row.getDistributorId() == null ? 0 : row.getDistributorId();
			if (d <= 0 || totalStoreShops.contains(d)) {
				out.add(row);
			}
		}
		return out;
	}

	private static LambdaQueryWrapper<SubscribeNotice> basePendingQuery(long companyId, long itemId) {
		return new LambdaQueryWrapper<SubscribeNotice>()
				.eq(SubscribeNotice::getCompanyId, companyId)
				.eq(SubscribeNotice::getRelId, itemId)
				.eq(SubscribeNotice::getSubType, "goods")
				.eq(SubscribeNotice::getSubStatus, "NO");
	}

	public static int normalizeDistributorId(long distributorId) {
		return (int) Math.min(Math.max(distributorId, 0L), Integer.MAX_VALUE);
	}

	public static boolean hasOpenId(SubscribeNotice row) {
		return row != null && row.getOpenId() != null && StringUtils.hasText(row.getOpenId());
	}
}
