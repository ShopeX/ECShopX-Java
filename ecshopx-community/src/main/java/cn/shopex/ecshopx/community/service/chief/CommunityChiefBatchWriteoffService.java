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

package cn.shopex.ecshopx.community.service.chief;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.community.service.CommunityActivityService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.service.normal.CommunityChiefBatchWriteoffOrderListService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderZitiWriteoffService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CommunityChiefBatchWriteoffService {

	private final CommunityActivityService communityActivityService;
	private final CommunityChiefBatchWriteoffOrderListService communityChiefBatchWriteoffOrderListService;
	private final NormalOrderZitiWriteoffService normalOrderZitiWriteoffService;

	public CommunityChiefBatchWriteoffService(
			CommunityActivityService communityActivityService,
			CommunityChiefBatchWriteoffOrderListService communityChiefBatchWriteoffOrderListService,
			NormalOrderZitiWriteoffService normalOrderZitiWriteoffService) {
		this.communityActivityService = communityActivityService;
		this.communityChiefBatchWriteoffOrderListService = communityChiefBatchWriteoffOrderListService;
		this.normalOrderZitiWriteoffService = normalOrderZitiWriteoffService;
	}

	public void executeBatchWriteoff(long companyId, long chiefId, long activityId) {
		if (chiefId <= 0L) {
			throw new ForbiddenException("只有团长可以核销订单");
		}
		if (activityId <= 0L) {
			throw new ResourceException("活动ID必填");
		}
		communityActivityService.loadForChiefBatchWriteoffOrThrow(companyId, chiefId, activityId);
		List<NormalOrders> orders =
				communityChiefBatchWriteoffOrderListService.listPendingCommunityOrdersForChiefBatchWriteoff(
						companyId, activityId);
		for (NormalOrders o : orders) {
			normalOrderZitiWriteoffService.orderZitiWriteoffForChief(companyId, o.getOrderId(), chiefId);
		}
	}
}
