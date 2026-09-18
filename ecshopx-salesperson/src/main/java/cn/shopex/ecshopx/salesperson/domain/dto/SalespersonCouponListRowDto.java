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

package cn.shopex.ecshopx.salesperson.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Map;
import lombok.Data;

/**
 * 导购优惠券列表单行；字段与 {@code kaquan_discount_cards} 全列及联表 {@code id}、{@code send_num} 对齐。
 * <p>
 * 使用 {@link JsonInclude.Include#ALWAYS}，避免全局 {@code NON_NULL} 在序列化嵌套对象时省略 null 字段。
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SalespersonCouponListRowDto {

	private Long cardId;
	private Long companyId;
	private String cardType;
	private String brandName;
	private String logoUrl;
	private String title;
	private String color;
	private String notice;
	private String description;
	private String dateType;
	private Integer beginDate;
	private Integer endDate;
	private Integer fixedTerm;
	private String gradeIds;
	private String vipGradeIds;
	private Integer kqStatus;
	private Integer lockTime;
	private Integer sendBeginTime;
	private Integer sendEndTime;
	private String servicePhone;
	private String centerTitle;
	private String centerSubTitle;
	private String centerUrl;
	private String customUrlName;
	private String customUrl;
	private String customUrlSubTitle;
	private String promotionUrlName;
	private String promotionUrl;
	private String promotionUrlSubTitle;
	private Integer getLimit;
	private Integer useLimit;
	private String canShare;
	private String canGiveFriend;

	@JsonProperty("abstract")
	private String coverAbstract;

	private String iconUrlList;
	private String textImageList;
	private String timeLimit;
	private String gift;
	private String defaultDetail;
	private Integer discount;
	private Integer leastCost;
	private Integer reduceCost;
	private String dealDetail;
	private String acceptCategory;
	private String rejectCategory;
	private String objectUseFor;
	private String canUseWithOtherDiscount;
	private String usePlatform;
	private Integer quantity;
	private String useAllShops;
	private String relShopsIds;
	private String useScenes;
	private Integer selfConsumeCode;
	private String receive;
	private String distributorId;
	private Integer created;
	private Integer updated;
	private Integer mostCost;
	private Integer useBound;
	private String tagIds;
	private String brandIds;
	private String applyScope;
	private String cardCode;
	private String cardRuleCode;
	private String sourceType;
	private Long sourceId;
	private String dmCardId;
	private String dmUseChannel;
	private String couponType;
	private Integer guideIssueQuantity;

	/** {@code salesperson_rel_coupon.id} */
	private Long id;

	private Long sendNum;

	public static SalespersonCouponListRowDto fromRow(Map<String, Object> row) {
		SalespersonCouponListRowDto d = new SalespersonCouponListRowDto();
		if (row == null || row.isEmpty()) {
			return d;
		}
		d.setCardId(toLong(getCi(row, "card_id")));
		d.setCompanyId(toLong(getCi(row, "company_id")));
		d.setCardType(toStringOrNull(getCi(row, "card_type")));
		d.setBrandName(toStringOrNull(getCi(row, "brand_name")));
		d.setLogoUrl(toStringOrNull(getCi(row, "logo_url")));
		d.setTitle(toStringOrNull(getCi(row, "title")));
		d.setColor(toStringOrNull(getCi(row, "color")));
		d.setNotice(toStringOrNull(getCi(row, "notice")));
		d.setDescription(toStringOrNull(getCi(row, "description")));
		d.setDateType(toStringOrNull(getCi(row, "date_type")));
		d.setBeginDate(toInteger(getCi(row, "begin_date")));
		d.setEndDate(toInteger(getCi(row, "end_date")));
		d.setFixedTerm(toInteger(getCi(row, "fixed_term")));
		d.setGradeIds(toStringOrNull(getCi(row, "grade_ids")));
		d.setVipGradeIds(toStringOrNull(getCi(row, "vip_grade_ids")));
		d.setKqStatus(toInteger(getCi(row, "kq_status")));
		d.setLockTime(toInteger(getCi(row, "lock_time")));
		d.setSendBeginTime(toInteger(getCi(row, "send_begin_time")));
		d.setSendEndTime(toInteger(getCi(row, "send_end_time")));
		d.setServicePhone(toStringOrNull(getCi(row, "service_phone")));
		d.setCenterTitle(toStringOrNull(getCi(row, "center_title")));
		d.setCenterSubTitle(toStringOrNull(getCi(row, "center_sub_title")));
		d.setCenterUrl(toStringOrNull(getCi(row, "center_url")));
		d.setCustomUrlName(toStringOrNull(getCi(row, "custom_url_name")));
		d.setCustomUrl(toStringOrNull(getCi(row, "custom_url")));
		d.setCustomUrlSubTitle(toStringOrNull(getCi(row, "custom_url_sub_title")));
		d.setPromotionUrlName(toStringOrNull(getCi(row, "promotion_url_name")));
		d.setPromotionUrl(toStringOrNull(getCi(row, "promotion_url")));
		d.setPromotionUrlSubTitle(toStringOrNull(getCi(row, "promotion_url_sub_title")));
		d.setGetLimit(toInteger(getCi(row, "get_limit")));
		d.setUseLimit(toInteger(getCi(row, "use_limit")));
		d.setCanShare(toStringOrNull(getCi(row, "can_share")));
		d.setCanGiveFriend(toStringOrNull(getCi(row, "can_give_friend")));
		d.setCoverAbstract(toStringOrNull(getCi(row, "abstract")));
		d.setIconUrlList(toStringOrNull(getCi(row, "icon_url_list")));
		d.setTextImageList(toStringOrNull(getCi(row, "text_image_list")));
		d.setTimeLimit(toStringOrNull(getCi(row, "time_limit")));
		d.setGift(toStringOrNull(getCi(row, "gift")));
		d.setDefaultDetail(toStringOrNull(getCi(row, "default_detail")));
		d.setDiscount(toInteger(getCi(row, "discount")));
		d.setLeastCost(toInteger(getCi(row, "least_cost")));
		d.setReduceCost(toInteger(getCi(row, "reduce_cost")));
		d.setDealDetail(toStringOrNull(getCi(row, "deal_detail")));
		d.setAcceptCategory(toStringOrNull(getCi(row, "accept_category")));
		d.setRejectCategory(toStringOrNull(getCi(row, "reject_category")));
		d.setObjectUseFor(toStringOrNull(getCi(row, "object_use_for")));
		d.setCanUseWithOtherDiscount(toStringOrNull(getCi(row, "can_use_with_other_discount")));
		d.setUsePlatform(toStringOrNull(getCi(row, "use_platform")));
		d.setQuantity(toInteger(getCi(row, "quantity")));
		d.setUseAllShops(toStringOrNull(getCi(row, "use_all_shops")));
		d.setRelShopsIds(toStringOrNull(getCi(row, "rel_shops_ids")));
		d.setUseScenes(toStringOrNull(getCi(row, "use_scenes")));
		d.setSelfConsumeCode(toInteger(getCi(row, "self_consume_code")));
		d.setReceive(toStringOrNull(getCi(row, "receive")));
		d.setDistributorId(toStringOrNull(getCi(row, "distributor_id")));
		d.setCreated(toInteger(getCi(row, "created")));
		d.setUpdated(toInteger(getCi(row, "updated")));
		d.setMostCost(toInteger(getCi(row, "most_cost")));
		d.setUseBound(toInteger(getCi(row, "use_bound")));
		d.setTagIds(toStringOrNull(getCi(row, "tag_ids")));
		d.setBrandIds(toStringOrNull(getCi(row, "brand_ids")));
		d.setApplyScope(toStringOrNull(getCi(row, "apply_scope")));
		d.setCardCode(toStringOrNull(getCi(row, "card_code")));
		d.setCardRuleCode(toStringOrNull(getCi(row, "card_rule_code")));
		d.setSourceType(toStringOrNull(getCi(row, "source_type")));
		d.setSourceId(toLong(getCi(row, "source_id")));
		d.setDmCardId(toStringOrNull(getCi(row, "dm_card_id")));
		d.setDmUseChannel(toStringOrNull(getCi(row, "dm_use_channel")));
		d.setCouponType(toStringOrNull(getCi(row, "coupon_type")));
		d.setGuideIssueQuantity(toInteger(getCi(row, "guide_issue_quantity")));
		d.setId(toLong(getCi(row, "id")));
		d.setSendNum(toLong(getCi(row, "send_num")));
		return d;
	}

	private static Object getCi(Map<String, Object> row, String key) {
		if (row.containsKey(key)) {
			return row.get(key);
		}
		for (Map.Entry<String, Object> e : row.entrySet()) {
			String k = e.getKey();
			if (k != null && k.equalsIgnoreCase(key)) {
				return e.getValue();
			}
		}
		return null;
	}

	private static Long toLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer toInteger(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String toStringOrNull(Object o) {
		if (o == null) {
			return null;
		}
		return o.toString();
	}
}
