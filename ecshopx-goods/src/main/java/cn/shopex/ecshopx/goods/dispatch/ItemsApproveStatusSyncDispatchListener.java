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

package cn.shopex.ecshopx.goods.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.service.items.ItemsApproveStatusSyncService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ItemsApproveStatusSyncDispatchListener implements DispatchListener {

	private final ItemsApproveStatusSyncService itemsApproveStatusSyncService;

	public ItemsApproveStatusSyncDispatchListener(ItemsApproveStatusSyncService itemsApproveStatusSyncService) {
		this.itemsApproveStatusSyncService = itemsApproveStatusSyncService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (payload == null) {
			throw new BadRequestException("dispatch payload required");
		}
		Object companyRaw = payload.get("company_id");
		Object goodsRaw = payload.get("goods_id");
		Object statusRaw = payload.get("approve_status");
		if (companyRaw == null || goodsRaw == null || statusRaw == null) {
			throw new BadRequestException("company_id, goods_id and approve_status are required");
		}
		long companyId = asLong(companyRaw);
		long goodsId = asLong(goodsRaw);
		String approveStatus = String.valueOf(statusRaw).trim();
		if (approveStatus.isEmpty()) {
			throw new BadRequestException("approve_status must not be blank");
		}
		itemsApproveStatusSyncService.syncDistributorItemsForApproveStatus(companyId, goodsId, approveStatus);
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
