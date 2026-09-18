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
import cn.shopex.ecshopx.employeepurchase.domain.Activities;
import cn.shopex.ecshopx.employeepurchase.dto.ActivityGoodsInsertRow;
import cn.shopex.ecshopx.employeepurchase.dto.ActivityItemInsertRow;
import cn.shopex.ecshopx.employeepurchase.mapper.ActivitiesMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeePurchaseActivityItemInsertMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeePurchaseActivityItemWriteService {

	private final ActivitiesMapper activitiesMapper;
	private final ItemsMapper itemsMapper;
	private final EmployeePurchaseActivityItemInsertMapper employeePurchaseActivityItemInsertMapper;
	private final EmployeePurchaseActivityItemsCategoryRedisService employeePurchaseActivityItemsCategoryRedisService;

	public EmployeePurchaseActivityItemWriteService(
			ActivitiesMapper activitiesMapper,
			ItemsMapper itemsMapper,
			EmployeePurchaseActivityItemInsertMapper employeePurchaseActivityItemInsertMapper,
			EmployeePurchaseActivityItemsCategoryRedisService employeePurchaseActivityItemsCategoryRedisService) {
		this.activitiesMapper = activitiesMapper;
		this.itemsMapper = itemsMapper;
		this.employeePurchaseActivityItemInsertMapper = employeePurchaseActivityItemInsertMapper;
		this.employeePurchaseActivityItemsCategoryRedisService = employeePurchaseActivityItemsCategoryRedisService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void writeOneBatch(
			long activityId,
			long companyId,
			List<Long> itemIds,
			long jwtDistributorId,
			Integer activityStoreOverride) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		try {
			Activities activity = activitiesMapper.selectOne(
					Wrappers.<Activities>lambdaQuery().eq(Activities::getId, activityId));
			if (activity == null) {
				throw new ResourceException("活动不存在");
			}
			List<Items> itemRows = itemsMapper.selectList(
					Wrappers.<Items>lambdaQuery()
							.eq(Items::getCompanyId, companyId)
							.in(Items::getItemId, itemIds)
							.select(Items::getItemId, Items::getGoodsId, Items::getPrice, Items::getStore));
			if (itemRows.isEmpty()) {
				throw new ResourceException("请选择活动商品");
			}
			int now = (int) (System.currentTimeMillis() / 1000);
			List<ActivityItemInsertRow> itemInsertRows = new ArrayList<>();
			Map<Long, ActivityGoodsInsertRow> goodsByGoodsId = new LinkedHashMap<>();
			for (Items it : itemRows) {
				Long iid = it.getItemId();
				Long gid = it.getGoodsId();
				if (iid == null || gid == null) {
					continue;
				}
				ActivityItemInsertRow ir = new ActivityItemInsertRow();
				ir.setActivityId(activityId);
				ir.setItemId(iid);
				ir.setGoodsId(gid);
				ir.setCompanyId(companyId);
				ir.setActivityPrice(it.getPrice() != null ? it.getPrice() : 0);
				if (activityStoreOverride != null) {
					ir.setActivityStore(activityStoreOverride);
				} else {
					ir.setActivityStore(it.getStore() != null ? it.getStore() : 0);
				}
				ir.setLimitFee(0);
				ir.setLimitNum(0);
				ir.setSort(0);
				ir.setCreated(now);
				itemInsertRows.add(ir);
				if (!goodsByGoodsId.containsKey(gid)) {
					ActivityGoodsInsertRow gr = new ActivityGoodsInsertRow();
					gr.setActivityId(activityId);
					gr.setGoodsId(gid);
					gr.setCompanyId(companyId);
					goodsByGoodsId.put(gid, gr);
				}
			}
			if (!itemInsertRows.isEmpty()) {
				employeePurchaseActivityItemInsertMapper.insertIgnoreActivityItems(itemInsertRows);
			}
			List<ActivityGoodsInsertRow> goodsRows = new ArrayList<>(goodsByGoodsId.values());
			if (!goodsRows.isEmpty()) {
				employeePurchaseActivityItemInsertMapper.insertIgnoreActivityGoods(goodsRows);
			}
			employeePurchaseActivityItemsCategoryRedisService.store(companyId, activityId, jwtDistributorId);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException(e.getMessage() == null ? "error" : e.getMessage());
		}
	}
}
