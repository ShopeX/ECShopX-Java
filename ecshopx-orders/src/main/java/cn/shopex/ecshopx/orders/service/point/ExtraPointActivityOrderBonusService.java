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

package cn.shopex.ecshopx.orders.service.point;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.OrderPromotionsExtraPointActivity;
import cn.shopex.ecshopx.orders.mapper.OrderKaquanVipRelUserLiteMapper;
import cn.shopex.ecshopx.orders.mapper.OrderPromotionsExtraPointActivityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 订单场景下额外积分活动贡献分（读取 promotions_extrapoint_activity，与 promotions 域表共用）。
 */
@Service
public class ExtraPointActivityOrderBonusService {

	private final OrderPromotionsExtraPointActivityMapper activityMapper;
	private final OrderKaquanVipRelUserLiteMapper vipRelUserLiteMapper;
	private final MemberAccountService memberAccountService;
	private final ObjectMapper objectMapper;
	private final ZoneId businessZoneId;

	public ExtraPointActivityOrderBonusService(
			OrderPromotionsExtraPointActivityMapper activityMapper,
			OrderKaquanVipRelUserLiteMapper vipRelUserLiteMapper,
			MemberAccountService memberAccountService,
			ObjectMapper objectMapper,
			@Value("${ecshopx.business.zone-id:}") String businessZoneIdProperty) {
		this.activityMapper = activityMapper;
		this.vipRelUserLiteMapper = vipRelUserLiteMapper;
		this.memberAccountService = memberAccountService;
		this.objectMapper = objectMapper;
		if (StringUtils.hasText(businessZoneIdProperty)) {
			this.businessZoneId = ZoneId.of(businessZoneIdProperty.trim());
		} else {
			this.businessZoneId = ZoneId.systemDefault();
		}
	}

	public int computeExtraBonus(long companyId, Map<String, Object> filter, BigDecimal basePoint) {
		if (basePoint == null || filter == null) {
			return 0;
		}
		long userId = longVal(filter.get("user_id"), 0L);
		if (userId <= 0L) {
			return 0;
		}
		long now = Instant.now().getEpochSecond();
		List<OrderPromotionsExtraPointActivity> rows =
				activityMapper.selectList(
						new LambdaQueryWrapper<OrderPromotionsExtraPointActivity>()
								.eq(OrderPromotionsExtraPointActivity::getCompanyId, companyId)
								.eq(OrderPromotionsExtraPointActivity::getActivityStatus, "valid")
								.lt(OrderPromotionsExtraPointActivity::getBeginTime, now)
								.gt(OrderPromotionsExtraPointActivity::getEndTime, now)
								.orderByAsc(OrderPromotionsExtraPointActivity::getActivityId));
		if (rows.isEmpty()) {
			return 0;
		}
		long orderShopId = longVal(filter.get("shop_id"), 0L);
		long totalFeeFen = longVal(filter.get("total_fee"), 0L);
		Map<String, Object> member = memberAccountService.getMemberInfo(userId, companyId);
		long gradeId = longVal(member.get("grade_id"), 0L);
		long nowSec = Instant.now().getEpochSecond();
		String lvType =
				vipRelUserLiteMapper.selectActiveVipType((int) companyId, userId, nowSec);
		if (lvType == null) {
			lvType = "";
		} else {
			lvType = lvType.trim();
		}

		for (OrderPromotionsExtraPointActivity row : rows) {
			if (!shopMatches(row, orderShopId)) {
				continue;
			}
			if (!validGradeMatches(row.getValidGrade(), gradeId, lvType)) {
				continue;
			}
			Map<String, Object> triggerRoot = parseTriggerRoot(row.getTriggerCondition());
			if (!triggerTimeMatches(triggerRoot, now)) {
				continue;
			}
			BigDecimal triggerYuan = triggerAmountYuan(triggerRoot);
			if (triggerYuan.compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal thresholdFen = triggerYuan.multiply(BigDecimal.valueOf(100L));
				if (thresholdFen.compareTo(BigDecimal.valueOf(totalFeeFen)) > 0) {
					continue;
				}
			}
			int piece = activityBonusPiece(row, basePoint);
			if (piece != 0) {
				return piece;
			}
		}
		return 0;
	}

	private int activityBonusPiece(OrderPromotionsExtraPointActivity row, BigDecimal basePoint) {
		String ctype = normalizeConditionType(row.getConditionType());
		Integer cv = row.getConditionValue();
		if (!StringUtils.hasText(ctype) || cv == null) {
			return 0;
		}
		if ("multiple".equalsIgnoreCase(ctype)) {
			BigDecimal factor = BigDecimal.valueOf(cv.doubleValue()).subtract(BigDecimal.ONE);
			return basePoint.multiply(factor).setScale(0, RoundingMode.HALF_UP).intValue();
		}
		if ("plus".equalsIgnoreCase(ctype)) {
			return BigDecimal.valueOf(cv.longValue()).setScale(0, RoundingMode.HALF_UP).intValue();
		}
		return 0;
	}

	private boolean shopMatches(OrderPromotionsExtraPointActivity row, long orderShopId) {
		String us = row.getUseShop() == null ? "" : row.getUseShop().trim();
		if (!"1".equals(us)) {
			return true;
		}
		String raw = row.getShopIds();
		if (!StringUtils.hasText(raw) || "all".equalsIgnoreCase(raw.trim())) {
			return true;
		}
		for (String p : raw.split(",")) {
			String t = p.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				if (Long.parseLong(t) == orderShopId) {
					return true;
				}
			} catch (NumberFormatException e) {
				// skip
			}
		}
		return false;
	}

	private boolean validGradeMatches(String validGradeJson, long gradeId, String lvType) {
		if (!StringUtils.hasText(validGradeJson)) {
			return true;
		}
		List<Object> grades;
		try {
			grades = objectMapper.readValue(validGradeJson.trim(), new TypeReference<List<Object>>() {});
		} catch (Exception e) {
			return true;
		}
		if (grades == null || grades.isEmpty()) {
			return true;
		}
		String gid = String.valueOf(gradeId);
		String lvt = lvType == null ? "" : lvType.trim();
		for (Object o : grades) {
			if (o == null) {
				continue;
			}
			String s = String.valueOf(o).trim();
			if (s.equals(gid) || (!lvt.isEmpty() && s.equals(lvt))) {
				return true;
			}
		}
		return false;
	}

	private Map<String, Object> parseTriggerRoot(String json) {
		if (!StringUtils.hasText(json)) {
			return new LinkedHashMap<>();
		}
		try {
			return objectMapper.readValue(json.trim(), new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return new LinkedHashMap<>();
		}
	}

	private boolean triggerTimeMatches(Map<String, Object> triggerRoot, long nowEpoch) {
		Object ttObj = triggerRoot.get("trigger_time");
		if (!(ttObj instanceof Map<?, ?> raw)) {
			return true;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> triggerTime = (Map<String, Object>) raw;
		String type = String.valueOf(triggerTime.getOrDefault("type", "")).trim();
		if (!StringUtils.hasText(type)) {
			return true;
		}
		var zdt = Instant.ofEpochSecond(nowEpoch).atZone(businessZoneId);
		int month = zdt.getMonthValue();
		int day = zdt.getDayOfMonth();
		int dow = zdt.getDayOfWeek().getValue();
		return switch (type) {
			case "every_year" ->
					month == parseIntLoose(triggerTime.get("month"))
							&& day == parseIntLoose(triggerTime.get("day"));
			case "every_month" -> day == parseIntLoose(triggerTime.get("day"));
			case "every_week" -> dow == parseIntLoose(triggerTime.get("week"));
			case "date" -> {
				long begin = parseEpochFlexible(triggerTime.get("begin_time"));
				long end = parseEpochFlexible(triggerTime.get("end_time"));
				if (begin <= 0L) {
					yield true;
				}
				if (end > 0L) {
					yield nowEpoch >= begin && nowEpoch <= end;
				}
				yield nowEpoch >= begin;
			}
			default -> true;
		};
	}

	private static int parseIntLoose(Object v) {
		if (v == null) {
			return -1;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static long parseEpochFlexible(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String t = v.toString().trim();
		if (t.isEmpty()) {
			return 0L;
		}
		if (t.matches("^-?\\d+$")) {
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		return 0L;
	}

	private BigDecimal triggerAmountYuan(Map<String, Object> triggerRoot) {
		Object ta = triggerRoot.get("trigger_amount");
		if (ta == null) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(ta.toString().trim());
		} catch (NumberFormatException e) {
			return BigDecimal.ZERO;
		}
	}

	private String normalizeConditionType(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		String t = raw.trim();
		try {
			Object o = objectMapper.readValue(t, Object.class);
			if (o instanceof String s) {
				return s.trim();
			}
			if (o instanceof Map<?, ?> m) {
				Object ty = m.get("type");
				return ty == null ? "" : String.valueOf(ty).trim();
			}
		} catch (Exception ignored) {
		}
		if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
			return t.substring(1, t.length() - 1).trim();
		}
		return t;
	}

	private static long longVal(Object v, long d) {
		if (v == null) {
			return d;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return d;
		}
	}
}
