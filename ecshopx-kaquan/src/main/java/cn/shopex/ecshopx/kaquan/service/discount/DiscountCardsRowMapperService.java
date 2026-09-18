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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardsRowMapperService {

	public static Map<String, Object> snakeCaseMapFrom(DiscountCards e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("card_id", e.getCardId());
		m.put("company_id", e.getCompanyId());
		m.put("card_type", e.getCardType());
		m.put("brand_name", e.getBrandName());
		m.put("logo_url", e.getLogoUrl());
		m.put("title", e.getTitle());
		m.put("color", e.getColor());
		m.put("notice", e.getNotice());
		m.put("description", e.getDescription());
		m.put("date_type", e.getDateType());
		m.put("begin_date", e.getBeginDate());
		m.put("end_date", e.getEndDate());
		m.put("begin_time", e.getBeginDate());
		m.put("end_time", e.getEndDate());
		m.put("fixed_term", e.getFixedTerm());
		m.put("service_phone", e.getServicePhone());
		m.put("center_title", e.getCenterTitle());
		m.put("center_sub_title", e.getCenterSubTitle());
		m.put("center_url", e.getCenterUrl());
		m.put("custom_url_name", e.getCustomUrlName());
		m.put("custom_url", e.getCustomUrl());
		m.put("custom_url_sub_title", e.getCustomUrlSubTitle());
		m.put("promotion_url_name", e.getPromotionUrlName());
		m.put("promotion_url", e.getPromotionUrl());
		m.put("promotion_url_sub_title", e.getPromotionUrlSubTitle());
		m.put("get_limit", e.getGetLimit());
		m.put("use_limit", e.getUseLimit());
		m.put("can_share", e.getCanShare());
		m.put("can_give_friend", e.getCanGiveFriend());
		m.put("abstract", e.getCoverAbstract());
		m.put("icon_url_list", e.getIconUrlList());
		m.put("text_image_list", e.getTextImageList());
		m.put("time_limit", e.getTimeLimit());
		m.put("gift", e.getGift());
		m.put("default_detail", e.getDefaultDetail());
		m.put("discount", e.getDiscount());
		m.put("least_cost", e.getLeastCost());
		m.put("reduce_cost", e.getReduceCost());
		m.put("deal_detail", e.getDealDetail());
		m.put("accept_category", e.getAcceptCategory());
		m.put("reject_category", e.getRejectCategory());
		m.put("object_use_for", e.getObjectUseFor());
		m.put("can_use_with_other_discount", e.getCanUseWithOtherDiscount());
		m.put("use_platform", e.getUsePlatform());
		m.put("quantity", e.getQuantity());
		m.put("use_all_shops", e.getUseAllShops());
		m.put("rel_shops_ids", e.getRelShopsIds());
		m.put("use_scenes", e.getUseScenes());
		m.put("self_consume_code", e.getSelfConsumeCode());
		m.put("receive", e.getReceive());
		m.put("distributor_id", e.getDistributorId());
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		m.put("most_cost", e.getMostCost());
		m.put("use_bound", e.getUseBound());
		m.put("tag_ids", splitCommaIds(e.getTagIds()));
		m.put("brand_ids", splitCommaIds(e.getBrandIds()));
		m.put("apply_scope", e.getApplyScope());
		m.put("card_code", e.getCardCode());
		m.put("card_rule_code", e.getCardRuleCode());
		m.put("kq_status", e.getKqStatus());
		m.put("send_begin_time", e.getSendBeginTime());
		m.put("send_end_time", e.getSendEndTime());
		m.put("lock_time", e.getLockTime());
		m.put("source_type", e.getSourceType());
		m.put("source_id", e.getSourceId());
		m.put("dm_card_id", e.getDmCardId());
		m.put("dm_use_channel", e.getDmUseChannel());
		m.put("grade_ids", splitCsvToList(e.getGradeIds()));
		m.put("vip_grade_ids", splitCsvToList(e.getVipGradeIds()));
		m.put("coupon_type", e.getCouponType());
		m.put("guide_issue_quantity", e.getGuideIssueQuantity());
		return m;
	}

	public Map<String, Object> toSnakeCaseMap(DiscountCards e) {
		return snakeCaseMapFrom(e);
	}

	private static List<String> splitCommaIds(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim();
		if (t.startsWith(",")) {
			t = t.substring(1);
		}
		if (t.endsWith(",")) {
			t = t.substring(0, t.length() - 1);
		}
		List<String> out = new ArrayList<>();
		for (String p : t.split(",")) {
			if (StringUtils.hasText(p.trim())) {
				out.add(p.trim());
			}
		}
		return out;
	}

	private static List<String> splitCsvToList(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim();
		if (t.startsWith(",")) {
			t = t.substring(1);
		}
		if (t.endsWith(",")) {
			t = t.substring(0, t.length() - 1);
		}
		if (!StringUtils.hasText(t)) {
			return List.of();
		}
		List<String> out = new ArrayList<>();
		for (String p : t.split(",")) {
			if (StringUtils.hasText(p.trim())) {
				out.add(p.trim());
			}
		}
		return out;
	}

}
