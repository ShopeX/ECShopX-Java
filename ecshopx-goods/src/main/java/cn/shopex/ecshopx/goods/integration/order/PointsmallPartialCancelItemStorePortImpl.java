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

package cn.shopex.ecshopx.goods.integration.order;

import cn.shopex.ecshopx.common.port.order.PointsmallPartialCancelItemStorePort;
import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PointsmallPartialCancelItemStorePortImpl implements PointsmallPartialCancelItemStorePort {

	private final StringRedisTemplate companysRedisTemplate;
	private final PointsmallItemsMapper pointsmallItemsMapper;

	public PointsmallPartialCancelItemStorePortImpl(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			PointsmallItemsMapper pointsmallItemsMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.pointsmallItemsMapper = pointsmallItemsMapper;
	}

	@Override
	public boolean minusItemStore(long companyId, long itemId, int num, boolean isTotalStore) {
		if (itemId <= 0L) {
			return true;
		}
		String key = "pointsmall_item_store:" + itemId;
		Long store = companysRedisTemplate.opsForValue().increment(key, -num);
		if (store != null && store < 0L) {
			companysRedisTemplate.opsForValue().increment(key, num);
			return false;
		}
		long st = store != null ? store : 0L;
		int storeInt = (int) Math.min(st, Integer.MAX_VALUE);
		if (isTotalStore) {
			pointsmallItemsMapper.update(
					null,
					new LambdaUpdateWrapper<PointsmallItems>()
							.eq(PointsmallItems::getCompanyId, companyId)
							.eq(PointsmallItems::getItemId, itemId)
							.set(PointsmallItems::getStore, storeInt));
		}
		return true;
	}
}
