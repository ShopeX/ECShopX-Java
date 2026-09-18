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

package cn.shopex.ecshopx.promotions.service.give;

import cn.shopex.ecshopx.kaquan.service.discount.UserDiscountReceiveCardService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.promotions.domain.CouponGiveErrorLog;
import cn.shopex.ecshopx.promotions.domain.CouponGiveLog;
import cn.shopex.ecshopx.promotions.mapper.CouponGiveErrorLogMapper;
import cn.shopex.ecshopx.promotions.mapper.CouponGiveLogMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PromotionActivityGiveAsyncService {

	private static final Logger log = LoggerFactory.getLogger(PromotionActivityGiveAsyncService.class);

	private final CouponGiveLogMapper couponGiveLogMapper;
	private final CouponGiveErrorLogMapper couponGiveErrorLogMapper;
	private final UserDiscountReceiveCardService userDiscountReceiveCardService;
	private final MemberAccountService memberAccountService;

	public PromotionActivityGiveAsyncService(
			CouponGiveLogMapper couponGiveLogMapper,
			CouponGiveErrorLogMapper couponGiveErrorLogMapper,
			UserDiscountReceiveCardService userDiscountReceiveCardService,
			MemberAccountService memberAccountService) {
		this.couponGiveLogMapper = couponGiveLogMapper;
		this.couponGiveErrorLogMapper = couponGiveErrorLogMapper;
		this.userDiscountReceiveCardService = userDiscountReceiveCardService;
		this.memberAccountService = memberAccountService;
	}

	public void executeScheduleGive(
			long companyId,
			long distributorId,
			String sender,
			List<Long> userIds,
			List<Long> couponCardIds,
			String sourceFrom,
			long triggerTimeEpochSec) {
		int nowSec = (int) Math.min(triggerTimeEpochSec, (long) Integer.MAX_VALUE);

		CouponGiveLog logRow = new CouponGiveLog();
		logRow.setCompanyId(companyId);
		logRow.setDistributorId(distributorId);
		logRow.setSender(sender);
		logRow.setNumber((long) userIds.size() * (long) couponCardIds.size());
		logRow.setError(0L);
		logRow.setCreated(nowSec);
		logRow.setUpdated(nowSec);
		couponGiveLogMapper.insert(logRow);
		long giveId = logRow.getGiveLogId();

		long errorNum = 0L;
		for (Long userId : userIds) {
			for (Long cardId : couponCardIds) {
				try {
					String mobile =
							java.util.Objects.toString(
											memberAccountService.getMemberInfo(userId, companyId).getOrDefault("mobile", ""),
											"")
									.trim();
					userDiscountReceiveCardService.receiveCard(
							companyId, userId, mobile, cardId, 0L, "", sourceFrom);
				} catch (Exception e) {
					errorNum++;
					CouponGiveErrorLog err = new CouponGiveErrorLog();
					err.setGiveId(giveId);
					err.setUid(userId);
					err.setCompanyId(companyId);
					err.setCardId(cardId);
					String note = e.getMessage() == null ? "" : e.getMessage();
					err.setNote(truncate(note, 500));
					err.setCreated(nowSec);
					err.setUpdated(nowSec);
					couponGiveErrorLogMapper.insert(err);
					log.debug("give card failed giveId={} uid={} cardId={}", giveId, userId, cardId, e);
				}
			}
		}

		LambdaUpdateWrapper<CouponGiveLog> uw = new LambdaUpdateWrapper<>();
		uw.eq(CouponGiveLog::getGiveLogId, giveId)
				.set(CouponGiveLog::getError, errorNum)
				.set(CouponGiveLog::getUpdated, nowSec);
		couponGiveLogMapper.update(null, uw);
	}

	private static String truncate(String s, int max) {
		if (s.length() <= max) {
			return s;
		}
		return s.substring(0, max);
	}
}
