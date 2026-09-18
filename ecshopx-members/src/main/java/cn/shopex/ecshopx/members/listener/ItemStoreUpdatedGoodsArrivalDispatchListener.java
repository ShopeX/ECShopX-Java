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

package cn.shopex.ecshopx.members.listener;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.goods.ItemCompanyIdResolver;
import cn.shopex.ecshopx.members.domain.SubscribeNotice;
import cn.shopex.ecshopx.members.mapper.SubscribeNoticeMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * When store becomes positive, marks matching goods arrival subscribe rows as notified (sub_type=goods, sub_status NO→SUCCESS).
 */
@Component
@RequiredArgsConstructor
public class ItemStoreUpdatedGoodsArrivalDispatchListener implements DispatchListener {

	private final ItemCompanyIdResolver itemCompanyIdResolver;
	private final SubscribeNoticeMapper subscribeNoticeMapper;

	@Override
	public void onEvent(Map<String, Object> payload) {
		long itemId = toLong(payload.get("item_id"));
		int store = toInt(payload.get("store"));
		long distributorId = toLong(payload.get("distributor_id"));
		if (store <= 0) {
			return;
		}
		Optional<Long> companyId = itemCompanyIdResolver.findCompanyIdByItemId(itemId);
		if (companyId.isEmpty()) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		int dist = (int) Math.min(distributorId, Integer.MAX_VALUE);
		subscribeNoticeMapper.update(
				null,
				new LambdaUpdateWrapper<SubscribeNotice>()
						.eq(SubscribeNotice::getCompanyId, companyId.get())
						.eq(SubscribeNotice::getRelId, itemId)
						.eq(SubscribeNotice::getSubType, "goods")
						.eq(SubscribeNotice::getSubStatus, "NO")
						.eq(SubscribeNotice::getDistributorId, dist)
						.set(SubscribeNotice::getSubStatus, "SUCCESS")
						.set(SubscribeNotice::getUpdated, (long) now));
	}

	private static long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw));
	}

	private static int toInt(Object raw) {
		if (raw instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(raw));
	}
}
