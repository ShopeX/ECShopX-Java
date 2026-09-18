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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsRelGoods;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PromotionGroupsActivityAdminRowAssembler {

	private final PromotionGroupsRelGoodsReadService promotionGroupsRelGoodsReadService;

	public PromotionGroupsActivityAdminRowAssembler(
			PromotionGroupsRelGoodsReadService promotionGroupsRelGoodsReadService) {
		this.promotionGroupsRelGoodsReadService = promotionGroupsRelGoodsReadService;
	}

	public Map<String, Object> toRow(PromotionGroupsActivity e, int nowEpochSeconds) {
		Map<String, Object> m = new LinkedHashMap<>();
		long begin = e.getBeginTime() == null ? 0L : e.getBeginTime();
		long end = e.getEndTime() == null ? 0L : e.getEndTime();
		int remainingTime;
		String showStatus;
		if (begin > (long) nowEpochSeconds) {
			remainingTime = (int) (begin - (long) nowEpochSeconds);
			showStatus = "nostart";
		} else {
			long diff = end - (long) nowEpochSeconds;
			remainingTime = diff > 0 ? (int) diff : 0;
			showStatus = "noend";
		}
		m.put("groups_activity_id", e.getGroupsActivityId());
		m.put("company_id", e.getCompanyId());
		m.put("act_name", e.getActName());
		m.put("goods_id", e.getGoodsId());
		m.put("group_goods_type", e.getGroupGoodsType());
		m.put("pics", e.getPics());
		m.put("act_price", e.getActPrice());
		m.put("person_num", e.getPersonNum() == null ? 0 : e.getPersonNum().intValue());
		m.put("begin_time", e.getBeginTime());
		m.put("end_time", e.getEndTime());
		m.put("limit_buy_num", e.getLimitBuyNum());
		m.put("limit_time", e.getLimitTime());
		m.put("store", e.getStore());
		m.put("free_post", e.getFreePost());
		m.put("rig_up", e.getRigUp());
		m.put("robot", e.getRobot());
		m.put("share_desc", e.getShareDesc());
		m.put("disabled", e.getDisabled());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("remaining_time", remainingTime);
		m.put("last_seconds", remainingTime);
		m.put("show_status", showStatus);

		long companyId = e.getCompanyId() == null ? 0L : e.getCompanyId();
		long actId = e.getGroupsActivityId() == null ? 0L : e.getGroupsActivityId();
		m.put("items", buildItems(companyId, actId, e));

		return m;
	}

	private List<Map<String, Object>> buildItems(long companyId, long actId, PromotionGroupsActivity e) {
		List<PromotionGroupsRelGoods> rel = promotionGroupsRelGoodsReadService.listByActivityId(companyId, actId);
		if (rel.isEmpty()) {
			return List.of(syntheticLegacyItem(e));
		}
		return rel.stream().map(this::toItemMap).toList();
	}

	private Map<String, Object> syntheticLegacyItem(PromotionGroupsActivity e) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("item_id", e.getGoodsId());
		item.put("act_price", e.getActPrice());
		item.put("store", e.getStore());
		item.put("item_spec_desc", "");
		item.put("item_title", "");
		item.put("item_pic", "");
		return item;
	}

	private Map<String, Object> toItemMap(PromotionGroupsRelGoods rel) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("item_id", rel.getItemId());
		item.put("act_price", rel.getActivityPrice());
		item.put("store", rel.getActivityStore());
		item.put("item_spec_desc", rel.getItemSpecDesc() != null ? rel.getItemSpecDesc() : "");
		item.put("item_title", rel.getItemTitle() != null ? rel.getItemTitle() : "");
		item.put("item_pic", rel.getItemPic() != null ? rel.getItemPic() : "");
		return item;
	}
}
