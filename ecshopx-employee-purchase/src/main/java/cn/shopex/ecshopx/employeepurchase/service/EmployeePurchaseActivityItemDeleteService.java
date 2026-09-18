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
import cn.shopex.ecshopx.employeepurchase.domain.ActivityGoods;
import cn.shopex.ecshopx.employeepurchase.domain.ActivityItems;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityGoodsMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivityItemsMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeePurchaseActivityItemDeleteService {

	private final ActivityItemsMapper activityItemsMapper;
	private final ActivityGoodsMapper activityGoodsMapper;
	private final EmployeePurchaseActivityItemsCategoryRedisService employeePurchaseActivityItemsCategoryRedisService;

	public EmployeePurchaseActivityItemDeleteService(
			ActivityItemsMapper activityItemsMapper,
			ActivityGoodsMapper activityGoodsMapper,
			EmployeePurchaseActivityItemsCategoryRedisService employeePurchaseActivityItemsCategoryRedisService) {
		this.activityItemsMapper = activityItemsMapper;
		this.activityGoodsMapper = activityGoodsMapper;
		this.employeePurchaseActivityItemsCategoryRedisService = employeePurchaseActivityItemsCategoryRedisService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteActivityItems(
			long companyId, long activityId, long itemId, boolean allSpec, long jwtDistributorId) {
		ActivityItems item =
				activityItemsMapper.selectOne(
						Wrappers.<ActivityItems>lambdaQuery()
								.eq(ActivityItems::getCompanyId, companyId)
								.eq(ActivityItems::getActivityId, activityId)
								.eq(ActivityItems::getItemId, itemId)
								.last("LIMIT 1"));
		if (item == null) {
			return;
		}
		try {
			Long goodsId = item.getGoodsId();

			if (goodsId == null || !allSpec) {
				activityItemsMapper.delete(
						Wrappers.<ActivityItems>lambdaQuery()
								.eq(ActivityItems::getCompanyId, companyId)
								.eq(ActivityItems::getActivityId, activityId)
								.eq(ActivityItems::getItemId, itemId));
			} else {
				activityItemsMapper.delete(
						Wrappers.<ActivityItems>lambdaQuery()
								.eq(ActivityItems::getCompanyId, companyId)
								.eq(ActivityItems::getActivityId, activityId)
								.eq(ActivityItems::getGoodsId, goodsId));
			}

			if (goodsId != null) {
				long remain =
						activityItemsMapper.selectCount(
								Wrappers.<ActivityItems>lambdaQuery()
										.eq(ActivityItems::getCompanyId, companyId)
										.eq(ActivityItems::getActivityId, activityId)
										.eq(ActivityItems::getGoodsId, goodsId));
				if (remain == 0) {
					activityGoodsMapper.delete(
							Wrappers.<ActivityGoods>lambdaQuery()
									.eq(ActivityGoods::getCompanyId, companyId)
									.eq(ActivityGoods::getActivityId, activityId)
									.eq(ActivityGoods::getGoodsId, goodsId));
				}
			}

			employeePurchaseActivityItemsCategoryRedisService.store(companyId, activityId, jwtDistributorId);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage() == null ? "error" : e.getMessage());
		}
	}
}
