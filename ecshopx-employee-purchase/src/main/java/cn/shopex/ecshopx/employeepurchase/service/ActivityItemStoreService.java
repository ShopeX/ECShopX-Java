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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import org.springframework.stereotype.Service;

/** 对齐 PHP ActivityItemsRepository::minusActivityItemStore / addActivityItemStore。 */
@Service
public class ActivityItemStoreService {

	private final ActivityItemsMapper activityItemsMapper;

	public ActivityItemStoreService(ActivityItemsMapper activityItemsMapper) {
		this.activityItemsMapper = activityItemsMapper;
	}

	public void minusActivityItemStore(long companyId, long activityId, long itemId, int num) {
		if (num <= 0) {
			return;
		}
		int affected = activityItemsMapper.minusActivityItemStore(companyId, activityId, itemId, num);
		if (affected <= 0) {
			throw new ResourceException("库存不足");
		}
	}

	public void addActivityItemStore(long companyId, long activityId, long itemId, int num) {
		if (num <= 0) {
			return;
		}
		int affected = activityItemsMapper.addActivityItemStore(companyId, activityId, itemId, num);
		if (affected <= 0) {
			throw new ResourceException("商品不参与活动");
		}
	}
}
