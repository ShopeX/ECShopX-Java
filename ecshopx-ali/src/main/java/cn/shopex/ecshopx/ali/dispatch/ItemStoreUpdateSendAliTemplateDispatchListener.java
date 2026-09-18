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

package cn.shopex.ecshopx.ali.dispatch;

import cn.shopex.ecshopx.ali.service.alitemplate.ItemStoreGoodsArrivalAliTemplateOrchestrator;
import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ItemStoreUpdateSendAliTemplateDispatchListener implements DispatchListener {

	private final ItemStoreGoodsArrivalAliTemplateOrchestrator itemStoreGoodsArrivalAliTemplateOrchestrator;

	@Override
	public void onEvent(Map<String, Object> payload) {
		long itemId = toLong(payload.get("item_id"));
		int store = toInt(payload.get("store"));
		long distributorId = toLong(payload.get("distributor_id"));
		itemStoreGoodsArrivalAliTemplateOrchestrator.dispatchIfApplicable(itemId, store, distributorId);
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
