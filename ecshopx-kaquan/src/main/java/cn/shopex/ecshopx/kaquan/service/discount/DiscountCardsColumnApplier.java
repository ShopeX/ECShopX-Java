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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.util.StringUtils;

/**
 * 将合并后的请求/业务字段映射到 {@link DiscountCards} 实体（含金额/折扣换算、时间戳等列赋值）。
 */
public final class DiscountCardsColumnApplier {

	private DiscountCardsColumnApplier() {}

	public static void apply(DiscountCards e, Map<String, Object> data, ObjectMapper objectMapper) {
		if (data.get("company_id") != null) {
			e.setCompanyId(((Number) data.get("company_id")).longValue());
		}
		if (StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("card_type")))) {
			e.setCardType(DiscountCardParamNormalize.stringVal(data.get("card_type")));
		}
		if (data.containsKey("coupon_type")) {
			e.setCouponType(DiscountCardParamNormalize.normalizeCouponType(data.get("coupon_type")));
		}
		if (StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("title")))) {
			e.setTitle(DiscountCardParamNormalize.stringVal(data.get("title")));
		}
		if (StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("color")))) {
			e.setColor(DiscountCardParamNormalize.stringVal(data.get("color")));
		}
		if (StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("description")))) {
			e.setDescription(DiscountCardParamNormalize.stringVal(data.get("description")));
		}
		if (StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("date_type")))) {
			e.setDateType(DiscountCardParamNormalize.stringVal(data.get("date_type")));
		}
		if (data.containsKey("begin_date")) {
			e.setBeginDate(DiscountCardParamNormalize.parseIntFlexible(data.get("begin_date"), 0));
		}
		if (data.containsKey("end_date")) {
			Object ed = data.get("end_date");
			if (ed instanceof String s && !StringUtils.hasText(s)) {
				e.setEndDate(0);
			} else {
				e.setEndDate(DiscountCardParamNormalize.parseIntFlexible(ed, 0));
			}
		}
		if (data.containsKey("fixed_term")) {
			int ft = DiscountCardParamNormalize.parseIntFlexible(data.get("fixed_term"), 0);
			if (ft > 0) {
				e.setFixedTerm(ft);
			}
		}
		if (StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("service_phone")))) {
			e.setServicePhone(DiscountCardParamNormalize.stringVal(data.get("service_phone")));
		}
		if (StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("custom_url_name")))) {
			e.setCustomUrlName(DiscountCardParamNormalize.stringVal(data.get("custom_url_name")));
		}
		if (StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("custom_url")))) {
			e.setCustomUrl(DiscountCardParamNormalize.stringVal(data.get("custom_url")));
		}
		if (StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("custom_url_sub_title")))) {
			e.setCustomUrlSubTitle(DiscountCardParamNormalize.stringVal(data.get("custom_url_sub_title")));
		}
		if (data.containsKey("get_limit") && DiscountCardParamNormalize.parseIntFlexible(data.get("get_limit"), 0) > 0) {
			e.setGetLimit(DiscountCardParamNormalize.parseIntFlexible(data.get("get_limit"), 1));
		}
		if (data.containsKey("use_limit") && DiscountCardParamNormalize.parseIntFlexible(data.get("use_limit"), 0) > 0) {
			e.setUseLimit(DiscountCardParamNormalize.parseIntFlexible(data.get("use_limit"), 0));
		}
		applyOptionalStoredText(e::setCoverAbstract, data.get("abstract"), objectMapper);
		applyOptionalStoredText(e::setIconUrlList, data.get("icon_url_list"), objectMapper);
		applyOptionalStoredText(e::setTextImageList, data.get("text_image_list"), objectMapper);
		if (DiscountCardParamNormalize.hasPhpTruthyBodyValue(data.get("time_limit"))) {
			e.setTimeLimit(serializeTimeLimit(data.get("time_limit"), objectMapper));
		}
		applyOptionalStoredText(e::setGift, data.get("gift"), objectMapper);
		applyOptionalStoredText(e::setDefaultDetail, data.get("default_detail"), objectMapper);
		if (data.containsKey("discount") && data.get("discount") != null && StringUtils.hasText(String.valueOf(data.get("discount")).trim())) {
			BigDecimal d = new BigDecimal(String.valueOf(data.get("discount")).trim());
			int stored = BigDecimal.valueOf(100).subtract(d.multiply(BigDecimal.TEN)).intValue();
			e.setDiscount(stored);
		}
		if (data.containsKey("least_cost") && data.get("least_cost") != null && StringUtils.hasText(String.valueOf(data.get("least_cost")).trim())) {
			BigDecimal v = new BigDecimal(String.valueOf(data.get("least_cost")).trim());
			e.setLeastCost(v.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue());
		}
		if (data.containsKey("reduce_cost") && data.get("reduce_cost") != null && StringUtils.hasText(String.valueOf(data.get("reduce_cost")).trim())) {
			BigDecimal v = new BigDecimal(String.valueOf(data.get("reduce_cost")).trim());
			e.setReduceCost(v.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue());
		}
		if (StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("deal_detail")))) {
			e.setDealDetail(DiscountCardParamNormalize.stringVal(data.get("deal_detail")));
		}
		if (StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("accept_category")))) {
			e.setAcceptCategory(DiscountCardParamNormalize.stringVal(data.get("accept_category")));
		}
		if (StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("reject_category")))) {
			e.setRejectCategory(DiscountCardParamNormalize.stringVal(data.get("reject_category")));
		}
		if (StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("object_use_for")))) {
			e.setObjectUseFor(DiscountCardParamNormalize.stringVal(data.get("object_use_for")));
		}
		if (data.containsKey("can_use_with_other_discount")) {
			Object v = data.get("can_use_with_other_discount");
			e.setCanUseWithOtherDiscount("true".equalsIgnoreCase(String.valueOf(v)) ? "true" : "false");
		}
		if (data.containsKey("use_platform")) {
			e.setUsePlatform(DiscountCardParamNormalize.stringVal(data.get("use_platform")));
		}
		if (data.containsKey("quantity") && DiscountCardParamNormalize.parseIntFlexible(data.get("quantity"), 0) > 0) {
			e.setQuantity(DiscountCardParamNormalize.parseIntFlexible(data.get("quantity"), 0));
		}
		if (data.containsKey("guide_issue_quantity")) {
			e.setGuideIssueQuantity(DiscountCardParamNormalize.parseIntFlexible(data.get("guide_issue_quantity"), 0));
		}
		if (data.containsKey("use_all_shops")) {
			String uas = DiscountCardParamNormalize.stringVal(data.get("use_all_shops"));
			if ("true".equalsIgnoreCase(uas)) {
				e.setUseAllShops("1");
				e.setRelShopsIds(",");
				e.setDistributorId(",");
			} else {
				e.setUseAllShops("false");
				List<Object> shops = DiscountCardParamNormalize.phpArrayOrEmpty(data.get("rel_shops_ids"));
				if (!shops.isEmpty()) {
					StringBuilder sb = new StringBuilder(",");
					for (Object o : shops) {
						sb.append(o).append(",");
					}
					e.setRelShopsIds(sb.toString());
				} else {
					e.setRelShopsIds(",");
				}
				List<Object> dist = DiscountCardParamNormalize.phpArrayOrEmpty(data.get("distributor_id"));
				if (!dist.isEmpty()) {
					StringBuilder sb = new StringBuilder(",");
					for (Object o : dist) {
						sb.append(o).append(",");
					}
					e.setDistributorId(sb.toString());
				} else {
					e.setDistributorId(",");
				}
			}
		}
		if (StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("use_scenes")))) {
			e.setUseScenes(DiscountCardParamNormalize.stringVal(data.get("use_scenes")));
		}
		if (data.containsKey("self_consume_code")) {
			e.setSelfConsumeCode(DiscountCardParamNormalize.parseIntFlexible(data.get("self_consume_code"), 0));
		}
		if (data.containsKey("receive")) {
			Object r = data.get("receive");
			boolean one = Boolean.TRUE.equals(r) || "1".equals(String.valueOf(r)) || "true".equalsIgnoreCase(String.valueOf(r));
			e.setReceive(one ? "1" : "0");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		e.setCreated(now);
		e.setUpdated(now);
		if (data.containsKey("most_cost") && data.get("most_cost") != null && StringUtils.hasText(String.valueOf(data.get("most_cost")).trim())) {
			BigDecimal v = new BigDecimal(String.valueOf(data.get("most_cost")).trim());
			e.setMostCost(v.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue());
		}
		if (data.containsKey("use_bound")) {
			e.setUseBound(DiscountCardParamNormalize.parseIntFlexible(data.get("use_bound"), 0));
		}
		if (data.containsKey("tag_ids")) {
			e.setTagIds(encodeCsvIds(data.get("tag_ids")));
		}
		if (data.containsKey("brand_ids")) {
			e.setBrandIds(encodeCsvIds(data.get("brand_ids")));
		}
		if (data.containsKey("apply_scope")) {
			e.setApplyScope(DiscountCardParamNormalize.stringVal(data.get("apply_scope")));
		}
		if (data.containsKey("card_code")) {
			e.setCardCode(DiscountCardParamNormalize.stringVal(data.get("card_code")));
		}
		if (data.containsKey("card_rule_code")) {
			e.setCardRuleCode(DiscountCardParamNormalize.stringVal(data.get("card_rule_code")));
		}
		if (data.containsKey("send_end_time")) {
			e.setSendEndTime(DiscountCardParamNormalize.parseIntFlexible(data.get("send_end_time"), 0));
		}
		if (data.containsKey("send_begin_time")) {
			e.setSendBeginTime(DiscountCardParamNormalize.parseIntFlexible(data.get("send_begin_time"), 0));
		}
		if (data.containsKey("kq_status")) {
			e.setKqStatus(DiscountCardParamNormalize.parseIntFlexible(data.get("kq_status"), 0));
		}
		if (data.containsKey("lock_time")) {
			e.setLockTime(DiscountCardParamNormalize.parseIntFlexible(data.get("lock_time"), 0));
		}
		if (data.containsKey("grade_ids")) {
			e.setGradeIds(encodeCsvIds(data.get("grade_ids")));
		}
		if (data.containsKey("vip_grade_ids")) {
			e.setVipGradeIds(encodeCsvIds(data.get("vip_grade_ids")));
		}
		if (data.containsKey("source_type")) {
			e.setSourceType(DiscountCardParamNormalize.stringVal(data.get("source_type")));
		}
		if (data.containsKey("source_id")) {
			e.setSourceId(DiscountCardParamNormalize.longFromObject(data.get("source_id"), 0L));
		}
	}

	/**
	 * 仅对 {@code data} 中出现的键写回实体，供更新路径使用（不写入 {@code created}）。
	 */
	public static void applyForUpdate(DiscountCards e, Map<String, Object> data, ObjectMapper objectMapper) {
		if (data.containsKey("company_id") && data.get("company_id") != null) {
			e.setCompanyId(((Number) data.get("company_id")).longValue());
		}
		if (data.containsKey("card_type") && StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("card_type")))) {
			e.setCardType(DiscountCardParamNormalize.stringVal(data.get("card_type")));
		}
		if (data.containsKey("coupon_type")) {
			e.setCouponType(DiscountCardParamNormalize.normalizeCouponType(data.get("coupon_type")));
		}
		if (data.containsKey("title") && StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("title")))) {
			e.setTitle(DiscountCardParamNormalize.stringVal(data.get("title")));
		}
		if (data.containsKey("color") && StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("color")))) {
			e.setColor(DiscountCardParamNormalize.stringVal(data.get("color")));
		}
		if (data.containsKey("description") && StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("description")))) {
			e.setDescription(DiscountCardParamNormalize.stringVal(data.get("description")));
		}
		if (data.containsKey("date_type") && StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("date_type")))) {
			e.setDateType(DiscountCardParamNormalize.stringVal(data.get("date_type")));
		}
		if (data.containsKey("begin_date")) {
			e.setBeginDate(DiscountCardParamNormalize.parseIntFlexible(data.get("begin_date"), 0));
		}
		if (data.containsKey("end_date")) {
			Object ed = data.get("end_date");
			if (ed instanceof String s && !StringUtils.hasText(s)) {
				e.setEndDate(0);
			} else {
				e.setEndDate(DiscountCardParamNormalize.parseIntFlexible(ed, 0));
			}
		}
		if (data.containsKey("fixed_term")) {
			int ft = DiscountCardParamNormalize.parseIntFlexible(data.get("fixed_term"), 0);
			e.setFixedTerm(ft);
		}
		if (data.containsKey("service_phone") && StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("service_phone")))) {
			e.setServicePhone(DiscountCardParamNormalize.stringVal(data.get("service_phone")));
		}
		if (data.containsKey("custom_url_name") && StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("custom_url_name")))) {
			e.setCustomUrlName(DiscountCardParamNormalize.stringVal(data.get("custom_url_name")));
		}
		if (data.containsKey("custom_url") && StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("custom_url")))) {
			e.setCustomUrl(DiscountCardParamNormalize.stringVal(data.get("custom_url")));
		}
		if (data.containsKey("custom_url_sub_title")
				&& StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("custom_url_sub_title")))) {
			e.setCustomUrlSubTitle(DiscountCardParamNormalize.stringVal(data.get("custom_url_sub_title")));
		}
		if (data.containsKey("get_limit") && DiscountCardParamNormalize.parseIntFlexible(data.get("get_limit"), 0) > 0) {
			e.setGetLimit(DiscountCardParamNormalize.parseIntFlexible(data.get("get_limit"), 1));
		}
		if (data.containsKey("use_limit") && DiscountCardParamNormalize.parseIntFlexible(data.get("use_limit"), 0) > 0) {
			e.setUseLimit(DiscountCardParamNormalize.parseIntFlexible(data.get("use_limit"), 0));
		}
		if (data.containsKey("abstract")) {
			applyOptionalStoredText(e::setCoverAbstract, data.get("abstract"), objectMapper);
		}
		if (data.containsKey("icon_url_list")) {
			applyOptionalStoredText(e::setIconUrlList, data.get("icon_url_list"), objectMapper);
		}
		if (data.containsKey("text_image_list")) {
			applyOptionalStoredText(e::setTextImageList, data.get("text_image_list"), objectMapper);
		}
		if (data.containsKey("time_limit") && DiscountCardParamNormalize.hasPhpTruthyBodyValue(data.get("time_limit"))) {
			e.setTimeLimit(serializeTimeLimit(data.get("time_limit"), objectMapper));
		}
		if (data.containsKey("gift")) {
			applyOptionalStoredText(e::setGift, data.get("gift"), objectMapper);
		}
		if (data.containsKey("default_detail")) {
			applyOptionalStoredText(e::setDefaultDetail, data.get("default_detail"), objectMapper);
		}
		if (data.containsKey("discount") && data.get("discount") != null && StringUtils.hasText(String.valueOf(data.get("discount")).trim())) {
			BigDecimal d = new BigDecimal(String.valueOf(data.get("discount")).trim());
			int stored = BigDecimal.valueOf(100).subtract(d.multiply(BigDecimal.TEN)).intValue();
			e.setDiscount(stored);
		}
		if (data.containsKey("least_cost") && data.get("least_cost") != null && StringUtils.hasText(String.valueOf(data.get("least_cost")).trim())) {
			BigDecimal v = new BigDecimal(String.valueOf(data.get("least_cost")).trim());
			e.setLeastCost(v.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue());
		}
		if (data.containsKey("reduce_cost") && data.get("reduce_cost") != null && StringUtils.hasText(String.valueOf(data.get("reduce_cost")).trim())) {
			BigDecimal v = new BigDecimal(String.valueOf(data.get("reduce_cost")).trim());
			e.setReduceCost(v.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue());
		}
		if (data.containsKey("deal_detail") && StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("deal_detail")))) {
			e.setDealDetail(DiscountCardParamNormalize.stringVal(data.get("deal_detail")));
		}
		if (data.containsKey("accept_category") && StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("accept_category")))) {
			e.setAcceptCategory(DiscountCardParamNormalize.stringVal(data.get("accept_category")));
		}
		if (data.containsKey("reject_category") && StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("reject_category")))) {
			e.setRejectCategory(DiscountCardParamNormalize.stringVal(data.get("reject_category")));
		}
		if (data.containsKey("object_use_for") && StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("object_use_for")))) {
			e.setObjectUseFor(DiscountCardParamNormalize.stringVal(data.get("object_use_for")));
		}
		if (data.containsKey("can_use_with_other_discount")) {
			Object v = data.get("can_use_with_other_discount");
			e.setCanUseWithOtherDiscount("true".equalsIgnoreCase(String.valueOf(v)) ? "true" : "false");
		}
		if (data.containsKey("use_platform")) {
			e.setUsePlatform(DiscountCardParamNormalize.stringVal(data.get("use_platform")));
		}
		if (data.containsKey("quantity")) {
			e.setQuantity(DiscountCardParamNormalize.parseIntFlexible(data.get("quantity"), 0));
		}
		if (data.containsKey("guide_issue_quantity")) {
			e.setGuideIssueQuantity(DiscountCardParamNormalize.parseIntFlexible(data.get("guide_issue_quantity"), 0));
		}
		if (data.containsKey("use_all_shops")) {
			String uas = DiscountCardParamNormalize.stringVal(data.get("use_all_shops"));
			if ("true".equalsIgnoreCase(uas)) {
				e.setUseAllShops("1");
				e.setRelShopsIds(",");
				e.setDistributorId(",");
			} else {
				e.setUseAllShops("false");
				List<Object> shops = DiscountCardParamNormalize.phpArrayOrEmpty(data.get("rel_shops_ids"));
				if (!shops.isEmpty()) {
					StringBuilder sb = new StringBuilder(",");
					for (Object o : shops) {
						sb.append(o).append(",");
					}
					e.setRelShopsIds(sb.toString());
				} else {
					e.setRelShopsIds(",");
				}
				List<Object> dist = DiscountCardParamNormalize.phpArrayOrEmpty(data.get("distributor_id"));
				if (!dist.isEmpty()) {
					StringBuilder sb = new StringBuilder(",");
					for (Object o : dist) {
						sb.append(o).append(",");
					}
					e.setDistributorId(sb.toString());
				} else {
					e.setDistributorId(",");
				}
			}
		}
		if (data.containsKey("use_scenes") && StringUtils.hasText(DiscountCardParamNormalize.stringVal(data.get("use_scenes")))) {
			e.setUseScenes(DiscountCardParamNormalize.stringVal(data.get("use_scenes")));
		}
		if (data.containsKey("self_consume_code")) {
			e.setSelfConsumeCode(DiscountCardParamNormalize.parseIntFlexible(data.get("self_consume_code"), 0));
		}
		if (data.containsKey("receive")) {
			Object r = data.get("receive");
			boolean one = Boolean.TRUE.equals(r) || "1".equals(String.valueOf(r)) || "true".equalsIgnoreCase(String.valueOf(r));
			e.setReceive(one ? "1" : "0");
		}
		if (data.containsKey("most_cost") && data.get("most_cost") != null && StringUtils.hasText(String.valueOf(data.get("most_cost")).trim())) {
			BigDecimal v = new BigDecimal(String.valueOf(data.get("most_cost")).trim());
			e.setMostCost(v.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue());
		}
		if (data.containsKey("use_bound")) {
			e.setUseBound(DiscountCardParamNormalize.parseIntFlexible(data.get("use_bound"), 0));
		}
		if (data.containsKey("tag_ids")) {
			e.setTagIds(encodeCsvIds(data.get("tag_ids")));
		}
		if (data.containsKey("brand_ids")) {
			e.setBrandIds(encodeCsvIds(data.get("brand_ids")));
		}
		if (data.containsKey("apply_scope")) {
			e.setApplyScope(DiscountCardParamNormalize.stringVal(data.get("apply_scope")));
		}
		if (data.containsKey("card_code")) {
			e.setCardCode(DiscountCardParamNormalize.stringVal(data.get("card_code")));
		}
		if (data.containsKey("card_rule_code")) {
			e.setCardRuleCode(DiscountCardParamNormalize.stringVal(data.get("card_rule_code")));
		}
		if (data.containsKey("send_end_time")) {
			e.setSendEndTime(DiscountCardParamNormalize.parseIntFlexible(data.get("send_end_time"), 0));
		}
		if (data.containsKey("send_begin_time")) {
			e.setSendBeginTime(DiscountCardParamNormalize.parseIntFlexible(data.get("send_begin_time"), 0));
		}
		if (data.containsKey("kq_status")) {
			e.setKqStatus(DiscountCardParamNormalize.parseIntFlexible(data.get("kq_status"), 0));
		}
		if (data.containsKey("lock_time")) {
			e.setLockTime(DiscountCardParamNormalize.parseIntFlexible(data.get("lock_time"), 0));
		}
		if (data.containsKey("grade_ids")) {
			e.setGradeIds(encodeCsvIds(data.get("grade_ids")));
		}
		if (data.containsKey("vip_grade_ids")) {
			e.setVipGradeIds(encodeCsvIds(data.get("vip_grade_ids")));
		}
		if (data.containsKey("source_type")) {
			e.setSourceType(DiscountCardParamNormalize.stringVal(data.get("source_type")));
		}
		if (data.containsKey("source_id")) {
			e.setSourceId(DiscountCardParamNormalize.longFromObject(data.get("source_id"), 0L));
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		e.setUpdated(now);
	}

	private static String encodeCsvIds(Object raw) {
		if (raw instanceof List<?> l) {
			if (l.isEmpty()) {
				return "";
			}
			StringBuilder sb = new StringBuilder(",");
			for (Object o : l) {
				sb.append(o).append(",");
			}
			return sb.toString();
		}
		return DiscountCardParamNormalize.stringVal(raw);
	}

	private static void applyOptionalStoredText(Consumer<String> setter, Object raw, ObjectMapper objectMapper) {
		if (!DiscountCardParamNormalize.hasPhpTruthyBodyValue(raw)) {
			return;
		}
		if (raw instanceof Collection<?> || raw instanceof Map<?, ?>) {
			try {
				setter.accept(objectMapper.writeValueAsString(raw));
			} catch (JsonProcessingException e) {
				setter.accept(String.valueOf(raw));
			}
			return;
		}
		setter.accept(DiscountCardParamNormalize.stringVal(raw));
	}

	private static String serializeTimeLimit(Object tl, ObjectMapper objectMapper) {
		if (tl == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(tl);
		} catch (JsonProcessingException e) {
			return String.valueOf(tl);
		}
	}
}
