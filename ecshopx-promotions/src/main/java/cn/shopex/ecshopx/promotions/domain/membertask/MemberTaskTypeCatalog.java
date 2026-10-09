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

package cn.shopex.ecshopx.promotions.domain.membertask;

import java.util.LinkedHashMap;
import java.util.Map;

/** 任务类型码与原型中文标签。 */
public final class MemberTaskTypeCatalog {

	private static final Map<String, String> CATEGORY_LABELS = new LinkedHashMap<>();

	private static final Map<String, Map<String, String>> DETAIL_LABELS = new LinkedHashMap<>();

	static {
		CATEGORY_LABELS.put("newcomer", "新人任务");
		CATEGORY_LABELS.put("purchase", "购买");
		CATEGORY_LABELS.put("share", "分享裂变");
		CATEGORY_LABELS.put("behavior", "会员行为");

		Map<String, String> newcomer = new LinkedHashMap<>();
		newcomer.put("first_order", "完成首单");
		newcomer.put("profile", "填写个人信息");
		DETAIL_LABELS.put("newcomer", Map.copyOf(newcomer));

		Map<String, String> purchase = new LinkedHashMap<>();
		purchase.put("single_spend", "完成单笔消费");
		purchase.put("total_spend", "完成累计消费");
		DETAIL_LABELS.put("purchase", Map.copyOf(purchase));

		Map<String, String> share = new LinkedHashMap<>();
		share.put("share_order", "分享好友下单");
		share.put("invite_register", "邀请好友注册");
		DETAIL_LABELS.put("share", Map.copyOf(share));

		Map<String, String> behavior = new LinkedHashMap<>();
		behavior.put("sign_in", "签到");
		behavior.put("browse", "浏览页面");
		behavior.put("favorite", "收藏");
		DETAIL_LABELS.put("behavior", Map.copyOf(behavior));
	}

	private MemberTaskTypeCatalog() {}

	public static String categoryLabel(String categoryCode) {
		if (categoryCode == null || categoryCode.isBlank()) {
			return "";
		}
		return CATEGORY_LABELS.getOrDefault(categoryCode, categoryCode);
	}

	public static String detailLabel(String categoryCode, String detailCode) {
		if (detailCode == null || detailCode.isBlank()) {
			return "";
		}
		Map<String, String> details = DETAIL_LABELS.get(categoryCode);
		if (details == null) {
			return detailCode;
		}
		return details.getOrDefault(detailCode, detailCode);
	}

	public static String typeLabel(String categoryCode, String detailCode) {
		String cat = categoryLabel(categoryCode);
		String det = detailLabel(categoryCode, detailCode);
		if (cat.isEmpty() && det.isEmpty()) {
			return "";
		}
		if (det.isEmpty()) {
			return cat;
		}
		if (cat.isEmpty()) {
			return det;
		}
		return cat + " / " + det;
	}
}
