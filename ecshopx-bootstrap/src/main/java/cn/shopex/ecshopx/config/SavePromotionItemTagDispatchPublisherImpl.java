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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.common.dispatch.SavePromotionItemTagJobDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test-cron")
public class SavePromotionItemTagDispatchPublisherImpl implements SavePromotionItemTagJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public SavePromotionItemTagDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publishSavePromotionItemTag(
			long companyId,
			long promotionId,
			String tagType,
			int startTime,
			int endTime,
			String itemType,
			List<Long> itemIds,
			Map<Long, BigDecimal> activityPriceByItemId) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("promotion_id", promotionId);
		payload.put("tag_type", tagType == null ? "" : tagType);
		payload.put("start_time", startTime);
		payload.put("end_time", endTime);
		payload.put("item_type", itemType == null ? "normal" : itemType);
		payload.put("item_ids", new ArrayList<>(itemIds));
		Map<String, Object> priceMap = new LinkedHashMap<>();
		if (activityPriceByItemId != null) {
			for (Map.Entry<Long, BigDecimal> en : activityPriceByItemId.entrySet()) {
				if (en.getKey() != null && en.getValue() != null) {
					priceMap.put(String.valueOf(en.getKey()), en.getValue());
				}
			}
		}
		payload.put("activity_price_by_item_id", priceMap);
		dispatchFacade.dispatchJob(
				PromotionsDispatchJobNames.SAVE_PROMOTION_ITEM_TAG,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
	}
}
