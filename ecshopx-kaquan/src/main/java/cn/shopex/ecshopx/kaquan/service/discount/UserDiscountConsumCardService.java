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
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountLogs;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountLogsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.salesperson.service.SalespersonCouponStatisticsPayCompleteService;
import cn.shopex.ecshopx.wechat.service.WxaTemplateMsgCouponNotifyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserDiscountConsumCardService {

	private static final String OUTER_PAY_BILL = "买单核销";
	private static final String DEFAULT_SHOP_NAME = "微商城";

	private final UserDiscountMapper userDiscountMapper;
	private final UserDiscountLogsMapper userDiscountLogsMapper;
	private final WxShopsListForUserDiscountService wxShopsListForUserDiscountService;
	private final DiscountCardKaquanDetailForConsumeService discountCardKaquanDetailForConsumeService;
	private final SalespersonCouponStatisticsPayCompleteService salespersonCouponStatisticsPayCompleteService;
	private final MemberAccountService memberAccountService;
	private final WxaTemplateMsgCouponNotifyService wxaTemplateMsgCouponNotifyService;
	private final UserDiscountUserRecordDetailAssembler userDiscountUserRecordDetailAssembler;

	public UserDiscountConsumCardService(
			UserDiscountMapper userDiscountMapper,
			UserDiscountLogsMapper userDiscountLogsMapper,
			WxShopsListForUserDiscountService wxShopsListForUserDiscountService,
			DiscountCardKaquanDetailForConsumeService discountCardKaquanDetailForConsumeService,
			SalespersonCouponStatisticsPayCompleteService salespersonCouponStatisticsPayCompleteService,
			MemberAccountService memberAccountService,
			WxaTemplateMsgCouponNotifyService wxaTemplateMsgCouponNotifyService,
			UserDiscountUserRecordDetailAssembler userDiscountUserRecordDetailAssembler) {
		this.userDiscountMapper = userDiscountMapper;
		this.userDiscountLogsMapper = userDiscountLogsMapper;
		this.wxShopsListForUserDiscountService = wxShopsListForUserDiscountService;
		this.discountCardKaquanDetailForConsumeService = discountCardKaquanDetailForConsumeService;
		this.salespersonCouponStatisticsPayCompleteService = salespersonCouponStatisticsPayCompleteService;
		this.memberAccountService = memberAccountService;
		this.wxaTemplateMsgCouponNotifyService = wxaTemplateMsgCouponNotifyService;
		this.userDiscountUserRecordDetailAssembler = userDiscountUserRecordDetailAssembler;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> consumePayBillCard(long companyId, long userId, String mobile, String code) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("consume_outer_str", OUTER_PAY_BILL);
		params.put("user_id", userId);

		LambdaQueryWrapper<UserDiscount> cardFilter = new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getCompanyId, companyId)
				.eq(UserDiscount::getCode, code)
				.eq(UserDiscount::getUserId, userId)
				.last("LIMIT 1");
		UserDiscount row = userDiscountMapper.selectOne(cardFilter);
		if (row == null || row.getStatus() == null) {
			throw new ResourceException("优惠券使用失败，该优惠券已被使用或无效");
		}

		Map<String, Object> detail = userDiscountUserRecordDetailAssembler.toDetailMap(row);
		Object relShopsForQuery = detail.get("rel_shops_ids");
		Map<String, Object> shopList = wxShopsListForUserDiscountService.listShopsPoi(companyId, relShopsForQuery);
		detail.put("shop_list", shopList);

		long cardId = row.getCardId() != null ? row.getCardId() : 0L;
		if (cardId <= 0L) {
			throw new ResourceException("该优惠券已失效");
		}
		Map<String, Object> cardInfo = discountCardKaquanDetailForConsumeService.loadDetail(companyId, cardId);
		detail.put("card_info", cardInfo);

		assertUserCardConsumable(row, detail, params, cardInfo, shopList, false);

		Long userDiscountPk = row.getId();
		long spId = row.getSalespersonId() != null ? row.getSalespersonId() : 0L;
		salespersonCouponStatisticsPayCompleteService.tryPayIncrement(companyId, spId, userDiscountPk);

		Map<String, Object> postdata = buildPostdata(params, cardInfo, shopList);
		Map<String, Object> result = userConsumeCardUpdate(companyId, code, userId, postdata);

		if (Boolean.TRUE.equals(result.get("status"))) {
			writeLog(companyId, userId, row, mobile, postdata);
			sendTemplateSafe(companyId, userId, row, cardInfo);
		}
		return result;
	}

	private static final String OUTER_SELF_CONSUME = "用户自助核销";

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> userUsedCardSelfConsume(
			long companyId,
			long userId,
			String code,
			String shopId,
			String verifyCode,
			String remarkAmount,
			String consumeOuterStrOverride) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put(
				"consume_outer_str",
				StringUtils.hasText(consumeOuterStrOverride) ? consumeOuterStrOverride.trim() : OUTER_SELF_CONSUME);
		if (StringUtils.hasText(verifyCode)) {
			params.put("verify_code", verifyCode.trim());
		}
		if (StringUtils.hasText(remarkAmount)) {
			params.put("remark_amount", remarkAmount.trim());
		}
		params.put("shop_id", shopId);
		params.put("user_id", userId);

		LambdaQueryWrapper<UserDiscount> cardFilter = new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getCompanyId, companyId)
				.eq(UserDiscount::getCode, code)
				.eq(UserDiscount::getUserId, userId)
				.last("LIMIT 1");
		UserDiscount row = userDiscountMapper.selectOne(cardFilter);
		if (row == null || row.getStatus() == null) {
			throw new ResourceException("优惠券使用失败，该优惠券已被使用或无效");
		}

		Map<String, Object> detail = userDiscountUserRecordDetailAssembler.toDetailMap(row);
		Object relShopsForQuery = detail.get("rel_shops_ids");
		Map<String, Object> shopList = wxShopsListForUserDiscountService.listShopsPoi(companyId, relShopsForQuery);
		detail.put("shop_list", shopList);

		long cardId = row.getCardId() != null ? row.getCardId() : 0L;
		if (cardId <= 0L) {
			throw new ResourceException("该优惠券已失效");
		}
		Map<String, Object> cardInfo = discountCardKaquanDetailForConsumeService.loadDetail(companyId, cardId);
		detail.put("card_info", cardInfo);

		assertUserCardConsumable(row, detail, params, cardInfo, shopList, true);

		Long userDiscountPk = row.getId();
		long spId = row.getSalespersonId() != null ? row.getSalespersonId() : 0L;
		salespersonCouponStatisticsPayCompleteService.tryPayIncrement(companyId, spId, userDiscountPk);

		Map<String, Object> postdata = buildPostdata(params, cardInfo, shopList);
		Map<String, Object> result = userConsumeCardUpdate(companyId, code, userId, postdata);

		if (Boolean.TRUE.equals(result.get("status"))) {
			writeLog(companyId, userId, row, null, postdata);
		}
		try {
			sendTemplateSafe(companyId, userId, row, cardInfo);
		} catch (RuntimeException ignored) {
			// non-fatal: template delivery must not block consume response
		}
		return result;
	}

	private void assertUserCardConsumable(
			UserDiscount row,
			Map<String, Object> detail,
			Map<String, Object> params,
			Map<String, Object> cardInfo,
			Map<String, Object> shopList,
			boolean useCompositeCompanyStoreLocationLabel) {
		Integer st = row.getStatus();
		if (st == null || (st != 1 && st != 4)) {
			throw new ResourceException("优惠券使用失败，该优惠券已被使用或无效");
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (row.getBeginDate() != null && row.getBeginDate() > now) {
			throw new ResourceException("优惠券使用失败，该优惠券未到使用日期");
		}
		if (row.getEndDate() != null && row.getEndDate() <= now) {
			throw new ResourceException("优惠券使用失败，该优惠券已过期");
		}

		Object itemRaw = params.get("item_id");
		if (hasEffectiveParam(itemRaw)) {
			long itemId = parseLongParam(itemRaw);
			if (itemId > 0L) {
				Object rel = detail.get("rel_item_ids");
				if (!"all".equals(rel)) {
					if (rel instanceof List<?> list && !listContainsItemId(list, itemId)) {
						throw new ResourceException("优惠券使用失败，该优惠券不适用该商品");
					}
				}
			}
		}

		String useScenes = stringVal(cardInfo.get("use_scenes"));
		Object verifyRaw = params.get("verify_code");
		if ("SELF".equals(useScenes) && hasEffectiveParam(verifyRaw)) {
			String expected = stringVal(cardInfo.get("self_consume_code"));
			String actual = verifyRaw == null ? "" : verifyRaw.toString().trim();
			if (!expected.equals(actual)) {
				throw new ResourceException("优惠券使用失败，您的验证码错误");
			}
		}

		Object shopRaw = params.get("shop_id");
		if (hasEffectiveParam(shopRaw)) {
			long shopWxId = parseLongParam(shopRaw);
			if (shopWxId > 0L) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> lst = (List<Map<String, Object>>) shopList.get("list");
				if (lst != null) {
					for (Map<String, Object> entry : lst) {
						Object wx = entry.get("wxShopId");
						long wid = wx instanceof Number n ? n.longValue() : parseLongParam(wx);
						if (wid == shopWxId) {
							String storeName = stringVal(entry.get("storeName"));
							String companyName = stringVal(entry.get("companyName"));
							String locationName;
							if (useCompositeCompanyStoreLocationLabel) {
								if (StringUtils.hasText(companyName) && StringUtils.hasText(storeName)) {
									locationName = companyName + "(" + storeName + ")";
								} else if (StringUtils.hasText(companyName)) {
									locationName = companyName;
								} else {
									locationName = storeName;
								}
							} else {
								locationName = StringUtils.hasText(storeName) ? storeName : companyName;
							}
							params.put("location_name", locationName);
							params.put("location_id", String.valueOf(shopWxId));
							params.remove("shop_id");
							break;
						}
					}
				}
			}
		}
	}

	private static boolean hasEffectiveParam(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof String s) {
			return StringUtils.hasText(s.trim()) && !"0".equals(s.trim());
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		return true;
	}

	private static long parseLongParam(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean listContainsItemId(List<?> list, long itemId) {
		String target = String.valueOf(itemId);
		for (Object o : list) {
			if (o == null) {
				continue;
			}
			if (o instanceof Number n && n.longValue() == itemId) {
				return true;
			}
			if (target.equals(o.toString().trim())) {
				return true;
			}
		}
		return false;
	}

	private Map<String, Object> buildPostdata(Map<String, Object> params, Map<String, Object> cardInfo,
			@SuppressWarnings("unused") Map<String, Object> shopList) {
		Map<String, Object> postdata = new LinkedHashMap<>(params);
		postdata.put("consume_source", stringVal(cardInfo.get("use_scenes")));
		if (!postdata.containsKey("status")) {
			postdata.put("status", 2);
		}
		return postdata;
	}

	private Map<String, Object> userConsumeCardUpdate(long companyId, String code, long userId, Map<String, Object> postdata) {
		UserDiscount one = userDiscountMapper.selectOne(new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getCompanyId, companyId)
				.eq(UserDiscount::getCode, code)
				.eq(UserDiscount::getUserId, userId)
				.last("LIMIT 1"));
		if (one == null) {
			return Map.of("status", false);
		}
		LambdaUpdateWrapper<UserDiscount> uw = new LambdaUpdateWrapper<UserDiscount>()
				.eq(UserDiscount::getId, one.getId())
				.eq(UserDiscount::getCompanyId, companyId);

		if (postdata.containsKey("consume_source")) {
			uw.set(UserDiscount::getConsumeSource, nullToNull(stringVal(postdata.get("consume_source"))));
		}
		if (postdata.containsKey("consume_outer_str")) {
			uw.set(UserDiscount::getConsumeOuterStr, nullToNull(stringVal(postdata.get("consume_outer_str"))));
		}
		if (postdata.containsKey("status")) {
			uw.set(UserDiscount::getStatus, intFrom(postdata.get("status"), 2));
		}
		if (postdata.containsKey("location_name")) {
			uw.set(UserDiscount::getLocationName, nullToNull(stringVal(postdata.get("location_name"))));
		}
		if (postdata.containsKey("location_id")) {
			uw.set(UserDiscount::getLocationId, nullToNull(stringVal(postdata.get("location_id"))));
		}
		if (postdata.containsKey("trans_id")) {
			uw.set(UserDiscount::getTransId, nullToNull(stringVal(postdata.get("trans_id"))));
		}
		if (postdata.containsKey("verify_code")) {
			uw.set(UserDiscount::getVerifyCode, nullToNull(stringVal(postdata.get("verify_code"))));
		}
		if (postdata.containsKey("staff_open_id")) {
			uw.set(UserDiscount::getStaffOpenId, nullToNull(stringVal(postdata.get("staff_open_id"))));
		}
		if (postdata.containsKey("remark_amount")) {
			uw.set(UserDiscount::getRemarkAmount, nullToNull(stringVal(postdata.get("remark_amount"))));
		}
		if (postdata.containsKey("fee")) {
			uw.set(UserDiscount::getFee, nullToNull(stringVal(postdata.get("fee"))));
		}
		if (postdata.containsKey("original_fee")) {
			uw.set(UserDiscount::getOriginalFee, nullToNull(stringVal(postdata.get("original_fee"))));
		}

		userDiscountMapper.update(null, uw);
		return Map.of("status", true);
	}

	private static String nullToNull(String s) {
		return StringUtils.hasText(s) ? s : null;
	}

	private static int intFrom(Object o, int dflt) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return dflt;
		}
	}

	private void writeLog(long companyId, long userId, UserDiscount row, String mobile, Map<String, Object> postdata) {
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		String logMobile = StringUtils.hasText(mobile) ? mobile : stringVal(memberInfo.get("mobile"));
		int now = (int) (System.currentTimeMillis() / 1000L);
		UserDiscountLogs logRow = new UserDiscountLogs();
		logRow.setUserId(userId);
		logRow.setCompanyId(companyId);
		logRow.setMobile(logMobile);
		logRow.setUsername(stringVal(memberInfo.get("username")));
		logRow.setCardId(row.getCardId());
		logRow.setCode(row.getCode());
		logRow.setTitle(row.getTitle());
		logRow.setCardType(row.getCardType());
		String shopNameForLog = StringUtils.hasText(stringVal(postdata.get("location_name")))
				? stringVal(postdata.get("location_name"))
				: DEFAULT_SHOP_NAME;
		logRow.setShopName(shopNameForLog);
		logRow.setUsedTime(now);
		logRow.setUsedStatus("consume");
		String order = postdata.containsKey("trans_id") ? stringVal(postdata.get("trans_id")) : "";
		logRow.setUsedOrder(order);
		userDiscountLogsMapper.insert(logRow);
	}

	private void sendTemplateSafe(long companyId, long userId, UserDiscount row, Map<String, Object> cardInfo) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("card_type", row.getCardType());
		payload.put("least_cost", row.getLeastCost());
		payload.put("reduce_cost", row.getReduceCost());
		payload.put("discount", row.getDiscount());
		payload.put("title", row.getTitle());
		payload.put("begin_date", row.getBeginDate());
		payload.put("end_date", row.getEndDate());
		payload.put("status", 2);
		if (cardInfo != null) {
			for (String k : List.of("least_cost", "reduce_cost", "discount", "gift", "title")) {
				if (!payload.containsKey(k) || payload.get(k) == null) {
					payload.put(k, cardInfo.get(k));
				}
			}
		}
		wxaTemplateMsgCouponNotifyService.sendCardUsed(companyId, userId, payload);
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}

	/**
	 * Order-create path: marks the user's discount card consumed and writes the usage log, matching the mall
	 * {@code userConsumeCard} contract (consume text, trade id, fee snapshot).
	 */
	@Transactional(rollbackFor = Exception.class)
	public void consumeCouponForShopadminOrderCreate(
			long companyId, long userId, String couponCode, String consumeOuterStr, String transId, String fee) {
		if (!StringUtils.hasText(couponCode)) {
			return;
		}
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("consume_outer_str", consumeOuterStr);
		params.put("trans_id", transId);
		params.put("fee", fee);
		params.put("user_id", userId);

		LambdaQueryWrapper<UserDiscount> cardFilter = new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getCompanyId, companyId)
				.eq(UserDiscount::getCode, couponCode)
				.eq(UserDiscount::getUserId, userId)
				.last("LIMIT 1");
		UserDiscount row = userDiscountMapper.selectOne(cardFilter);
		if (row == null || row.getStatus() == null) {
			throw new ResourceException("优惠券使用失败，该优惠券已被使用或无效");
		}
		Map<String, Object> detail = userDiscountUserRecordDetailAssembler.toDetailMap(row);
		Object relShopsForQuery = detail.get("rel_shops_ids");
		Map<String, Object> shopList = wxShopsListForUserDiscountService.listShopsPoi(companyId, relShopsForQuery);
		detail.put("shop_list", shopList);

		long cardId = row.getCardId() != null ? row.getCardId() : 0L;
		if (cardId <= 0L) {
			throw new ResourceException("该优惠券已失效");
		}
		Map<String, Object> cardInfo = discountCardKaquanDetailForConsumeService.loadDetail(companyId, cardId);
		detail.put("card_info", cardInfo);

		assertUserCardConsumable(row, detail, params, cardInfo, shopList, false);

		Long userDiscountPk = row.getId();
		long spId = row.getSalespersonId() != null ? row.getSalespersonId() : 0L;
		salespersonCouponStatisticsPayCompleteService.tryPayIncrement(companyId, spId, userDiscountPk);

		Map<String, Object> postdata = buildPostdata(params, cardInfo, shopList);
		Map<String, Object> result = userConsumeCardUpdate(companyId, couponCode, userId, postdata);
		if (!Boolean.TRUE.equals(result.get("status"))) {
			throw new ResourceException("优惠券使用失败，该优惠券已被使用或无效");
		}
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		String mobile = memberInfo.get("mobile") == null ? "" : String.valueOf(memberInfo.get("mobile"));
		writeLog(companyId, userId, row, mobile, postdata);
		sendTemplateSafe(companyId, userId, row, cardInfo);
	}

	@Transactional(rollbackFor = Exception.class)
	public void consumeCouponForTradeFinishPayBill(
			long companyId, long userId, String couponCode, String transId, String fee, String shopId) {
		if (!StringUtils.hasText(couponCode)) {
			return;
		}
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("consume_outer_str", OUTER_PAY_BILL);
		params.put("trans_id", transId);
		params.put("fee", fee);
		params.put("user_id", userId);
		if (StringUtils.hasText(shopId)) {
			params.put("shop_id", shopId);
		}

		LambdaQueryWrapper<UserDiscount> cardFilter = new LambdaQueryWrapper<UserDiscount>()
				.eq(UserDiscount::getCompanyId, companyId)
				.eq(UserDiscount::getCode, couponCode)
				.eq(UserDiscount::getUserId, userId)
				.last("LIMIT 1");
		UserDiscount row = userDiscountMapper.selectOne(cardFilter);
		if (row == null || row.getStatus() == null) {
			throw new ResourceException("优惠券使用失败，该优惠券已被使用或无效");
		}
		Map<String, Object> detail = userDiscountUserRecordDetailAssembler.toDetailMap(row);
		Object relShopsForQuery = detail.get("rel_shops_ids");
		Map<String, Object> shopList = wxShopsListForUserDiscountService.listShopsPoi(companyId, relShopsForQuery);
		detail.put("shop_list", shopList);

		long cardId = row.getCardId() != null ? row.getCardId() : 0L;
		if (cardId <= 0L) {
			throw new ResourceException("该优惠券已失效");
		}
		Map<String, Object> cardInfo = discountCardKaquanDetailForConsumeService.loadDetail(companyId, cardId);
		detail.put("card_info", cardInfo);

		assertUserCardConsumable(row, detail, params, cardInfo, shopList, false);

		Long userDiscountPk = row.getId();
		long spId = row.getSalespersonId() != null ? row.getSalespersonId() : 0L;
		salespersonCouponStatisticsPayCompleteService.tryPayIncrement(companyId, spId, userDiscountPk);

		Map<String, Object> postdata = buildPostdata(params, cardInfo, shopList);
		Map<String, Object> result = userConsumeCardUpdate(companyId, couponCode, userId, postdata);
		if (!Boolean.TRUE.equals(result.get("status"))) {
			throw new ResourceException("优惠券使用失败，该优惠券已被使用或无效");
		}
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		String mobile = memberInfo.get("mobile") == null ? "" : String.valueOf(memberInfo.get("mobile"));
		writeLog(companyId, userId, row, mobile, postdata);
		sendTemplateSafe(companyId, userId, row, cardInfo);
	}
}
