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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.DiscountCards;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountCardAggRow;
import cn.shopex.ecshopx.kaquan.mapper.DiscountCardsMapper;
import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.salesperson.service.SalespersonTaskCouponCompleteService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserDiscountCardPackageGrantService {

	private static final DateTimeFormatter LOCAL_DATE_TIME =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	private final UserDiscountMapper userDiscountMapper;
	private final DiscountCardsMapper discountCardsMapper;
	private final RelItemsMapper relItemsMapper;
	private final MemberAccountService memberAccountService;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final SalespersonTaskCouponCompleteService salespersonTaskCouponCompleteService;
	private final DiscountCardsRowMapperService discountCardsRowMapperService;
	private final ObjectMapper objectMapper;
	private final SecureRandom secureRandom = new SecureRandom();

	public UserDiscountCardPackageGrantService(UserDiscountMapper userDiscountMapper,
			DiscountCardsMapper discountCardsMapper,
			RelItemsMapper relItemsMapper,
			MemberAccountService memberAccountService,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			SalespersonTaskCouponCompleteService salespersonTaskCouponCompleteService,
			DiscountCardsRowMapperService discountCardsRowMapperService,
			ObjectMapper objectMapper) {
		this.userDiscountMapper = userDiscountMapper;
		this.discountCardsMapper = discountCardsMapper;
		this.relItemsMapper = relItemsMapper;
		this.memberAccountService = memberAccountService;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.salespersonTaskCouponCompleteService = salespersonTaskCouponCompleteService;
		this.discountCardsRowMapperService = discountCardsRowMapperService;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> checkCardList(long companyId, long userId, List<Map<String, Object>> cardList,
			String from) {
		if (cardList == null || cardList.isEmpty()) {
			return List.of();
		}
		List<Long> cardIdList = new ArrayList<>();
		for (Map<String, Object> c : cardList) {
			long cid = longFrom(c.get("card_id"));
			if (cid > 0L) {
				cardIdList.add(cid);
			}
		}
		Map<Long, Integer> discountNumIndex = toNumMap(userDiscountMapper.countIssuedGroupByCardId(companyId, cardIdList));
		Map<Long, Integer> discountUserNumIndex =
				toNumMap(userDiscountMapper.countIssuedGroupByCardIdForUser(companyId, userId, cardIdList));

		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		Map<String, Object> vipGrade = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);

		List<Map<String, Object>> checkResult = new ArrayList<>();
		long nowTime = System.currentTimeMillis() / 1000L;
		for (Map<String, Object> item : cardList) {
			int giveNum = intFrom(item.get("give_num"), 1);
			int i = 0;
			while (i < giveNum) {
				Map<String, Object> one = new LinkedHashMap<>(item);
				long endEpoch = parseEndDateToEpoch(one.get("end_date"), nowTime);
				if (endEpoch > 0L && endEpoch <= nowTime) {
					checkResult.add(fail(one, KaquanDiscountCardMessages.RECEIVE_COUPON_EXPIRED));
					i++;
					continue;
				}

				long cardId = longFrom(one.get("card_id"));
				int usedCardNumber = discountNumIndex.getOrDefault(cardId, 0);
				int quantity = intFrom(one.get("quantity"), 0);
				if (quantity <= usedCardNumber) {
					checkResult.add(fail(one, KaquanDiscountCardMessages.RECEIVE_COUPON_OUT_OF_STOCK));
					i++;
					continue;
				} else {
					discountNumIndex.put(cardId, usedCardNumber + 1);
				}

				if ("template".equals(from)) {
					String recv = String.valueOf(one.getOrDefault("receive", ""));
					if (!"1".equals(recv) && !"true".equalsIgnoreCase(recv)) {
						checkResult.add(fail(one, KaquanDiscountCardMessages.COUPON_NOT_FRONTEND_RECEIVABLE));
						i++;
						continue;
					}
				}

				int kq = intFrom(one.get("kq_status"), -1);
				if (kq != DiscountNewGiftCardUpdateService.STATUS_NORMAL) {
					checkResult.add(fail(one, KaquanDiscountCardMessages.COUPON_STATUS_ABNORMAL));
					i++;
					continue;
				}

				Object gradeIdsRaw = one.get("grade_ids");
				if (hasIdRestriction(gradeIdsRaw)) {
					List<String> gradeIds = splitIds(gradeIdsRaw);
					String memberGrade = String.valueOf(memberInfo.getOrDefault("grade_id", ""));
					if (!gradeIds.contains(memberGrade)) {
						checkResult.add(fail(one, KaquanDiscountCardMessages.MEMBER_GRADE_NOT_MATCH));
						i++;
						continue;
					}
				}

				Object vipGradeIdsRaw = one.get("vip_grade_ids");
				if (hasIdRestriction(vipGradeIdsRaw)) {
					if (!Boolean.TRUE.equals(vipGrade.get("is_open"))) {
						checkResult.add(fail(one, KaquanDiscountCardMessages.VIP_GRADE_NOT_MATCH_NOT_OPEN));
						i++;
						continue;
					}
					List<String> vipGradeIds = splitIds(vipGradeIdsRaw);
					String vg = String.valueOf(vipGrade.getOrDefault("vip_grade_id", ""));
					if (!vipGradeIds.contains(vg)) {
						checkResult.add(fail(one, KaquanDiscountCardMessages.VIP_GRADE_NOT_MATCH));
						i++;
						continue;
					}
				}

				int userGetNum = discountUserNumIndex.getOrDefault(cardId, 0);
				int getLimit = intFrom(one.get("get_limit"), 0);
				if (userGetNum >= getLimit) {
					checkResult.add(fail(one, KaquanDiscountCardMessages.USER_EXCEED_COUPON_LIMIT));
					i++;
					continue;
				} else {
					discountUserNumIndex.put(cardId, userGetNum + 1);
				}

				checkResult.add(ok(one));
				i++;
			}
		}
		return checkResult;
	}

	public List<Map<String, Object>> userGetCardList(long companyId, long userId,
			List<Map<String, Object>> cardList, String from, long salespersonId) {
		Map<String, String> sourceFromZh = Map.of(
				"vip_grade", "会员等级领优惠券包",
				"grade", "等级领优惠券包",
				"template", "模版领优惠券包");
		String sourceFromZhVal = sourceFromZh.getOrDefault(from, "其它");
		List<Map<String, Object>> checkCardList = checkCardList(companyId, userId, cardList, from);
		List<Map<String, Object>> result = new ArrayList<>();
		List<Map<String, Object>> cards = new ArrayList<>();
		for (Map<String, Object> item : checkCardList) {
			if (Boolean.TRUE.equals(item.get("success"))) {
				@SuppressWarnings("unchecked")
				Map<String, Object> cardInfo = (Map<String, Object>) item.get("card_info");
				cards.add(cardInfo);
			} else {
				@SuppressWarnings("unchecked")
				Map<String, Object> cardInfoTemp = (Map<String, Object>) item.get("card_info");
				result.add(rowResult(cardInfoTemp, false, str(item.get("message")), userId, companyId));
			}
		}
		for (Map<String, Object> cardInfo : cards) {
			DiscountCards entity = loadCard(companyId, longFrom(cardInfo.get("card_id")));
			Map<String, Object> handled = applyHandler(entity, cardInfo);
			String code = allocUniqueCode(companyId);
			insertUserDiscount(companyId, userId, salespersonId, sourceFromZhVal, code, handled);
			salespersonTaskCouponCompleteService.completeGetCoupon(companyId, salespersonId, userId, "coupons_user",
					longFrom(cardInfo.get("card_id")));
			result.add(rowResult(cardInfo, true, "", userId, companyId));
		}
		return result;
	}

	private static Map<String, Object> rowResult(Map<String, Object> cardInfo, boolean success, String message,
			long userId, long companyId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", cardInfo.get("id"));
		m.put("receive_id", cardInfo.get("receive_id"));
		m.put("success", success);
		m.put("message", message == null ? "" : message);
		m.put("card_id", longFrom(cardInfo.get("card_id")));
		m.put("user_id", userId);
		m.put("company_id", companyId);
		return m;
	}

	private void insertUserDiscount(long companyId, long userId, long salespersonId, String sourceType, String code,
			Map<String, Object> cardInfo) {
		UserDiscount existing = userDiscountMapper.selectOne(new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getCode, code)
				.eq(UserDiscount::getCardId, longFrom(cardInfo.get("card_id")))
				.last("LIMIT 1"));
		if (existing != null) {
			if (existing.getUserId() != null && existing.getUserId() == userId) {
				throw new ResourceException(KaquanDiscountCardMessages.ALREADY_RECEIVED_COUPON);
			}
			throw new ResourceException(KaquanDiscountCardMessages.FAILED_TO_RECEIVE_COUPON);
		}
		UserDiscount row = new UserDiscount();
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setCardId(longFrom(cardInfo.get("card_id")));
		row.setCode(code);
		row.setStatus(1);
		row.setSourceType(sourceType);
		row.setUseScenes(str(cardInfo.get("use_scenes")));
		row.setTitle(str(cardInfo.get("title")));
		row.setColor(str(cardInfo.get("color")));
		row.setDiscount(intFrom(cardInfo.get("discount"), 0));
		row.setCardType(str(cardInfo.get("card_type")));
		row.setLeastCost(intFrom(cardInfo.get("least_cost"), 0));
		row.setReduceCost(intFrom(cardInfo.get("reduce_cost"), 0));
		row.setUseCondition(str(cardInfo.get("use_condition")));
		row.setRelShopsIds(str(cardInfo.get("rel_shops_ids")));
		row.setUseBound(intFrom(cardInfo.get("use_bound"), 0));
		row.setRelItemIds(str(cardInfo.get("rel_item_ids")));
		row.setRelDistributorIds(str(cardInfo.get("distributor_id")));
		row.setBeginDate(intFrom(cardInfo.get("begin_date"), 0));
		row.setEndDate(intFrom(cardInfo.get("end_date"), 0));
		row.setUsePlatform(strOrDefault(cardInfo.get("use_platform"), "store"));
		row.setMostCost(intFrom(cardInfo.get("most_cost"), 99999900));
		row.setGetDate((int) Math.min(System.currentTimeMillis() / 1000L, Integer.MAX_VALUE));
		row.setSalespersonId(salespersonId);
		row.setApplyScope(str(cardInfo.get("apply_scope")));
		userDiscountMapper.insert(row);
	}

	private Map<String, Object> applyHandler(DiscountCards entity, Map<String, Object> overlay) {
		Map<String, Object> cardInfo = new LinkedHashMap<>(discountCardsRowMapperService.toSnakeCaseMap(entity));
		cardInfo.putAll(overlay);
		int discount = intFrom(cardInfo.get("discount"), 0);
		int least = intFrom(cardInfo.get("least_cost"), 0);
		int reduce = intFrom(cardInfo.get("reduce_cost"), 0);
		int most = intFrom(cardInfo.get("most_cost"), 0);
		if (most <= 0) {
			most = 99999900;
		}
		cardInfo.put("discount", discount);
		cardInfo.put("least_cost", least);
		cardInfo.put("reduce_cost", reduce);
		cardInfo.put("most_cost", most);
		Map<String, Object> useCondition = new LinkedHashMap<>();
		useCondition.put("accept_category", cardInfo.get("accept_category"));
		useCondition.put("reject_category", cardInfo.get("reject_category"));
		useCondition.put("least_cost", least);
		useCondition.put("object_use_for", cardInfo.get("object_use_for"));
		useCondition.put("can_use_with_other_discount", cardInfo.get("can_use_with_other_discount"));
		try {
			cardInfo.put("use_condition", objectMapper.writeValueAsString(useCondition));
		} catch (Exception e) {
			cardInfo.put("use_condition", "{}");
		}
		Object relShops = cardInfo.get("rel_shops_ids");
		if (relShops instanceof List<?> list && !list.isEmpty()) {
			cardInfo.put("rel_shops_ids", "," + list.stream().map(String::valueOf).collect(Collectors.joining(",")) + ",");
		} else if (relShops == null || !StringUtils.hasText(String.valueOf(relShops))) {
			cardInfo.put("rel_shops_ids", "all");
		} else {
			String rs = String.valueOf(relShops);
			if (!rs.startsWith(",")) {
				cardInfo.put("rel_shops_ids", "," + rs.trim().replaceAll("^,+|,+$/", "") + ",");
			}
		}
		Object dist = cardInfo.get("distributor_id");
		if (dist instanceof List<?> dlist && !dlist.isEmpty()) {
			cardInfo.put("distributor_id", "," + dlist.stream().map(String::valueOf).collect(Collectors.joining(",")) + ",");
		} else if (dist == null || !StringUtils.hasText(String.valueOf(dist))) {
			cardInfo.put("distributor_id", "all");
		} else {
			String d = String.valueOf(dist);
			if (!d.startsWith(",")) {
				cardInfo.put("distributor_id", "," + d.trim().replaceAll("^,+|,+$/", "") + ",");
			}
		}
		DiscountCardUserReceiveFieldNormalizer.fillRelScopeFromKaquanRelItems(cardInfo, entity, relItemsMapper);
		DiscountCardUserReceiveFieldNormalizer.applyUseBoundRelItemIds(cardInfo);
		if ("new_gift".equals(String.valueOf(cardInfo.get("card_type")))) {
			cardInfo.put("distributor_id", "");
			cardInfo.put("rel_item_ids", "");
		}
		cardInfo.put("begin_date", intFrom(cardInfo.get("begin_time"), intFrom(cardInfo.get("begin_date"), 0)));
		cardInfo.put("end_date", intFrom(cardInfo.get("end_time"), intFrom(cardInfo.get("end_date"), 0)));
		return cardInfo;
	}

	private DiscountCards loadCard(long companyId, long cardId) {
		DiscountCards c = discountCardsMapper.selectOne(new LambdaQueryWrapper<DiscountCards>()
				.eq(DiscountCards::getCompanyId, companyId)
				.eq(DiscountCards::getCardId, cardId)
				.last("LIMIT 1"));
		if (c == null) {
			throw new ResourceException(KaquanDiscountCardMessages.COUPON_INVALID);
		}
		return c;
	}

	private String allocUniqueCode(long companyId) {
		for (int attempt = 0; attempt < 20; attempt++) {
			String code = randomCode(12);
			long cnt = userDiscountMapper.selectCount(new LambdaQueryWrapper<UserDiscount>()
					.eq(UserDiscount::getCompanyId, companyId)
					.eq(UserDiscount::getCode, code));
			if (cnt == 0L) {
				return code;
			}
		}
		throw new ResourceException(KaquanDiscountCardMessages.DATA_CREATE_FAILED_RETRY);
	}

	private String randomCode(int len) {
		byte[] buf = new byte[len];
		secureRandom.nextBytes(buf);
		char[] out = new char[len];
		for (int i = 0; i < len; i++) {
			int idx = (buf[i] & 0xff) % CHARSET.length();
			out[i] = CHARSET.charAt(idx);
		}
		return new String(out);
	}

	private static Map<String, Object> fail(Map<String, Object> cardInfo, String message) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("card_info", cardInfo);
		m.put("message", message);
		m.put("success", false);
		return m;
	}

	private static Map<String, Object> ok(Map<String, Object> cardInfo) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("card_info", cardInfo);
		m.put("message", "");
		m.put("success", true);
		return m;
	}

	private static Map<Long, Integer> toNumMap(List<UserDiscountCardAggRow> rows) {
		Map<Long, Integer> m = new HashMap<>();
		if (rows == null) {
			return m;
		}
		for (UserDiscountCardAggRow r : rows) {
			if (r.getCardId() != null && r.getNum() != null) {
				m.put(r.getCardId(), r.getNum().intValue());
			}
		}
		return m;
	}

	/** 空列表 / "[]" / 空白视为不限制；禁止把 List 打成 "[1]" 再和会员等级比较。 */
	static boolean hasIdRestriction(Object raw) {
		if (raw == null) {
			return false;
		}
		if (raw instanceof List<?> list) {
			return !list.isEmpty();
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s) || "[]".equals(s) || "null".equalsIgnoreCase(s)) {
			return false;
		}
		return true;
	}

	static List<String> splitIds(Object raw) {
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				String t = String.valueOf(o).trim();
				if (StringUtils.hasText(t) && !"[".equals(t) && !"]".equals(t)) {
					out.add(t);
				}
			}
			return out;
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty() || "[]".equals(s)) {
			return List.of();
		}
		s = s.replaceAll("^,+|,+$", "");
		String[] parts = s.split(",");
		List<String> out = new ArrayList<>();
		for (String p : parts) {
			String t = p.trim();
			if (!t.isEmpty()) {
				out.add(t);
			}
		}
		return out;
	}

	private static long parseEndDateToEpoch(Object endRaw, long nowFallback) {
		if (endRaw == null) {
			return 0L;
		}
		if (endRaw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(endRaw).trim();
		if (s.contains("-")) {
			try {
				return java.time.LocalDateTime.parse(s, LOCAL_DATE_TIME)
						.atZone(ZoneId.systemDefault())
						.toEpochSecond();
			} catch (Exception e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long longFrom(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intFrom(Object raw, int def) {
		if (raw == null) {
			return def;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static String strOrDefault(Object o, String d) {
		String s = str(o);
		return s.isEmpty() ? d : s;
	}
}
