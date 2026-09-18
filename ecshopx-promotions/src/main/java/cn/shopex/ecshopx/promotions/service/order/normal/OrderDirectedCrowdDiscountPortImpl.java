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

package cn.shopex.ecshopx.promotions.service.order.normal;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderDirectedCrowdDiscountPort;
import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.promotions.domain.SpecificCrowdDiscount;
import cn.shopex.ecshopx.promotions.domain.SpecificCrowdDiscountRelUser;
import cn.shopex.ecshopx.promotions.mapper.SpecificCrowdDiscountMapper;
import cn.shopex.ecshopx.promotions.mapper.SpecificCrowdDiscountRelUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderDirectedCrowdDiscountPortImpl implements OrderDirectedCrowdDiscountPort {

	private static final long STATUS_PUBLISHED = 2L;

	private final SpecificCrowdDiscountMapper specificCrowdDiscountMapper;
	private final SpecificCrowdDiscountRelUserMapper specificCrowdDiscountRelUserMapper;
	private final MemberRelTagsMapper memberRelTagsMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final StringRedisTemplate stringRedisTemplate;

	public OrderDirectedCrowdDiscountPortImpl(
			SpecificCrowdDiscountMapper specificCrowdDiscountMapper,
			SpecificCrowdDiscountRelUserMapper specificCrowdDiscountRelUserMapper,
			MemberRelTagsMapper memberRelTagsMapper,
			MemberTagsMapper memberTagsMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
		this.specificCrowdDiscountMapper = specificCrowdDiscountMapper;
		this.specificCrowdDiscountRelUserMapper = specificCrowdDiscountRelUserMapper;
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void applySetUserTotalDiscountIfNeeded(NormalOrderCreateParams p) {
		Map<String, Object> od = p.getOrderData();
		if (od == null) {
			return;
		}
		String orderClass = stringVal(od.get("order_class"));
		if ("pointsmall".equals(orderClass) || "community".equals(orderClass)) {
			return;
		}
		long companyId = longVal(od.get("company_id"), 0L);
		long userId = longVal(od.get("user_id"), 0L);
		if (companyId <= 0L || userId <= 0L) {
			return;
		}
		SpecificCrowdDiscount activity = resolveValidUserOrientation(companyId, userId);
		if (activity == null || activity.getStatus() == null || activity.getStatus() != STATUS_PUBLISHED) {
			return;
		}
		long activityId = activity.getId();
		long limitFen = activity.getLimitTotalMoney() == null ? Long.MAX_VALUE : activity.getLimitTotalMoney().longValue();
		long userTotalDiscount = readRedisUsedDiscountFen(companyId, userId, activity);
		if (limitFen <= userTotalDiscount) {
			return;
		}
		int now = (int) Instant.now().getEpochSecond();
		Integer cycleType = activity.getCycleType();
		if (cycleType != null && cycleType == 2) {
			long endTime = longVal(activity.getEndTime(), 0L);
			if (endTime <= now) {
				return;
			}
		}
		applyOrderDataDiscount(companyId, userId, od, activity, userTotalDiscount, limitFen);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void persistUserTotalDiscountIfNeeded(NormalOrderCreateParams p) {
		Map<String, Object> od = p.getOrderData();
		if (od == null) {
			return;
		}
		long companyId = longVal(od.get("company_id"), 0L);
		long userId = longVal(od.get("user_id"), 0L);
		if (companyId <= 0L || userId <= 0L) {
			return;
		}
		String orderId = stringVal(od.get("order_id"));
		if (!StringUtils.hasText(orderId)) {
			Map<String, Object> insert = p.getOrdersInsertResult();
			if (insert != null && insert.get("order_id") != null) {
				orderId = stringVal(insert.get("order_id"));
			}
		}
		if (!StringUtils.hasText(orderId)) {
			return;
		}
		for (Map<String, Object> entry : collectDiscountInfoEntries(od.get("discount_info"))) {
			if (!"member_tag_targeted_promotion".equals(stringVal(entry.get("type")))) {
				continue;
			}
			long activityId = longVal(entry.get("id"), 0L);
			long discountFee = longVal(entry.get("discount_fee"), 0L);
			if (activityId <= 0L || discountFee <= 0L) {
				continue;
			}
			SpecificCrowdDiscount activity =
					specificCrowdDiscountMapper.selectOne(
							new LambdaQueryWrapper<SpecificCrowdDiscount>()
									.eq(SpecificCrowdDiscount::getCompanyId, companyId)
									.eq(SpecificCrowdDiscount::getId, activityId)
									.last("LIMIT 1"));
			if (activity == null) {
				continue;
			}
			int now = (int) Instant.now().getEpochSecond();
			String specificName = resolveTagName(companyId, activity.getSpecificId());
			SpecificCrowdDiscountRelUser logRow = new SpecificCrowdDiscountRelUser();
			logRow.setCompanyId(companyId);
			logRow.setUserId(userId);
			logRow.setOrderId(orderId);
			logRow.setDiscountFee(discountFee);
			logRow.setActivityId(activityId);
			logRow.setSpecificId(activity.getSpecificId());
			logRow.setSpecificName(specificName);
			logRow.setActivityMonth(resolveActivityMonth(activity.getCycleType()));
			logRow.setActionType("plus");
			logRow.setCreated(now);
			logRow.setUpdated(now);
			specificCrowdDiscountRelUserMapper.insert(logRow);
			String redisKey = redisUserTotalDiscountKey(companyId, activity);
			String field = redisDiscountField(userId, activityId);
			stringRedisTemplate.opsForHash().increment(redisKey, field, discountFee);
		}
	}

	@SuppressWarnings("unchecked")
	private void applyOrderDataDiscount(
			long companyId,
			long userId,
			Map<String, Object> od,
			SpecificCrowdDiscount activity,
			long userTotalDiscount,
			long limitFen) {
		long totalFee = longVal(od.get("total_fee"), 0L);
		if (totalFee <= 0L) {
			return;
		}
		long freightFee = longVal(od.get("freight_fee"), 0L);
		long basisFee = freightFee > 0L ? totalFee - freightFee : totalFee;
		if (basisFee <= 0L) {
			return;
		}
		Object itemsRaw = od.get("items");
		if (!(itemsRaw instanceof List<?> rawItems) || rawItems.isEmpty()) {
			return;
		}
		List<Map<String, Object>> items = new ArrayList<>();
		for (Object o : rawItems) {
			if (o instanceof Map<?, ?> m) {
				items.add(new LinkedHashMap<>((Map<String, Object>) m));
			}
		}
		if (items.isEmpty()) {
			return;
		}
		items.sort(Comparator.comparingLong(item -> longVal(item.get("total_fee"), 0L)));
		long itemTotalFee = items.stream().mapToLong(item -> longVal(item.get("total_fee"), 0L)).sum();
		if (itemTotalFee <= 0L) {
			return;
		}
		BigDecimal payRate =
				BigDecimal.valueOf(longVal(activity.getDiscount(), 0L))
						.divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP);
		BigDecimal totalDiscountBd =
				BigDecimal.valueOf(basisFee)
						.multiply(BigDecimal.ONE.subtract(payRate))
						.setScale(0, RoundingMode.HALF_UP);
		long totalDiscountFee = totalDiscountBd.longValue();
		if (totalDiscountFee > limitFen) {
			totalDiscountFee = limitFen;
		}
		long newUserTotalDiscount = userTotalDiscount + totalDiscountFee;
		if (newUserTotalDiscount > limitFen) {
			totalDiscountFee = limitFen - userTotalDiscount;
		}
		if (totalDiscountFee <= 0L) {
			return;
		}
		long activityId = activity.getId();
		long payPercent = longVal(activity.getDiscount(), 0L);
		String specificName = resolveTagName(companyId, activity.getSpecificId());
		Map<String, Object> discountInfoTemplate = new LinkedHashMap<>();
		discountInfoTemplate.put("id", activityId);
		discountInfoTemplate.put("type", "member_tag_targeted_promotion");
		discountInfoTemplate.put("rule", "专属优惠" + payPercent + "%");
		discountInfoTemplate.put("info", "专属优惠");

		List<Map<String, Object>> itemsPromotion = mutableItemsPromotion(od.get("items_promotion"));
		int orderItemCount = items.size();
		long allocated = 0L;
		for (int i = 0; i < orderItemCount; i++) {
			Map<String, Object> item = items.get(i);
			long discountFee;
			if (i == orderItemCount - 1) {
				discountFee = totalDiscountFee - allocated;
			} else if (orderItemCount == 1) {
				discountFee = totalDiscountFee;
			} else {
				BigDecimal percent =
						BigDecimal.valueOf(longVal(item.get("total_fee"), 0L))
								.divide(BigDecimal.valueOf(itemTotalFee), 5, RoundingMode.HALF_UP)
								.setScale(4, RoundingMode.HALF_UP);
				discountFee =
						BigDecimal.valueOf(totalDiscountFee)
								.multiply(percent)
								.setScale(0, RoundingMode.HALF_UP)
								.longValue();
				allocated += discountFee;
			}
			item.put("discount_fee", intVal(item.get("discount_fee"), 0) + (int) Math.min(discountFee, Integer.MAX_VALUE));
			item.put("coupon_discount", discountFee);
			item.put("total_fee", longVal(item.get("total_fee"), 0L) - discountFee);

			Map<String, Object> itemDiscountInfo = new LinkedHashMap<>(discountInfoTemplate);
			itemDiscountInfo.put("discount_fee", discountFee);
			List<Map<String, Object>> itemDi = mutableDiscountInfoList(item.get("discount_info"));
			itemDi.add(itemDiscountInfo);
			item.put("discount_info", itemDi);

			Map<String, Object> promotionRow = new LinkedHashMap<>();
			promotionRow.put("company_id", item.get("company_id"));
			promotionRow.put("user_id", userId);
			promotionRow.put("shop_id", longVal(item.get("distributor_id"), 0L));
			promotionRow.put("item_id", item.get("item_id"));
			promotionRow.put("item_name", item.get("item_name"));
			promotionRow.put("item_type", "normal");
			promotionRow.put("order_type", "normal");
			promotionRow.put("activity_id", activityId);
			promotionRow.put("activity_type", "member_tag_targeted_promotion");
			promotionRow.put("activity_name", "定向促销");
			promotionRow.put("activity_tag", "定向促销");
			promotionRow.put("activity_desc", itemDiscountInfo);
			promotionRow.put("activity_rule", "指定会员优惠." + payPercent);
			itemsPromotion.add(promotionRow);
		}
		od.put("items", items);
		od.put("items_promotion", itemsPromotion);
		od.put("discount_fee", intVal(od.get("discount_fee"), 0) + (int) Math.min(totalDiscountFee, Integer.MAX_VALUE));
		od.put("total_fee", totalFee - totalDiscountFee);

		Map<String, Object> orderDiscountInfo = new LinkedHashMap<>(discountInfoTemplate);
		orderDiscountInfo.put("discount_fee", totalDiscountFee);
		List<Map<String, Object>> orderDi = mutableDiscountInfoList(od.get("discount_info"));
		orderDi.add(orderDiscountInfo);
		od.put("discount_info", orderDi);
	}

	private SpecificCrowdDiscount resolveValidUserOrientation(long companyId, long userId) {
		List<MemberRelTags> rels =
				memberRelTagsMapper.selectList(
						new LambdaQueryWrapper<MemberRelTags>()
								.eq(MemberRelTags::getCompanyId, companyId)
								.eq(MemberRelTags::getUserId, userId));
		if (rels == null || rels.isEmpty()) {
			return null;
		}
		Set<Long> tagIds =
				rels.stream()
						.map(MemberRelTags::getTagId)
						.filter(Objects::nonNull)
						.filter(id -> id > 0L)
						.collect(Collectors.toSet());
		if (tagIds.isEmpty()) {
			return null;
		}
		List<SpecificCrowdDiscount> candidates =
				specificCrowdDiscountMapper.selectList(
						new LambdaQueryWrapper<SpecificCrowdDiscount>()
								.eq(SpecificCrowdDiscount::getCompanyId, companyId)
								.eq(SpecificCrowdDiscount::getStatus, STATUS_PUBLISHED)
								.eq(SpecificCrowdDiscount::getSpecificType, "member_tag")
								.in(SpecificCrowdDiscount::getSpecificId, tagIds)
								.orderByDesc(SpecificCrowdDiscount::getStartTime)
								.orderByDesc(SpecificCrowdDiscount::getId)
								.last("LIMIT 1"));
		if (candidates == null || candidates.isEmpty()) {
			return null;
		}
		SpecificCrowdDiscount activity = candidates.get(0);
		Integer cycleType = activity.getCycleType();
		if (cycleType != null && cycleType == 1) {
			return activity;
		}
		int now = (int) Instant.now().getEpochSecond();
		long startTime = longVal(activity.getStartTime(), 0L);
		long endTime = longVal(activity.getEndTime(), 0L);
		if (startTime <= now && now < endTime) {
			return activity;
		}
		return null;
	}

	private long readRedisUsedDiscountFen(long companyId, long userId, SpecificCrowdDiscount activity) {
		String redisKey = redisUserTotalDiscountKey(companyId, activity);
		String field = redisDiscountField(userId, activity.getId());
		Object raw = stringRedisTemplate.opsForHash().get(redisKey, field);
		if (raw == null) {
			return 0L;
		}
		return longVal(raw, 0L);
	}

	private static String redisUserTotalDiscountKey(long companyId, SpecificCrowdDiscount activity) {
		Integer cycleType = activity.getCycleType();
		if (cycleType != null && cycleType == 1) {
			int month = LocalDate.now(ZoneId.systemDefault()).getMonthValue();
			return "userTotalDiscount:" + companyId + "_" + month;
		}
		return "userTotalDiscount:" + companyId;
	}

	private static String redisDiscountField(long userId, long activityId) {
		return "discount_" + userId + "_" + activityId;
	}

	private static String resolveActivityMonth(Integer cycleType) {
		LocalDate today = LocalDate.now(ZoneId.systemDefault());
		if (cycleType != null && cycleType == 1) {
			return String.valueOf(today.getMonthValue());
		}
		return today.getMonthValue() + "." + today.getDayOfMonth();
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> mutableDiscountInfoList(Object raw) {
		List<Map<String, Object>> list = new ArrayList<>();
		if (raw instanceof List<?> entries) {
			for (Object o : entries) {
				if (o instanceof Map<?, ?> m) {
					list.add(new LinkedHashMap<>((Map<String, Object>) m));
				}
			}
		} else if (raw instanceof Map<?, ?> map) {
			for (Object value : map.values()) {
				if (value instanceof Map<?, ?> m) {
					list.add(new LinkedHashMap<>((Map<String, Object>) m));
				}
			}
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> mutableItemsPromotion(Object raw) {
		List<Map<String, Object>> list = new ArrayList<>();
		if (raw instanceof List<?> entries) {
			for (Object o : entries) {
				if (o instanceof Map<?, ?> m) {
					list.add(new LinkedHashMap<>((Map<String, Object>) m));
				}
			}
		}
		return list;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> collectDiscountInfoEntries(Object raw) {
		List<Map<String, Object>> list = new ArrayList<>();
		if (raw instanceof List<?> entries) {
			for (Object o : entries) {
				if (o instanceof Map<?, ?> m) {
					list.add((Map<String, Object>) m);
				}
			}
		} else if (raw instanceof Map<?, ?> map) {
			for (Object value : map.values()) {
				if (value instanceof Map<?, ?> m) {
					list.add((Map<String, Object>) m);
				}
			}
		}
		return list;
	}

	private String resolveTagName(long companyId, Long tagId) {
		if (tagId == null || tagId <= 0L) {
			return "";
		}
		MemberTags tag =
				memberTagsMapper.selectOne(
						new LambdaQueryWrapper<MemberTags>()
								.eq(MemberTags::getCompanyId, companyId)
								.eq(MemberTags::getTagId, tagId)
								.last("LIMIT 1"));
		if (tag == null || !StringUtils.hasText(tag.getTagName())) {
			return "";
		}
		return tag.getTagName();
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int intVal(Object v, int def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
