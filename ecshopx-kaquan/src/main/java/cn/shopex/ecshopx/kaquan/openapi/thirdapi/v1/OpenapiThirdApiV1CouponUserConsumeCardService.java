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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.kaquan.domain.UserDiscount;
import cn.shopex.ecshopx.kaquan.domain.UserDiscountLogs;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountLogsMapper;
import cn.shopex.ecshopx.kaquan.mapper.UserDiscountMapper;
import cn.shopex.ecshopx.kaquan.service.discount.DiscountCardKaquanDetailForConsumeService;
import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountUserRecordDetailAssembler;
import cn.shopex.ecshopx.kaquan.service.discount.WxShopsListForUserDiscountService;
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
public class OpenapiThirdApiV1CouponUserConsumeCardService {

	private static final String CONSUME_OUTER_STR_THIRD_PARTY = "第三方核销";
	private static final String DEFAULT_SHOP_NAME = "微商城";
	private static final String MSG_USED_OR_INVALID = "优惠券使用失败，该优惠券已被使用或无效";
	private static final String MSG_NOT_YET_VALID = "优惠券使用失败，该优惠券未到使用日期";
	private static final String MSG_EXPIRED = "优惠券使用失败，该优惠券已过期";

	private final UserDiscountMapper userDiscountMapper;
	private final UserDiscountLogsMapper userDiscountLogsMapper;
	private final UserDiscountUserRecordDetailAssembler userDiscountUserRecordDetailAssembler;
	private final WxShopsListForUserDiscountService wxShopsListForUserDiscountService;
	private final DiscountCardKaquanDetailForConsumeService discountCardKaquanDetailForConsumeService;
	private final SalespersonCouponStatisticsPayCompleteService salespersonCouponStatisticsPayCompleteService;
	private final MemberAccountService memberAccountService;
	private final WxaTemplateMsgCouponNotifyService wxaTemplateMsgCouponNotifyService;

	public OpenapiThirdApiV1CouponUserConsumeCardService(
			UserDiscountMapper userDiscountMapper,
			UserDiscountLogsMapper userDiscountLogsMapper,
			UserDiscountUserRecordDetailAssembler userDiscountUserRecordDetailAssembler,
			WxShopsListForUserDiscountService wxShopsListForUserDiscountService,
			DiscountCardKaquanDetailForConsumeService discountCardKaquanDetailForConsumeService,
			SalespersonCouponStatisticsPayCompleteService salespersonCouponStatisticsPayCompleteService,
			MemberAccountService memberAccountService,
			WxaTemplateMsgCouponNotifyService wxaTemplateMsgCouponNotifyService) {
		this.userDiscountMapper = userDiscountMapper;
		this.userDiscountLogsMapper = userDiscountLogsMapper;
		this.userDiscountUserRecordDetailAssembler = userDiscountUserRecordDetailAssembler;
		this.wxShopsListForUserDiscountService = wxShopsListForUserDiscountService;
		this.discountCardKaquanDetailForConsumeService = discountCardKaquanDetailForConsumeService;
		this.salespersonCouponStatisticsPayCompleteService = salespersonCouponStatisticsPayCompleteService;
		this.memberAccountService = memberAccountService;
		this.wxaTemplateMsgCouponNotifyService = wxaTemplateMsgCouponNotifyService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> executeOpenapiUserConsumeCard(
			long companyId, String couponCode, String couponStatus) {
		int targetStatus = "1".equals(couponStatus) ? 2 : 6;

		UserDiscount row = userDiscountMapper.selectOne(
				new LambdaQueryWrapper<UserDiscount>()
						.eq(UserDiscount::getCompanyId, companyId)
						.eq(UserDiscount::getCode, couponCode)
						.last("LIMIT 1"));
		if (row == null || row.getStatus() == null) {
			throw new ResourceException(MSG_USED_OR_INVALID);
		}

		Map<String, Object> detail = userDiscountUserRecordDetailAssembler.toDetailMap(row);
		Object relShopsForQuery = detail.get("rel_shops_ids");
		Map<String, Object> shopList = wxShopsListForUserDiscountService.listShopsPoi(companyId, relShopsForQuery);
		detail.put("shop_list", shopList);

		long cardId = row.getCardId() != null ? row.getCardId() : 0L;
		if (cardId <= 0L) {
			throw new ResourceException(MSG_USED_OR_INVALID);
		}
		Map<String, Object> cardInfo = discountCardKaquanDetailForConsumeService.loadDetail(companyId, cardId);
		detail.put("card_info", cardInfo);

		assertOpenApiConsumable(row);

		Long userDiscountPk = row.getId();
		long spId = row.getSalespersonId() != null ? row.getSalespersonId() : 0L;
		salespersonCouponStatisticsPayCompleteService.tryPayIncrement(companyId, spId, userDiscountPk);

		Map<String, Object> postdata = new LinkedHashMap<>();
		postdata.put("consume_outer_str", CONSUME_OUTER_STR_THIRD_PARTY);
		postdata.put("consume_source", stringVal(cardInfo.get("use_scenes")));
		postdata.put("status", targetStatus);

		Map<String, Object> result = openApiUserConsumeCardUpdate(companyId, couponCode, postdata);

		if (Boolean.TRUE.equals(result.get("status"))) {
			long userId = row.getUserId() != null ? row.getUserId() : 0L;
			writeConsumeLog(companyId, userId, row, postdata);
		}

		long userId = row.getUserId() != null ? row.getUserId() : 0L;
		sendTemplateWithTargetStatus(companyId, userId, row, cardInfo, targetStatus);
		return result;
	}

	private void assertOpenApiConsumable(UserDiscount row) {
		Integer st = row.getStatus();
		if (st == null || (st != 1 && st != 4)) {
			throw new ResourceException(MSG_USED_OR_INVALID);
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		if (row.getBeginDate() != null && row.getBeginDate() > now) {
			throw new ResourceException(MSG_NOT_YET_VALID);
		}
		if (row.getEndDate() != null && row.getEndDate() <= now) {
			throw new ResourceException(MSG_EXPIRED);
		}
	}

	private Map<String, Object> openApiUserConsumeCardUpdate(
			long companyId, String code, Map<String, Object> postdata) {
		UserDiscount one = userDiscountMapper.selectOne(
				new LambdaQueryWrapper<UserDiscount>()
						.eq(UserDiscount::getCompanyId, companyId)
						.eq(UserDiscount::getCode, code)
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
		userDiscountMapper.update(null, uw);
		return Map.of("status", true);
	}

	private void writeConsumeLog(long companyId, long userId, UserDiscount row, Map<String, Object> postdata) {
		Map<String, Object> memberInfo = userId > 0L
				? memberAccountService.getMemberInfo(userId, companyId)
				: Map.of();
		String logMobile = stringVal(memberInfo.get("mobile"));
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
		logRow.setShopName(DEFAULT_SHOP_NAME);
		logRow.setUsedTime(now);
		logRow.setUsedStatus("consume");
		logRow.setUsedOrder("");
		userDiscountLogsMapper.insert(logRow);
	}

	private void sendTemplateWithTargetStatus(
			long companyId, long userId, UserDiscount row,
			Map<String, Object> cardInfo, int targetStatus) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("card_type", row.getCardType());
		payload.put("least_cost", row.getLeastCost());
		payload.put("reduce_cost", row.getReduceCost());
		payload.put("discount", row.getDiscount());
		payload.put("title", row.getTitle());
		payload.put("begin_date", row.getBeginDate());
		payload.put("end_date", row.getEndDate());
		payload.put("status", targetStatus);
		if (cardInfo != null) {
			for (String k : List.of("least_cost", "reduce_cost", "discount", "gift", "title")) {
				if (!payload.containsKey(k) || payload.get(k) == null) {
					payload.put(k, cardInfo.get(k));
				}
			}
		}
		wxaTemplateMsgCouponNotifyService.sendCardUsed(companyId, userId, payload);
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

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o).trim();
	}
}
